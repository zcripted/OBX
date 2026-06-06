package dev.zcripted.obx.core.platform.resourcepack;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class ResourcePackListener implements Listener {

    private final ObxPlugin plugin;
    private final AutoResourcePackManager manager;

    public ResourcePackListener(ObxPlugin plugin, AutoResourcePackManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (manager == null || !manager.isEnabled()) {
            return;
        }
        // Re-check at execution time: the 20-tick delay can outlive a module/plugin
        // disable or a player quitting, so guard against applying a pack to a stale
        // manager or an offline player. Dispatched on the player's OWN region thread
        // (runAtEntityLater) — setResourcePack is a per-player op that must not run from
        // the global region thread on Folia (would trip thread-ownership checks).
        plugin.getSchedulerAdapter().runAtEntityLater(event.getPlayer(), () -> {
            if (manager.isEnabled() && event.getPlayer().isOnline()) {
                manager.applyPackOnJoin(event.getPlayer());
            }
        }, null, 20L);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (manager == null || !manager.isEnabled()) {
            return;
        }
        manager.handlePackStatus(event);
    }
}
