package dev.zcripted.obx.feature.scoreboard.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.scoreboard.format.ScoreboardRenderer;
import dev.zcripted.obx.feature.scoreboard.service.ScoreboardService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Shows the sidebar to a player once they finish joining (one-tick delay so the
 * online count is current). The periodic {@code ScoreboardRefreshTask} keeps it
 * updated thereafter.
 */
public final class ScoreboardJoinListener implements Listener {

    private final ObxPlugin plugin;
    private final ScoreboardService service;

    public ScoreboardJoinListener(ObxPlugin plugin, ScoreboardService service) {
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
                ScoreboardRenderer.apply(plugin, service, event.getPlayer());
            }
        }, 2L);
    }
}
