package dev.zcripted.obx.feature.scoreboard.scheduler;

import dev.zcripted.obx.core.ObxPlugin;
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
    private dev.zcripted.obx.core.platform.scheduler.CancellableTask task;

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
        // On Folia, scoreboard mutation must run on each player's entity region, not the
        // global region the repeating task fires on — otherwise the sidebar never applies.
        final boolean folia = plugin.getSchedulerAdapter().isFolia();
        Runnable refresh = () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (folia) {
                    plugin.getSchedulerAdapter().runAtEntity(player, () -> ScoreboardRenderer.apply(plugin, service, player));
                } else {
                    ScoreboardRenderer.apply(plugin, service, player);
                }
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