package dev.zcripted.obx.feature.world.service;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PerPlayerTimeService implements Listener {

    private final OBX plugin;
    private final SqliteDataStore store;

    public PerPlayerTimeService(OBX plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            plugin.getLogger().warning("PerPlayerTimeService disabled — SQLite store unavailable.");
            return;
        }
        store.execute("CREATE TABLE IF NOT EXISTS player_time (" +
                "uuid TEXT PRIMARY KEY," +
                "player_time INTEGER NOT NULL," +
                "relative INTEGER NOT NULL DEFAULT 1)");
    }

    public void setTime(Player player, long time, boolean relative) {
        if (player == null) return;
        player.setPlayerTime(time, relative);
        if (store.isAvailable()) {
            store.executeUpdateAsync(
                    "INSERT INTO player_time (uuid, player_time, relative) VALUES (?, ?, ?)" +
                            " ON CONFLICT(uuid) DO UPDATE SET player_time = excluded.player_time, relative = excluded.relative",
                    player.getUniqueId(), time, relative ? 1 : 0);
        }
    }

    public void reset(Player player) {
        if (player == null) return;
        player.resetPlayerTime();
        if (store.isAvailable()) {
            store.executeUpdateAsync("DELETE FROM player_time WHERE uuid = ?", player.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!store.isAvailable()) return;
        UUID uuid = player.getUniqueId();
        store.queryFirst("SELECT player_time, relative FROM player_time WHERE uuid = ?", rs -> {
            long time = rs.getLong("player_time");
            boolean relative = rs.getInt("relative") == 1;
            player.setPlayerTime(time, relative);
            return Boolean.TRUE;
        }, uuid);
    }
}
