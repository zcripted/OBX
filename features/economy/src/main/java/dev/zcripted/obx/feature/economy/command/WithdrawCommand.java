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
 * {@code /withdraw <amount>} (aliases: banknote, cheque) — converts wallet money
 * into a single-use banknote item via {@link BanknoteService}. Right-clicking the
 * note redeems it (see {@code BanknoteListener}).
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
        if (args.length < 1) {
            languages.send(player, "economy.note.usage");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException ignored) {
            languages.send(player, "economy.invalid-amount", Placeholders.with("input", args[0]));
            return true;
        }
        if (!Double.isFinite(amount) || amount <= 0.0) {
            languages.send(player, "economy.amount-positive");
            return true;
        }
        BanknoteService notes = plugin.getServiceRegistry().get(BanknoteService.class);
        if (notes == null || economy == null) {
            languages.send(player, "economy.note.unavailable");
            return true;
        }
        double value = EconomyService.sanitize(amount);
        if (economy.getBalance(player.getUniqueId()) < value) {
            languages.send(player, "economy.note.insufficient", Placeholders.with(
                    "amount", economy.format(value),
                    "balance", economy.format(economy.getBalance(player.getUniqueId()))));
            return true;
        }
        String token = notes.issue(player, value);
        if (token == null) {
            languages.send(player, "economy.note.failed");
            return true;
        }
        languages.send(player, "economy.note.issued", Placeholders.with(
                "amount", economy.format(value),
                "balance", economy.format(economy.getBalance(player.getUniqueId()))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> amounts = new ArrayList<>();
            for (String amount : new String[]{"100", "500", "1000", "10000"}) {
                if (amount.startsWith(args[0])) {
                    amounts.add(amount);
                }
            }
            return amounts;
        }
        return Collections.emptyList();
    }
}
