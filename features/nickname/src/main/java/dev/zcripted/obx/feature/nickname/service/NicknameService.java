package dev.zcripted.obx.feature.nickname.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NicknameService {

    private final ObxPlugin plugin;
    private final SqliteDataStore store;
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    public NicknameService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
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
                rs -> new java.util.AbstractMap.SimpleEntry<>(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("nickname")))) {
            cache.put(row.getKey(), row.getValue());
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
        String colored = allowColor
                ? ChatColor.translateAlternateColorCodes('&', rawNickname)
                : ChatColor.stripColor(rawNickname);
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

    private void applyToPlayer(Player player, String displayName) {
        player.setDisplayName(displayName);
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
