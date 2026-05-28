package dev.sergeantfuzzy.sfcore.command.world;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WeatherCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;

    public WeatherCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.weather")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "world.weather.usage");
            return true;
        }
        String mode = args[0].toLowerCase();
        World world;
        if (args.length >= 2) {
            world = sender.getServer().getWorld(args[1]);
            if (world == null) {
                languages.send(sender, "world.unknown", Placeholders.with("world", args[1]));
                return true;
            }
        } else if (sender instanceof Player) {
            world = ((Player) sender).getWorld();
        } else {
            List<World> worlds = sender.getServer().getWorlds();
            if (worlds.isEmpty()) return true;
            world = worlds.get(0);
        }
        switch (mode) {
            case "clear":
            case "sun":
                world.setStorm(false);
                world.setThundering(false);
                break;
            case "rain":
            case "downfall":
                world.setStorm(true);
                world.setThundering(false);
                break;
            case "thunder":
            case "storm":
                world.setStorm(true);
                world.setThundering(true);
                break;
            default:
                languages.send(sender, "world.weather.usage");
                return true;
        }
        languages.send(sender, "world.weather.changed",
                Placeholders.with("world", world.getName(), "weather", mode));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return new ArrayList<>(Arrays.asList("clear", "rain", "thunder"));
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (World world : sender.getServer().getWorlds()) names.add(world.getName());
            return names;
        }
        return Collections.emptyList();
    }
}
