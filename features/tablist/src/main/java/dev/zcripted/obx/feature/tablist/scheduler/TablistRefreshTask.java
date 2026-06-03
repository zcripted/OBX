package dev.zcripted.obx.feature.tablist.scheduler;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.feature.tablist.format.TablistRenderer;
import dev.zcripted.obx.feature.tablist.format.TablistTeams;
import dev.zcripted.obx.api.tablist.TablistService;
import org.bukkit.entity.Player;

/**
 * Periodically pushes the configured tablist to every online player so
 * placeholders like {@code {online}}, {@code {ping}}, {@code {time}}, and
 * {@code {tps}} stay current. Cancellable so that a {@code /obx reload}
 * (or a config change in {@code systems/tablist.yml}) can restart with a
 * fresh refresh interval.
 */
public final class TablistRefreshTask {

    private final ObxPlugin plugin;
    private final TablistService service;
    private dev.zcripted.obx.core.platform.scheduler.CancellableTask task;

    public TablistRefreshTask(ObxPlugin plugin, TablistService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void start() {
        cancel();
        if (!service.isEnabled()) {
            // Tablist off — drop any staff-grouping teams we may have created.
            TablistTeams.reset();
            return;
        }
        if (!service.isStaffGroupingEnabled()) {
            // Grouping toggled off at runtime — clean up the teams.
            TablistTeams.reset();
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
