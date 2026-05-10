package dev.sergeantfuzzy.sfcore.storage;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.util.perf.AsyncYamlSaver;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class DataService {

    private final Main plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private AsyncYamlSaver saver;

    public DataService(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        try {
            if (!dataFile.exists()) {
                if (dataFile.getParentFile() != null) {
                    dataFile.getParentFile().mkdirs();
                }
                dataFile.createNewFile();
            }
            data = YamlConfiguration.loadConfiguration(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load data.yml: " + e.getMessage());
        }
        saver = data == null ? null : new AsyncYamlSaver(plugin, data, dataFile, "data.yml");
    }

    public void reload() {
        load();
    }

    /** Synchronous flush — use from disable/reload paths only. */
    public void save() {
        if (saver != null) {
            saver.flushNow();
        }
    }

    private void markDirty() {
        if (saver != null) {
            saver.markDirty();
        }
    }

    public void setSpawn(Location location) {
        setSpawn(location, null, null, null);
    }

    public void setSpawn(Location location, UUID setter, String setterName, String setAt) {
        if (data == null || location == null) {
            return;
        }
        data.set("spawn", null);
        ConfigurationSection section = data.createSection("spawn");
        LocationSerializer.serialize(section, location);
        if (setter != null) {
            section.set("setBy", setter.toString());
        }
        if (setterName != null) {
            section.set("setByName", setterName);
        }
        if (setAt != null) {
            section.set("setAt", setAt);
        }
        markDirty();
    }

    public Location getSpawn() {
        SpawnInfo info = getSpawnInfo();
        return info == null ? null : info.getLocation();
    }

    public SpawnInfo getSpawnInfo() {
        if (data == null) {
            return null;
        }
        ConfigurationSection section = data.getConfigurationSection("spawn");
        Location location = LocationSerializer.deserialize(section, plugin);
        if (location == null || section == null) {
            return null;
        }
        UUID setBy = null;
        String setByRaw = section.getString("setBy");
        if (setByRaw != null && !setByRaw.isEmpty()) {
            try {
                setBy = UUID.fromString(setByRaw);
            } catch (IllegalArgumentException ignored) {
            }
        }
        String setByName = section.getString("setByName");
        String setAt = section.getString("setAt");
        return new SpawnInfo(location, setBy, setByName, setAt);
    }

    public void deleteSpawn() {
        if (data == null) {
            return;
        }
        data.set("spawn", null);
        markDirty();
    }



    public void setBack(UUID uuid, Location location) {
        if (data == null || location == null) {
            return;
        }
        ConfigurationSection player = getPlayerSection(uuid);
        player.set("back", null);
        ConfigurationSection section = player.createSection("back");
        LocationSerializer.serialize(section, location);
        markDirty();
    }

    public Location getBack(UUID uuid) {
        if (data == null) {
            return null;
        }
        ConfigurationSection section = data.getConfigurationSection("players." + uuid + ".back");
        return LocationSerializer.deserialize(section, plugin);
    }

    public void setHome(UUID uuid, String homeName, Location location) {
        if (data == null || location == null) {
            return;
        }
        String key = formatHomeKey(homeName);
        ConfigurationSection homes = getHomesSection(uuid);
        homes.set(key, null);
        ConfigurationSection section = homes.createSection(key);
        LocationSerializer.serialize(section, location);
        markDirty();
    }

    public boolean deleteHome(UUID uuid, String homeName) {
        if (data == null) {
            return false;
        }
        String key = formatHomeKey(homeName);
        String path = "players." + uuid + ".homes." + key;
        if (!data.contains(path)) {
            return false;
        }
        data.set(path, null);
        markDirty();
        return true;
    }

    public Location getHome(UUID uuid, String homeName) {
        if (data == null) {
            return null;
        }
        String key = formatHomeKey(homeName);
        ConfigurationSection section = data.getConfigurationSection("players." + uuid + ".homes." + key);
        return LocationSerializer.deserialize(section, plugin);
    }

    public Set<String> getHomes(UUID uuid) {
        if (data == null) {
            return Collections.emptySet();
        }
        ConfigurationSection section = data.getConfigurationSection("players." + uuid + ".homes");
        if (section == null) {
            return Collections.emptySet();
        }
        return section.getKeys(false);
    }

    public int countHomes(UUID uuid) {
        return getHomes(uuid).size();
    }

    private ConfigurationSection getPlayerSection(UUID uuid) {
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            players = data.createSection("players");
        }
        ConfigurationSection player = players.getConfigurationSection(uuid.toString());
        if (player == null) {
            player = players.createSection(uuid.toString());
        }
        return player;
    }

    private ConfigurationSection getHomesSection(UUID uuid) {
        ConfigurationSection player = getPlayerSection(uuid);
        ConfigurationSection homes = player.getConfigurationSection("homes");
        if (homes == null) {
            homes = player.createSection("homes");
        }
        return homes;
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

        public Location getLocation() {
            return location;
        }

        public UUID getSetBy() {
            return setBy;
        }

        public String getSetByName() {
            return setByName;
        }

        public String getSetAt() {
            return setAt;
        }
    }
}
