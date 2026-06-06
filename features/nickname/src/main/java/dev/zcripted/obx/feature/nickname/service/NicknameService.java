package dev.zcripted.obx.feature.nickname.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import dev.zcripted.obx.util.text.MessageSanitizer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NicknameService {

    private final ObxPlugin plugin;
    private final SqliteDataStore store;
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();
    /**
     * Visible characters a nickname may contain (applied to the color-stripped form). The
     * default — ASCII letters, digits, underscore and single spaces — blocks Unicode homoglyph
     * impersonation (e.g. Cyrillic look-alikes) while still permitting multi-word nicknames.
     * Servers can widen it via {@code nickname.allowed-pattern} in config.yml.
     */
    private final java.util.regex.Pattern allowedPattern;

    public NicknameService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
        this.allowedPattern = compilePattern(
                plugin.getConfig().getString("nickname.allowed-pattern", "[A-Za-z0-9_ ]+"));
    }

    private java.util.regex.Pattern compilePattern(String regex) {
        try {
            return java.util.regex.Pattern.compile(regex);
        } catch (RuntimeException invalid) {
            plugin.getLogger().warning("Invalid nickname.allowed-pattern '" + regex + "', using default.");
            return java.util.regex.Pattern.compile("[A-Za-z0-9_ ]+");
        }
    }

    /** Trims and collapses internal whitespace so a trailing/padded nick can't dodge the taken-check. */
    public String normalize(String raw) {
        return raw == null ? null : raw.trim().replaceAll("\\s+", " ");
    }

    /**
     * True if the color-stripped nickname is within the allowed character set. Combined with
     * {@link #normalize(String)} and {@link #isNameTaken(UUID, String)}, this closes the
     * whitespace-padding and homoglyph impersonation vectors.
     */
    public boolean isAllowedNick(String strippedNick) {
        return strippedNick != null && !strippedNick.isEmpty() && allowedPattern.matcher(strippedNick).matches();
    }

    public void load() {
        if (!store.isAvailable()) {
            plugin.getLogger().warning("NicknameService disabled — SQLite store unavailable.");
            return;
        }
        store.execute("CREATE TABLE IF NOT EXISTS nicknames (" +
                "uuid TEXT PRIMARY KEY," +
                "nickname TEXT NOT NULL)");
        cache.clear();
        for (Map.Entry<UUID, String> row : store.queryAll(
                "SELECT uuid, nickname FROM nicknames",
                rs -> {
                    // Isolate a single corrupt uuid cell: returning null skips that one row instead of
                    // throwing out of the mapper and dropping EVERY nickname from the cache.
                    try {
                        return new java.util.AbstractMap.SimpleEntry<>(
                                UUID.fromString(rs.getString("uuid")), rs.getString("nickname"));
                    } catch (IllegalArgumentException badUuid) {
                        return null;
                    }
                })) {
            if (row != null && row.getKey() != null) {
                cache.put(row.getKey(), row.getValue());
            }
        }
    }

    public void reload() { load(); }

    public String getNickname(UUID uuid) {
        return uuid == null ? null : cache.get(uuid);
    }

    public boolean hasNickname(UUID uuid) {
        return uuid != null && cache.containsKey(uuid);
    }

    public void setNickname(Player player, String rawNickname, boolean allowColor) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (rawNickname == null || rawNickname.isEmpty()) {
            clearNickname(player);
            return;
        }
        // Neutralize MiniMessage angle brackets (and strip &-codes when colour isn't
        // allowed) so a nickname can't inject <click>/<hover> onto other players' screens
        // anywhere {displayname} is rendered through Adventure.
        String safe = MessageSanitizer.sanitize(rawNickname, allowColor);
        String colored = allowColor
                ? ChatColor.translateAlternateColorCodes('&', safe)
                : safe;
        cache.put(uuid, colored);
        if (store.isAvailable()) {
            store.executeUpdateAsync(
                    "INSERT INTO nicknames(uuid, nickname) VALUES (?, ?)" +
                            " ON CONFLICT(uuid) DO UPDATE SET nickname = excluded.nickname",
                    uuid, colored);
        }
        applyToPlayer(player, colored);
    }

    public void clearNickname(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        cache.remove(uuid);
        if (store.isAvailable()) {
            store.executeUpdateAsync("DELETE FROM nicknames WHERE uuid = ?", uuid);
        }
        applyToPlayer(player, player.getName());
    }

    public void applyOnJoin(Player player) {
        String stored = cache.get(player.getUniqueId());
        if (stored != null) {
            applyToPlayer(player, stored);
        }
    }

    /**
     * Returns true if {@code strippedNick} (color-stripped) would impersonate another player:
     * it matches another online player's real name or another player's stored nickname.
     */
    public boolean isNameTaken(UUID self, String strippedNick) {
        if (strippedNick == null || strippedNick.trim().isEmpty()) {
            return false;
        }
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.getUniqueId().equals(self) && online.getName().equalsIgnoreCase(strippedNick)) {
                return true;
            }
        }
        for (Map.Entry<UUID, String> row : cache.entrySet()) {
            if (!row.getKey().equals(self) && strippedNick.equalsIgnoreCase(ChatColor.stripColor(row.getValue()))) {
                return true;
            }
        }
        // Block impersonating an OFFLINE player's real name too, using Paper's non-blocking cached
        // lookup (never a synchronous Mojang web request). Skipped on non-Paper forks.
        try {
            Object cached = org.bukkit.Bukkit.class.getMethod("getOfflinePlayerIfCached", String.class)
                    .invoke(null, strippedNick);
            if (cached != null) {
                Object uuid = cached.getClass().getMethod("getUniqueId").invoke(cached);
                boolean playedBefore = Boolean.TRUE.equals(cached.getClass().getMethod("hasPlayedBefore").invoke(cached));
                if (playedBefore && !(uuid instanceof UUID && uuid.equals(self))) {
                    return true;
                }
            }
        } catch (Throwable ignoredNoPaperApi) {
            // Older/non-Paper fork without getOfflinePlayerIfCached — online + nick checks above stand.
        }
        return false;
    }

    /** The color-stripped form of what {@code rawNickname} would render as (for impersonation checks). */
    public String previewStripped(String rawNickname, boolean allowColor) {
        String safe = MessageSanitizer.sanitize(rawNickname, allowColor);
        return ChatColor.stripColor(allowColor ? ChatColor.translateAlternateColorCodes('&', safe) : safe);
    }

    private void applyToPlayer(Player player, String displayName) {
        // Re-neutralize on apply so even a nickname stored before this fix can't carry
        // MiniMessage tags into {displayname} surfaces.
        String safeDisplay = displayName == null ? player.getName() : displayName.replace('<', '‹').replace('>', '›');
        player.setDisplayName(safeDisplay);
        displayName = safeDisplay;
        // setPlayerListName truncates to 16 chars on legacy clients; trim to keep behaviour predictable.
        String tab = ChatColor.stripColor(displayName);
        if (tab != null && tab.length() > 16) {
            tab = tab.substring(0, 16);
        }
        try {
            // Never substring a colored name (it can split a §x color sequence and
            // leak a stray §). If it fits, keep the colored name; otherwise fall back
            // to the color-stripped, ≤16 tab name.
            player.setPlayerListName(displayName.length() <= 16 ? displayName : tab);
        } catch (IllegalArgumentException ignored) {
            player.setPlayerListName(tab);
        }
    }
}
