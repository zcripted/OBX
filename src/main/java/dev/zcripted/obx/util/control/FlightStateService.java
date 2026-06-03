package dev.zcripted.obx.util.control;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.storage.SqliteDataStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class FlightStateService implements Listener {

    private static final float DEFAULT_SPEED = 0.1f;

    private final Main plugin;
    private final SqliteDataStore store;

    public FlightStateService(Main plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            plugin.getLogger().warning("FlightStateService disabled — SQLite store unavailable.");
            return;
        }
        store.execute("CREATE TABLE IF NOT EXISTS fly_state (" +
                "uuid TEXT PRIMARY KEY," +
                "allow_flight INTEGER NOT NULL DEFAULT 0," +
                "is_flying INTEGER NOT NULL DEFAULT 0," +
                "fly_speed REAL NOT NULL DEFAULT 0.1," +
                "walk_speed REAL NOT NULL DEFAULT 0.2)");
    }

    public void setFlight(Player player, boolean enabled) {
        if (player == null) return;
        player.setAllowFlight(enabled);
        player.setFlying(enabled && player.isFlying());
        if (enabled) {
            player.setFlying(true);
        }
        if (!store.isAvailable()) return;
        store.executeUpdateAsync(
                "INSERT INTO fly_state (uuid, allow_flight, is_flying, fly_speed, walk_speed) VALUES (?, ?, ?, ?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET allow_flight = excluded.allow_flight, is_flying = excluded.is_flying",
                player.getUniqueId(), enabled, enabled, player.getFlySpeed(), player.getWalkSpeed());
    }

    public boolean isFlightAllowed(Player player) {
        return player != null && player.getAllowFlight();
    }

    public void setFlySpeed(Player player, float speed) {
        if (player == null) return;
        float clamped = Math.max(-1.0f, Math.min(1.0f, speed));
        player.setFlySpeed(clamped);
        if (!store.isAvailable()) return;
        store.executeUpdateAsync(
                "INSERT INTO fly_state (uuid, fly_speed) VALUES (?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET fly_speed = excluded.fly_speed",
                player.getUniqueId(), clamped);
    }

    public void setWalkSpeed(Player player, float speed) {
        if (player == null) return;
        float clamped = Math.max(-1.0f, Math.min(1.0f, speed));
        player.setWalkSpeed(clamped);
        if (!store.isAvailable()) return;
        store.executeUpdateAsync(
                "INSERT INTO fly_state (uuid, walk_speed) VALUES (?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET walk_speed = excluded.walk_speed",
                player.getUniqueId(), clamped);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!store.isAvailable()) return;
        UUID uuid = player.getUniqueId();
        store.queryFirst(
                "SELECT allow_flight, is_flying, fly_speed, walk_speed FROM fly_state WHERE uuid = ?",
                rs -> {
                    boolean allow = rs.getInt("allow_flight") == 1;
                    boolean flying = rs.getInt("is_flying") == 1;
                    float flySpeed = (float) rs.getDouble("fly_speed");
                    float walkSpeed = (float) rs.getDouble("walk_speed");
                    if (allow && player.hasPermission("obx.fly")) {
                        player.setAllowFlight(true);
                        player.setFlying(flying);
                    }
                    if (flySpeed > 0.0f && flySpeed <= 1.0f) {
                        player.setFlySpeed(flySpeed);
                    }
                    if (walkSpeed > 0.0f && walkSpeed <= 1.0f) {
                        player.setWalkSpeed(walkSpeed);
                    }
                    return Boolean.TRUE;
                },
                uuid);
    }

    public float scaleFromInput(double input) {
        // Accepts a CMI-style 0-10 range and maps to Bukkit's -1.0 to 1.0
        if (input <= 0.0) return 0.0f;
        if (input >= 10.0) return 1.0f;
        return (float) (input / 10.0);
    }

    public double scaleToDisplay(float value) {
        return value * 10.0;
    }
}
