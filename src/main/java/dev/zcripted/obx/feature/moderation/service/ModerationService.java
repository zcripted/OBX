package dev.zcripted.obx.feature.moderation.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Moderation profile + history store. The legacy YAML schema is migrated to
 * SQLite on first boot (and the file renamed). All external API entry points
 * stayed the same so command/GUI callers don't need updates.
 */
public class ModerationService implements dev.zcripted.obx.api.moderation.ModerationApi {
    private static final String DEFAULT_REASON = "No reason provided.";
    private static final String DEFAULT_TEMPBAN_DURATION = "7d";
    private static final long DEFAULT_TEMPBAN_MILLIS = 7L * 24L * 60L * 60L * 1000L;
    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")
            .withZone(ZoneId.of("America/Detroit"));

    private final ObxPlugin plugin;
    private final SqliteDataStore store;
    private boolean missingWebhookWarningSent;

    public ModerationService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            plugin.getLogger().severe("ModerationService disabled — SQLite store unavailable.");
            return;
        }
        store.execute("CREATE TABLE IF NOT EXISTS moderation_profiles (" +
                "uuid TEXT PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "fake_profile INTEGER NOT NULL DEFAULT 0," +
                "last_updated TEXT)");
        store.execute("CREATE INDEX IF NOT EXISTS idx_moderation_profiles_name ON moderation_profiles(LOWER(name))");
        store.execute("CREATE TABLE IF NOT EXISTS moderation_mutes (" +
                "uuid TEXT PRIMARY KEY," +
                "active INTEGER NOT NULL DEFAULT 0," +
                "reason TEXT," +
                "actor TEXT," +
                "issued_at TEXT)");
        store.execute("CREATE TABLE IF NOT EXISTS moderation_warnings (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid TEXT NOT NULL," +
                "actor TEXT," +
                "reason TEXT," +
                "issued_at TEXT)");
        store.execute("CREATE INDEX IF NOT EXISTS idx_moderation_warnings_uuid ON moderation_warnings(uuid)");
        store.execute("CREATE TABLE IF NOT EXISTS moderation_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid TEXT NOT NULL," +
                "action TEXT NOT NULL," +
                "actor TEXT," +
                "reason TEXT," +
                "issued_at TEXT," +
                "duration TEXT," +
                "details TEXT)");
        store.execute("CREATE INDEX IF NOT EXISTS idx_moderation_history_uuid ON moderation_history(uuid)");
        migrateLegacyYaml();
    }

    public void reload() { load(); }

    /** No-op for the public lifecycle hooks — SQLite auto-commits. */
    public void save() { /* SQLite auto-commit */ }

    private void migrateLegacyYaml() {
        File legacy = new File(plugin.getDataFolder(), "moderation.yml");
        if (!legacy.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            renameMigrated(legacy);
            return;
        }
        int profiles = 0;
        int mutes = 0;
        int warnings = 0;
        int historyEntries = 0;
        for (String key : players.getKeys(false)) {
            ConfigurationSection player = players.getConfigurationSection(key);
            if (player == null) continue;
            String name = clean(player.getString("name"), key);
            UUID uuid = resolveOrSynthesizeUuid(name, player.getString("uuid"));
            boolean fake = player.getBoolean("fake-profile", false);
            String lastUpdated = clean(player.getString("last-updated"), nowStamp());
            store.executeUpdate(
                    "INSERT OR REPLACE INTO moderation_profiles (uuid, name, fake_profile, last_updated) VALUES (?, ?, ?, ?)",
                    uuid, name, fake, lastUpdated);
            profiles++;
            ConfigurationSection mute = player.getConfigurationSection("mute");
            if (mute != null && mute.getBoolean("active", false)) {
                store.executeUpdate(
                        "INSERT OR REPLACE INTO moderation_mutes (uuid, active, reason, actor, issued_at) VALUES (?, 1, ?, ?, ?)",
                        uuid, clean(mute.getString("reason"), DEFAULT_REASON),
                        clean(mute.getString("actor"), "Unknown"),
                        clean(mute.getString("issued-at"), nowStamp()));
                mutes++;
            }
            ConfigurationSection warningsSection = player.getConfigurationSection("warnings");
            if (warningsSection != null) {
                for (Map<?, ?> entry : warningsSection.getMapList("history")) {
                    store.executeUpdate(
                            "INSERT INTO moderation_warnings (uuid, actor, reason, issued_at) VALUES (?, ?, ?, ?)",
                            uuid,
                            clean(stringValue(entry.get("actor")), "Unknown"),
                            clean(stringValue(entry.get("reason")), DEFAULT_REASON),
                            clean(stringValue(entry.get("issued-at")), nowStamp()));
                    warnings++;
                }
            }
            ConfigurationSection historySection = player.getConfigurationSection("history");
            if (historySection != null) {
                for (Map<?, ?> entry : historySection.getMapList("entries")) {
                    store.executeUpdate(
                            "INSERT INTO moderation_history (uuid, action, actor, reason, issued_at, duration, details) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            uuid,
                            clean(stringValue(entry.get("action")), "updated"),
                            clean(stringValue(entry.get("actor")), "Unknown"),
                            clean(stringValue(entry.get("reason")), DEFAULT_REASON),
                            clean(stringValue(entry.get("issued-at")), nowStamp()),
                            stringValue(entry.get("duration")),
                            stringValue(entry.get("details")));
                    historyEntries++;
                }
            }
        }
        plugin.getLogger().info("Migrated " + profiles + " moderation profile(s), " + mutes +
                " mute(s), " + warnings + " warning(s), and " + historyEntries +
                " history entr(ies) from moderation.yml.");
        renameMigrated(legacy);
    }

    private void renameMigrated(File legacy) {
        File renamed = new File(legacy.getParentFile(), legacy.getName() + ".migrated");
        if (!legacy.renameTo(renamed)) {
            plugin.getLogger().warning("Could not rename moderation.yml after migration.");
        }
    }

    private UUID resolveOrSynthesizeUuid(String name, String rawUuid) {
        if (rawUuid != null && !rawUuid.isEmpty()) {
            try { return UUID.fromString(rawUuid); }
            catch (IllegalArgumentException ignored) { /* fall through to synthetic */ }
        }
        return UUID.nameUUIDFromBytes(("obx:" + name.toLowerCase(Locale.ENGLISH)).getBytes(StandardCharsets.UTF_8));
    }

    public String getDefaultReason() {
        return clean(plugin.getConfig().getString("moderation.defaults.reason"), DEFAULT_REASON);
    }

    public String getDefaultTempBanDuration() {
        return clean(plugin.getConfig().getString("moderation.defaults.tempban-duration"), DEFAULT_TEMPBAN_DURATION);
    }

    public long getDefaultTempBanDurationMillis() {
        Long parsed = parseDuration(getDefaultTempBanDuration());
        return parsed == null ? DEFAULT_TEMPBAN_MILLIS : parsed;
    }

    public boolean isMuted(String playerName) {
        UUID uuid = resolveUuidByName(playerName);
        if (uuid == null) return false;
        return store.queryFirst("SELECT active FROM moderation_mutes WHERE uuid = ?",
                rs -> rs.getInt("active"), uuid).orElse(0) == 1;
    }

    public String getMuteReason(String playerName) {
        UUID uuid = resolveUuidByName(playerName);
        if (uuid == null) return getDefaultReason();
        return store.queryFirst("SELECT reason FROM moderation_mutes WHERE uuid = ? AND active = 1",
                rs -> clean(rs.getString("reason"), getDefaultReason()), uuid).orElse(getDefaultReason());
    }

    public boolean mute(ResolvedProfile target, String actor, String reason) {
        if (target == null || !store.isAvailable()) return false;
        upsertProfile(target);
        String cleanedReason = clean(reason, getDefaultReason());
        store.executeUpdate(
                "INSERT OR REPLACE INTO moderation_mutes (uuid, active, reason, actor, issued_at) VALUES (?, 1, ?, ?, ?)",
                target.getUniqueId(), cleanedReason, actor, nowStamp());
        logAction("Mute", actor, target, cleanedReason, null, null);
        return true;
    }

    public boolean unmute(ResolvedProfile target, String actor, String reason) {
        if (target == null || !store.isAvailable()) return false;
        boolean active = store.queryFirst("SELECT active FROM moderation_mutes WHERE uuid = ?",
                rs -> rs.getInt("active"), target.getUniqueId()).orElse(0) == 1;
        if (!active) return false;
        store.executeUpdate("DELETE FROM moderation_mutes WHERE uuid = ?", target.getUniqueId());
        upsertProfile(target);
        logAction("Unmute", actor, target, clean(reason, getDefaultReason()), null, null);
        return true;
    }

    public int warn(ResolvedProfile target, String actor, String reason) {
        if (target == null || !store.isAvailable()) return 0;
        upsertProfile(target);
        String cleanedReason = clean(reason, getDefaultReason());
        store.executeUpdate(
                "INSERT INTO moderation_warnings (uuid, actor, reason, issued_at) VALUES (?, ?, ?, ?)",
                target.getUniqueId(), actor, cleanedReason, nowStamp());
        int count = store.queryFirst("SELECT COUNT(*) AS c FROM moderation_warnings WHERE uuid = ?",
                rs -> rs.getInt("c"), target.getUniqueId()).orElse(0);
        logAction("Warn", actor, target, cleanedReason, null, "Total warnings: " + count);
        return count;
    }

    public boolean ban(ResolvedProfile target, String actor, String reason) {
        if (target == null) return false;
        upsertProfile(target);
        String cleanedReason = clean(reason, getDefaultReason());
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), cleanedReason, null, actor);
        if (target.isOnline()) {
            target.getPlayer().kickPlayer(plugin.getLanguageManager().get(
                    target.getPlayer(),
                    "player.moderation.ban.kick-message",
                    Placeholders.with("reason", cleanedReason)
            ));
        }
        logAction("Ban", actor, target, cleanedReason, "Permanent", null);
        return true;
    }

    public boolean tempBan(ResolvedProfile target, String actor, String reason) {
        if (target == null) return false;
        upsertProfile(target);
        String cleanedReason = clean(reason, getDefaultReason());
        Date expiresAt = new Date(System.currentTimeMillis() + getDefaultTempBanDurationMillis());
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), cleanedReason, expiresAt, actor);
        if (target.isOnline()) {
            Map<String, String> placeholders = new LinkedHashMap<>();
            placeholders.put("reason", cleanedReason);
            placeholders.put("duration", getDefaultTempBanDuration());
            target.getPlayer().kickPlayer(plugin.getLanguageManager().get(target.getPlayer(),
                    "player.moderation.tempban.kick-message", placeholders));
        }
        logAction("Tempban", actor, target, cleanedReason, getDefaultTempBanDuration(), "Expires: " + formatDate(expiresAt));
        return true;
    }

    public boolean unban(String playerName, String actor, String reason) {
        BanEntry activeEntry = findActiveBan(playerName);
        String effectiveTarget = activeEntry != null && activeEntry.getTarget() != null ? activeEntry.getTarget() : clean(playerName, playerName);
        boolean banned = activeEntry != null || Bukkit.getBanList(BanList.Type.NAME).isBanned(effectiveTarget);
        if (!banned) return false;
        Bukkit.getBanList(BanList.Type.NAME).pardon(effectiveTarget);
        ResolvedProfile resolved = resolveKnownProfile(effectiveTarget);
        if (resolved == null) {
            resolved = new ResolvedProfile(effectiveTarget,
                    UUID.nameUUIDFromBytes(("obx:" + effectiveTarget.toLowerCase(Locale.ENGLISH)).getBytes(StandardCharsets.UTF_8)),
                    false, null);
        }
        upsertProfile(resolved);
        logAction("Unban", actor, resolved, clean(reason, getDefaultReason()), null, null);
        return true;
    }

    public boolean kick(ResolvedProfile target, String actor, String reason) {
        if (target == null) return false;
        upsertProfile(target);
        String cleanedReason = clean(reason, getDefaultReason());
        if (target.isOnline()) {
            target.getPlayer().kickPlayer(plugin.getLanguageManager().get(
                    target.getPlayer(),
                    "player.moderation.kick.kick-message",
                    Placeholders.with("reason", cleanedReason)
            ));
        }
        logAction("Kick", actor, target, cleanedReason, null, null);
        return true;
    }

    public boolean hasActiveBan(String playerName) {
        return findActiveBan(playerName) != null;
    }

    public ResolvedProfile resolveKnownProfile(String input) {
        return resolveProfile(input, false);
    }

    public ResolvedProfile resolvePunishmentProfile(String input) {
        return resolveProfile(input, true);
    }

    public List<String> getFakeProfileNames() {
        ConfigurationSection profilesSection = plugin.getConfig().getConfigurationSection("testing.fake-profiles");
        if (profilesSection == null) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (String key : profilesSection.getKeys(false)) {
            ConfigurationSection profileSection = profilesSection.getConfigurationSection(key);
            String name = profileSection == null ? key : clean(profileSection.getString("name"), key);
            names.add(name);
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public List<String> getMutedProfileNames() {
        if (!store.isAvailable()) return Collections.emptyList();
        return store.queryAll(
                "SELECT p.name AS name FROM moderation_profiles p" +
                        " INNER JOIN moderation_mutes m ON m.uuid = p.uuid AND m.active = 1" +
                        " ORDER BY LOWER(p.name)",
                rs -> rs.getString("name"));
    }

    public List<BanView> getActiveBans() {
        Set<BanEntry> entries = Bukkit.getBanList(BanList.Type.NAME).getBanEntries();
        if (entries == null || entries.isEmpty()) return Collections.emptyList();
        List<String> expiredTargets = new ArrayList<>();
        List<BanView> bans = new ArrayList<>();
        Date now = new Date();
        for (BanEntry entry : entries) {
            if (entry == null || entry.getTarget() == null || entry.getTarget().trim().isEmpty()) continue;
            Date expiration = entry.getExpiration();
            if (expiration != null && expiration.before(now)) {
                expiredTargets.add(entry.getTarget());
                continue;
            }
            bans.add(new BanView(
                    entry.getTarget(),
                    clean(entry.getReason(), getDefaultReason()),
                    clean(entry.getSource(), "Unknown"),
                    expiration));
        }
        for (String expired : expiredTargets) {
            Bukkit.getBanList(BanList.Type.NAME).pardon(expired);
        }
        Collections.sort(bans);
        return bans;
    }

    public ModerationStatusProfile getStatusProfile(String input) {
        ResolvedProfile target = resolveStatusProfile(input);
        if (target == null) return null;

        BanEntry activeBanEntry = findActiveBan(target.getName());
        BanView activeBan = activeBanEntry == null ? null : new BanView(
                activeBanEntry.getTarget(),
                clean(activeBanEntry.getReason(), getDefaultReason()),
                clean(activeBanEntry.getSource(), "Unknown"),
                activeBanEntry.getExpiration());

        MuteView activeMute = null;
        if (store.isAvailable()) {
            activeMute = store.queryFirst(
                    "SELECT actor, reason, issued_at FROM moderation_mutes WHERE uuid = ? AND active = 1",
                    rs -> new MuteView(
                            clean(rs.getString("actor"), "Unknown"),
                            clean(rs.getString("reason"), getDefaultReason()),
                            clean(rs.getString("issued_at"), "Unknown")),
                    target.getUniqueId()).orElse(null);
        }

        List<WarningView> warnings = new ArrayList<>();
        if (store.isAvailable()) {
            warnings = store.queryAll(
                    "SELECT actor, reason, issued_at FROM moderation_warnings WHERE uuid = ? ORDER BY id ASC",
                    rs -> new WarningView(
                            clean(rs.getString("actor"), "Unknown"),
                            clean(rs.getString("reason"), getDefaultReason()),
                            clean(rs.getString("issued_at"), "Unknown")),
                    target.getUniqueId());
        }
        WarningView lastWarning = warnings.isEmpty() ? null : warnings.get(warnings.size() - 1);

        List<ActionHistoryEntry> history = getActionHistory(target.getUniqueId());
        Map<String, Integer> actionCounts = countActions(history);
        int warningCount = Math.max(warnings.size(), actionCounts.get("warn"));
        actionCounts.put("warn", warningCount);

        ActionHistoryEntry lastAction = history.isEmpty() ? null : history.get(history.size() - 1);
        List<ActionHistoryEntry> recentActions = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0 && recentActions.size() < 5; i--) {
            recentActions.add(history.get(i));
        }

        String lastUpdated = "Never";
        if (store.isAvailable()) {
            lastUpdated = store.queryFirst("SELECT last_updated FROM moderation_profiles WHERE uuid = ?",
                    rs -> clean(rs.getString("last_updated"), "Never"), target.getUniqueId()).orElse("Never");
        }
        return new ModerationStatusProfile(target, lastUpdated, activeBan, activeMute, warningCount,
                lastWarning, actionCounts, lastAction, recentActions);
    }

    public List<String> getStatusProfileNames() {
        Set<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
        names.addAll(getFakeProfileNames());
        names.addAll(getStoredProfileNames());
        for (BanView ban : getActiveBans()) names.add(ban.getTarget());
        return new ArrayList<>(names);
    }

    public String formatDate(Date date) {
        if (date == null) return "Permanent";
        return DISPLAY_TIMESTAMP.format(date.toInstant());
    }

    private List<String> getStoredProfileNames() {
        if (!store.isAvailable()) return Collections.emptyList();
        return store.queryAll("SELECT name FROM moderation_profiles ORDER BY LOWER(name)",
                rs -> rs.getString("name"));
    }

    private ResolvedProfile resolveStatusProfile(String input) {
        ResolvedProfile resolved = resolveKnownProfile(input);
        if (resolved != null) return resolved;
        BanEntry activeBan = findActiveBan(input);
        if (activeBan == null || activeBan.getTarget() == null || activeBan.getTarget().trim().isEmpty()) return null;
        ResolvedProfile stored = findStoredProfile(activeBan.getTarget());
        if (stored != null) return stored;
        return new ResolvedProfile(activeBan.getTarget(),
                UUID.nameUUIDFromBytes(("obx:" + activeBan.getTarget().toLowerCase(Locale.ENGLISH)).getBytes(StandardCharsets.UTF_8)),
                false, null);
    }

    private ResolvedProfile resolveProfile(String input, boolean allowSynthetic) {
        String trimmed = clean(input, "");
        if (trimmed.isEmpty()) return null;

        Player online = findOnlinePlayer(trimmed);
        if (online != null) {
            return new ResolvedProfile(online.getName(), online.getUniqueId(), false, online);
        }
        ResolvedProfile fake = findConfiguredFakeProfile(trimmed);
        if (fake != null) return fake;
        ResolvedProfile stored = findStoredProfile(trimmed);
        if (stored != null) return stored;
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline == null || offline.getName() == null) continue;
            if (offline.getName().equalsIgnoreCase(trimmed)) {
                return new ResolvedProfile(offline.getName(), offline.getUniqueId(), false, null);
            }
        }
        if (!allowSynthetic) return null;
        UUID syntheticUuid = UUID.nameUUIDFromBytes(("obx:" + trimmed.toLowerCase(Locale.ENGLISH)).getBytes(StandardCharsets.UTF_8));
        return new ResolvedProfile(trimmed, syntheticUuid, false, null);
    }

    private ResolvedProfile findConfiguredFakeProfile(String input) {
        ConfigurationSection profilesSection = plugin.getConfig().getConfigurationSection("testing.fake-profiles");
        if (profilesSection == null) return null;
        for (String key : profilesSection.getKeys(false)) {
            ConfigurationSection profileSection = profilesSection.getConfigurationSection(key);
            String name = profileSection == null ? key : clean(profileSection.getString("name"), key);
            if (!name.equalsIgnoreCase(input) && !key.equalsIgnoreCase(input)) continue;
            UUID uuid = null;
            if (profileSection != null) {
                String rawUuid = clean(profileSection.getString("uuid"), null);
                if (rawUuid != null) {
                    try { uuid = UUID.fromString(rawUuid); }
                    catch (IllegalArgumentException ignored) { uuid = null; }
                }
            }
            if (uuid == null) {
                uuid = UUID.nameUUIDFromBytes(("obx-fake:" + name.toLowerCase(Locale.ENGLISH)).getBytes(StandardCharsets.UTF_8));
            }
            return new ResolvedProfile(name, uuid, true, null);
        }
        return null;
    }

    private ResolvedProfile findStoredProfile(String input) {
        if (!store.isAvailable()) return null;
        return store.queryFirst(
                "SELECT uuid, name, fake_profile FROM moderation_profiles WHERE LOWER(name) = LOWER(?)",
                rs -> {
                    UUID uuid;
                    try { uuid = UUID.fromString(rs.getString("uuid")); }
                    catch (IllegalArgumentException ignored) { return null; }
                    String storedName = rs.getString("name");
                    boolean fake = rs.getInt("fake_profile") == 1;
                    Player onlineMatch = findOnlinePlayer(storedName);
                    if (onlineMatch != null) {
                        return new ResolvedProfile(onlineMatch.getName(), onlineMatch.getUniqueId(), fake, onlineMatch);
                    }
                    return new ResolvedProfile(storedName, uuid, fake, null);
                }, input).orElse(null);
    }

    private UUID resolveUuidByName(String name) {
        if (!store.isAvailable() || name == null) return null;
        return store.queryFirst("SELECT uuid FROM moderation_profiles WHERE LOWER(name) = LOWER(?)",
                rs -> {
                    try { return UUID.fromString(rs.getString("uuid")); }
                    catch (IllegalArgumentException ignored) { return null; }
                }, name).orElse(null);
    }

    private void upsertProfile(ResolvedProfile target) {
        if (target == null || target.getUniqueId() == null || !store.isAvailable()) return;
        store.executeUpdate(
                "INSERT INTO moderation_profiles (uuid, name, fake_profile, last_updated) VALUES (?, ?, ?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, fake_profile = excluded.fake_profile, last_updated = excluded.last_updated",
                target.getUniqueId(), target.getName(), target.isFakeProfile(), nowStamp());
    }

    private Player findOnlinePlayer(String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) return exact;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    private BanEntry findActiveBan(String playerName) {
        String normalized = clean(playerName, "");
        if (normalized.isEmpty()) return null;
        Date now = new Date();
        List<String> expiredTargets = new ArrayList<>();
        BanEntry match = null;
        for (BanEntry entry : Bukkit.getBanList(BanList.Type.NAME).getBanEntries()) {
            if (entry == null || entry.getTarget() == null) continue;
            Date expiration = entry.getExpiration();
            if (expiration != null && expiration.before(now)) {
                expiredTargets.add(entry.getTarget());
                continue;
            }
            if (entry.getTarget().equalsIgnoreCase(normalized)) {
                match = entry;
            }
        }
        for (String expired : expiredTargets) {
            Bukkit.getBanList(BanList.Type.NAME).pardon(expired);
        }
        return match;
    }

    private List<ActionHistoryEntry> getActionHistory(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return Collections.emptyList();
        return store.queryAll(
                "SELECT action, actor, reason, issued_at, duration, details FROM moderation_history" +
                        " WHERE uuid = ? ORDER BY id ASC",
                rs -> new ActionHistoryEntry(
                        normalizeActionKey(rs.getString("action")),
                        clean(rs.getString("actor"), "Unknown"),
                        clean(rs.getString("reason"), getDefaultReason()),
                        clean(rs.getString("issued_at"), "Unknown"),
                        clean(rs.getString("duration"), ""),
                        clean(rs.getString("details"), "")),
                uuid);
    }

    private Map<String, Integer> countActions(List<ActionHistoryEntry> history) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("ban", 0);
        counts.put("tempban", 0);
        counts.put("kick", 0);
        counts.put("warn", 0);
        counts.put("mute", 0);
        counts.put("unmute", 0);
        counts.put("unban", 0);
        for (ActionHistoryEntry entry : history) {
            if (!counts.containsKey(entry.getAction())) continue;
            counts.put(entry.getAction(), counts.get(entry.getAction()) + 1);
        }
        return counts;
    }

    private void appendActionHistory(ResolvedProfile target, String action, String actor, String reason, String duration, String extraLine) {
        if (target == null || target.getUniqueId() == null || !store.isAvailable()) return;
        store.executeUpdate(
                "INSERT INTO moderation_history (uuid, action, actor, reason, issued_at, duration, details) VALUES (?, ?, ?, ?, ?, ?, ?)",
                target.getUniqueId(),
                normalizeActionKey(action),
                clean(actor, "Unknown"),
                clean(reason, getDefaultReason()),
                nowStamp(),
                duration == null || duration.trim().isEmpty() ? null : duration.trim(),
                extraLine == null || extraLine.trim().isEmpty() ? null : extraLine.trim());
        store.executeUpdate("UPDATE moderation_profiles SET last_updated = ? WHERE uuid = ?",
                nowStamp(), target.getUniqueId());
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void logAction(String action, String actor, ResolvedProfile target, String reason, String duration, String extraLine) {
        appendActionHistory(target, action, actor, reason, duration, extraLine);

        if (plugin.getConfig().getBoolean("discord.moderation.log-to-console", true)) {
            plugin.writeConsoleLine(buildConsoleMessage(action, actor, target.getName(), reason, duration, extraLine));
        }

        if (!plugin.getConfig().getBoolean("discord.moderation.enabled", true)) return;

        String webhookUrl = clean(plugin.getConfig().getString("discord.moderation.webhook-url"), "");
        if (webhookUrl.isEmpty()) {
            if (!missingWebhookWarningSent) {
                missingWebhookWarningSent = true;
                plugin.getLogger().warning("Discord moderation logging is enabled, but discord.moderation.webhook-url is empty. Server/channel IDs alone cannot post messages.");
            }
            return;
        }

        String serverId = clean(plugin.getConfig().getString("discord.moderation.server-id"), "unknown");
        String channelId = clean(plugin.getConfig().getString("discord.moderation.channel-id"), "unknown");
        String username = clean(plugin.getConfig().getString("discord.moderation.webhook-username"), "OBX Moderation");
        String avatarUrl = clean(plugin.getConfig().getString("discord.moderation.avatar-url"), "");
        String footerIconUrl = clean(plugin.getConfig().getString("discord.moderation.footer-icon-url"), avatarUrl);
        String timestamp = Instant.now().toString();
        String payload = buildDiscordPayload(username, avatarUrl, footerIconUrl, action, actor, target.getName(),
                reason, duration, extraLine, serverId, channelId, timestamp);

        plugin.getSchedulerAdapter().runAsync(() -> sendWebhook(webhookUrl, payload));
    }

    private String buildConsoleMessage(String action, String actor, String target, String reason, String duration, String extraLine) {
        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.DARK_PURPLE).append(ChatColor.BOLD).append("Moderation:").append(ChatColor.RESET).append(' ');
        builder.append(ChatColor.YELLOW).append(formatActionVerb(action)).append(ChatColor.RESET).append(' ');
        builder.append(ChatColor.LIGHT_PURPLE).append(target).append(ChatColor.RESET);
        builder.append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.RESET);
        builder.append(ChatColor.GRAY).append(ChatColor.BOLD).append("By:").append(ChatColor.RESET).append(' ');
        builder.append(ChatColor.GREEN).append(actor).append(ChatColor.RESET);
        builder.append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.RESET);
        builder.append(ChatColor.GRAY).append(ChatColor.BOLD).append("reason=").append(ChatColor.RESET);
        builder.append(ChatColor.WHITE).append(reason).append(ChatColor.RESET);
        if (duration != null && !duration.isEmpty()) {
            builder.append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.RESET);
            builder.append(ChatColor.GRAY).append(ChatColor.BOLD).append("duration=").append(ChatColor.RESET);
            builder.append(ChatColor.WHITE).append(duration).append(ChatColor.RESET);
        }
        if (extraLine != null && !extraLine.isEmpty()) {
            builder.append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.RESET);
            builder.append(ChatColor.WHITE).append(extraLine).append(ChatColor.RESET);
        }
        return builder.toString();
    }

    private String formatActionVerb(String action) {
        if (action == null) return "Updated";
        switch (action.toLowerCase(Locale.ENGLISH)) {
            case "ban":     return "Banned";
            case "unban":   return "Unbanned";
            case "kick":    return "Kicked";
            case "mute":    return "Muted";
            case "unmute":  return "Unmuted";
            case "tempban": return "Temp-Banned";
            case "warn":    return "Warned";
            default:        return action;
        }
    }

    private String buildDiscordPayload(String username, String avatarUrl, String footerIconUrl, String action, String actor,
                                       String target, String reason, String duration, String extraLine, String serverId,
                                       String channelId, String timestamp) {
        StringBuilder fields = new StringBuilder();
        String actionLabel = formatActionVerb(action);
        appendDiscordField(fields, "Action", asInlineCode(actionLabel), true);
        appendDiscordField(fields, "Target", asInlineCode(target), true);
        appendDiscordField(fields, "By", asInlineCode(actor), true);
        appendDiscordField(fields, "Reason", reason, false);
        if (duration != null && !duration.isEmpty()) {
            appendDiscordField(fields, "Duration", asInlineCode(duration), true);
        }
        appendDiscordField(fields, "Server ID", asInlineCode(serverId), true);
        appendDiscordField(fields, "Channel", "<#" + channelId + ">", true);
        if (extraLine != null && !extraLine.isEmpty()) {
            appendDiscordField(fields, "Details", extraLine, false);
        }
        return new StringBuilder()
                .append("{")
                .append("\"username\":\"").append(escapeJson(username)).append("\",")
                .append(avatarUrl.isEmpty() ? "" : "\"avatar_url\":\"" + escapeJson(avatarUrl) + "\",")
                .append("\"embeds\":[{")
                .append("\"title\":\"").append(escapeJson("Moderation | Action: " + actionLabel + " " + target)).append("\",")
                .append("\"description\":\"").append(escapeJson("A staff moderation action was recorded by OBX.")).append("\",")
                .append("\"color\":").append(discordColor(action)).append(",")
                .append("\"fields\":[").append(fields).append("],")
                .append("\"footer\":{")
                .append("\"text\":\"").append(escapeJson("OBX Moderation")).append("\"")
                .append(footerIconUrl.isEmpty() ? "" : ",\"icon_url\":\"" + escapeJson(footerIconUrl) + "\"")
                .append("},")
                .append("\"timestamp\":\"").append(escapeJson(timestamp)).append("\"")
                .append("}]")
                .append("}")
                .toString();
    }

    private String asInlineCode(String value) { return "`" + value + "`"; }

    private void appendDiscordField(StringBuilder builder, String name, String value, boolean inline) {
        if (builder.length() > 0) builder.append(',');
        builder.append('{')
                .append("\"name\":\"").append(escapeJson(name)).append("\",")
                .append("\"value\":\"").append(escapeJson(value)).append("\",")
                .append("\"inline\":").append(inline)
                .append('}');
    }

    private int discordColor(String action) {
        if (action == null) return 16766720;
        switch (action.toLowerCase(Locale.ENGLISH)) {
            case "ban":     return 15158332;
            case "tempban": return 15105570;
            case "unban":   return 3066993;
            case "kick":    return 16098851;
            case "mute":    return 10181046;
            case "unmute":  return 3447003;
            case "warn":    return 15844367;
            default:        return 16766720;
        }
    }

    private void sendWebhook(String webhookUrl, String payload) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(webhookUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                plugin.getLogger().warning("Failed to post moderation log to Discord webhook: HTTP " + responseCode);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to post moderation log to Discord webhook: " + exception.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String nowStamp() { return DISPLAY_TIMESTAMP.format(Instant.now()); }

    private String normalizeActionKey(String value) {
        return clean(value, "updated").toLowerCase(Locale.ENGLISH);
    }

    private String clean(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private Long parseDuration(String input) {
        String configured = clean(input, DEFAULT_TEMPBAN_DURATION).toLowerCase(Locale.ENGLISH).trim();
        if (configured.length() < 2) return null;
        // Split a leading numeric amount from a 1–2 char unit suffix so multi-char
        // units (mo) parse alongside s/m/h/d/w/y.
        int split = 0;
        while (split < configured.length() && Character.isDigit(configured.charAt(split))) {
            split++;
        }
        if (split == 0 || split == configured.length()) return null;
        long amount;
        try { amount = Long.parseLong(configured.substring(0, split)); }
        catch (NumberFormatException exception) { return null; }
        if (amount <= 0L) return null;
        switch (configured.substring(split)) {
            case "s": return amount * 1000L;
            case "m": return amount * 60L * 1000L;
            case "h": return amount * 60L * 60L * 1000L;
            case "d": return amount * 24L * 60L * 60L * 1000L;
            case "w": return amount * 7L * 24L * 60L * 60L * 1000L;
            case "mo": return amount * 30L * 24L * 60L * 60L * 1000L;
            case "y": return amount * 365L * 24L * 60L * 60L * 1000L;
            default: return null;
        }
    }

    private String escapeJson(String input) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            switch (character) {
                case '\\': builder.append("\\\\"); break;
                case '"':  builder.append("\\\""); break;
                case '\n': builder.append("\\n"); break;
                case '\r': builder.append("\\r"); break;
                case '\t': builder.append("\\t"); break;
                default:   builder.append(character); break;
            }
        }
        return builder.toString();
    }

    public static final class ResolvedProfile {
        private final String name;
        private final UUID uniqueId;
        private final boolean fakeProfile;
        private final Player player;

        public ResolvedProfile(String name, UUID uniqueId, boolean fakeProfile, Player player) {
            this.name = name;
            this.uniqueId = uniqueId;
            this.fakeProfile = fakeProfile;
            this.player = player;
        }

        public String getName() { return name; }
        public UUID getUniqueId() { return uniqueId; }
        public boolean isFakeProfile() { return fakeProfile; }
        public Player getPlayer() { return player; }
        public boolean isOnline() { return player != null && player.isOnline(); }
    }

    public static final class BanView implements Comparable<BanView> {
        private final String target;
        private final String reason;
        private final String source;
        private final Date expiresAt;

        public BanView(String target, String reason, String source, Date expiresAt) {
            this.target = target;
            this.reason = reason;
            this.source = source;
            this.expiresAt = expiresAt == null ? null : new Date(expiresAt.getTime());
        }

        public String getTarget() { return target; }
        public String getReason() { return reason; }
        public String getSource() { return source; }
        public Date getExpiresAt() { return expiresAt == null ? null : new Date(expiresAt.getTime()); }

        @Override
        public int compareTo(BanView other) {
            return String.CASE_INSENSITIVE_ORDER.compare(target, other.target);
        }
    }

    public static final class MuteView {
        private final String actor;
        private final String reason;
        private final String issuedAt;

        public MuteView(String actor, String reason, String issuedAt) {
            this.actor = actor;
            this.reason = reason;
            this.issuedAt = issuedAt;
        }

        public String getActor() { return actor; }
        public String getReason() { return reason; }
        public String getIssuedAt() { return issuedAt; }
    }

    public static final class WarningView {
        private final String actor;
        private final String reason;
        private final String issuedAt;

        public WarningView(String actor, String reason, String issuedAt) {
            this.actor = actor;
            this.reason = reason;
            this.issuedAt = issuedAt;
        }

        public String getActor() { return actor; }
        public String getReason() { return reason; }
        public String getIssuedAt() { return issuedAt; }
    }

    public static final class ActionHistoryEntry {
        private final String action;
        private final String actor;
        private final String reason;
        private final String issuedAt;
        private final String duration;
        private final String details;

        public ActionHistoryEntry(String action, String actor, String reason, String issuedAt, String duration, String details) {
            this.action = action;
            this.actor = actor;
            this.reason = reason;
            this.issuedAt = issuedAt;
            this.duration = duration;
            this.details = details;
        }

        public String getAction() { return action; }
        public String getActor() { return actor; }
        public String getReason() { return reason; }
        public String getIssuedAt() { return issuedAt; }
        public String getDuration() { return duration; }
        public String getDetails() { return details; }
    }

    public static final class ModerationStatusProfile {
        private final ResolvedProfile profile;
        private final String lastUpdated;
        private final BanView activeBan;
        private final MuteView activeMute;
        private final int warningCount;
        private final WarningView lastWarning;
        private final Map<String, Integer> actionCounts;
        private final ActionHistoryEntry lastAction;
        private final List<ActionHistoryEntry> recentActions;

        public ModerationStatusProfile(ResolvedProfile profile, String lastUpdated, BanView activeBan, MuteView activeMute,
                                       int warningCount, WarningView lastWarning, Map<String, Integer> actionCounts,
                                       ActionHistoryEntry lastAction, List<ActionHistoryEntry> recentActions) {
            this.profile = profile;
            this.lastUpdated = lastUpdated;
            this.activeBan = activeBan;
            this.activeMute = activeMute;
            this.warningCount = warningCount;
            this.lastWarning = lastWarning;
            this.actionCounts = new LinkedHashMap<>(actionCounts);
            this.lastAction = lastAction;
            this.recentActions = new ArrayList<>(recentActions);
        }

        public ResolvedProfile getProfile() { return profile; }
        public String getLastUpdated() { return lastUpdated; }
        public BanView getActiveBan() { return activeBan; }
        public MuteView getActiveMute() { return activeMute; }
        public int getWarningCount() { return warningCount; }
        public WarningView getLastWarning() { return lastWarning; }

        public int getActionCount(String action) {
            Integer count = actionCounts.get(action);
            return count == null ? 0 : count;
        }

        public ActionHistoryEntry getLastAction() { return lastAction; }
        public List<ActionHistoryEntry> getRecentActions() { return new ArrayList<>(recentActions); }
    }
}
