package dev.zcripted.obx.feature.economy.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.command.AbstractObxCommand;
import dev.zcripted.obx.feature.economy.service.BankService;
import dev.zcripted.obx.feature.economy.bank.BankMenu;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@code /bank [balance|deposit <amount>|withdraw <amount>]} — the interest-bearing
 * savings account backed by {@link BankService}. No args = balance overview
 * (wallet + banked + daily rate) in the box style.
 */
public class BankCommand extends AbstractObxCommand implements TabCompleter {

    private final EconomyService economy;

    public BankCommand(ObxPlugin plugin) {
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
        if (!player.hasPermission("obx.bank")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        BankService bank = plugin.getServiceRegistry().get(BankService.class);
        if (bank == null || !bank.isEnabled() || economy == null) {
            languages.send(player, "economy.bank.disabled");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("balance")) {
            sendOverview(player, bank);
            return true;
        }
        if (args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")) {
            BankMenu.open(plugin, player, 0);
            return true;
        }
        String action = args[0].toLowerCase(java.util.Locale.ENGLISH);
        if (!action.equals("deposit") && !action.equals("withdraw")) {
            languages.send(player, "economy.bank.usage");
            return true;
        }
        if (args.length < 2) {
            languages.send(player, "economy.bank.usage");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ignored) {
            languages.send(player, "economy.invalid-amount", Placeholders.with("input", args[1]));
            return true;
        }
        if (!Double.isFinite(amount) || amount <= 0.0) {
            languages.send(player, "economy.amount-positive");
            return true;
        }
        boolean ok = action.equals("deposit")
                ? bank.deposit(player.getUniqueId(), player.getName(), amount)
                : bank.withdraw(player.getUniqueId(), player.getName(), amount);
        if (!ok) {
            languages.send(player, action.equals("deposit")
                    ? "economy.bank.deposit-failed" : "economy.bank.withdraw-failed",
                    Placeholders.with("amount", economy.format(EconomyService.sanitize(amount))));
            return true;
        }
        languages.send(player, action.equals("deposit") ? "economy.bank.deposited" : "economy.bank.withdrawn",
                Placeholders.with(
                        "amount", economy.format(EconomyService.sanitize(amount)),
                        "bank", economy.format(bank.balance(player.getUniqueId(), player.getName())),
                        "balance", economy.format(economy.getBalance(player.getUniqueId()))));
        return true;
    }

    private void sendOverview(Player player, BankService bank) {
        double rate = plugin.getConfig().getDouble("economy.bank.interest-percent-daily", 0.5);
        languages.send(player, "economy.bank.overview", Placeholders.with(
                "bank", economy.format(bank.balance(player.getUniqueId(), player.getName())),
                "balance", economy.format(economy.getBalance(player.getUniqueId())),
                "rate", String.valueOf(Math.max(0.0, rate))));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new java.util.ArrayList<>();
            for (String option : Arrays.asList("balance", "deposit", "withdraw", "gui", "menu")) {
                if (option.startsWith(args[0].toLowerCase(java.util.Locale.ENGLISH))) {
                    options.add(option);
                }
            }
            return options;
        }
        return Collections.emptyList();
    }
}