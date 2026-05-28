package dev.sergeantfuzzy.sfcore.command.info;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;

    public NearCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.near")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        int radius = 64;
        if (args.length >= 1) {
            try { radius = Math.max(1, Math.min(1024, Integer.parseInt(args[0]))); } catch (NumberFormatException ignored) {}
        }
        Location origin = player.getLocation();
        double radiusSquared = (double) radius * radius;
        List<String> names = new ArrayList<>();
        for (Player other : player.getWorld().getPlayers()) {
            if (other.getUniqueId().equals(player.getUniqueId())) continue;
            double distanceSquared = other.getLocation().distanceSquared(origin);
            if (distanceSquared <= radiusSquared) {
                int distance = (int) Math.round(Math.sqrt(distanceSquared));
                names.add(other.getName() + " §7(§e" + distance + "m§7)");
            }
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("radius", String.valueOf(radius));
        placeholders.put("count", String.valueOf(names.size()));
        if (names.isEmpty()) {
            languages.send(player, "info.near.none", placeholders);
            return true;
        }
        placeholders.put("players", String.join("§r§7, ", names));
        languages.send(player, "info.near.list", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> matches = new java.util.ArrayList<>();
        for (String suggestion : new String[]{"16", "32", "64", "128", "256"}) {
            if (suggestion.startsWith(prefix)) matches.add(suggestion);
        }
        return matches;
    }
}
