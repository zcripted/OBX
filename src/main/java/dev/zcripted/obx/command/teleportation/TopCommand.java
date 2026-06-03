package dev.zcripted.obx.command.teleportation;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.storage.DataService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TopCommand extends AbstractObxCommand implements TabCompleter {

    private static final Set<String> UNSAFE_LANDING = new HashSet<>(Arrays.asList(
            "LAVA",
            "STATIONARY_LAVA",
            "FIRE",
            "SOUL_FIRE",
            "CAMPFIRE",
            "SOUL_CAMPFIRE",
            "MAGMA_BLOCK",
            "CACTUS",
            "SWEET_BERRY_BUSH",
            "WITHER_ROSE",
            "POWDER_SNOW",
            "POINTED_DRIPSTONE",
            "DRIPSTONE_BLOCK",
            "NETHER_PORTAL",
            "END_PORTAL",
            "END_GATEWAY",
            "COBWEB",
            "WEB",
            "BAMBOO",
            "SCAFFOLDING",
            "SAND",
            "RED_SAND",
            "GRAVEL",
            "CONCRETE_POWDER",
            "ANVIL",
            "CHIPPED_ANVIL",
            "DAMAGED_ANVIL",
            "DRAGON_EGG"
    ));

    private final DataService dataService;

    public TopCommand(OBX plugin) {
        super(plugin);
        this.dataService = plugin.getDataService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.top")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        Player target = null;
        if (args.length > 0) {
            target = resolvePlayer(args[0]);
            if (target == null) {
                languages.send(sender, "teleport.top.target-not-found", Placeholders.with("player", args[0]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            languages.send(sender, "teleport.top.usage-console");
            return true;
        }

        Location origin = target.getLocation();
        Location safe = findSafeTop(origin);
        if (safe == null) {
            languages.send(sender, "teleport.top.no-safe", Placeholders.with("target", target.getName()));
            return true;
        }

        dataService.setBack(target.getUniqueId(), origin);
        plugin.getTeleportManager().teleportPlayer(target, safe, "teleport.top.teleporting", null);

        if (!(sender instanceof Player) || !((Player) sender).getUniqueId().equals(target.getUniqueId())) {
            languages.send(sender, "teleport.top.sent-other", Placeholders.with("target", target.getName()));
            languages.send(target, "teleport.top.sent-target", Placeholders.with("sender", sender.getName()));
        }
        return true;
    }

    private Location findSafeTop(Location origin) {
        if (origin == null || origin.getWorld() == null) {
            return null;
        }
        World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();
        int minY = getMinHeight(world);
        int startY = Math.max(minY, origin.getBlockY() + 1);
        int maxY = world.getMaxHeight() - 1;
        for (int y = maxY; y >= startY; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (!isSafeLanding(block.getType())) {
                continue;
            }
            Block above = block.getRelative(BlockFace.UP);
            Block above2 = above.getRelative(BlockFace.UP);
            if (!isClearSpace(above) || !isClearSpace(above2)) {
                continue;
            }
            Location target = block.getLocation().add(0.5, 1.0, 0.5);
            target.setYaw(origin.getYaw());
            target.setPitch(origin.getPitch());
            return target;
        }
        return null;
    }

    private boolean isSafeLanding(Material material) {
        if (material == null) {
            return false;
        }
        if (!material.isSolid()) {
            return false;
        }
        String name = material.name();
        if (isTreeMaterial(name)) {
            return false;
        }
        return !UNSAFE_LANDING.contains(name);
    }

    private boolean isClearSpace(Block block) {
        if (block == null) {
            return false;
        }
        if (block.isLiquid()) {
            return false;
        }
        Material type = block.getType();
        if (type == null) {
            return false;
        }
        if (isTreeMaterial(type.name())) {
            return false;
        }
        if (UNSAFE_LANDING.contains(type.name())) {
            return false;
        }
        return !type.isSolid();
    }

    private boolean isTreeMaterial(String name) {
        if (name == null) {
            return false;
        }
        return name.contains("LEAVES")
                || name.contains("LOG")
                || name.contains("WOOD")
                || name.contains("STEM")
                || name.contains("HYPHAE")
                || name.contains("WART_BLOCK");
    }

    private int getMinHeight(World world) {
        try {
            Method method = world.getClass().getMethod("getMinHeight");
            Object result = method.invoke(world);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private Player resolvePlayer(String name) {
        if (name == null) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String current = args[0].toLowerCase(Locale.ENGLISH);
        List<String> suggestions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (name.toLowerCase(Locale.ENGLISH).startsWith(current)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }
}
