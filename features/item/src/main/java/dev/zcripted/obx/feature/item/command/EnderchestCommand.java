package dev.zcripted.obx.feature.item.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnderchestCommand extends AbstractObxCommand implements TabCompleter {


    public EnderchestCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player viewer = (Player) sender;
        if (args.length >= 1) {
            if (!viewer.hasPermission("obx.enderchest.others")) {
                languages.send(viewer, "core.no-permission");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                languages.send(viewer, "teleport.tp.not-online", Placeholders.with("player", args[0]));
                return true;
            }
            viewer.openInventory(target.getEnderChest());
            languages.send(viewer, "inventory.enderchest.other", Placeholders.with("player", target.getName()));
            return true;
        }
        if (!viewer.hasPermission("obx.enderchest")) {
            languages.send(viewer, "core.no-permission");
            return true;
        }
        viewer.openInventory(viewer.getEnderChest());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("obx.enderchest.others")) {
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