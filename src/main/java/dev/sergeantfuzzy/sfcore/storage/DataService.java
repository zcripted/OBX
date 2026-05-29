package dev.sergeantfuzzy.sfcore.storage;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Home / back / spawn persistence — SQLite-backed. A one-shot migration on
 * first boot copies any legacy data.yml content into the new tables and
 * renames the YAML file. After that point the YAML is no longer read or
 * written; the plugin treats SQLite as the source of truth.
 */
public class DataService {

    private final Main plugin;
    private final SqliteDataStore store;

    public DataService(Main plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            plugin.getLogger().severe("[SF-Core] DataService disabled — SQLite store unavailable.");
            return;
        }
        store.execute("CREATE TABLE IF NOT EXISTS homes (" +
                "uuid TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "world TEXT," +
                "x REAL, y REAL, z REAL," +
                "yaw REAL, pitch REAL," +
                "PRIMARY KEY (uuid, name))");
        store.execute("CREATE TABLE IF NOT EXISTS back_locations (" +
                "uuid TEXT PRIMARY KEY," +
                "world TEXT," +
                "x REAL, y REAL, z REAL," +
                "yaw REAL, pitch REAL)");
        store.execute("CREATE TABLE IF NOT EXISTS server_spawn (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                "world TEXT," +
                "x REAL, y REAL, z REAL," +
                "yaw REAL, pitch REAL," +
                "set_by_uuid TEXT," +
                "set_by_name TEXT," +
                "set_at TEXT)");
        migrateLegacyYaml();
    }

    public void reload() { load(); }

    /** No-op — SQLite writes auto-commit. Kept for API parity with the YAML era. */
    public void save() { /* SQLite auto-commit */ }

    private void migrateLegacyYaml() {
        File legacy = new File(plugin.getDataFolder(), "data.yml");
        if (!legacy.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(legacy);
        int homes = 0;
        int backs = 0;
        int spawn = 0;

        // spawn
        ConfigurationSection spawnSection = data.getConfigurationSection("spawn");
        if (spawnSection != null) {
            String world = spawnSection.getString("world");
            if (world != null) {
                store.executeUpdate(
                        "INSERT OR REPLACE INTO server_spawn (id, world, x, y, z, yaw, pitch, set_by_uuid, set_by_name, set_at)" +
                                " VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        world, spawnSection.getDouble("x"), spawnSection.getDouble("y"),
                        spawnSection.getDouble("z"),
                        (float) spawnSection.getDouble("yaw"),
                        (float) spawnSection.getDouble("pitch"),
                        spawnSection.getString("setBy"),
                        spawnSection.getString("setByName"),
                        spawnSection.getString("setAt"));
                spawn++;
            }
        }

        // per-player
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                UUID uuid;
                try { uuid = UUID.fromString(key); }
                catch (IllegalArgumentException ignored) { continue; }
                ConfigurationSection player = players.getConfigurationSection(key);
                if (player == null) continue;

                ConfigurationSection back = player.getConfigurationSection("back");
                if (back != null && back.getString("world") != null) {
                    store.executeUpdate(
                            "INSERT OR REPLACE INTO back_locations (uuid, world, x, y, z, yaw, pitch)" +
                                    " VALUES (?, ?, ?, ?, ?, ?, ?)",
                            uuid, back.getString("world"),
                            back.getDouble("x"), back.getDouble("y"), back.getDouble("z"),
                            (float) back.getDouble("yaw"), (float) back.getDouble("pitch"));
                    backs++;
                }

                ConfigurationSection homesSection = player.getConfigurationSection("homes");
                if (homesSection != null) {
                    for (String homeKey : homesSection.getKeys(false)) {
                        ConfigurationSection home = homesSection.getConfigurationSection(homeKey);
                        if (home == null || home.getString("world") == null) continue;
                        store.executeUpdate(
                                "INSERT OR REPLACE INTO homes (uuid, name, world, x, y, z, yaw, pitch)" +
                                        " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                uuid, homeKey.toLowerCase(), home.getString("world"),
                                home.getDouble("x"), home.getDouble("y"), home.getDouble("z"),
                                (float) home.getDouble("yaw"), (float) home.getDouble("pitch"));
                        homes++;
                    }
                }
            }
        }

        plugin.getLogger().info("[SF-Core] Migrated " + spawn + " spawn row(s), " + backs + " back row(s), " +
                homes + " home row(s) from data.yml.");
        File renamed = new File(legacy.getParentFile(), legacy.getName() + ".migrated");
        if (!legacy.renameTo(renamed)) {
            plugin.getLogger().warning("[SF-Core] Could not rename data.yml after migration.");
        }
    }

    // ---- spawn ----

    public void setSpawn(Location location) {
        setSpawn(location, null, null, null);
    }

    public void setSpawn(Location location, UUID setter, String setterName, String setAt) {
        if (!store.isAvailable() || location == null || location.getWorld() == null) return;
        store.executeUpdate(
                "INSERT INTO server_spawn (id, world, x, y, z, yaw, pitch, set_by_uuid, set_by_name, set_at)" +
                        " VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                        " ON CONFLICT(id) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y," +
                        " z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch," +
                        " set_by_uuid = excluded.set_by_uuid, set_by_name = excluded.set_by_name," +
                        " set_at = excluded.set_at",
                location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(),
                setter == null ? null : setter.toString(), setterName, setAt);
    }

    public Location getSpawn() {
        SpawnInfo info = getSpawnInfo();
        return info == null ? null : info.getLocation();
    }

    public SpawnInfo getSpawnInfo() {
        if (!store.isAvailable()) return null;
        return store.queryFirst(
                "SELECT world, x, y, z, yaw, pitch, set_by_uuid, set_by_name, set_at FROM server_spawn WHERE id = 1",
                rs -> {
                    String worldName = rs.getString("world");
                    if (worldName == null) return null;
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) return null;
                    Location location = new Location(world, rs.getDouble("x"), rs.getDouble("y"),
                            rs.getDouble("z"), (float) rs.getDouble("yaw"), (float) rs.getDouble("pitch"));
                    UUID setBy = null;
                    String rawUuid = rs.getString("set_by_uuid");
                    if (rawUuid != null && !rawUuid.isEmpty()) {
                        try { setBy = UUID.fromString(rawUuid); }
                        catch (IllegalArgumentException ignored) { }
                    }
                    return new SpawnInfo(location, setBy, rs.getString("set_by_name"), rs.getString("set_at"));
                }).orElse(null);
    }

    public void deleteSpawn() {
        if (!store.isAvailable()) return;
        store.executeUpdate("DELETE FROM server_spawn WHERE id = 1");
    }

    // ---- back ----

    public void setBack(UUID uuid, Location location) {
        if (!store.isAvailable() || uuid == null || location == null || location.getWorld() == null) return;
        store.executeUpdate(
                "INSERT INTO back_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y," +
                        " z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch",
                uuid, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public Location getBack(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return null;
        return store.queryFirst(
                "SELECT world, x, y, z, yaw, pitch FROM back_locations WHERE uuid = ?",
                rs -> {
                    String worldName = rs.getString("world");
                    if (worldName == null) return null;
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) return null;
                    return new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            (float) rs.getDouble("yaw"), (float) rs.getDouble("pitch"));
                }, uuid).orElse(null);
    }

    // ---- homes ----

    public void setHome(UUID uuid, String homeName, Location location) {
        if (!store.isAvailable() || uuid == null || location == null || location.getWorld() == null) return;
        String key = formatHomeKey(homeName);
        store.executeUpdate(
                "INSERT INTO homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)" +
                        " ON CONFLICT(uuid, name) DO UPDATE SET world = excluded.world, x = excluded.x," +
                        " y = excluded.y, z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch",
                uuid, key, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public boolean deleteHome(UUID uuid, String homeName) {
        if (!store.isAvailable() || uuid == null) return false;
        String key = formatHomeKey(homeName);
        // Use queryFirst as an existence probe so we can return the same boolean
        // contract the previous YAML implementation exposed.
        boolean present = store.queryFirst("SELECT 1 FROM homes WHERE uuid = ? AND name = ?",
                rs -> rs.getInt(1), uuid, key).isPresent();
        if (!present) return false;
        store.executeUpdate("DELETE FROM homes WHERE uuid = ? AND name = ?", uuid, key);
        return true;
    }

    public Location getHome(UUID uuid, String homeName) {
        if (uuid == null || !store.isAvailable()) return null;
        String key = formatHomeKey(homeName);
        return store.queryFirst(
                "SELECT world, x, y, z, yaw, pitch FROM homes WHERE uuid = ? AND name = ?",
                rs -> {
                    String worldName = rs.getString("world");
                    if (worldName == null) return null;
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) return null;
                    return new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            (float) rs.getDouble("yaw"), (float) rs.getDouble("pitch"));
                }, uuid, key).orElse(null);
    }

    public Set<String> getHomes(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return Collections.emptySet();
        Set<String> names = new LinkedHashSet<>();
        for (String name : store.queryAll("SELECT name FROM homes WHERE uuid = ? ORDER BY name",
                rs -> rs.getString("name"), uuid)) {
            if (name != null) names.add(name);
        }
        return names;
    }

    public int countHomes(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return 0;
        return store.queryFirst("SELECT COUNT(*) AS c FROM homes WHERE uuid = ?",
                rs -> rs.getInt("c"), uuid).orElse(0);
    }

    private String formatHomeKey(String homeName) {
        if (homeName == null || homeName.trim().isEmpty()) {
            return "home";
        }
        return homeName.toLowerCase().trim();
    }

    public static final class SpawnInfo {
        private final Location location;
        private final UUID setBy;
        private final String setByName;
        private final String setAt;

        public SpawnInfo(Location location, UUID setBy, String setByName, String setAt) {
            this.location = location;
            this.setBy = setBy;
            this.setByName = setByName;
            this.setAt = setAt;
        }

        public Location getLocation() { return location; }
        public UUID getSetBy() { return setBy; }
        public String getSetByName() { return setByName; }
        public String getSetAt() { return setAt; }
    }
}
