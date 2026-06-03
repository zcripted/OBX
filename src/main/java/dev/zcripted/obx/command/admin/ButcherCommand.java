package dev.zcripted.obx.command.admin;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ButcherCommand extends AbstractObxCommand implements TabCompleter {


    public ButcherCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.butcher")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        int radius = 32;
        if (args.length >= 1) {
            try { radius = Math.max(1, Math.min(256, Integer.parseInt(args[0]))); }
            catch (NumberFormatException ignored) {}
        }
        EntityType filter = null;
        if (args.length >= 2 && !args[1].equalsIgnoreCase("all")) {
            try { filter = EntityType.valueOf(args[1].toUpperCase()); }
            catch (IllegalArgumentException ignored) {
                languages.send(player, "mob.unknown-type", Placeholders.with("type", args[1]));
                return true;
            }
        }
        int killed = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity instanceof Player) continue;
            if (filter != null && entity.getType() != filter) continue;
            ((LivingEntity) entity).setHealth(0.0);
            killed++;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(killed));
        placeholders.put("radius", String.valueOf(radius));
        languages.send(player, "mob.butcher.result", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> matches = new java.util.ArrayList<>();
            for (String radius : new String[]{"16", "32", "64", "128"}) {
                if (radius.startsWith(prefix)) matches.add(radius);
            }
            return matches;
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            List<String> matches = new java.util.ArrayList<>();
            if ("all".startsWith(prefix)) matches.add("all");
            for (EntityType type : EntityType.values()) {
                if (!type.isAlive()) continue;
                if (type == EntityType.PLAYER) continue;
                if (type.name().toLowerCase().startsWith(prefix)) matches.add(type.name());
            }
            return matches;
        }
        return Collections.emptyList();
    }
}
