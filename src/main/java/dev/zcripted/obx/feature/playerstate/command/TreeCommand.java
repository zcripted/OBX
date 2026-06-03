package dev.zcripted.obx.feature.playerstate.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeCommand extends AbstractObxCommand implements TabCompleter {


    public TreeCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.tree")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        TreeType type = TreeType.TREE;
        if (args.length >= 1) {
            try { type = TreeType.valueOf(args[0].toUpperCase()); }
            catch (IllegalArgumentException ignored) {
                languages.send(player, "mob.tree.unknown", Placeholders.with("type", args[0]));
                return true;
            }
        }
        Block block = player.getTargetBlock((java.util.Set<org.bukkit.Material>) null, 100);
        Location target = block == null ? player.getLocation() : block.getLocation().add(0, 1, 0);
        if (target.getWorld() == null) return true;
        boolean grew = target.getWorld().generateTree(target, type);
        if (grew) {
            languages.send(player, "mob.tree.grown", Placeholders.with("type", type.name()));
        } else {
            languages.send(player, "mob.tree.failed");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (TreeType type : TreeType.values()) {
                if (type.name().toLowerCase().startsWith(prefix)) names.add(type.name());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
