package dev.zcripted.obx.platform.bukkit.resourcepack;

import dev.zcripted.obx.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class ResourcePackListener implements Listener {

    private final Main plugin;
    private final AutoResourcePackManager manager;

    public ResourcePackListener(Main plugin, AutoResourcePackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (manager == null || !manager.isEnabled()) {
            return;
        }
        plugin.getSchedulerAdapter().runLater(() -> manager.applyPackOnJoin(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (manager == null || !manager.isEnabled()) {
            return;
        }
        manager.handlePackStatus(event);
    }
}
