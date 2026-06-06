package dev.zcripted.obx.feature.world.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class DayCommand extends AbstractObxCommand implements TabCompleter {

    private final long target;

    public DayCommand(ObxPlugin plugin, long target) {
        super(plugin);
        this.target = target;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.time")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        World world;
        if (args.length >= 1) {
            world = sender.getServer().getWorld(args[0]);
            if (world == null) {
                languages.send(sender, "world.unknown", Placeholders.with("world", args[0]));
                return true;
            }
        } else if (sender instanceof Player) {
            world = ((Player) sender).getWorld();
        } else {
            List<World> worlds = sender.getServer().getWorlds();
            if (worlds.isEmpty()) return true;
            world = worlds.get(0);
        }
        world.setTime(target);
        dev.zcripted.obx.feature.world.service.ServerControlActions.timeMessage(plugin, sender, world.getName(), target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new java.util.ArrayList<>();
            for (World world : sender.getServer().getWorlds()) names.add(world.getName());
            return names;
        }
        return Collections.emptyList();
    }
}
