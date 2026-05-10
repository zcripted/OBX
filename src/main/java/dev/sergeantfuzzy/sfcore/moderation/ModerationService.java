package dev.sergeantfuzzy.sfcore.moderation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.util.perf.AsyncYamlSaver;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
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

public class ModerationService {
    private static final String DEFAULT_REASON = "No reason provided.";
    private static final String DEFAULT_TEMPBAN_DURATION = "7d";
    private static final long DEFAULT_TEMPBAN_MILLIS = 7L * 24L * 60L * 60L * 1000L;
    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")
            .withZone(ZoneId.of("America/Detroit"));

    private final Main plugin;
    private final File dataFile;

    private YamlConfiguration data;
    private AsyncYamlSaver saver;
    private boolean missingWebhookWarningSent;

    public ModerationService(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "moderation.yml");
    }

    public void load() {
        try {
            if (!dataFile.exists()) {
                if (dataFile.getParentFile() != null && !dataFile.getParentFile().exists()) {
                    dataFile.getParentFile().mkdirs();
                }
                try {
                    plugin.saveResource("moderation.yml", false);
                } catch (IllegalArgumentException ignored) {
                    dataFile.createNewFile();
                }
            }
            data = YamlConfiguration.loadConfiguration(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to load moderation.yml: " + exception.getMessage());
        }
        saver = data == null ? null : new AsyncYamlSaver(plugin, data, dataFile, "moderation.yml");
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

    public String getMuteReason(String playerName) {
        ConfigurationSection muteSection = getMuteSection(playerName);
        if (muteSection == null || !muteSection.getBoolean("active", false)) {
            return getDefaultReason();
        }
        return clean(muteSection.getString("reason"), getDefaultReason());
    }

    public boolean isMuted(String playerName) {
        ConfigurationSection muteSection = getMuteSection(playerName);
        return muteSection != null && muteSection.getBoolean("active", false);
    }

    public boolean mute(ResolvedProfile target, String actor, String reason) {
        if (target == null || data == null) {
            return false;
        }
        ConfigurationSection playerSection = getPlayerSection(target.getName());
        writeProfileMetadata(playerSection, target);
        ConfigurationSection muteSection = ensureSection(playerSection, "mute");
        muteSection.set("active", true);
        muteSection.set("reason", clean(reason, getDefaultReason()));
        muteSection.set("actor", actor);
        muteSection.set("issued-at", nowStamp());
        markDirty();
        logAction("Mute", actor, target.getName(), clean(reason, getDefaultReason()), null, null);
        return true;
    }

    public boolean unmute(ResolvedProfile target, String actor, String reason) {
        if (target == null || data == null) {
            return false;
        }
        ConfigurationSection muteSection = getMuteSection(target.getName());
        if (muteSection == null || !muteSection.getBoolean("active", false)) {
            return false;
        }
        muteSection.set("active", false);
        muteSection.set("reason", null);
        muteSection.set("actor", null);
        muteSection.set("issued-at", null);
        ConfigurationSection playerSection = getPlayerSection(target.getName());
        writeProfileMetadata(playerSection, target);
        markDirty();
        logAction("Unmute", actor, target.getName(), clean(reason, getDefaultReason()), null, null);
        return true;
    }

    public int warn(ResolvedProfile target, String actor, String reason) {
        if (target == null || data == null) {
            return 0;
        }
        ConfigurationSection playerSection = getPlayerSection(target.getName());
        writeProfileMetadata(playerSection, target);
        ConfigurationSection warningsSection = ensureSection(playerSection, "warnings");
        List<Map<?, ?>> existing = warningsSection.getMapList("history");
        List<Map<String, Object>> history = new ArrayList<>();
        for (Map<?, ?> entry : existing) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> historyEntry : entry.entrySet()) {
                if (historyEntry.getKey() != null) {
                    copy.put(String.valueOf(historyEntry.getKey()), historyEntry.getValue());
                }
            }
            history.add(copy);
        }
        Map<String, Object> warning = new LinkedHashMap<>();
        warning.put("actor", actor);
        warning.put("reason", clean(reason, getDefaultReason()));
        warning.put("issued-at", nowStamp());
        history.add(warning);
        warningsSection.set("count", history.size());
        warningsSection.set("history", history);
        markDirty();
        logAction("Warn", actor, target.getName(), clean(reason, getDefaultReason()), null, "Total warnings: " + history.size());
        return history.size();
    }

    public boolean ban(ResolvedProfile target, String actor, String reason) {
        if (target == null) {
            return false;
        }
        recordProfile(target);
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), clean(reason, getDefaultReason()), null, actor);
        if (target.isOnline()) {
            target.getPlayer().kickPlayer(plugin.getLanguageManager().get(
                    target.getPlayer(),
                    "player.moderation.ban.kick-message",
                    Placeholders.with("reason", clean(reason, getDefaultReason()))
            ));
        }
        logAction("Ban", actor, target.getName(), clean(reason, getDefaultReason()), "Permanent", null);
        return true;
    }

    public boolean tempBan(ResolvedProfile target, String actor, String reason) {
        if (target == null) {
            return false;
        }
        recordProfile(target);
        Date expiresAt = new Date(System.currentTimeMillis() + getDefaultTempBanDurationMillis());
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), clean(reason, getDefaultReason()), expiresAt, actor);
        if (target.isOnline()) {
            Map<String, String> placeholders = new LinkedHashMap<>();
            placeholders.put("reason", clean(reason, getDefaultReason()));
            placeholders.put("duration", getDefaultTempBanDuration());
            target.getPlayer().kickPlayer(plugin.getLanguageManager().get(target.getPlayer(), "player.moderation.tempban.kick-message", placeholders));
        }
        logAction("Tempban", actor, target.getName(), clean(reason, getDefaultReason()), getDefaultTempBanDuration(), "Expires: " + formatDate(expiresAt));
        return true;
    }

    public boolean unban(String playerName, String actor, String reason) {
        BanEntry activeEntry = findActiveBan(playerName);
        String effectiveTarget = activeEntry != null && activeEntry.getTarget() != null ? activeEntry.getTarget() : clean(playerName, playerName);
        boolean banned = activeEntry != null || Bukkit.getBanList(BanList.Type.NAME).isBanned(effectiveTarget);
        if (!banned) {
            return false;
        }
        Bukkit.getBanList(BanList.Type.NAME).pardon(effectiveTarget);
        logAction("Unban", actor, effectiveTarget, clean(reason, getDefaultReason()), null, null);
        return true;
    }

    public boolean kick(ResolvedProfile target, String actor, String reason) {
        if (target == null) {
            return false;
        }
        recordProfile(target);
        if (target.isOnline()) {
            target.getPlayer().kickPlayer(plugin.getLanguageManager().get(
                    target.getPlayer(),
                    "player.moderation.kick.kick-message",
                    Placeholders.with("reason", clean(reason, getDefaultReason()))
            ));
        }
        logAction("Kick", actor, target.getName(), clean(reason, getDefaultReason()), null, null);
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
        if (profilesSection == null) {
            return Collections.emptyList();
        }
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
        if (data == null) {
            return Collections.emptyList();
        }
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (String key : players.getKeys(false)) {
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            if (section.getBoolean("mute.active", false)) {
                names.add(clean(section.getString("name"), key));
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public List<BanView> getActiveBans() {
        Set<BanEntry> entries = Bukkit.getBanList(BanList.Type.NAME).getBanEntries();
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> expiredTargets = new ArrayList<>();
        List<BanView> bans = new ArrayList<>();
        Date now = new Date();
        for (BanEntry entry : entries) {
            if (entry == null || entry.getTarget() == null || entry.getTarget().trim().isEmpty()) {
                continue;
            }
            Date expiration = entry.getExpiration();
            if (expiration != null && expiration.before(now)) {
                expiredTargets.add(entry.getTarget());
                continue;
            }
            bans.add(new BanView(
                    entry.getTarget(),
                    clean(entry.getReason(), getDefaultReason()),
                    clean(entry.getSource(), "Unknown"),
                    expiration
            ));
        }
        for (String expired : expiredTargets) {
            Bukkit.getBanList(BanList.Type.NAME).pardon(expired);
        }
        Collections.sort(bans);
        return bans;
    }

    public ModerationStatusProfile getStatusProfile(String input) {
        ResolvedProfile target = resolveStatusProfile(input);
        if (target == null) {
            return null;
        }

        ConfigurationSection playerSection = getPlayerSectionIfPresent(target.getName());
        BanEntry activeBanEntry = findActiveBan(target.getName());
        BanView activeBan = activeBanEntry == null ? null : new BanView(
                activeBanEntry.getTarget(),
                clean(activeBanEntry.getReason(), getDefaultReason()),
                clean(activeBanEntry.getSource(), "Unknown"),
                activeBanEntry.getExpiration()
        );

        MuteView activeMute = null;
        ConfigurationSection muteSection = playerSection == null ? null : playerSection.getConfigurationSection("mute");
        if (muteSection != null && muteSection.getBoolean("active", false)) {
            activeMute = new MuteView(
                    clean(muteSection.getString("actor"), "Unknown"),
                    clean(muteSection.getString("reason"), getDefaultReason()),
                    clean(muteSection.getString("issued-at"), "Unknown")
            );
        }

        WarningView lastWarning = null;
        int warningCount = 0;
        ConfigurationSection warningsSection = playerSection == null ? null : playerSection.getConfigurationSection("warnings");
        if (warningsSection != null) {
            List<Map<String, Object>> warningHistory = copyMapList(warningsSection.getMapList("history"));
            warningCount = Math.max(warningsSection.getInt("count", warningHistory.size()), warningHistory.size());
            if (!warningHistory.isEmpty()) {
                Map<String, Object> last = warningHistory.get(warningHistory.size() - 1);
                lastWarning = new WarningView(
                        clean(stringValue(last.get("actor")), "Unknown"),
                        clean(stringValue(last.get("reason")), getDefaultReason()),
                        clean(stringValue(last.get("issued-at")), "Unknown")
                );
            }
        }

        List<ActionHistoryEntry> history = getActionHistory(playerSection);
        Map<String, Integer> actionCounts = countActions(history);
        if (warningCount > actionCounts.get("warn")) {
            actionCounts.put("warn", warningCount);
        } else {
            warningCount = actionCounts.get("warn");
        }

        ActionHistoryEntry lastAction = history.isEmpty() ? null : history.get(history.size() - 1);
        List<ActionHistoryEntry> recentActions = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0 && recentActions.size() < 5; i--) {
            recentActions.add(history.get(i));
        }

        String lastUpdated = playerSection == null ? "Never" : clean(playerSection.getString("last-updated"), "Never");
        return new ModerationStatusProfile(target, lastUpdated, activeBan, activeMute, warningCount, lastWarning, actionCounts, lastAction, recentActions);
    }

    public List<String> getStatusProfileNames() {
        Set<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        names.addAll(getFakeProfileNames());
        names.addAll(getStoredProfileNames());
        for (BanView ban : getActiveBans()) {
            names.add(ban.getTarget());
        }
        return new ArrayList<>(names);
    }

    public String formatDate(Date date) {
        if (date == null) {
            return "Permanent";
        }
        return DISPLAY_TIMESTAMP.format(date.toInstant());
    }

    private ResolvedProfile resolveStatusProfile(String input) {
        ResolvedProfile resolved = resolveKnownProfile(input);
        if (resolved != null) {
            return resolved;
        }
        BanEntry activeBan = findActiveBan(input);
        if (activeBan == null || activeBan.getTarget() == null || activeBan.getTarget().trim().isEmpty()) {
            return null;
        }
        ResolvedProfile stored = findStoredProfile(activeBan.getTarget());
        if (stored != null) {
            return stored;
        }
        return new ResolvedProfile(activeBan.getTarget(), null, false, null);
    }

    private ResolvedProfile resolveProfile(String input, boolean allowSynthetic) {
        String trimmed = clean(input, "");
        if (trimmed.isEmpty()) {
            return null;
        }

        Player online = findOnlinePlayer(trimmed);
        if (online != null) {
            return new ResolvedProfile(online.getName(), online.getUniqueId(), false, online);
        }

        ResolvedProfile fake = findConfiguredFakeProfile(trimmed);
        if (fake != null) {
            return fake;
        }

        ResolvedProfile stored = findStoredProfile(trimmed);
        if (stored != null) {
            return stored;
        }

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline == null || offline.getName() == null) {
                continue;
            }
            if (offline.getName().equalsIgnoreCase(trimmed)) {
                return new ResolvedProfile(offline.getName(), offline.getUniqueId(), false, null);
            }
        }

        if (!allowSynthetic) {
            return null;
        }

        UUID syntheticUuid = UUID.nameUUIDFromBytes(("sf-core:" + trimmed.toLowerCase(Locale.ENGLISH)).getBytes(StandardCharsets.UTF_8));
        return new ResolvedProfile(trimmed, syntheticUuid, false, null);
    }

    private ResolvedProfile findConfiguredFakeProfile(String input) {
        ConfigurationSection profilesSection = plugin.getConfig().getConfigurationSection("testing.fake-profiles");
        if (profilesSection == null) {
            return null;
        }
        for (String key : profilesSection.getKeys(false)) {
            ConfigurationSection profileSection = profilesSection.getConfigurationSection(key);
            String name = profileSection == null ? key : clean(profileSection.getString("name"), key);
            if (!name.equalsIgnoreCase(input) && !key.equalsIgnoreCase(input)) {
                continue;
            }
            UUID uuid = null;
            if (profileSection != null) {
                String rawUuid = clean(profileSection.getString("uuid"), null);
                if (rawUuid != null) {
                    try {
                        uuid = UUID.fromString(rawUuid);
                    } catch (IllegalArgumentException ignored) {
                        uuid = null;
                    }
                }
            }
            if (uuid == null) {
                uuid = UUID.nameUUIDFromBytes(("sf-core-fake:" + name.toLowerCase(Locale.ENGLISH)).getBytes(StandardCharsets.UTF_8));
            }
            return new ResolvedProfile(name, uuid, true, null);
        }
        return null;
    }

    private ResolvedProfile findStoredProfile(String input) {
        if (data == null) {
            return null;
        }
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            return null;
        }
        for (String key : players.getKeys(false)) {
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String storedName = clean(section.getString("name"), key);
            if (!storedName.equalsIgnoreCase(input)) {
                continue;
            }
            UUID uuid = null;
            String rawUuid = clean(section.getString("uuid"), null);
            if (rawUuid != null) {
                try {
                    uuid = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException ignored) {
                    uuid = null;
                }
            }
            boolean fake = section.getBoolean("fake-profile", false);
            Player online = findOnlinePlayer(storedName);
            if (online != null) {
                return new ResolvedProfile(online.getName(), online.getUniqueId(), fake, online);
            }
            return new ResolvedProfile(storedName, uuid, fake, null);
        }
        return null;
    }

    private Player findOnlinePlayer(String name) {
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

    private BanEntry findActiveBan(String playerName) {
        String normalized = clean(playerName, "");
        if (normalized.isEmpty()) {
            return null;
        }
        Date now = new Date();
        List<String> expiredTargets = new ArrayList<>();
        BanEntry match = null;
        for (BanEntry entry : Bukkit.getBanList(BanList.Type.NAME).getBanEntries()) {
            if (entry == null || entry.getTarget() == null) {
                continue;
            }
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

    private List<String> getStoredProfileNames() {
        if (data == null) {
            return Collections.emptyList();
        }
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            return Collections.emptyList();
        }
        Set<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String key : players.getKeys(false)) {
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            names.add(clean(section.getString("name"), key));
        }
        return new ArrayList<>(names);
    }

    private void recordProfile(ResolvedProfile target) {
        if (target == null || data == null) {
            return;
        }
        ConfigurationSection section = getPlayerSection(target.getName());
        writeProfileMetadata(section, target);
        markDirty();
    }

    private ConfigurationSection getMuteSection(String playerName) {
        if (data == null) {
            return null;
        }
        ConfigurationSection playerSection = getPlayerSectionIfPresent(playerName);
        return playerSection == null ? null : playerSection.getConfigurationSection("mute");
    }

    private ConfigurationSection getPlayerSection(String playerName) {
        ConfigurationSection players = ensureSection(data, "players");
        String key = normalize(playerName);
        ConfigurationSection section = players.getConfigurationSection(key);
        if (section == null) {
            section = players.createSection(key);
        }
        return section;
    }

    private ConfigurationSection getPlayerSectionIfPresent(String playerName) {
        if (data == null) {
            return null;
        }
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            return null;
        }
        String key = normalize(playerName);
        ConfigurationSection direct = players.getConfigurationSection(key);
        if (direct != null) {
            return direct;
        }
        for (String existingKey : players.getKeys(false)) {
            ConfigurationSection section = players.getConfigurationSection(existingKey);
            if (section == null) {
                continue;
            }
            String storedName = clean(section.getString("name"), existingKey);
            if (storedName.equalsIgnoreCase(playerName)) {
                return section;
            }
        }
        return null;
    }

    private void writeProfileMetadata(ConfigurationSection section, ResolvedProfile target) {
        if (section == null || target == null) {
            return;
        }
        section.set("name", target.getName());
        if (target.getUniqueId() != null) {
            section.set("uuid", target.getUniqueId().toString());
        }
        section.set("fake-profile", target.isFakeProfile());
        section.set("last-updated", nowStamp());
    }

    private ConfigurationSection ensureSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            section = parent.createSection(path);
        }
        return section;
    }

    private List<ActionHistoryEntry> getActionHistory(ConfigurationSection playerSection) {
        if (playerSection == null) {
            return Collections.emptyList();
        }
        ConfigurationSection historySection = playerSection.getConfigurationSection("history");
        if (historySection == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> storedEntries = copyMapList(historySection.getMapList("entries"));
        List<ActionHistoryEntry> history = new ArrayList<>();
        for (Map<String, Object> entry : storedEntries) {
            history.add(new ActionHistoryEntry(
                    normalizeActionKey(stringValue(entry.get("action"))),
                    clean(stringValue(entry.get("actor")), "Unknown"),
                    clean(stringValue(entry.get("reason")), getDefaultReason()),
                    clean(stringValue(entry.get("issued-at")), "Unknown"),
                    clean(stringValue(entry.get("duration")), ""),
                    clean(stringValue(entry.get("details")), "")
            ));
        }
        return history;
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
            if (!counts.containsKey(entry.getAction())) {
                continue;
            }
            counts.put(entry.getAction(), counts.get(entry.getAction()) + 1);
        }
        return counts;
    }

    private void appendActionHistory(String target, String action, String actor, String reason, String duration, String extraLine) {
        if (data == null) {
            return;
        }
        ConfigurationSection playerSection = getPlayerSection(target);
        playerSection.set("name", target);
        if (!playerSection.contains("fake-profile")) {
            playerSection.set("fake-profile", false);
        }
        playerSection.set("last-updated", nowStamp());

        ConfigurationSection historySection = ensureSection(playerSection, "history");
        List<Map<String, Object>> entries = copyMapList(historySection.getMapList("entries"));
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("action", normalizeActionKey(action));
        entry.put("actor", clean(actor, "Unknown"));
        entry.put("reason", clean(reason, getDefaultReason()));
        entry.put("issued-at", nowStamp());
        if (duration != null && !duration.trim().isEmpty()) {
            entry.put("duration", duration.trim());
        }
        if (extraLine != null && !extraLine.trim().isEmpty()) {
            entry.put("details", extraLine.trim());
        }
        entries.add(entry);
        historySection.set("count", entries.size());
        historySection.set("entries", entries);
        markDirty();
    }

    private List<Map<String, Object>> copyMapList(List<Map<?, ?>> entries) {
        List<Map<String, Object>> copied = new ArrayList<>();
        for (Map<?, ?> entry : entries) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> value : entry.entrySet()) {
                if (value.getKey() != null) {
                    normalized.put(String.valueOf(value.getKey()), value.getValue());
                }
            }
            copied.add(normalized);
        }
        return copied;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void logAction(String action, String actor, String target, String reason, String duration, String extraLine) {
        appendActionHistory(target, action, actor, reason, duration, extraLine);

        if (plugin.getConfig().getBoolean("discord.moderation.log-to-console", true)) {
            plugin.writeConsoleLine(buildConsoleMessage(action, actor, target, reason, duration, extraLine));
        }

        if (!plugin.getConfig().getBoolean("discord.moderation.enabled", true)) {
            return;
        }

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
        String username = clean(plugin.getConfig().getString("discord.moderation.webhook-username"), "SF-Core Moderation");
        String avatarUrl = clean(plugin.getConfig().getString("discord.moderation.avatar-url"), "");
        String footerIconUrl = clean(plugin.getConfig().getString("discord.moderation.footer-icon-url"), avatarUrl);
        String timestamp = Instant.now().toString();
        String payload = buildDiscordPayload(username, avatarUrl, footerIconUrl, action, actor, target, reason, duration, extraLine, serverId, channelId, timestamp);

        plugin.getSchedulerAdapter().runAsync(() -> sendWebhook(webhookUrl, payload));
    }

    private String buildConsoleMessage(String action, String actor, String target, String reason, String duration, String extraLine) {
        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.GOLD).append(ChatColor.BOLD).append("Moderation:").append(ChatColor.RESET).append(' ');
        builder.append(ChatColor.YELLOW).append(formatActionVerb(action)).append(ChatColor.RESET).append(' ');
        builder.append(ChatColor.AQUA).append(target).append(ChatColor.RESET);
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
        if (action == null) {
            return "Updated";
        }
        switch (action.toLowerCase(Locale.ENGLISH)) {
            case "ban":
                return "Banned";
            case "unban":
                return "Unbanned";
            case "kick":
                return "Kicked";
            case "mute":
                return "Muted";
            case "unmute":
                return "Unmuted";
            case "tempban":
                return "Temp-Banned";
            case "warn":
                return "Warned";
            default:
                return action;
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
                .append("\"description\":\"").append(escapeJson("A staff moderation action was recorded by SF-Core.")).append("\",")
                .append("\"color\":").append(discordColor(action)).append(",")
                .append("\"fields\":[").append(fields).append("],")
                .append("\"footer\":{")
                .append("\"text\":\"").append(escapeJson("SF-Core Moderation")).append("\"")
                .append(footerIconUrl.isEmpty() ? "" : ",\"icon_url\":\"" + escapeJson(footerIconUrl) + "\"")
                .append("},")
                .append("\"timestamp\":\"").append(escapeJson(timestamp)).append("\"")
                .append("}]")
                .append("}")
                .toString();
    }

    private String asInlineCode(String value) {
        return "`" + value + "`";
    }

    private void appendDiscordField(StringBuilder builder, String name, String value, boolean inline) {
        if (builder.length() > 0) {
            builder.append(',');
        }
        builder.append('{')
                .append("\"name\":\"").append(escapeJson(name)).append("\",")
                .append("\"value\":\"").append(escapeJson(value)).append("\",")
                .append("\"inline\":").append(inline)
                .append('}');
    }

    private int discordColor(String action) {
        if (action == null) {
            return 16766720;
        }
        switch (action.toLowerCase(Locale.ENGLISH)) {
            case "ban":
                return 15158332;
            case "tempban":
                return 15105570;
            case "unban":
                return 3066993;
            case "kick":
                return 16098851;
            case "mute":
                return 10181046;
            case "unmute":
                return 3447003;
            case "warn":
                return 15844367;
            default:
                return 16766720;
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
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String nowStamp() {
        return DISPLAY_TIMESTAMP.format(Instant.now());
    }

    private String normalizeActionKey(String value) {
        return clean(value, "updated").toLowerCase(Locale.ENGLISH);
    }

    private String normalize(String value) {
        return clean(value, "").toLowerCase(Locale.ENGLISH);
    }

    private String clean(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private Long parseDuration(String input) {
        String configured = clean(input, DEFAULT_TEMPBAN_DURATION).toLowerCase(Locale.ENGLISH);
        if (configured.length() < 2) {
            return null;
        }
        char unit = configured.charAt(configured.length() - 1);
        String rawAmount = configured.substring(0, configured.length() - 1).trim();
        long amount;
        try {
            amount = Long.parseLong(rawAmount);
        } catch (NumberFormatException exception) {
            return null;
        }
        if (amount <= 0L) {
            return null;
        }
        switch (unit) {
            case 'm':
                return amount * 60L * 1000L;
            case 'h':
                return amount * 60L * 60L * 1000L;
            case 'd':
                return amount * 24L * 60L * 60L * 1000L;
            case 'w':
                return amount * 7L * 24L * 60L * 60L * 1000L;
            default:
                return null;
        }
    }

    private String escapeJson(String input) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            switch (character) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(character);
                    break;
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

        public String getName() {
            return name;
        }

        public UUID getUniqueId() {
            return uniqueId;
        }

        public boolean isFakeProfile() {
            return fakeProfile;
        }

        public Player getPlayer() {
            return player;
        }

        public boolean isOnline() {
            return player != null && player.isOnline();
        }
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

        public String getTarget() {
            return target;
        }

        public String getReason() {
            return reason;
        }

        public String getSource() {
            return source;
        }

        public Date getExpiresAt() {
            return expiresAt == null ? null : new Date(expiresAt.getTime());
        }

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

        public String getActor() {
            return actor;
        }

        public String getReason() {
            return reason;
        }

        public String getIssuedAt() {
            return issuedAt;
        }
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

        public String getActor() {
            return actor;
        }

        public String getReason() {
            return reason;
        }

        public String getIssuedAt() {
            return issuedAt;
        }
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

        public String getAction() {
            return action;
        }

        public String getActor() {
            return actor;
        }

        public String getReason() {
            return reason;
        }

        public String getIssuedAt() {
            return issuedAt;
        }

        public String getDuration() {
            return duration;
        }

        public String getDetails() {
            return details;
        }
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

        public ResolvedProfile getProfile() {
            return profile;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }

        public BanView getActiveBan() {
            return activeBan;
        }

        public MuteView getActiveMute() {
            return activeMute;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public WarningView getLastWarning() {
            return lastWarning;
        }

        public int getActionCount(String action) {
            Integer count = actionCounts.get(action);
            return count == null ? 0 : count;
        }

        public ActionHistoryEntry getLastAction() {
            return lastAction;
        }

        public List<ActionHistoryEntry> getRecentActions() {
            return new ArrayList<>(recentActions);
        }
    }
}
