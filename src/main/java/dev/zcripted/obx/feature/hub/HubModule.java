package dev.zcripted.obx.feature.hub;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.hub.command.HubCommand;
import dev.zcripted.obx.feature.hub.gui.ServerSelectorListener;
import dev.zcripted.obx.api.hub.HubKitApplier;
import dev.zcripted.obx.feature.hub.launchpad.LaunchpadCooldownManager;
import dev.zcripted.obx.feature.hub.listener.HubFallDamageListener;
import dev.zcripted.obx.feature.hub.listener.HubFishingListener;
import dev.zcripted.obx.feature.hub.listener.HubItemProtectionListener;
import dev.zcripted.obx.api.hub.HubItemUseListener;
import dev.zcripted.obx.feature.hub.listener.HubJoinListener;
import dev.zcripted.obx.feature.hub.listener.HubLaunchpadListener;
import dev.zcripted.obx.feature.hub.messaging.BungeeMessenger;
import dev.zcripted.obx.feature.hub.service.HubService;

/**
 * Hub / lobby feature. Dormant internally when systems/hub.yml has
 * {@code enabled: false}; the listeners early-exit cheaply in that state.
 */
public final class HubModule extends AbstractModule {

    @Override
    public String id() {
        return "hub";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        HubService hub = service(HubService.class, new HubService(plugin));
        hub.load();
        HubKitApplier kitApplier = service(HubKitApplier.class, new dev.zcripted.obx.feature.hub.kit.HubKitApplierImpl(plugin, hub));
        LaunchpadCooldownManager launchpad = service(LaunchpadCooldownManager.class, new LaunchpadCooldownManager(plugin, hub));
        BungeeMessenger bungee = service(BungeeMessenger.class, new BungeeMessenger(plugin, hub));
        bungee.register();

        command("hub", new HubCommand(plugin, hub, kitApplier));

        listener(new HubJoinListener(plugin, hub, kitApplier));
        dev.zcripted.obx.feature.hub.listener.HubItemUseListenerImpl itemUse =
                new dev.zcripted.obx.feature.hub.listener.HubItemUseListenerImpl(plugin, hub);
        service(HubItemUseListener.class, itemUse);
        listener(itemUse);
        listener(new HubItemProtectionListener(plugin, hub));
        listener(new HubFishingListener(plugin, hub));
        listener(new HubLaunchpadListener(plugin, hub, launchpad));
        listener(new HubFallDamageListener(launchpad));
        listener(new ServerSelectorListener(plugin));

        launchpad.start();
        onDisable(launchpad::stop);
        onDisable(bungee::unregister);
        onDisable(hub::save);
    }

    @Override
    public void reload(ObxPlugin plugin) {
        HubService hub = plugin.getHubService();
        if (hub != null) {
            hub.reload();
        }
    }
}
