package dev.zcripted.obx.feature.teleport;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.teleport.command.BackCommand;
import dev.zcripted.obx.feature.teleport.command.DelHomeCommand;
import dev.zcripted.obx.feature.teleport.command.HomeCommand;
import dev.zcripted.obx.feature.teleport.command.HomesCommand;
import dev.zcripted.obx.feature.teleport.command.PositionCommand;
import dev.zcripted.obx.feature.teleport.command.SetHomeCommand;
import dev.zcripted.obx.feature.teleport.command.SpawnCommand;
import dev.zcripted.obx.feature.teleport.command.TeleportCommand;
import dev.zcripted.obx.feature.teleport.command.TopCommand;
import dev.zcripted.obx.feature.teleport.command.TpAllCommand;
import dev.zcripted.obx.feature.teleport.command.TpCancelCommand;
import dev.zcripted.obx.feature.teleport.command.TpPosCommand;
import dev.zcripted.obx.feature.teleport.command.TpToggleCommand;
import dev.zcripted.obx.feature.teleport.command.TpaCommand;
import dev.zcripted.obx.feature.teleport.listener.BackListener;
import dev.zcripted.obx.api.teleport.TeleportManager;
import dev.zcripted.obx.feature.teleport.service.TeleportRequestService;
import dev.zcripted.obx.feature.teleport.service.TpaService;

/**
 * Teleport feature: tp/tpa/back/top/pos + home/spawn movement. Owns the
 * {@link TeleportManager} (also a warmup listener) and TPA/request services.
 */
public final class TeleportModule extends AbstractModule {

    @Override
    public String id() {
        return "teleport";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        dev.zcripted.obx.feature.teleport.service.TeleportManagerImpl teleportManager =
                new dev.zcripted.obx.feature.teleport.service.TeleportManagerImpl(plugin, plugin.getLanguageManager());
        service(TeleportManager.class, teleportManager);
        service(TeleportRequestService.class, new TeleportRequestService(plugin));
        TpaService tpaService = service(TpaService.class, new TpaService(plugin));
        // TpaService is itself a Listener (onQuit clears pending requests) and runs an expiry
        // sweeper. Both were previously unwired — register it and start the sweeper so requests
        // expire and per-player state doesn't leak across disconnects.
        listener(tpaService);
        tpaService.start();

        // SpawnCommand backs both /spawn and /setspawn and is itself a listener.
        SpawnCommand spawnCommand = new SpawnCommand(plugin);
        command("home", new HomeCommand(plugin));
        command("sethome", new SetHomeCommand(plugin));
        command("delhome", new DelHomeCommand(plugin));
        command("homes", new HomesCommand(plugin));
        command("spawn", spawnCommand);
        command("setspawn", spawnCommand);
        command("back", new BackCommand(plugin));
        command("tp", new TeleportCommand(plugin, TeleportCommand.Mode.TO));
        command("tphere", new TeleportCommand(plugin, TeleportCommand.Mode.HERE));
        command("tpa", new TpaCommand(plugin, TpaCommand.Mode.REQUEST));
        command("tpaccept", new TpaCommand(plugin, TpaCommand.Mode.ACCEPT));
        command("tpdeny", new TpaCommand(plugin, TpaCommand.Mode.DENY));
        command("pos", new PositionCommand(plugin));
        command("top", new TopCommand(plugin));
        command("tpcancel", new TpCancelCommand(plugin));
        command("tptoggle", new TpToggleCommand(plugin));
        command("tppos", new TpPosCommand(plugin));
        command("tpall", new TpAllCommand(plugin));

        listener(spawnCommand);
        listener(teleportManager);
        listener(new BackListener(plugin));

        onDisable(teleportManager::cancelAll);
        onDisable(tpaService::stop);
    }
}