package dev.sergeantfuzzy.sfcore.tablist.scheduler;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.platform.scheduler.SchedulerAdapter;
import dev.sergeantfuzzy.sfcore.tablist.format.TablistRenderer;
import dev.sergeantfuzzy.sfcore.tablist.service.TablistService;
import org.bukkit.entity.Player;

/**
 * Periodically pushes the configured tablist to every online player so
 * placeholders like {@code {online}}, {@code {ping}}, {@code {time}}, and
 * {@code {tps}} stay current. Cancellable so that a {@code /sf reload}
 * (or a config change in {@code systems/tablist.yml}) can restart with a
 * fresh refresh interval.
 */
public final class TablistRefreshTask {

    private final Main plugin;
    private final TablistService service;
    private SchedulerAdapter.CancellableTask task;

    public TablistRefreshTask(Main plugin, TablistService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void start() {
        cancel();
        if (!service.isEnabled()) {
            return;
        }
        int interval = service.getRefreshIntervalTicks();
        Runnable refresh = () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                TablistRenderer.apply(plugin, service, player);
            }
        };
        if (interval <= 0) {
            // One-shot push for everyone currently online; further updates only run on join.
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
                // Already cancelled - no-op.
            }
            task = null;
        }
    }
}
