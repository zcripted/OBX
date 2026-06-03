package dev.zcripted.obx.messaging;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.storage.SqliteDataStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageService {

    public static final int MAILBOX_LIMIT = 50;

    public static final class MailEntry {
        private final String from;
        private final String fromUuid;
        private final String body;
        private final String sentAt;

        public MailEntry(String from, String fromUuid, String body, String sentAt) {
            this.from = from;
            this.fromUuid = fromUuid;
            this.body = body;
            this.sentAt = sentAt;
        }

        public String getFrom() { return from; }
        public String getFromUuid() { return fromUuid; }
        public String getBody() { return body; }
        public String getSentAt() { return sentAt; }
    }

    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")
            .withZone(ZoneId.of("America/Detroit"));

    private final Main plugin;
    private final SqliteDataStore store;
    private final java.util.Map<UUID, UUID> lastRecipient = new ConcurrentHashMap<>();
    private final Set<UUID> socialSpies = ConcurrentHashMap.newKeySet();

    public MessageService(Main plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            plugin.getLogger().warning("MessageService disabled — SQLite store unavailable.");
            return;
        }
        store.execute("CREATE TABLE IF NOT EXISTS ignores (" +
                "owner_uuid TEXT NOT NULL," +
                "target_uuid TEXT NOT NULL," +
                "PRIMARY KEY (owner_uuid, target_uuid))");
        store.execute("CREATE TABLE IF NOT EXISTS mail (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "recipient_uuid TEXT NOT NULL," +
                "from_uuid TEXT," +
                "from_name TEXT," +
                "body TEXT," +
                "sent_at TEXT NOT NULL)");
        store.execute("CREATE INDEX IF NOT EXISTS idx_mail_recipient ON mail(recipient_uuid)");
        migrateLegacyYaml();
    }

    public void reload() { load(); }
    public void save() { /* auto-commit */ }

    private void migrateLegacyYaml() {
        File legacy = new File(plugin.getDataFolder(), "messaging.yml");
        if (!legacy.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            renameMigrated(legacy);
            return;
        }
        int ignoresMigrated = 0;
        int mailMigrated = 0;
        for (String uuidKey : players.getKeys(false)) {
            ConfigurationSection playerSection = players.getConfigurationSection(uuidKey);
            if (playerSection == null) continue;
            UUID owner;
            try { owner = UUID.fromString(uuidKey); } catch (IllegalArgumentException ignored) { continue; }
            for (String target : playerSection.getStringList("ignored")) {
                try {
                    UUID targetUuid = UUID.fromString(target);
                    store.executeUpdate("INSERT OR IGNORE INTO ignores(owner_uuid, target_uuid) VALUES(?, ?)",
                            owner, targetUuid);
                    ignoresMigrated++;
                } catch (IllegalArgumentException ignored) { /* skip */ }
            }
            ConfigurationSection mailSection = playerSection.getConfigurationSection("mail");
            if (mailSection != null) {
                for (String mailId : mailSection.getKeys(false)) {
                    ConfigurationSection entry = mailSection.getConfigurationSection(mailId);
                    if (entry == null) continue;
                    store.executeUpdate(
                            "INSERT INTO mail (recipient_uuid, from_uuid, from_name, body, sent_at) VALUES (?, ?, ?, ?, ?)",
                            owner, entry.getString("fromUuid", ""), entry.getString("from", "Unknown"),
                            entry.getString("body", ""), entry.getString("sentAt", ""));
                    mailMigrated++;
                }
            }
        }
        plugin.getLogger().info("Migrated " + ignoresMigrated + " ignore row(s) and " + mailMigrated + " mail row(s) from messaging.yml.");
        renameMigrated(legacy);
    }

    private void renameMigrated(File legacy) {
        File renamed = new File(legacy.getParentFile(), legacy.getName() + ".migrated");
        if (!legacy.renameTo(renamed)) {
            plugin.getLogger().warning("Could not rename " + legacy.getName() + " after migration.");
        }
    }

    public void setLastRecipient(UUID sender, UUID receiver) {
        if (sender == null || receiver == null) return;
        lastRecipient.put(sender, receiver);
        lastRecipient.put(receiver, sender);
    }

    public UUID getLastRecipient(UUID uuid) {
        return uuid == null ? null : lastRecipient.get(uuid);
    }

    public boolean isSocialSpy(UUID uuid) {
        return uuid != null && socialSpies.contains(uuid);
    }

    public boolean toggleSocialSpy(UUID uuid) {
        if (uuid == null) return false;
        if (socialSpies.remove(uuid)) return false;
        socialSpies.add(uuid);
        return true;
    }

    public Set<UUID> getSocialSpies() {
        return Collections.unmodifiableSet(socialSpies);
    }

    public boolean isIgnoring(UUID owner, UUID target) {
        if (owner == null || target == null || !store.isAvailable()) return false;
        return store.queryFirst("SELECT 1 FROM ignores WHERE owner_uuid = ? AND target_uuid = ?",
                rs -> rs.getInt(1), owner, target).isPresent();
    }

    public boolean addIgnore(UUID owner, UUID target) {
        if (owner == null || target == null || !store.isAvailable()) return false;
        if (isIgnoring(owner, target)) return false;
        store.executeUpdate("INSERT OR IGNORE INTO ignores(owner_uuid, target_uuid) VALUES(?, ?)", owner, target);
        return true;
    }

    public boolean removeIgnore(UUID owner, UUID target) {
        if (owner == null || target == null || !store.isAvailable()) return false;
        if (!isIgnoring(owner, target)) return false;
        store.executeUpdate("DELETE FROM ignores WHERE owner_uuid = ? AND target_uuid = ?", owner, target);
        return true;
    }

    public List<UUID> getIgnoredUuids(UUID owner) {
        if (owner == null || !store.isAvailable()) return Collections.emptyList();
        return store.queryAll("SELECT target_uuid FROM ignores WHERE owner_uuid = ?",
                rs -> {
                    try { return UUID.fromString(rs.getString("target_uuid")); }
                    catch (IllegalArgumentException ex) { return null; }
                }, owner);
    }

    public enum MailResult { OK, MAILBOX_FULL }

    public MailResult sendMail(UUID recipient, String recipientName, UUID fromUuid, String fromName, String body) {
        if (recipient == null || !store.isAvailable()) return MailResult.OK;
        int count = mailCount(recipient);
        if (count >= MAILBOX_LIMIT) return MailResult.MAILBOX_FULL;
        store.executeUpdate(
                "INSERT INTO mail (recipient_uuid, from_uuid, from_name, body, sent_at) VALUES (?, ?, ?, ?, ?)",
                recipient,
                fromUuid == null ? "" : fromUuid.toString(),
                fromName == null ? "Unknown" : fromName,
                body == null ? "" : body,
                DISPLAY_TIMESTAMP.format(Instant.now()));
        return MailResult.OK;
    }

    public List<MailEntry> readMail(UUID owner) {
        if (owner == null || !store.isAvailable()) return Collections.emptyList();
        return store.queryAll(
                "SELECT from_name, from_uuid, body, sent_at FROM mail WHERE recipient_uuid = ? ORDER BY id ASC",
                rs -> new MailEntry(
                        rs.getString("from_name"),
                        rs.getString("from_uuid"),
                        rs.getString("body"),
                        rs.getString("sent_at")),
                owner);
    }

    public int mailCount(UUID owner) {
        if (owner == null || !store.isAvailable()) return 0;
        return store.queryFirst("SELECT COUNT(*) AS c FROM mail WHERE recipient_uuid = ?",
                rs -> rs.getInt("c"), owner).orElse(0);
    }

    public int clearMail(UUID owner) {
        if (owner == null || !store.isAvailable()) return 0;
        int count = mailCount(owner);
        if (count > 0) {
            store.executeUpdate("DELETE FROM mail WHERE recipient_uuid = ?", owner);
        }
        return count;
    }

    public void clearSpiesOnQuit(UUID uuid) {
        if (uuid != null) {
            socialSpies.remove(uuid);
            lastRecipient.remove(uuid);
        }
    }
}
