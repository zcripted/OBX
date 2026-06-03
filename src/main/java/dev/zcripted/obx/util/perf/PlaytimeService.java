package dev.zcripted.obx.util.perf;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.storage.SqliteDataStore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeService implements Listener {

    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")
            .withZone(ZoneId.of("America/Detroit"));

    private final Main plugin;
    private final SqliteDataStore store;
    private final Map<UUID, Long> sessionStarts = new ConcurrentHashMap<>();

    public PlaytimeService(Main plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            plugin.getLogger().warning("PlaytimeService disabled — SQLite store unavailable.");
            return;
        }
        store.execute("CREATE TABLE IF NOT EXISTS playtime (" +
                "uuid TEXT PRIMARY KEY," +
                "name TEXT," +
                "total_seconds INTEGER NOT NULL DEFAULT 0," +
                "first_seen TEXT," +
                "last_seen TEXT" +
                ")");
        migrateLegacyYaml();
        long now = System.currentTimeMillis();
        for (Player online : Bukkit.getOnlinePlayers()) {
            sessionStarts.putIfAbsent(online.getUniqueId(), now);
        }
    }

    public void reload() { load(); }

    public void save() {
        flushAll();
    }

    private void migrateLegacyYaml() {
        File legacy = new File(plugin.getDataFolder(), "playtime.yml");
        if (!legacy.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            renameMigrated(legacy);
            return;
        }
        int migrated = 0;
        for (String key : players.getKeys(false)) {
            ConfigurationSection entry = players.getConfigurationSection(key);
            if (entry == null) continue;
            try {
                UUID uuid = UUID.fromString(key);
                upsert(uuid, entry.getString("name"),
                        entry.getLong("total-seconds", 0L),
                        entry.getString("first-seen"),
                        entry.getString("last-seen"));
                migrated++;
            } catch (IllegalArgumentException ignored) { /* skip malformed */ }
        }
        plugin.getLogger().info("Migrated " + migrated + " playtime row(s) from playtime.yml.");
        renameMigrated(legacy);
    }

    private void renameMigrated(File legacy) {
        File renamed = new File(legacy.getParentFile(), legacy.getName() + ".migrated");
        if (!legacy.renameTo(renamed)) {
            plugin.getLogger().warning("Could not rename " + legacy.getName() + " after migration.");
        }
    }

    private void upsert(UUID uuid, String name, long totalSeconds, String firstSeen, String lastSeen) {
        store.executeUpdate(
                "INSERT INTO playtime (uuid, name, total_seconds, first_seen, last_seen) VALUES (?, ?, ?, ?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, total_seconds=excluded.total_seconds," +
                        " first_seen=COALESCE(excluded.first_seen, first_seen), last_seen=excluded.last_seen",
                uuid, name, totalSeconds, firstSeen, lastSeen);
    }

    public long getTotalPlaytimeSeconds(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return 0L;
        long stored = store.queryFirst("SELECT total_seconds FROM playtime WHERE uuid = ?",
                rs -> rs.getLong("total_seconds"), uuid).orElse(0L);
        Long start = sessionStarts.get(uuid);
        if (start != null) {
            stored += (System.currentTimeMillis() - start) / 1000L;
        }
        return stored;
    }

    public long getSessionSeconds(UUID uuid) {
        Long start = sessionStarts.get(uuid);
        return start == null ? 0L : (System.currentTimeMillis() - start) / 1000L;
    }

    public boolean hasSeen(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return false;
        return store.queryFirst("SELECT 1 FROM playtime WHERE uuid = ?", rs -> rs.getInt(1), uuid).isPresent();
    }

    public String getFirstSeen(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return null;
        return store.queryFirst("SELECT first_seen FROM playtime WHERE uuid = ?",
                rs -> rs.getString("first_seen"), uuid).orElse(null);
    }

    public String getLastSeen(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return null;
        return store.queryFirst("SELECT last_seen FROM playtime WHERE uuid = ?",
                rs -> rs.getString("last_seen"), uuid).orElse(null);
    }

    public String getLastKnownName(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return null;
        return store.queryFirst("SELECT name FROM playtime WHERE uuid = ?",
                rs -> rs.getString("name"), uuid).orElse(null);
    }

    public UUID findUuidByName(String name) {
        if (name == null || !store.isAvailable()) return null;
        return store.queryFirst("SELECT uuid FROM playtime WHERE LOWER(name) = LOWER(?)",
                rs -> UUID.fromString(rs.getString("uuid")), name).orElse(null);
    }

    public String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0L) return "0s";
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder builder = new StringBuilder();
        if (days > 0L) builder.append(days).append("d ");
        if (hours > 0L) builder.append(hours).append("h ");
        if (minutes > 0L) builder.append(minutes).append("m ");
        if (seconds > 0L && days == 0L) builder.append(seconds).append("s");
        String result = builder.toString().trim();
        return result.isEmpty() ? "0s" : result;
    }

    private void flushAll() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : sessionStarts.entrySet()) {
            UUID uuid = entry.getKey();
            long start = entry.getValue();
            long elapsedSeconds = (now - start) / 1000L;
            if (elapsedSeconds <= 0L) continue;
            String lastSeen = DISPLAY_TIMESTAMP.format(Instant.now());
            store.executeUpdate(
                    "UPDATE playtime SET total_seconds = total_seconds + ?, last_seen = ? WHERE uuid = ?",
                    elapsedSeconds, lastSeen, uuid);
            sessionStarts.put(uuid, now);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        sessionStarts.put(uuid, now);
        String firstSeen = DISPLAY_TIMESTAMP.format(Instant.now());
        store.executeUpdateAsync(
                "INSERT INTO playtime (uuid, name, total_seconds, first_seen, last_seen) VALUES (?, ?, 0, ?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, last_seen=excluded.last_seen",
                uuid, player.getName(), firstSeen, firstSeen);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Long start = sessionStarts.remove(uuid);
        if (start == null) return;
        long elapsedSeconds = (System.currentTimeMillis() - start) / 1000L;
        String lastSeen = DISPLAY_TIMESTAMP.format(Instant.now());
        if (elapsedSeconds > 0L) {
            store.executeUpdateAsync(
                    "UPDATE playtime SET total_seconds = total_seconds + ?, name = ?, last_seen = ? WHERE uuid = ?",
                    elapsedSeconds, player.getName(), lastSeen, uuid);
        } else {
            store.executeUpdateAsync(
                    "UPDATE playtime SET name = ?, last_seen = ? WHERE uuid = ?",
                    player.getName(), lastSeen, uuid);
        }
    }

    public OfflinePlayer offline(UUID uuid) {
        return uuid == null ? null : plugin.getServer().getOfflinePlayer(uuid);
    }
}
