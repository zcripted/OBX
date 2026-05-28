package dev.sergeantfuzzy.sfcore.tablist.listener;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.tablist.format.TablistRenderer;
import dev.sergeantfuzzy.sfcore.tablist.format.TablistTeams;
import dev.sergeantfuzzy.sfcore.tablist.service.TablistService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Pushes the configured tablist to a player as soon as they finish joining.
 * Runs on a one-tick delay so the join event has a chance to update
 * {@link org.bukkit.Server#getOnlinePlayers()} before the {{online}}
 * placeholder is resolved. Routed through the SF-Core scheduler adapter so
 * the same code path is safe on Folia (entity-bound) and Bukkit-style forks.
 */
public final class TablistJoinListener implements Listener {

    private final Main plugin;
    private final TablistService service;

    public TablistJoinListener(Main plugin, TablistService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        plugin.getSchedulerAdapter().runLater(() -> {
            if (event.getPlayer().isOnline()) {
                TablistRenderer.apply(plugin, service, event.getPlayer());
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        // Drop the leaver from the SF-Core tablist teams so their name doesn't
        // linger in a team's entry list after they disconnect.
        TablistTeams.remove(event.getPlayer());
    }
}
