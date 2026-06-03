package dev.zcripted.obx.feature.staff.service;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the wall-clock time at which each currently-online player joined the
 * server, so the staff GUI can show "time online since recent join" without
 * relying on {@code OfflinePlayer.getLastLogin()} (which is missing on the
 * 1.8 spigot-api baseline this plugin compiles against).
 *
 * <p>State is purely in-memory and is keyed by {@link UUID}. {@link PlayerQuitEvent}
 * removes the entry so an online lookup never returns a stale timestamp from a
 * previous session.
 */
public final class StaffSessionTracker implements Listener {

    private final Map<UUID, Long> sessionStarts = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sessionStarts.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        sessionStarts.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Returns the wall-clock millisecond timestamp at which {@code player}
     * joined their current session, or {@link System#currentTimeMillis()}
     * when the tracker hasn't observed a join yet (e.g. during startup before
     * the listener was registered, or for the staff member running /staff
     * during the same tick they joined).
     */
    public long getSessionStart(UUID uuid) {
        Long start = sessionStarts.get(uuid);
        return start == null ? System.currentTimeMillis() : start;
    }

    /**
     * Returns the elapsed milliseconds since {@code player} joined their
     * current session, clamped at zero.
     */
    public long getSessionDuration(UUID uuid) {
        long start = getSessionStart(uuid);
        long now = System.currentTimeMillis();
        return Math.max(0L, now - start);
    }
}
