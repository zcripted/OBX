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
        if (args.length < 2) {
            languages.send(sender, "economy.eco.usage");
            return true;
        }
        String action = args[0].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null && target.getFirstPlayed() == 0L) {
            languages.send(sender, "economy.unknown-player", Placeholders.with("player", args[1]));
            return true;
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
        switch (action) {
            case "give":
                economy.deposit(target.getUniqueId(), target.getName(), amount);
                break;
            case "take":
                economy.withdraw(target.getUniqueId(), target.getName(), amount);
                break;
            case "set":
                economy.setBalance(target.getUniqueId(), target.getName(), amount);
                break;
            case "reset":
                economy.resetBalance(target.getUniqueId(), target.getName());
                break;
            default:
                languages.send(sender, "economy.eco.usage");
                return true;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("amount", economy.format(amount));
        placeholders.put("balance", economy.format(economy.getBalance(target.getUniqueId())));
        languages.send(sender, "economy.eco." + action, placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("give", "take", "set", "reset"), args[0]);
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) names.add(online.getName());
            return filter(names, args[1]);
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
