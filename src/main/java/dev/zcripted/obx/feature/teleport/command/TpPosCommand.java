package dev.zcripted.obx.feature.teleport.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TpPosCommand extends AbstractObxCommand implements TabCompleter {


    public TpPosCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.tppos")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 3) {
            languages.send(player, "teleport.tppos.usage");
            return true;
        }
        Double x = parseCoordinate(args[0], player.getLocation().getX());
        Double y = parseCoordinate(args[1], player.getLocation().getY());
        Double z = parseCoordinate(args[2], player.getLocation().getZ());
        if (x == null || y == null || z == null) {
            languages.send(player, "teleport.tppos.invalid");
            return true;
        }
        World world = player.getWorld();
        if (args.length >= 4) {
            World resolved = Bukkit.getWorld(args[3]);
            if (resolved == null) {
                languages.send(player, "teleport.tppos.unknown-world", Placeholders.with("world", args[3]));
                return true;
            }
            world = resolved;
        }
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        if (args.length >= 5) {
            try { yaw = Float.parseFloat(args[4]); } catch (NumberFormatException ignored) { }
        }
        if (args.length >= 6) {
            try { pitch = Float.parseFloat(args[5]); } catch (NumberFormatException ignored) { }
        }
        Location destination = new Location(world, x, y, z, yaw, pitch);
        plugin.getDataService().setBack(player.getUniqueId(), player.getLocation());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("x", format(x));
        placeholders.put("y", format(y));
        placeholders.put("z", format(z));
        placeholders.put("world", world.getName());
        plugin.getTeleportManager().teleportPlayer(player, destination, "teleport.tppos.teleporting", placeholders);
        return true;
    }

    private Double parseCoordinate(String raw, double current) {
        if (raw == null || raw.isEmpty()) {
            return current;
        }
        if (raw.startsWith("~")) {
            String suffix = raw.substring(1);
            if (suffix.isEmpty()) {
                return current;
            }
            try {
                return current + Double.parseDouble(suffix);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String format(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length >= 1 && args.length <= 3) {
            // x/y/z slots: surface "~" so staff know they can pass relative coords.
            String prefix = args[args.length - 1].toLowerCase();
            List<String> matches = new ArrayList<>();
            if ("~".startsWith(prefix) || prefix.startsWith("~")) {
                matches.add("~");
            }
            return matches;
        }
        if (args.length == 4) {
            List<String> worlds = new ArrayList<>();
            String prefix = args[3].toLowerCase();
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().startsWith(prefix)) {
                    worlds.add(world.getName());
                }
            }
            return worlds;
        }
        return Collections.emptyList();
    }
}
