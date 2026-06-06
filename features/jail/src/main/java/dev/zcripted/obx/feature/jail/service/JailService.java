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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    /** Max blocks a jailed player may stray from their anchor before being pulled back. */
    private volatile double containmentRadius = 10.0;

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
        if (jailsConfig != null) {
            // Optional top-level key in jails.yml; clamped so a jail can never be smaller than 2 blocks.
            containmentRadius = Math.max(2.0, jailsConfig.getDouble("containment-radius", 10.0));
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

    // In-memory jail-state cache so the PlayerMove handler (every step a jailed player
    // takes) never queries SQLite on the main thread. Loaded on join; evicted on quit.
    private final Map<UUID, JailState> stateCache = new ConcurrentHashMap<>();
    private final Set<UUID> notJailedCache = ConcurrentHashMap.newKeySet();

    /** Loads a player's jail state from the DB into the cache — call on join. */
    public void refreshCache(UUID uuid) {
        if (uuid == null) return;
        JailState state = queryStateFromDb(uuid);
        if (state != null) {
            stateCache.put(uuid, state);
            notJailedCache.remove(uuid);
        } else {
            stateCache.remove(uuid);
            notJailedCache.add(uuid);
        }
    }

    /** Drops a player's cached jail state — call on quit. */
    public void evictCache(UUID uuid) {
        if (uuid == null) return;
        stateCache.remove(uuid);
        notJailedCache.remove(uuid);
    }

    private JailState queryStateFromDb(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return null;
        return store.queryFirst(
                "SELECT jail_name, jailed_at, duration_seconds, reason FROM jail_state WHERE uuid = ?",
                rs -> new JailState(uuid, rs.getString("jail_name"), rs.getLong("jailed_at"),
                        rs.getLong("duration_seconds"), rs.getString("reason")),
                uuid).orElse(null);
    }

    public JailState getState(UUID uuid) {
        if (uuid == null) return null;
        JailState cached = stateCache.get(uuid);
        if (cached != null) {
            return cached;
        }
        if (notJailedCache.contains(uuid)) {
            return null; // known not jailed — no DB hit on the hot path
        }
        return queryStateFromDb(uuid); // not cached (e.g. an offline lookup) — read directly
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
        notJailedCache.remove(uuid);
        // Only hold a cache entry for online players (loaded on join, evicted on quit). Caching an
        // offline target would leak an entry that quit never fires for; their join reloads it from DB.
        if (Bukkit.getPlayer(uuid) != null) {
            stateCache.put(uuid, new JailState(uuid, jailName.toLowerCase(), System.currentTimeMillis(), durationSeconds, reason));
        } else {
            stateCache.remove(uuid);
        }
    }

    public void clearState(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return;
        // Synchronous so an unjust crash can't leave a stale jail record after release.
        store.executeUpdate("DELETE FROM jail_state WHERE uuid = ?", uuid);
        stateCache.remove(uuid);
        notJailedCache.add(uuid);
    }

    /** The jail anchor location for a (cached) jailed player, or null if not jailed / no anchor. */
    public Location getJailAnchor(UUID uuid) {
        JailState state = getState(uuid);
        if (state == null) {
            return null;
        }
        Jail jail = getJail(state.getJailName());
        return jail == null ? null : jail.getLocation();
    }

    /** How far a jailed player may stray from their anchor before being pulled back (blocks). */
    public double getContainmentRadius() {
        return containmentRadius;
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

    /**
     * Where a released player is sent: the optional {@code release-location} in jails.yml,
     * else the primary world's spawn. Returns {@code null} only if no world is loaded.
     */
    public Location getReleaseLocation() {
        if (jailsConfig != null) {
            ConfigurationSection sub = jailsConfig.getConfigurationSection("release-location");
            if (sub != null) {
                Location loc = LocationSerializer.deserialize(sub, plugin);
                if (loc != null && loc.getWorld() != null) {
                    return loc;
                }
            }
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    /**
     * Teleports a freed player out of the jail to the release location. Dispatched on the
     * player's region thread so it is correct on Folia, and immediate (no warmup) so a
     * just-released player can't be left standing inside the jail build.
     */
    public void teleportToRelease(final org.bukkit.entity.Player player) {
        if (player == null) return;
        final Location release = getReleaseLocation();
        if (release == null || release.getWorld() == null) return;
        plugin.getSchedulerAdapter().runAtEntity(player, () -> player.teleport(release));
    }

    /**
     * Releases any online player whose jail term has expired: clears their state, teleports
     * them out, and notifies them. Runs on a low-frequency sweep so an expired player is freed
     * promptly rather than only on their next movement. Reads the online-only cache, so it
     * never touches the DB for un-jailed players.
     */
    public void sweepExpired() {
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            JailState state = stateCache.get(player.getUniqueId());
            if (state != null && state.isExpired()) {
                clearState(player.getUniqueId());
                teleportToRelease(player);
                plugin.getLanguageManager().send(player, "jail.expired");
            }
        }
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

    /** Cap on a parsed jail duration (~100 years, seconds) so an oversized value can't overflow
     *  long and wrap negative — which JailState.isExpired() would misread as already-expired and
     *  free the player instantly. Anything larger saturates here. */
    private static final long MAX_DURATION_SECONDS = 100L * 365L * 86400L;

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
                catch (NumberFormatException tooLarge) { return MAX_DURATION_SECONDS; }
                digits.setLength(0);
                long unit;
                switch (c) {
                    case 'd': unit = 86400L; break;
                    case 'h': unit = 3600L; break;
                    case 'm': unit = 60L; break;
                    case 's': unit = 1L; break;
                    default: return null;
                }
                // Saturating multiply+add: an oversized component clamps to the cap instead of
                // wrapping negative.
                try { total = Math.addExact(total, Math.multiplyExact(value, unit)); }
                catch (ArithmeticException overflow) { return MAX_DURATION_SECONDS; }
                if (total >= MAX_DURATION_SECONDS) return MAX_DURATION_SECONDS;
            }
        }
        if (digits.length() > 0) {
            try { total = Math.addExact(total, Long.parseLong(digits.toString())); }
            catch (NumberFormatException tooLarge) { return MAX_DURATION_SECONDS; }
            catch (ArithmeticException overflow) { return MAX_DURATION_SECONDS; }
            if (total >= MAX_DURATION_SECONDS) return MAX_DURATION_SECONDS;
        }
        return total;
    }

    public java.util.UUID resolveOfflineUuid(String name) {
        if (name == null) return null;
        // Try online first.
        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) return online.getUniqueId();
        }
        // Paper: non-blocking cached lookup — avoids a synchronous Mojang web request that
        // would hang the main thread for an un-cached name. Returns null when not cached.
        try {
            Object cached = Bukkit.class.getMethod("getOfflinePlayerIfCached", String.class).invoke(null, name);
            if (cached == null) {
                return null;
            }
            return (java.util.UUID) cached.getClass().getMethod("getUniqueId").invoke(cached);
        } catch (Throwable noPaperApi) {
            // Older/non-Paper forks: fall back to the (potentially blocking) lookup.
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            return offline.getUniqueId();
        }
    }
}
