package dev.zcripted.obx.feature.scoreboard.scheduler;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.feature.scoreboard.format.ScoreboardRenderer;
import dev.zcripted.obx.api.scoreboard.ScoreboardService;
import org.bukkit.entity.Player;

/**
 * Periodically re-renders the sidebar for every online player so live fields
 * ({@code {health}}, {@code {health_percent}}, {@code {hearts}}, {@code {online}})
 * stay current. Cancellable so a {@code /obx reload} (or a {@code systems/scoreboard.yml}
 * change) can restart it with a fresh interval. When the board is disabled, every
 * player is returned to the main scoreboard.
 */
public final class ScoreboardRefreshTask {

    private final ObxPlugin plugin;
    private final ScoreboardService service;
    private SchedulerAdapter.CancellableTask task;

    public ScoreboardRefreshTask(ObxPlugin plugin, ScoreboardService service) {
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
