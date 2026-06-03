package dev.zcripted.obx.feature.teleport.listener;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class BackListener implements Listener {

    private final ObxPlugin plugin;

    public BackListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        plugin.getDataService().setBack(event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        plugin.getDataService().setBack(event.getPlayer().getUniqueId(), event.getFrom());
    }
}

