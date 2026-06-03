package dev.zcripted.obx.feature.tablist;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.tablist.format.TablistTeams;
import dev.zcripted.obx.feature.tablist.listener.TablistJoinListener;
import dev.zcripted.obx.feature.tablist.scheduler.TablistRefreshTask;
import dev.zcripted.obx.api.tablist.TablistService;

/** Tablist header/footer + team nameplates feature with live refresh task. */
public final class TablistModule extends AbstractModule {

    private TablistRefreshTask refreshTask;

    @Override
    public String id() {
        return "tablist";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        TablistService service = service(TablistService.class, new dev.zcripted.obx.feature.tablist.service.TablistServiceImpl(plugin));
        service.load();
        refreshTask = new TablistRefreshTask(plugin, service);
        refreshTask.start();
        listener(new TablistJoinListener(plugin, service));
        onDisable(() -> {
            if (refreshTask != null) {
                refreshTask.cancel();
            }
            // Remove the OBX tablist sort teams so they don't linger after unload.
            TablistTeams.reset();
        });
    }

    @Override
    public void reload(ObxPlugin plugin) {
        TablistService service = plugin.getTablistService();
        if (service != null) {
            service.reload();
            if (refreshTask != null) {
                refreshTask.start();
            }
        }
    }
}
