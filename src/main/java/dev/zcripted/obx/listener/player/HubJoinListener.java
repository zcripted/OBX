package dev.zcripted.obx.listener.player;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.hub.HubService;
import dev.zcripted.obx.hub.kit.HubKitApplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Applies the hub kit when a player enters a hub world via join, respawn,
 * or world-change. Early-exits when hub-mode is off so listener overhead in
 * the non-hub state is a single boolean read.
 */
public final class HubJoinListener implements Listener {

    private final Main plugin;
    private final HubService hub;
    private final HubKitApplier applier;

    public HubJoinListener(Main plugin, HubService hub, HubKitApplier applier) {
        this.plugin = plugin;
        this.hub = hub;
        this.applier = applier;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!hub.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!hub.isInHubWorld(player)) {
            return;
        }
        // Apply on the next tick so any other plugin's join-handler restores
        // can settle first — matches the cleanest behaviour seen in lobby
        // plugins like CMI/Hub.
        plugin.getSchedulerAdapter().runLater(() -> applier.apply(player), 2L);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!hub.isEnabled() || !hub.kitGiveOnRespawn()) {
            return;
        }
        Player player = event.getPlayer();
        // Respawn fires before the player is fully re-placed in the world,
        // so apply on the next tick when getWorld() is reliable.
        plugin.getSchedulerAdapter().runLater(() -> {
            if (hub.isInHubWorld(player)) {
                applier.apply(player);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!hub.isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        boolean nowInHub = hub.isInHubWorld(player);
        boolean wasInHub = event.getFrom() != null && hub.isHubWorld(event.getFrom().getName());
        if (nowInHub && !wasInHub) {
            applier.apply(player);
        } else if (!nowInHub && wasInHub) {
            // Optional: clear the kit when leaving a hub world. We don't
            // forcibly clear inventory here because the destination world
            // may legitimately host the player's stored inventory (managed
            // by per-world inventory plugins). Strip hub flight grant only.
            try {
                if (!player.getGameMode().name().equals("CREATIVE")) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
