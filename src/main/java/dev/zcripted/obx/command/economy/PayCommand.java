package dev.zcripted.obx.command.economy;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.economy.EconomyService;
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
import java.util.List;
import java.util.Map;

public class PayCommand extends AbstractObxCommand implements TabCompleter {

    private final EconomyService economy;

    public PayCommand(OBX plugin) {
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
        if (args.length < 2) {
            languages.send(payer, "economy.pay.usage");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null && target.getFirstPlayed() == 0L) {
            languages.send(payer, "economy.unknown-player", Placeholders.with("player", args[0]));
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
        if (!economy.transfer(payer.getUniqueId(), payer.getName(), target.getUniqueId(), target.getName(), amount)) {
            languages.send(payer, "economy.pay.insufficient", Placeholders.with("amount", economy.format(amount)));
            return true;
        }
        Map<String, String> sent = new HashMap<>();
        sent.put("player", target.getName());
        sent.put("amount", economy.format(amount));
        languages.send(payer, "economy.pay.sent", sent);
        Player onlineTarget = target.isOnline() ? target.getPlayer() : null;
        if (onlineTarget != null) {
            Map<String, String> received = new HashMap<>();
            received.put("player", payer.getName());
            received.put("amount", economy.format(amount));
            languages.send(onlineTarget, "economy.pay.received", received);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && online.getUniqueId().equals(((Player) sender).getUniqueId())) continue;
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
