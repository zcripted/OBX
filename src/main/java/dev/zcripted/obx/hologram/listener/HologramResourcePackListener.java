package dev.zcripted.obx.hologram.listener;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.hologram.service.HologramService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/**
 * Re-shows holograms after a resource-pack reload. Vanilla Minecraft drops
 * every client-side entity reference when the pack swaps, so display entities
 * spawn but never appear until we re-issue {@code showEntity} for the player.
 * (Plan §9 — "Players see stale holograms after resource-pack reload".)
 *
 * <p>{@link PlayerResourcePackStatusEvent} is available on Spigot 1.8+.
 */
public final class HologramResourcePackListener implements Listener {

    private final OBX plugin;
    private final HologramService service;

    public HologramResourcePackListener(OBX plugin, HologramService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        if (service == null || !service.isActive() || service.getRenderer() == null) {
            return;
        }
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        if (status != PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED
                && status != PlayerResourcePackStatusEvent.Status.ACCEPTED) {
            return;
        }
        final Player player = event.getPlayer();
        plugin.getSchedulerAdapter().runLater(new Runnable() {
            @Override
            public void run() {
                service.getRenderer().resyncPlayer(player);
            }
        }, 4L);
    }
}
