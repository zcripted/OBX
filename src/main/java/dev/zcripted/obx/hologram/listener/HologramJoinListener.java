package dev.zcripted.obx.hologram.listener;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.hologram.service.HologramService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Re-synchronizes a player's hologram visibility on the four lifecycle events
 * where the client may have dropped or lost the entities:
 * join, respawn, world change, quit (clears state for the leaver).
 *
 * <p>Resource-pack reload is handled separately by
 * {@link HologramResourcePackListener} because Spigot 1.12 does not expose
 * the same event class as Paper 1.17+.
 */
public final class HologramJoinListener implements Listener {

    private final Main plugin;
    private final HologramService service;

    public HologramJoinListener(Main plugin, HologramService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (service == null || !service.isActive() || service.getRenderer() == null) {
            return;
        }
        // Delay a tick — Paper sometimes finishes the player's chunk send after
        // the join event fires, and showEntity called too early can no-op.
        final Player player = event.getPlayer();
        plugin.getSchedulerAdapter().runLater(new Runnable() {
            @Override
            public void run() {
                service.getRenderer().resyncPlayer(player);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        if (service == null || !service.isActive() || service.getRenderer() == null) {
            return;
        }
        final Player player = event.getPlayer();
        plugin.getSchedulerAdapter().runLater(new Runnable() {
            @Override
            public void run() {
                service.getRenderer().resyncPlayer(player);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (service == null || !service.isActive() || service.getRenderer() == null) {
            return;
        }
        service.getRenderer().resyncPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (service == null) {
            return;
        }
        // Remove the leaver from every hologram's viewer set so the next tick
        // does not try to update visibility for an offline player.
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        for (dev.zcripted.obx.hologram.model.Hologram hologram : service.getRegistry().all()) {
            hologram.getCurrentViewers().remove(uuid);
        }
        // Clear per-player page cursors so the next player to spawn on the
        // same UUID (rare but possible on cracked / offline-mode servers)
        // starts on page 0.
        dev.zcripted.obx.hologram.text.PageState.clear(uuid);
    }
}
