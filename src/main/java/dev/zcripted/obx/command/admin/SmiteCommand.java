package dev.zcripted.obx.command.admin;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmiteCommand extends AbstractObxCommand implements TabCompleter {


    public SmiteCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.smite")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        Location strikeLocation;
        String targetName;
        if (args.length >= 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                languages.send(sender, "teleport.tp.not-online", Placeholders.with("player", args[0]));
                return true;
            }
            strikeLocation = target.getLocation();
            targetName = target.getName();
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "mob.smite.usage");
                return true;
            }
            Player player = (Player) sender;
            Block block = player.getTargetBlock((java.util.Set<org.bukkit.Material>) null, 100);
            strikeLocation = block == null ? player.getLocation() : block.getLocation().add(0.5, 0.0, 0.5);
            targetName = "your crosshair";
        }
        if (strikeLocation == null || strikeLocation.getWorld() == null) return true;
        strikeLocation.getWorld().strikeLightning(strikeLocation);
        languages.send(sender, "mob.smite.struck", Placeholders.with("target", targetName));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
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
