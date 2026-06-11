package dev.zcripted.obx.feature.hologram.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hologram.packet.PacketAvailability;
import dev.zcripted.obx.feature.hologram.packet.PacketChannelInjector;
import dev.zcripted.obx.feature.hologram.service.HologramService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Wires the Netty channel injector to the player lifecycle. Inject on join
 * (deferred to a tick later — Paper finishes the client handshake mid-event),
 * eject on quit. When the packet layer is unavailable this listener no-ops
 * entirely; the renderer's raycast fallback handles interaction.
 */
public final class HologramConnectionListener implements Listener {

    private final ObxPlugin plugin;
    private final HologramService service;

    public HologramConnectionListener(ObxPlugin plugin, HologramService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (service == null || !service.isActive() || !PacketAvailability.isAvailable()) {
            return;
        }
        final Player player = event.getPlayer();
        plugin.getSchedulerAdapter().runLater(new Runnable() {
            @Override
            public void run() {
                PacketChannelInjector.inject(plugin, service, player);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!PacketAvailability.isAvailable()) {
            return;
        }
        PacketChannelInjector.eject(plugin, event.getPlayer());
    }
}