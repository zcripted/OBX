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

import java.util.Collections;
import java.util.List;

public class DayCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final long target;

    public DayCommand(Main plugin, long target) {
        this.languages = plugin.getLanguageManager();
        this.target = target;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.time")) {
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
        languages.send(sender, "world.time.changed",
                Placeholders.with("world", world.getName(), "time", target));
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
