package dev.zcripted.obx.feature.economy.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcoCommand extends AbstractObxCommand implements TabCompleter {

    private final EconomyService economy;

    public EcoCommand(ObxPlugin plugin) {
        super(plugin);
        this.economy = plugin.getEconomyService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.eco")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("log")) {
            return showLog(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("server")) {
            return serverAccount(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("digest")) {
            dev.zcripted.obx.feature.economy.report.EconomyReportService report =
                    plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.report.EconomyReportService.class);
            if (report != null) {
                report.generateDigest();
                languages.send(sender, "economy.eco.digest-sent");
            }
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "economy.eco.usage");
            return true;
        }
        String action = args[0].toLowerCase();
        // Resolve the target: the server's usercache first, then — for players the
        // server has forgotten (or never knew) — the economy table's last-known
        // name, so admin actions work fully OFFLINE for anyone who ever held an account.
        java.util.UUID targetUuid;
        String targetName;
        boolean freshAccount = false;
        OfflinePlayer cached = Bukkit.getOfflinePlayer(args[1]);
        if (cached.getName() != null || cached.getFirstPlayed() != 0L) {
            targetUuid = cached.getUniqueId();
            targetName = cached.getName() == null ? args[1] : cached.getName();
            // Never seen by this server AND no economy row: acting on this name will
            // CREATE the account. Seed it at exactly $0 (not the starting balance) so
            // "/eco give X" yields exactly X — and tell the admin a new account was made.
            freshAccount = cached.getFirstPlayed() == 0L && !cached.isOnline()
                    && !economy.findAccount(args[1]).isPresent();
        } else {
            java.util.Optional<EconomyService.BalanceEntry> account = economy.findAccount(args[1]);
            if (!account.isPresent()) {
                languages.send(sender, "economy.unknown-player", Placeholders.with("player", args[1]));
                return true;
            }
            targetUuid = account.get().getUuid();
            targetName = account.get().getName() == null ? args[1] : account.get().getName();
        }
        double amount = 0.0;
        if (!action.equals("reset")) {
            if (args.length < 3) {
                languages.send(sender, "economy.eco.usage");
                return true;
            }
            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException ignored) {
                languages.send(sender, "economy.invalid-amount", Placeholders.with("input", args[2]));
                return true;
            }
            if (!Double.isFinite(amount) || amount < 0.0) {
                languages.send(sender, "economy.amount-positive");
                return true;
            }
        }
        if (freshAccount) {
            economy.setBalance(targetUuid, targetName, 0.0);
            languages.send(sender, "economy.eco.created", Placeholders.with("player", targetName));
        }
        switch (action) {
            case "give":
                economy.deposit(targetUuid, targetName, amount);
                break;
            case "take":
                if (!economy.withdraw(targetUuid, targetName, amount)) {
                    // Not enough to take — don't report a misleading success.
                    Map<String, String> fail = new HashMap<>();
                    fail.put("player", targetName);
                    fail.put("amount", economy.format(amount));
                    fail.put("balance", economy.format(economy.getBalance(targetUuid)));
                    languages.send(sender, "economy.eco.take-failed", fail);
                    return true;
                }
                break;
            case "set":
                economy.setBalance(targetUuid, targetName, amount);
                break;
            case "reset":
                economy.resetBalance(targetUuid, targetName);
                amount = economy.getStartingBalance();
                break;
            default:
                languages.send(sender, "economy.eco.usage");
                return true;
        }
        double after = economy.getBalance(targetUuid);
        // Audit trail: every admin money movement is recorded with its actor.
        economy.logTransaction(sender.getName(), targetUuid, targetName,
                action.toUpperCase(java.util.Locale.ENGLISH), amount, after);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", targetName);
        placeholders.put("amount", economy.format(amount));
        placeholders.put("balance", economy.format(after));
        languages.send(sender, "economy.eco." + action, placeholders);
        return true;
    }

    /**
     * {@code /eco server} — the visible sink-revenue account: balance plus a
     * 7-day per-source breakdown (AH tax/fees, anvil fees, claim upkeep).
     * {@code /eco server withdraw <amount>} moves revenue into the admin's
     * wallet (for prize pools, refunds, …) and audits it as SERVER_WITHDRAW.
     */
    private boolean serverAccount(CommandSender sender, String[] args) {
        dev.zcripted.obx.feature.economy.sink.ServerAccountService account =
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.sink.ServerAccountService.class);
        if (account == null) {
            languages.send(sender, "economy.eco.server.unavailable");
            return true;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("withdraw")) {
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException ignored) {
                languages.send(sender, "economy.invalid-amount", Placeholders.with("input", args[2]));
                return true;
            }
            if (!Double.isFinite(amount) || amount <= 0.0) {
                languages.send(sender, "economy.amount-positive");
                return true;
            }
            if (account.withdrawTo((Player) sender, amount)) {
                languages.send(sender, "economy.eco.server.withdrawn", Placeholders.with(
                        "amount", economy.format(amount),
                        "balance", economy.format(account.balance())));
            } else {
                languages.send(sender, "economy.eco.server.withdraw-failed", Placeholders.with(
                        "amount", economy.format(amount),
                        "balance", economy.format(account.balance())));
            }
            return true;
        }
        languages.send(sender, "economy.eco.server.header",
                Placeholders.with("balance", economy.format(account.balance())));
        List<dev.zcripted.obx.feature.economy.sink.ServerAccountService.SourceTotal> sources =
                account.sourceTotals(System.currentTimeMillis() - 7L * 86_400_000L);
        if (sources.isEmpty()) {
            languages.send(sender, "economy.eco.server.none");
        }
        for (dev.zcripted.obx.feature.economy.sink.ServerAccountService.SourceTotal source : sources) {
            languages.send(sender, "economy.eco.server.source", Placeholders.with(
                    "source", source.action, "amount", economy.format(source.total)));
        }
        languages.send(sender, "economy.eco.server.footer");
        return true;
    }

    /** Audited actions an admin can filter the log by (tab-complete + validation). */
    private static final List<String> LOG_ACTIONS = Arrays.asList(
            "GIVE", "TAKE", "SET", "RESET", "PAY", "RECEIVE", "TAX", "SELL", "SHOP_BUY", "SHOP_SELL",
            "WITHDRAW", "REDEEM", "BANK_DEPOSIT", "BANK_WITHDRAW", "BANK_INTEREST",
            "AH_BUY", "AH_SELL", "AH_BUYOUT", "AH_BID", "AH_BID_REFUND", "AH_TAX", "AH_LISTING_FEE",
            "ANVIL_REPAIR", "SELL_WAND", "CLAIM_UPKEEP", "SERVER_WITHDRAW", "PAYDAY");

    private static final int LOG_PAGE_SIZE = 10;

    /**
     * {@code /eco log [player|*] [page] [action]} — newest-first audit entries,
     * paginated 10/page, optionally filtered to one action type. {@code *} (or
     * omitting the player) shows the whole economy.
     */
    private boolean showLog(CommandSender sender, String[] args) {
        java.util.UUID filter = null;
        String filterName = null;
        if (args.length >= 2 && !args[1].equals("*")) {
            OfflinePlayer cached = Bukkit.getOfflinePlayer(args[1]);
            if (cached.getName() != null || cached.getFirstPlayed() != 0L) {
                filter = cached.getUniqueId();
                filterName = cached.getName() == null ? args[1] : cached.getName();
            } else {
                java.util.Optional<EconomyService.BalanceEntry> account = economy.findAccount(args[1]);
                if (!account.isPresent()) {
                    languages.send(sender, "economy.unknown-player", Placeholders.with("player", args[1]));
                    return true;
                }
                filter = account.get().getUuid();
                filterName = account.get().getName();
            }
        }
        int page = 1;
        if (args.length >= 3) {
            try {
                page = Math.max(1, Math.min(1000, Integer.parseInt(args[2])));
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }
        String action = null;
        if (args.length >= 4) {
            String wanted = args[3].toUpperCase(java.util.Locale.ENGLISH);
            if (!LOG_ACTIONS.contains(wanted)) {
                languages.send(sender, "economy.eco.log.bad-action", Placeholders.with(
                        "action", args[3], "actions", String.join(", ", LOG_ACTIONS)));
                return true;
            }
            action = wanted;
        }
        List<EconomyService.TransactionEntry> entries =
                economy.recentTransactions(filter, action, LOG_PAGE_SIZE, (page - 1) * LOG_PAGE_SIZE);
        String scope = filterName == null ? languages.get(sender, "economy.eco.log.scope-all") : filterName;
        if (action != null) {
            scope = scope + " §8· §f" + action;
        }
        languages.send(sender, "economy.eco.log.header", Placeholders.with(
                "scope", scope, "count", entries.size(), "page", page));
        if (entries.isEmpty()) {
            languages.send(sender, "economy.eco.log.empty");
        }
        java.text.SimpleDateFormat time = new java.text.SimpleDateFormat("MM-dd HH:mm");
        for (EconomyService.TransactionEntry entry : entries) {
            Map<String, String> row = new HashMap<>();
            row.put("time", time.format(new java.util.Date(entry.getTime())));
            row.put("actor", entry.getActor() == null ? "?" : entry.getActor());
            row.put("target", entry.getTargetName() == null ? "?" : entry.getTargetName());
            row.put("action", entry.getAction());
            row.put("amount", economy.format(entry.getAmount()));
            row.put("balance", economy.format(entry.getBalanceAfter()));
            languages.send(sender, "economy.eco.log.entry", row);
        }
        languages.send(sender, "economy.eco.log.footer");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("give", "take", "set", "reset", "log", "server", "digest"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("server")) {
                return filter(Arrays.asList("withdraw"), args[1]);
            }
            List<String> names = new ArrayList<>();
            if (args[0].equalsIgnoreCase("log")) names.add("*");
            for (Player online : Bukkit.getOnlinePlayers()) names.add(online.getName());
            return filter(names, args[1]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("log")) {
            return filter(LOG_ACTIONS, args[3]);
        }
        return java.util.Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String value : options) {
            if (value.toLowerCase().startsWith(lower)) matches.add(value);
        }
        return matches;
    }
}