package dev.zcripted.obx.feature.playerinfo.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeService implements Listener {

    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")
            .withZone(ZoneId.of("America/Detroit"));

    private final ObxPlugin plugin;
    private final SqliteDataStore store;
    /** Flush anchor — reset to "now" on every periodic flush so total = stored + (now - anchor). */
    private final Map<UUID, Long> sessionStarts = new ConcurrentHashMap<>();
    /** True join time of the current session — NEVER reset by a flush, so session length stays accurate. */
    private final Map<UUID, Long> sessionJoins = new ConcurrentHashMap<>();
    /** Persist accumulated session time every 5 minutes so a crash loses at most one interval, not whole sessions. */
    private static final long FLUSH_INTERVAL_TICKS = 6000L;
    private dev.zcripted.obx.core.platform.scheduler.CancellableTask flushTask;

    public PlaytimeService(ObxPlugin plugin) {
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
                "last_seen TEXT," +
                "longest_session_seconds INTEGER NOT NULL DEFAULT 0," +
                "longest_session_at INTEGER NOT NULL DEFAULT 0" +
                ")");
        // Additive migration for tables created before the longest-session columns existed.
        ensureColumn("longest_session_seconds", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("longest_session_at", "INTEGER NOT NULL DEFAULT 0");
        migrateLegacyYaml();
        long now = System.currentTimeMillis();
        for (Player online : Bukkit.getOnlinePlayers()) {
            sessionStarts.putIfAbsent(online.getUniqueId(), now);
            sessionJoins.putIfAbsent(online.getUniqueId(), now);
        }
        startFlushTask();
    }

    public void reload() { load(); }

    public void save() {
        stopFlushTask();
        flushAll();
    }

    /** (Re)starts the periodic background flush so accumulated playtime survives a crash. */
    private void startFlushTask() {
        stopFlushTask();
        if (!store.isAvailable() || plugin.getSchedulerAdapter() == null) {
            return;
        }
        // The repeating tick runs on the main/global thread but immediately hands the DB work to the
        // async pool — flushAll only touches the (thread-safe) store and concurrent maps, no Bukkit API.
        flushTask = plugin.getSchedulerAdapter().runRepeating(
                () -> plugin.getSchedulerAdapter().runAsync(this::flushAll),
                FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS);
    }

    private void stopFlushTask() {
        if (flushTask != null) {
            try {
                flushTask.cancel();
            } catch (Throwable ignored) {
                // already cancelled
            }
            flushTask = null;
        }
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

    /** Adds {@code column} to the playtime table if it isn't already present (SQLite-safe migration). */
    private void ensureColumn(String column, String definition) {
        boolean exists = store.queryFirst(
                "SELECT COUNT(*) AS c FROM pragma_table_info('playtime') WHERE name = ?",
                rs -> rs.getInt("c") > 0, column).orElse(false);
        if (!exists) {
            store.execute("ALTER TABLE playtime ADD COLUMN " + column + " " + definition);
        }
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
        // Use the true join time (not the flush anchor, which is reset every flush) so the session
        // length reflects the whole session, not just the time since the last periodic flush.
        Long start = sessionJoins.get(uuid);
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

    /**
     * Full duration breakdown (years, months, days, hours, minutes, seconds) showing only the
     * relevant (non-zero) fields — e.g. {@code 23h 14m 7s} for under a day, {@code 2mo 4d 3h} for
     * longer spans. Months are approximated at 30 days and years at 365 days.
     */
    public String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0L) return "0s";
        long years = totalSeconds / 31_536_000L; // 365d
        totalSeconds %= 31_536_000L;
        long months = totalSeconds / 2_592_000L; // 30d
        totalSeconds %= 2_592_000L;
        long days = totalSeconds / 86_400L;
        totalSeconds %= 86_400L;
        long hours = totalSeconds / 3_600L;
        totalSeconds %= 3_600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder builder = new StringBuilder();
        if (years > 0L) builder.append(years).append("y ");
        if (months > 0L) builder.append(months).append("mo ");
        if (days > 0L) builder.append(days).append("d ");
        if (hours > 0L) builder.append(hours).append("h ");
        if (minutes > 0L) builder.append(minutes).append("m ");
        if (seconds > 0L) builder.append(seconds).append("s");
        String result = builder.toString().trim();
        return result.isEmpty() ? "0s" : result;
    }

    private static final DateTimeFormatter LONGEST_TIME = DateTimeFormatter.ofPattern("h:mm a")
            .withZone(ZoneId.of("America/Detroit"));
    private static final DateTimeFormatter LONGEST_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy '·' h:mm a")
            .withZone(ZoneId.of("America/Detroit"));

    /** The longest single session a player has on record, plus when it was recorded (epoch millis). */
    public static final class LongestSession {
        public final long seconds;
        public final long atMillis;
        LongestSession(long seconds, long atMillis) {
            this.seconds = seconds;
            this.atMillis = atMillis;
        }
    }

    /** A single playtime leaderboard row (total seconds incl. any in-progress session). */
    public static final class PlaytimeEntry {
        private final UUID uuid;
        private final String name;
        private final long totalSeconds;
        PlaytimeEntry(UUID uuid, String name, long totalSeconds) {
            this.uuid = uuid;
            this.name = name;
            this.totalSeconds = totalSeconds;
        }
        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public long getTotalSeconds() { return totalSeconds; }
    }

    /**
     * The player's longest recorded session. If their current in-progress session already exceeds
     * the stored record, that ongoing session is reported (timestamped "now").
     */
    public LongestSession getLongestSession(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return new LongestSession(0L, 0L);
        long[] row = store.queryFirst(
                "SELECT longest_session_seconds, longest_session_at FROM playtime WHERE uuid = ?",
                rs -> new long[]{rs.getLong("longest_session_seconds"), rs.getLong("longest_session_at")},
                uuid).orElse(new long[]{0L, 0L});
        long current = getSessionSeconds(uuid);
        if (current > row[0]) {
            return new LongestSession(current, System.currentTimeMillis());
        }
        return new LongestSession(row[0], row[1]);
    }

    /** Clean, professional timestamp: same-day → "Today · 3:00 PM"; otherwise "Jun 4, 2026 · 3:00 PM". */
    public String formatSessionTimestamp(long epochMillis) {
        if (epochMillis <= 0L) return "—";
        java.time.ZonedDateTime when = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("America/Detroit"));
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(ZoneId.of("America/Detroit"));
        if (when.toLocalDate().isEqual(now.toLocalDate())) {
            return "Today · " + LONGEST_TIME.format(when);
        }
        if (when.toLocalDate().isEqual(now.toLocalDate().minusDays(1))) {
            return "Yesterday · " + LONGEST_TIME.format(when);
        }
        return LONGEST_DATE.format(when);
    }

    /** Top players by total playtime (in-progress sessions included), highest first. */
    public java.util.List<PlaytimeEntry> topPlaytimes(int limit) {
        if (!store.isAvailable()) return java.util.Collections.emptyList();
        // Fetch a buffer beyond the requested size so an online player's in-progress session can
        // still rank correctly after augmentation, then re-sort and trim.
        java.util.List<PlaytimeEntry> rows = store.queryAll(
                "SELECT uuid, name, total_seconds FROM playtime ORDER BY total_seconds DESC LIMIT 50",
                rs -> {
                    UUID uuid;
                    try { uuid = UUID.fromString(rs.getString("uuid")); }
                    catch (RuntimeException ex) { return null; }
                    String name = rs.getString("name");
                    long total = rs.getLong("total_seconds");
                    Long start = sessionStarts.get(uuid);
                    if (start != null) {
                        total += (System.currentTimeMillis() - start) / 1000L;
                    }
                    if (name == null) name = uuid.toString().substring(0, 8);
                    return new PlaytimeEntry(uuid, name, total);
                });
        rows.removeIf(e -> e == null);
        rows.sort((a, b) -> Long.compare(b.totalSeconds, a.totalSeconds));
        return limit > 0 && rows.size() > limit ? new java.util.ArrayList<>(rows.subList(0, limit)) : rows;
    }

    /** Records a session length as the new longest if it beats the stored record (atomic). */
    private void updateLongest(UUID uuid, long sessionSeconds, long atMillis) {
        if (sessionSeconds <= 0L) return;
        store.executeUpdateAsync(
                "UPDATE playtime SET longest_session_seconds = ?, longest_session_at = ? WHERE uuid = ? AND ? > longest_session_seconds",
                sessionSeconds, atMillis, uuid, sessionSeconds);
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
            Long join = sessionJoins.get(uuid);
            if (join != null) {
                updateLongest(uuid, (now - join) / 1000L, now);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        sessionStarts.put(uuid, now);
        sessionJoins.put(uuid, now);
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
        long now = System.currentTimeMillis();
        Long join = sessionJoins.remove(uuid);
        long fullSessionSeconds = join == null ? 0L : (now - join) / 1000L;
        Long start = sessionStarts.remove(uuid);
        String lastSeen = DISPLAY_TIMESTAMP.format(Instant.now());
        if (start != null) {
            long elapsedSeconds = (now - start) / 1000L;
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
        // Record the full session (join → quit) as the longest if it beats the stored record.
        updateLongest(uuid, fullSessionSeconds, now);
    }

    public OfflinePlayer offline(UUID uuid) {
        return uuid == null ? null : plugin.getServer().getOfflinePlayer(uuid);
    }
}
