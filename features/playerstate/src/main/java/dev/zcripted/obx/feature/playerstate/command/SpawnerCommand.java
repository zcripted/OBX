package dev.zcripted.obx.feature.playerstate.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpawnerCommand extends AbstractObxCommand implements TabCompleter {


    public SpawnerCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.spawner")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "mob.spawner.usage");
            return true;
        }
        EntityType type;
        try { type = EntityType.valueOf(args[0].toUpperCase()); }
        catch (IllegalArgumentException ignored) {
            languages.send(player, "mob.unknown-type", Placeholders.with("type", args[0]));
            return true;
        }
        Block target = player.getTargetBlock((java.util.Set<org.bukkit.Material>) null, 10);
        if (target == null || !(target.getState() instanceof CreatureSpawner)) {
            languages.send(player, "mob.spawner.no-target");
            return true;
        }
        CreatureSpawner spawner = (CreatureSpawner) target.getState();
        spawner.setSpawnedType(type);
        spawner.update();
        languages.send(player, "mob.spawner.set", Placeholders.with("type", type.name()));
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