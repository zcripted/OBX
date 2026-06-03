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
import java.util.List;

public class FlyCommand extends AbstractObxCommand implements TabCompleter {

    private final FlightStateService flight;

    public FlyCommand(ObxPlugin plugin) {
        super(plugin);
        this.flight = plugin.getFlightStateService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("obx.fly.others")) {
                languages.send(sender, "core.no-permission");
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                languages.send(sender, "teleport.tp.not-online", Placeholders.with("player", args[0]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("obx.fly")) {
                languages.send(target, "core.no-permission");
                return true;
            }
        }
        boolean nowFlying = !target.getAllowFlight();
        flight.setFlight(target, nowFlying);
        languages.send(target, nowFlying ? "flight.fly.on-self" : "flight.fly.off-self");
        if (!sender.equals(target)) {
            languages.send(sender, nowFlying ? "flight.fly.on-other" : "flight.fly.off-other",
                    Placeholders.with("player", target.getName()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("obx.fly.others")) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
