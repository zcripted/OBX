package dev.zcripted.obx.feature.warp;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.warp.command.WarpCommand;
import dev.zcripted.obx.feature.warp.gui.WarpMenuInputListener;
import dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager;
import dev.zcripted.obx.feature.warp.gui.WarpMenuListener;
import dev.zcripted.obx.feature.warp.service.WarpService;

/** Warp feature: warp data + {@code /warp} command tree + warp GUIs. */
public final class WarpModule extends AbstractModule {

    @Override
    public String id() {
        return "warp";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        WarpService warp = service(WarpService.class, new WarpService(plugin));
        warp.load();
        WarpMenuInputManager inputManager = service(WarpMenuInputManager.class, new WarpMenuInputManager(plugin));
        command("warp", new WarpCommand(plugin));
        listener(new WarpMenuListener(plugin));
        listener(new WarpMenuInputListener(inputManager));
    }

    @Override
    public void reload(ObxPlugin plugin) {
        WarpService warp = plugin.getWarpService();
        if (warp != null) {
            warp.load();
        }
    }
}
