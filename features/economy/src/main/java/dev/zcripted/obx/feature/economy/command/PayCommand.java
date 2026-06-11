package dev.zcripted.obx.feature.economy.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PayCommand extends AbstractObxCommand implements TabCompleter {

    /** A large payment awaiting {@code /pay confirm} (anti fat-finger guard). */
    private static final class PendingPay {
        final java.util.UUID targetUuid;
        final String targetName;
        final double amount;
        final long expires;

        PendingPay(java.util.UUID targetUuid, String targetName, double amount, long expires) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.amount = amount;
            this.expires = expires;
        }
    }

    private static final long CONFIRM_WINDOW_MS = 30_000L;

    private final EconomyService economy;
    private final Map<java.util.UUID, PendingPay> pendingConfirms = new java.util.concurrent.ConcurrentHashMap<>();

    public PayCommand(ObxPlugin plugin) {
        super(plugin);
        this.economy = plugin.getEconomyService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player payer = (Player) sender;
        if (!payer.hasPermission("obx.pay")) {
            languages.send(payer, "core.no-permission");
            return true;
        }
        // /pay confirm — execute the pending large payment (if still valid).
        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            PendingPay pending = pendingConfirms.remove(payer.getUniqueId());
            if (pending == null || pending.expires < System.currentTimeMillis()) {
                languages.send(payer, "economy.pay.confirm.expired");
                return true;
            }
            performPay(payer, pending.targetUuid, pending.targetName, pending.amount);
            return true;
        }
        if (args.length < 2) {
            languages.send(payer, "economy.pay.usage");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String targetName = target.getName();
        // Reject anyone we can't resolve to a real, known account: a null name means
        // Mojang/profile lookup failed, and a player who has never joined has no balance
        // to credit. Capturing targetName once also avoids an NPE further down where the
        // name is used as a label/placeholder.
        if (targetName == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            // Couldn't resolve a real account. Offer a clickable box of similar/known
            // names (each suggests "/pay <name> ") when any look close; otherwise just
            // report the unknown player as before.
            if (!suggestSimilarNames(payer, args[0])) {
                languages.send(payer, "economy.unknown-player", Placeholders.with("player", args[0]));
            }
            return true;
        }
        if (target.getUniqueId().equals(payer.getUniqueId())) {
            languages.send(payer, "economy.pay.self");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ignored) {
            languages.send(payer, "economy.invalid-amount", Placeholders.with("input", args[1]));
            return true;
        }
        if (!Double.isFinite(amount) || amount <= 0.0) {
            languages.send(payer, "economy.amount-positive");
            return true;
        }
        // Large payments require an explicit /pay confirm within 30s
        // (config: economy.pay.confirm-threshold; 0 disables the gate).
        double threshold = plugin.getConfig().getDouble("economy.pay.confirm-threshold", 0.0);
        if (threshold > 0.0 && amount >= threshold) {
            pendingConfirms.put(payer.getUniqueId(), new PendingPay(
                    target.getUniqueId(), targetName, amount,
                    System.currentTimeMillis() + CONFIRM_WINDOW_MS));
            languages.send(payer, "economy.pay.confirm.prompt", Placeholders.with(
                    "player", targetName, "amount", economy.format(amount)));
            ComponentMessenger.InteractiveMessagePart confirmButton = languages.getInteractivePart(
                    payer, "economy.pay.confirm.button", Collections.<String, String>emptyMap());
            if (confirmButton != null) {
                ComponentMessenger.sendJoinedHoverMessages(payer, Collections.singletonList(confirmButton));
            }
            return true;
        }
        performPay(payer, target.getUniqueId(), targetName, amount);
        return true;
    }

    /**
     * Executes a validated payment: optional transfer tax
     * ({@code economy.pay.tax-percent}, burned as a money sink), the atomic transfer,
     * audit entries for both sides (+ TAX), and the result messages.
     */
    private void performPay(Player payer, java.util.UUID targetUuid, String targetName, double amount) {
        double taxPercent = Math.max(0.0, plugin.getConfig().getDouble("economy.pay.tax-percent", 0.0));
        double tax = taxPercent <= 0.0 ? 0.0 : EconomyService.sanitize(amount * taxPercent / 100.0);
        if (tax > 0.0 && economy.getBalance(payer.getUniqueId()) < amount + tax) {
            // Must afford the payment AND its tax — say so with the full total.
            languages.send(payer, "economy.pay.insufficient-tax", Placeholders.with(
                    "amount", economy.format(amount), "tax", economy.format(tax),
                    "total", economy.format(amount + tax)));
            return;
        }
        if (!economy.transfer(payer.getUniqueId(), payer.getName(), targetUuid, targetName, amount)) {
            // Distinguish a genuine decline (not enough money) from a store/transaction
            // failure, so the payer isn't told "insufficient funds" after a DB hiccup.
            if (economy.getBalance(payer.getUniqueId()) >= amount) {
                // The payer can afford it, so the decline is either a DB hiccup or the
                // recipient lacking headroom (would exceed MAX_BALANCE). Surface the
                // latter precisely instead of a generic failure.
                if (economy.getBalance(targetUuid) + amount > EconomyService.MAX_BALANCE) {
                    languages.send(payer, "economy.pay.recipient-full", Placeholders.with("player", targetName));
                } else {
                    languages.send(payer, "economy.pay.failed");
                }
            } else {
                languages.send(payer, "economy.pay.insufficient", Placeholders.with("amount", economy.format(amount)));
            }
            return;
        }
        if (tax > 0.0 && economy.withdraw(payer.getUniqueId(), payer.getName(), tax)) {
            // Burned (not redistributed) — transfer taxes are a deliberate money sink.
            economy.logTransaction(payer.getName(), payer.getUniqueId(), payer.getName(),
                    "TAX", tax, economy.getBalance(payer.getUniqueId()));
        }
        // Audit trail: one entry per side so /eco log <player> shows both directions.
        economy.logTransaction(payer.getName(), payer.getUniqueId(), payer.getName(),
                "PAY", amount, economy.getBalance(payer.getUniqueId()));
        economy.logTransaction(payer.getName(), targetUuid, targetName,
                "RECEIVE", amount, economy.getBalance(targetUuid));
        Map<String, String> sent = new HashMap<>();
        sent.put("player", targetName);
        sent.put("amount", economy.format(amount));
        languages.send(payer, "economy.pay.sent", sent);
        if (tax > 0.0) {
            languages.send(payer, "economy.pay.taxed", Placeholders.with("tax", economy.format(tax)));
        }
        Player onlineTarget = Bukkit.getPlayer(targetUuid);
        if (onlineTarget != null) {
            Map<String, String> received = new HashMap<>();
            received.put("player", payer.getName());
            received.put("amount", economy.format(amount));
            languages.send(onlineTarget, "economy.pay.received", received);
        }
    }

    /**
     * Shows a box-style, click-to-suggest list of player names that look like the
     * (unresolved) name the payer typed — drawn from online players plus every known
     * economy account. A candidate matches when it shares the typed prefix or any run
     * of at least 3 consecutive characters with what was typed. Clicking a name
     * suggests {@code /pay <name> } so the payer can just type the amount and send.
     *
     * @return {@code true} if at least one suggestion was shown (caller then skips the
     *         plain "unknown player" message); {@code false} if nothing looked similar.
     */
    private boolean suggestSimilarNames(Player payer, String typed) {
        List<String> matches = findSimilarNames(payer, typed);
        if (matches.isEmpty()) {
            return false;
        }
        languages.send(payer, "economy.pay.suggest.header", Placeholders.with("player", typed));
        for (String name : matches) {
            Map<String, String> ph = new HashMap<>();
            ph.put("player", name);
            ComponentMessenger.InteractiveMessagePart part =
                    languages.getInteractivePart(payer, "economy.pay.suggest.entry", ph);
            if (part != null) {
                ComponentMessenger.sendJoinedHoverMessages(payer, Collections.singletonList(part));
            }
        }
        languages.send(payer, "economy.pay.suggest.footer");
        return true;
    }

    /** Up to 8 known names similar to {@code typed}, best matches first (prefix, then shorter, then A→Z). */
    private List<String> findSimilarNames(Player payer, String typed) {
        final String needle = typed == null ? "" : typed.toLowerCase(Locale.ENGLISH);
        // lowercased name -> display name, so online + account sources de-dupe cleanly.
        LinkedHashMap<String, String> byLower = new LinkedHashMap<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(payer.getUniqueId())) {
                continue;
            }
            byLower.putIfAbsent(online.getName().toLowerCase(Locale.ENGLISH), online.getName());
        }
        try {
            // Cap the scan: this only feeds typo-suggestions, so a bounded top-N avoids a full-table
            // sort on the main thread (a griefer could otherwise spam unknown names to lag the server).
            for (EconomyService.BalanceEntry entry : economy.topBalances(200)) {
                String name = entry.getName();
                if (name == null || name.isEmpty() || name.equalsIgnoreCase(payer.getName())) {
                    continue;
                }
                byLower.putIfAbsent(name.toLowerCase(Locale.ENGLISH), name);
            }
        } catch (Throwable ignored) {
            // account enumeration is best-effort; online players alone still drive suggestions
        }

        List<String> matches = new ArrayList<>();
        for (Map.Entry<String, String> e : byLower.entrySet()) {
            String lower = e.getKey();
            if (lower.equals(needle)) {
                continue;
            }
            if ((!needle.isEmpty() && lower.startsWith(needle)) || sharesRun(lower, needle, 3)) {
                matches.add(e.getValue());
            }
        }
        matches.sort((a, b) -> {
            boolean sa = a.toLowerCase(Locale.ENGLISH).startsWith(needle) && !needle.isEmpty();
            boolean sb = b.toLowerCase(Locale.ENGLISH).startsWith(needle) && !needle.isEmpty();
            if (sa != sb) {
                return sa ? -1 : 1;
            }
            if (a.length() != b.length()) {
                return Integer.compare(a.length(), b.length());
            }
            return a.compareToIgnoreCase(b);
        });
        return matches.size() > 8 ? new ArrayList<>(matches.subList(0, 8)) : matches;
    }

    /** Whether {@code haystack} contains any {@code run}-length window of {@code needle}. */
    private static boolean sharesRun(String haystack, String needle, int run) {
        if (haystack == null || needle == null || needle.length() < run) {
            return false;
        }
        for (int i = 0; i + run <= needle.length(); i++) {
            if (haystack.contains(needle.substring(i, i + run))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Suggest every online player whose name starts with what's typed. We do NOT exclude the
            // sender here: returning an empty list (which self-exclusion causes when few players are
            // online) makes the client show no suggestions at all, which read as "tab-complete is
            // broken". Self-payment is still blocked at execution with a clear message.
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                    names.add(online.getName());
                }
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
            return names;
        }
        if (args.length == 2) {
            // Light amount hints; the player can still type any number.
            List<String> amounts = new ArrayList<>();
            for (String amount : new String[]{"10", "100", "1000"}) {
                if (amount.startsWith(args[1])) {
                    amounts.add(amount);
                }
            }
            return amounts;
        }
        return Collections.emptyList();
    }
}