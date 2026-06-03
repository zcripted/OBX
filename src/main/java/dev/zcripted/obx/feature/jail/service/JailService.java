package dev.zcripted.obx.feature.jail.service;

import dev.zcripted.obx.api.jail.Jail;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.LocationSerializer;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class JailService implements dev.zcripted.obx.api.jail.JailApi {

    public static final class JailState {
        private final UUID uuid;
        private final String jailName;
        private final long jailedAt;
        private final long durationSeconds;
        private final String reason;

        public JailState(UUID uuid, String jailName, long jailedAt, long durationSeconds, String reason) {
            this.uuid = uuid;
            this.jailName = jailName;
            this.jailedAt = jailedAt;
            this.durationSeconds = durationSeconds;
            this.reason = reason;
        }

        public UUID getUuid() { return uuid; }
        public String getJailName() { return jailName; }
        public long getJailedAt() { return jailedAt; }
        public long getDurationSeconds() { return durationSeconds; }
        public String getReason() { return reason; }

        public boolean isExpired() {
            if (durationSeconds <= 0) return false; // 0 = permanent
            return System.currentTimeMillis() / 1000L >= (jailedAt / 1000L) + durationSeconds;
        }

        public long getSecondsRemaining() {
            if (durationSeconds <= 0) return -1L;
            long elapsed = (System.currentTimeMillis() - jailedAt) / 1000L;
            return Math.max(0L, durationSeconds - elapsed);
        }
    }

    private final ObxPlugin plugin;
    private final SqliteDataStore store;
    private final File jailsFile;
    private YamlConfiguration jailsConfig;
    private final Map<String, Jail> jails = new LinkedHashMap<>();

    public JailService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
        this.jailsFile = new File(plugin.getDataFolder(), "jails.yml");
    }

    public void load() {
        if (store.isAvailable()) {
            store.execute("CREATE TABLE IF NOT EXISTS jail_state (" +
                    "uuid TEXT PRIMARY KEY," +
                    "jail_name TEXT NOT NULL," +
                    "jailed_at INTEGER NOT NULL," +
                    "duration_seconds INTEGER NOT NULL DEFAULT 0," +
                    "reason TEXT)");
        }
        try {
            if (!jailsFile.exists()) {
                if (jailsFile.getParentFile() != null && !jailsFile.getParentFile().exists()) {
                    jailsFile.getParentFile().mkdirs();
                }
                try { plugin.saveResource("jails.yml", false); }
                catch (IllegalArgumentException ignored) { jailsFile.createNewFile(); }
            }
            jailsConfig = YamlConfiguration.loadConfiguration(jailsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to load jails.yml: " + exception.getMessage());
        }
        parseJails();
    }

    public void reload() { load(); }

    private void parseJails() {
        jails.clear();
        if (jailsConfig == null) return;
        ConfigurationSection section = jailsConfig.getConfigurationSection("jails");
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            ConfigurationSection sub = section.getConfigurationSection(name);
            Location location = LocationSerializer.deserialize(sub, plugin);
            if (location != null) {
                jails.put(name.toLowerCase(), new Jail(name, location));
            }
        }
    }

    public Jail getJail(String name) {
        return name == null ? null : jails.get(name.toLowerCase());
    }

    public Collection<Jail> getJails() {
        return Collections.unmodifiableCollection(jails.values());
    }

    public boolean createJail(String name, Location location) {
        if (name == null || location == null) return false;
        if (jailsConfig == null) return false;
        ConfigurationSection root = jailsConfig.getConfigurationSection("jails");
        if (root == null) root = jailsConfig.createSection("jails");
        ConfigurationSection sub = root.createSection(name);
        LocationSerializer.serialize(sub, location);
        try { jailsConfig.save(jailsFile); }
        catch (IOException exception) {
            plugin.getLogger().warning("Failed to save jails.yml: " + exception.getMessage());
            return false;
        }
        jails.put(name.toLowerCase(), new Jail(name, location));
        return true;
    }

    public boolean deleteJail(String name) {
        if (name == null) return false;
        if (jails.remove(name.toLowerCase()) == null) return false;
        if (jailsConfig != null) {
            ConfigurationSection root = jailsConfig.getConfigurationSection("jails");
            if (root != null) {
                root.set(name, null);
                try { jailsConfig.save(jailsFile); }
                catch (IOException exception) {
                    plugin.getLogger().warning("Failed to save jails.yml: " + exception.getMessage());
                }
            }
        }
        return true;
    }

    public JailState getState(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return null;
        return store.queryFirst(
                "SELECT jail_name, jailed_at, duration_seconds, reason FROM jail_state WHERE uuid = ?",
                rs -> new JailState(uuid, rs.getString("jail_name"), rs.getLong("jailed_at"),
                        rs.getLong("duration_seconds"), rs.getString("reason")),
                uuid).orElse(null);
    }

    public boolean isJailed(UUID uuid) {
        JailState state = getState(uuid);
        if (state == null) return false;
        if (state.isExpired()) {
            clearState(uuid);
            return false;
        }
        return true;
    }

    public void jail(UUID uuid, String jailName, long durationSeconds, String reason) {
        if (uuid == null || jailName == null || !store.isAvailable()) return;
        store.executeUpdate(
                "INSERT INTO jail_state (uuid, jail_name, jailed_at, duration_seconds, reason) VALUES (?, ?, ?, ?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET jail_name = excluded.jail_name, jailed_at = excluded.jailed_at," +
                        " duration_seconds = excluded.duration_seconds, reason = excluded.reason",
                uuid, jailName.toLowerCase(), System.currentTimeMillis(), durationSeconds, reason);
    }

    public void clearState(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return;
        store.executeUpdateAsync("DELETE FROM jail_state WHERE uuid = ?", uuid);
    }

    /** Best-effort send to the jail anchor; falls back to whatever location is configured. */
    public boolean teleportToJail(org.bukkit.entity.Player player) {
        if (player == null) return false;
        JailState state = getState(player.getUniqueId());
        if (state == null) return false;
        Jail jail = getJail(state.getJailName());
        if (jail == null || jail.getLocation() == null) return false;
        plugin.getTeleportManager().teleportPlayer(player, jail.getLocation(),
                "jail.teleporting", dev.zcripted.obx.util.text.Placeholders.with("jail", jail.getName()));
        return true;
    }

    public String formatDuration(long seconds) {
        if (seconds <= 0) return "∞";
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long rem = seconds % 60L;
        StringBuilder builder = new StringBuilder();
        if (days > 0L) builder.append(days).append("d ");
        if (hours > 0L) builder.append(hours).append("h ");
        if (minutes > 0L) builder.append(minutes).append("m ");
        if (rem > 0L && days == 0L) builder.append(rem).append("s");
        String result = builder.toString().trim();
        return result.isEmpty() ? "0s" : result;
    }

    public Long parseDuration(String input) {
        if (input == null || input.isEmpty()) return 0L;
        String trimmed = input.trim().toLowerCase();
        if (trimmed.equals("perm") || trimmed.equals("permanent") || trimmed.equals("forever")) {
            return 0L;
        }
        long total = 0L;
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                long value;
                try { value = Long.parseLong(digits.toString()); }
                catch (NumberFormatException ignored) { return null; }
                digits.setLength(0);
                switch (c) {
                    case 'd': total += value * 86400L; break;
                    case 'h': total += value * 3600L; break;
                    case 'm': total += value * 60L; break;
                    case 's': total += value; break;
                    default: return null;
                }
            }
        }
        if (digits.length() > 0) {
            try { total += Long.parseLong(digits.toString()); }
            catch (NumberFormatException ignored) { return null; }
        }
        return total;
    }

    public java.util.UUID resolveOfflineUuid(String name) {
        if (name == null) return null;
        // Try online first.
        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) return online.getUniqueId();
        }
        org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.getUniqueId();
    }
}
