package dev.zcripted.obx.command.teleportation;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin direct teleports: {@code /tp} &amp; {@code /teleport} (go to a player, or move one
 * player to another) and {@code /tphere} (bring a player to you). Requires
 * {@code obx.teleport.admin}. Console can move players between each other but has no
 * location of its own, and there is no "console" player to teleport to.
 */
public final class TeleportCommand extends AbstractObxCommand implements TabCompleter {

    public enum Mode { TO, HERE }

    private final Mode mode;

    public TeleportCommand(Main plugin, Mode mode) {
        super(plugin);
        this.mode = mode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.teleport.admin")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (mode == Mode.HERE) {
            return handleHere(sender, args);
        }
        return handleTo(sender, args);
    }

    /** {@code /tphere <player>} — bring the player to the sender. */
    private boolean handleHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "teleport.tp.console-needs-two");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "teleport.tp.usage-here");
            return true;
        }
        Player self = (Player) sender;
        Player target = online(sender, args[0]);
        if (target == null) {
            return true;
        }
        moveTo(target, self, "teleport.tp.brought", self.getName(), target.getName());
        // /tphere shows the sender's own world + coordinates (where the target landed).
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("player", target.getName());
        putLocation(ph, self.getLocation());
        languages.send(self, "teleport.tp.brought-self", ph);
        return true;
    }

    /** {@code /tp <player>} (go to) or {@code /tp <player> <target>} (move one to another). */
    private boolean handleTo(CommandSender sender, String[] args) {
        if (args.length == 0) {
            languages.send(sender, "teleport.tp.usage");
            return true;
        }
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                languages.send(sender, "teleport.tp.console-needs-two");
                return true;
            }
            Player self = (Player) sender;
            Player dest = online(sender, args[0]);
            if (dest == null) {
                return true;
            }
            moveTo(self, dest, "teleport.tp.went", self.getName(), dest.getName());
            return true;
        }
        Player who = online(sender, args[0]);
        Player dest = online(sender, args[1]);
        if (who == null || dest == null) {
            return true;
        }
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("player", who.getName());
        ph.put("target", dest.getName());
        putLocation(ph, dest.getLocation());
        moveTo(who, dest, "teleport.tp.moved", who.getName(), dest.getName());
        languages.send(sender, "teleport.tp.moved-sender", ph);
        return true;
    }

    private void moveTo(Player who, Player destination, String messagePath, String whoName, String destName) {
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("player", whoName);
        ph.put("target", destName);
        // Destination world + coordinates for the styled second line of the message.
        putLocation(ph, destination.getLocation());
        plugin.getDataService().setBack(who.getUniqueId(), who.getLocation());
        plugin.getTeleportManager().teleportPlayer(who, destination.getLocation(), messagePath, ph);
    }

    /** Adds {world, x, y, z} (block coordinates) for the destination to the placeholder map. */
    private static void putLocation(Map<String, String> ph, org.bukkit.Location loc) {
        if (loc == null) {
            return;
        }
        ph.put("world", loc.getWorld() == null ? "world" : loc.getWorld().getName());
        ph.put("x", Integer.toString(loc.getBlockX()));
        ph.put("y", Integer.toString(loc.getBlockY()));
        ph.put("z", Integer.toString(loc.getBlockZ()));
    }

    private Player online(CommandSender sender, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            languages.send(sender, "teleport.tp.not-online", Collections.singletonMap("player", name));
        }
        return player;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("obx.teleport.admin") || args.length == 0 || args.length > 2) {
            return Collections.emptyList();
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ENGLISH);
        List<String> names = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                names.add(player.getName());
            }
        }
        return names;
    }
}
