package dev.zcripted.obx.core.storage;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class LocationSerializer {

    private LocationSerializer() {
    }

    public static void serialize(ConfigurationSection section, Location location) {
        if (section == null || location == null) {
            return;
        }
        section.set("world", location.getWorld() != null ? location.getWorld().getName() : null);
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    public static Location deserialize(ConfigurationSection section, JavaPlugin plugin) {
        if (section == null || plugin == null) {
            return null;
        }
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}

