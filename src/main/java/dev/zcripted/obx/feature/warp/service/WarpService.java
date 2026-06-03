package dev.zcripted.obx.feature.warp.service;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.perf.AsyncYamlSaver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class WarpService {

    private static final int MAX_NAME_LENGTH = 32;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1," + MAX_NAME_LENGTH + "}$");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    private final OBX plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private AsyncYamlSaver saver;

    /**
     * Snapshot of the parsed warp set. Rebuilt only on {@link #load()} and after every
     * mutating operation in this class. Hot-path callers (tab completion on /warp, the
     * warp GUI category counter, listVisibleWarpNames) read straight from this map and
     * never re-parse the YAML. The map iteration order is the insertion order from the
     * YAML, so display order stays stable across reloads.
     */
    private volatile Map<String, WarpEntry> warpsCache = Collections.emptyMap();

    public WarpService(OBX plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "warps.yml");
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
            if (!data.isConfigurationSection("warps")) {
                data.createSection("warps");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load warps.yml: " + e.getMessage());
        }
        saver = data == null ? null : new AsyncYamlSaver(plugin, data, dataFile, "warps.yml");
        rebuildCache();
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

    /** Re-derives the cached warp map from {@link #data}. Called only on cold/mutation paths. */
    private void rebuildCache() {
        if (data == null) {
            warpsCache = Collections.emptyMap();
            return;
        }
        ConfigurationSection section = data.getConfigurationSection("warps");
        if (section == null) {
            warpsCache = Collections.emptyMap();
            return;
        }
        java.util.Set<String> keys = section.getKeys(false);
        if (keys.isEmpty()) {
            warpsCache = Collections.emptyMap();
            return;
        }
        Map<String, WarpEntry> warps = new LinkedHashMap<>(keys.size());
        for (String key : keys) {
            WarpEntry entry = readWarp(key, section.getConfigurationSection(key));
            if (entry != null) {
                warps.put(key, entry);
            }
        }
        warpsCache = Collections.unmodifiableMap(warps);
    }

    public Map<String, WarpEntry> getWarps() {
        return warpsCache;
    }

    public WarpEntry getWarp(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return null;
        }
        return warpsCache.get(normalized);
    }

    public boolean setWarp(String name, Location location, String category, String icon, boolean isPublic, String permission, UUID setBy, String setByName) {
        if (data == null || location == null) {
            return false;
        }
        String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }
        ConfigurationSection warps = data.getConfigurationSection("warps");
        if (warps == null) {
            warps = data.createSection("warps");
        }
        ConfigurationSection section = warps.createSection(normalized);
        section.set("name", name);
        section.set("world", location.getWorld() == null ? "world" : location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
        section.set("category", category == null ? "general" : category);
        if (icon != null && Material.matchMaterial(icon) != null) {
            section.set("icon", icon.toUpperCase(Locale.ENGLISH));
        }
        section.set("public", isPublic);
        if (permission != null && !permission.isEmpty()) {
            section.set("permission", permission);
        }
        if (setBy != null) {
            section.set("setBy", setBy.toString());
        }
        if (setByName != null) {
            section.set("setByName", setByName);
        }
        section.set("setAt", DATE_FORMAT.format(Instant.now()));
        rebuildCache();
        markDirty();
        return true;
    }

    public boolean deleteWarp(String name) {
        if (data == null) {
            return false;
        }
        String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }
        String path = "warps." + normalized;
        if (!data.contains(path)) {
            return false;
        }
        data.set(path, null);
        rebuildCache();
        markDirty();
        return true;
    }

    public boolean renameWarp(String oldName, String newName) {
        if (data == null) {
            return false;
        }
        String oldKey = normalizeName(oldName);
        String newKey = normalizeName(newName);
        if (oldKey == null || newKey == null) {
            return false;
        }
        ConfigurationSection warps = data.getConfigurationSection("warps");
        if (warps == null || !warps.isConfigurationSection(oldKey)) {
            return false;
        }
        ConfigurationSection existing = warps.getConfigurationSection(oldKey);
        Map<String, Object> values = existing.getValues(true);
        warps.set(oldKey, null);
        ConfigurationSection newSection = warps.createSection(newKey);
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            newSection.set(entry.getKey(), entry.getValue());
        }
        newSection.set("name", newName);
        rebuildCache();
        markDirty();
        return true;
    }

    public boolean moveWarp(String name, Location location, UUID setBy, String setByName) {
        if (data == null || location == null) {
            return false;
        }
        String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }
        ConfigurationSection section = data.getConfigurationSection("warps." + normalized);
        if (section == null) {
            return false;
        }
        section.set("world", location.getWorld() == null ? "world" : location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
        if (setBy != null) {
            section.set("setBy", setBy.toString());
        }
        if (setByName != null) {
            section.set("setByName", setByName);
        }
        section.set("setAt", DATE_FORMAT.format(Instant.now()));
        rebuildCache();
        markDirty();
        return true;
    }

    public boolean setIcon(String name, String icon) {
        if (data == null) {
            return false;
        }
        String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }
        ConfigurationSection section = data.getConfigurationSection("warps." + normalized);
        if (section == null) {
            return false;
        }
        if (icon != null && Material.matchMaterial(icon) != null) {
            section.set("icon", icon.toUpperCase(Locale.ENGLISH));
        } else {
            section.set("icon", null);
        }
        rebuildCache();
        markDirty();
        return true;
    }

    public boolean setPublic(String name, boolean isPublic) {
        if (data == null) {
            return false;
        }
        String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }
        ConfigurationSection section = data.getConfigurationSection("warps." + normalized);
        if (section == null) {
            return false;
        }
        section.set("public", isPublic);
        rebuildCache();
        markDirty();
        return true;
    }

    public List<String> listWarpNames() {
        Map<String, WarpEntry> warps = warpsCache;
        if (warps.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>(warps.keySet());
        Collections.sort(names);
        return names;
    }

    public List<String> listVisibleWarpNames(Player viewer, boolean includeHidden) {
        Map<String, WarpEntry> warps = warpsCache;
        if (warps.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>(warps.size());
        for (WarpEntry entry : warps.values()) {
            if (!includeHidden && !entry.isPublic() && (viewer == null || !viewer.hasPermission("obx.warp.hidden.view"))) {
                continue;
            }
            if (viewer != null && entry.getPermission() != null && !entry.getPermission().isEmpty() && !viewer.hasPermission(entry.getPermission())) {
                continue;
            }
            names.add(entry.getName());
        }
        Collections.sort(names);
        return names;
    }

    public Map<String, Integer> categoryCounts() {
        Map<String, WarpEntry> warps = warpsCache;
        if (warps.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (WarpEntry entry : warps.values()) {
            String category = entry.getCategory();
            counts.merge(category, 1, Integer::sum);
        }
        return counts;
    }

    public List<String> categories() {
        Map<String, Integer> counts = categoryCounts();
        if (counts.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> cats = new ArrayList<>(counts.keySet());
        Collections.sort(cats);
        return cats;
    }

    private WarpEntry readWarp(String key, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String world = section.getString("world");
        Location location = null;
        if (world != null && Bukkit.getWorld(world) != null) {
            location = new Location(Bukkit.getWorld(world),
                    section.getDouble("x"),
                    section.getDouble("y"),
                    section.getDouble("z"),
                    (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch"));
        }
        String displayName = section.getString("name", key);
        String category = section.getString("category", "general");
        String icon = section.getString("icon");
        boolean isPublic = section.getBoolean("public", true);
        String permission = section.getString("permission");
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
        return new WarpEntry(key, displayName, location, category, icon, isPublic, permission, setBy, setByName, setAt);
    }

    public String normalizeName(String input) {
        if (input == null) {
            return null;
        }
        String cleaned = input.trim();
        if (cleaned.isEmpty() || cleaned.length() > MAX_NAME_LENGTH) {
            return null;
        }
        if (!NAME_PATTERN.matcher(cleaned).matches()) {
            return null;
        }
        return cleaned.toLowerCase(Locale.ENGLISH);
    }

    public static final class WarpEntry {
        private final String key;
        private final String name;
        private final Location location;
        private final String category;
        private final String icon;
        private final boolean isPublic;
        private final String permission;
        private final UUID setBy;
        private final String setByName;
        private final String setAt;

        public WarpEntry(String key, String name, Location location, String category, String icon, boolean isPublic, String permission, UUID setBy, String setByName, String setAt) {
            this.key = key;
            this.name = name;
            this.location = location;
            this.category = category == null ? "general" : category;
            this.icon = icon;
            this.isPublic = isPublic;
            this.permission = permission;
            this.setBy = setBy;
            this.setByName = setByName;
            this.setAt = setAt;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public Location getLocation() {
            return location;
        }

        public String getCategory() {
            return category;
        }

        public String getIcon() {
            return icon;
        }

        public boolean isPublic() {
            return isPublic;
        }

        public String getPermission() {
            return permission;
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
