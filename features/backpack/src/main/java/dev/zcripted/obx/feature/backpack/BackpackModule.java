package dev.zcripted.obx.feature.backpack;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.backpack.command.BackpackCommand;
import dev.zcripted.obx.feature.backpack.listener.BackpackListener;
import dev.zcripted.obx.feature.backpack.service.BackpackService;

/**
 * Backpack feature: per-player 3-row portable storage opened via {@code /backpack}
 * (aliases {@code /bp}, {@code /pack}), convertible to a dupe-guarded physical item.
 * See {@link BackpackService} for the storage/dupe-guard design.
 */
public final class BackpackModule extends AbstractModule {

    @Override
    public String id() {
        return "backpack";
    }

    @Override
    public String displayName() {
        return "Backpack";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        BackpackService service = service(BackpackService.class, new BackpackService(plugin));
        service.load();
        // BackpackCommand resolves the service from the registry in its constructor,
        // so it must be built after the service is registered (kit-module pattern).
        command("backpack", new BackpackCommand(plugin));
        listener(new BackpackListener(plugin, service));
        // Save + close every open backpack view before teardown so a reload/stop
        // never drops in-flight edits.
        onDisable(service::closeAndSaveAll);
    }
}