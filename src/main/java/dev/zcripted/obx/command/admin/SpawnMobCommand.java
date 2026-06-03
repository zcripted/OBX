package dev.zcripted.obx.command.admin;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnMobCommand extends AbstractObxCommand implements TabCompleter {


    public SpawnMobCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.spawnmob")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "mob.spawnmob.usage");
            return true;
        }
        EntityType type;
        try { type = EntityType.valueOf(args[0].toUpperCase()); }
        catch (IllegalArgumentException ignored) {
            languages.send(player, "mob.unknown-type", Placeholders.with("type", args[0]));
            return true;
        }
        if (!type.isSpawnable() || !type.isAlive()) {
            languages.send(player, "mob.unspawnable-type", Placeholders.with("type", args[0]));
            return true;
        }
        int count = 1;
        if (args.length >= 2) {
            try { count = Math.max(1, Math.min(50, Integer.parseInt(args[1]))); }
            catch (NumberFormatException ignored) {}
        }
        Block target = player.getTargetBlock((java.util.Set<org.bukkit.Material>) null, 100);
        Location spawnLoc;
        if (target != null && target.getType() != org.bukkit.Material.AIR) {
            spawnLoc = target.getLocation().add(0.5, 1.0, 0.5);
        } else {
            spawnLoc = player.getLocation();
        }
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (spawnLoc.getWorld() == null) break;
            spawnLoc.getWorld().spawnEntity(spawnLoc, type);
            spawned++;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(spawned));
        placeholders.put("type", type.name());
        languages.send(player, "mob.spawnmob.result", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (EntityType type : EntityType.values()) {
                if (type.isAlive() && type.isSpawnable() && type.name().toLowerCase().startsWith(prefix)) {
                    names.add(type.name());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
