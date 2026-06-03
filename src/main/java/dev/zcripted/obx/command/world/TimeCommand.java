package dev.zcripted.obx.command.world;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TimeCommand extends AbstractObxCommand implements TabCompleter {


    public TimeCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.time")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "world.time.usage");
            return true;
        }
        String action = args[0].toLowerCase();
        if (!action.equals("set") && !action.equals("add")) {
            languages.send(sender, "world.time.usage");
            return true;
        }
        Long parsed = parseTime(args[1]);
        if (parsed == null) {
            languages.send(sender, "world.time.invalid", Placeholders.with("input", args[1]));
            return true;
        }
        World world = resolveWorld(sender, args.length >= 3 ? args[2] : null);
        if (world == null) {
            languages.send(sender, "world.unknown", Placeholders.with("world", args.length >= 3 ? args[2] : "?"));
            return true;
        }
        if (action.equals("set")) {
            world.setTime(parsed);
        } else {
            world.setTime(world.getTime() + parsed);
        }
        languages.send(sender, "world.time.changed",
                Placeholders.with("world", world.getName(), "time", world.getTime()));
        return true;
    }

    private Long parseTime(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "day": case "morning": return 1000L;
            case "noon": return 6000L;
            case "night": case "evening": return 13000L;
            case "midnight": return 18000L;
            default:
                try { return Long.parseLong(value); }
                catch (NumberFormatException ignored) { return null; }
        }
    }

    private World resolveWorld(CommandSender sender, String name) {
        if (name != null) {
            return sender.getServer().getWorld(name);
        }
        if (sender instanceof Player) {
            return ((Player) sender).getWorld();
        }
        List<World> worlds = sender.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return new ArrayList<>(Arrays.asList("set", "add"));
        if (args.length == 2) return new ArrayList<>(Arrays.asList("day", "noon", "night", "midnight"));
        if (args.length == 3) {
            List<String> names = new ArrayList<>();
            for (World world : sender.getServer().getWorlds()) names.add(world.getName());
            return names;
        }
        return Collections.emptyList();
    }
}
