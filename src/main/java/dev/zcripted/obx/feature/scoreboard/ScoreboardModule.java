package dev.zcripted.obx.feature.scoreboard;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.scoreboard.listener.ScoreboardJoinListener;
import dev.zcripted.obx.feature.scoreboard.scheduler.ScoreboardRefreshTask;
import dev.zcripted.obx.feature.scoreboard.service.ScoreboardService;

/** Sidebar scoreboard feature: per-player board + live refresh task. */
public final class ScoreboardModule extends AbstractModule {

    private ScoreboardRefreshTask refreshTask;

    @Override
    public String id() {
        return "scoreboard";
    }

    @Override
    protected void onEnable(OBX plugin) {
        ScoreboardService service = service(ScoreboardService.class, new ScoreboardService(plugin));
        service.load();
        refreshTask = new ScoreboardRefreshTask(plugin, service);
        refreshTask.start();
        listener(new ScoreboardJoinListener(plugin, service));
        onDisable(() -> {
            if (refreshTask != null) {
                refreshTask.cancel();
            }
        });
    }

    @Override
    public void reload(OBX plugin) {
        ScoreboardService service = plugin.getScoreboardService();
        if (service != null) {
            service.reload();
            if (refreshTask != null) {
                refreshTask.start();
            }
        }
    }
}
