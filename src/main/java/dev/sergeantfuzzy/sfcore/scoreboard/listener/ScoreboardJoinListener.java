package dev.sergeantfuzzy.sfcore.scoreboard.listener;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.scoreboard.format.ScoreboardRenderer;
import dev.sergeantfuzzy.sfcore.scoreboard.service.ScoreboardService;
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

    private final Main plugin;
    private final ScoreboardService service;

    public ScoreboardJoinListener(Main plugin, ScoreboardService service) {
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
