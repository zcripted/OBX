package dev.zcripted.obx.feature.kit;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.kit.command.KitCommand;
import dev.zcripted.obx.feature.kit.listener.KitFirstJoinListener;
import dev.zcripted.obx.feature.kit.service.KitService;

/** Kit feature: {@code /kit} backed by {@link KitService}. */
public final class KitModule extends AbstractModule {

    @Override
    public String id() {
        return "kit";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        KitService service = service(KitService.class, new KitService(plugin));
        service.load();
        // KitCommand resolves the service via plugin.getServiceRegistry().get(dev.zcripted.obx.feature.kit.service.KitService.class) in its
        // constructor, so it must be built after the service is registered.
        command("kit", new KitCommand(plugin));
        listener(new KitFirstJoinListener(plugin, service));
    }
}
