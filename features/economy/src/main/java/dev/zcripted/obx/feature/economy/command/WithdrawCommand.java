package dev.zcripted.obx.feature.economy.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.command.AbstractObxCommand;
import dev.zcripted.obx.feature.economy.service.BanknoteService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code /withdraw <amount>} — converts wallet money into a signed banknote.
 *
 * <p>Also supports:
 * <ul>
 *   <li>{@code /withdraw all} — withdraw the player's full wallet balance as a single note.</li>
 *   <li>Configurable denominations at {@code economy.banknote.denominations} — when set,
 *       tab-completion suggests common note values.</li>
 * </ul>
 */
public class WithdrawCommand extends AbstractObxCommand implements TabCompleter {

    private final EconomyService economy;

    public WithdrawCommand(ObxPlugin plugin) {
        super(plugin);
        this.economy = plugin.getEconomyService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.withdraw")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        BanknoteService notes = plugin.getServiceRegistry().get(BanknoteService.class);
        if (notes == null || economy == null) {
            languages.send(player, "economy.note.disabled");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "economy.note.usage");
            return true;
        }

        double amount;
        if (args[0].equalsIgnoreCase("all")) {
            amount = economy.getBalance(player.getUniqueId());
            if (amount <= 0.0) {
                languages.send(player, "economy.note.no-funds");
                return true;
            }
        } else {
            try {
                amount = Double.parseDouble(args[0]);
            } catch (NumberFormatException ignored) {
                languages.send(player, "economy.invalid-amount", Placeholders.with("input", args[0]));
                return true;
            }
        }

        if (!Double.isFinite(amount) || amount <= 0.0) {
            languages.send(player, "economy.amount-positive");
            return true;
        }

        // For /withdraw all, we may need to issue multiple notes at different denominations
        if (args[0].equalsIgnoreCase("all") && economy != null) {
            double remaining = amount;
            int notesCreated = 0;
            List<Double> denoms = notes.denominations();
            if (denoms.isEmpty()) {
                // Single note for the full amount
                String token = notes.issue(player, remaining);
                if (token == null) {
                    languages.send(player, "economy.note.failed");
                    return true;
                }
                notesCreated = 1;
            } else {
                // Break into denomination notes from largest to smallest
                denoms.sort(Collections.<Double>reverseOrder());
                for (double denom : denoms) {
                    while (remaining >= denom && notesCreated < 36) {
                        String token = notes.issue(player, denom);
                        if (token == null) break;
                        remaining = EconomyService.sanitize(remaining - denom);
                        notesCreated++;
                    }
                }
                // Remaining as a single note
                if (remaining > 0.01) {
                    String token = notes.issue(player, remaining);
                    if (token != null) notesCreated++;
                }
            }
            if (notesCreated > 0) {
                languages.send(player, "economy.note.withdrawn-all",
                        Placeholders.with("count", notesCreated,
                                "amount", economy.format(amount)));
            } else {
                languages.send(player, "economy.note.failed");
            }
            return true;
        }

        String token = notes.issue(player, amount);
        if (token == null) {
            languages.send(player, "economy.note.failed");
            return true;
        }
        languages.send(player, "economy.note.withdrawn",
                Placeholders.with("amount", economy.format(EconomyService.sanitize(amount)),
                        "balance", economy.format(economy.getBalance(player.getUniqueId()))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> suggestions = new ArrayList<>();
        if ("all".startsWith(prefix)) {
            suggestions.add("all");
        }
        BanknoteService notes = plugin.getServiceRegistry().get(BanknoteService.class);
        if (notes != null) {
            for (double denom : notes.denominations()) {
                String formatted = String.valueOf((int) denom);
                if (formatted.startsWith(prefix)) {
                    suggestions.add(formatted);
                }
            }
        }
        return suggestions;
    }
}