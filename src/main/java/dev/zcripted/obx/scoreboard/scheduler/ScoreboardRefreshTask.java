package dev.zcripted.obx.scoreboard.scheduler;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.scoreboard.format.ScoreboardRenderer;
import dev.zcripted.obx.scoreboard.service.ScoreboardService;
import org.bukkit.entity.Player;

/**
 * Periodically re-renders the sidebar for every online player so live fields
 * ({@code {health}}, {@code {health_percent}}, {@code {hearts}}, {@code {online}})
 * stay current. Cancellable so a {@code /obx reload} (or a {@code systems/scoreboard.yml}
 * change) can restart it with a fresh interval. When the board is disabled, every
 * player is returned to the main scoreboard.
 */
public final class ScoreboardRefreshTask {

    private final OBX plugin;
    private final ScoreboardService service;
    private SchedulerAdapter.CancellableTask task;

    public ScoreboardRefreshTask(OBX plugin, ScoreboardService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void start() {
        cancel();
        if (!service.isEnabled()) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                ScoreboardRenderer.clear(player);
            }
            return;
        }
        int interval = service.getRefreshIntervalTicks();
        Runnable refresh = () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                ScoreboardRenderer.apply(plugin, service, player);
            }
        };
        if (interval <= 0) {
            plugin.getSchedulerAdapter().runNow(refresh);
            return;
        }
        task = plugin.getSchedulerAdapter().runRepeating(refresh, interval, interval);
    }

    public void cancel() {
        if (task != null) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
                // already cancelled
            }
            task = null;
        }
    }
}
