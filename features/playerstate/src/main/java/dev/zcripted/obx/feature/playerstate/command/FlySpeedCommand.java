package dev.zcripted.obx.feature.playerstate.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.playerstate.service.FlightStateService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlySpeedCommand extends AbstractObxCommand implements TabCompleter {

    private final FlightStateService flight;

    public FlySpeedCommand(ObxPlugin plugin) {
        super(plugin);
        this.flight = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.playerstate.service.FlightStateService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.flyspeed")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        // With a second arg, /flyspeed targets another player (admin/op only).
        boolean targetingOther = args.length >= 2;
        if (targetingOther && !sender.hasPermission("obx.flyspeed.others")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "flight.speed.usage-fly");
            return true;
        }

        Player target;
        if (targetingOther) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !target.isOnline()) {
                languages.send(sender, "teleport.tp.not-online", Placeholders.with("player", args[1]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            target = (Player) sender;
        }

        double value;
        try { value = Double.parseDouble(args[0]); }
        catch (NumberFormatException ignored) {
            languages.send(sender, "flight.speed.invalid", Placeholders.with("input", args[0]));
            return true;
        }
        if (value < 0.0 || value > 10.0) {
            languages.send(sender, "flight.speed.range");
            return true;
        }
        flight.setFlySpeed(target, flight.scaleFromInput(value));

        if (sender.equals(target)) {
            languages.send(target, "flight.speed.set-fly", Placeholders.with("value", value));
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("value", String.valueOf(value));
            placeholders.put("player", target.getName());
            languages.send(sender, "flight.speed.set-fly-other", placeholders);
            languages.send(target, "flight.speed.set-fly-target", placeholders);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (int i = 0; i <= 10; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(prefix)) matches.add(s);
            }
            return matches;
        }
        if (args.length == 2 && sender.hasPermission("obx.flyspeed.others")) {
            List<String> names = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}