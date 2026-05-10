package dev.sergeantfuzzy.sfcore.listener.teleport;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class BackListener implements Listener {

    private final Main plugin;

    public BackListener(Main plugin) {
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

