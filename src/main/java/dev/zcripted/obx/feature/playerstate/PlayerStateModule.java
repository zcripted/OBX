package dev.zcripted.obx.feature.playerstate;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.playerstate.command.AfkCommand;
import dev.zcripted.obx.feature.playerstate.command.ButcherCommand;
import dev.zcripted.obx.feature.playerstate.command.FeedCommand;
import dev.zcripted.obx.feature.playerstate.command.FlyCommand;
import dev.zcripted.obx.feature.playerstate.command.FlySpeedCommand;
import dev.zcripted.obx.feature.playerstate.command.GamemodeCommand;
import dev.zcripted.obx.feature.playerstate.command.GodCommand;
import dev.zcripted.obx.feature.playerstate.command.HealCommand;
import dev.zcripted.obx.feature.playerstate.command.KillCommand;
import dev.zcripted.obx.feature.playerstate.command.SmiteCommand;
import dev.zcripted.obx.feature.playerstate.command.SpawnMobCommand;
import dev.zcripted.obx.feature.playerstate.command.SpawnerCommand;
import dev.zcripted.obx.feature.playerstate.command.TreeCommand;
import dev.zcripted.obx.feature.playerstate.command.VitalCommand;
import dev.zcripted.obx.feature.playerstate.command.WalkSpeedCommand;
import dev.zcripted.obx.api.playerstate.AfkService;
import dev.zcripted.obx.feature.playerstate.service.FlightStateService;
import dev.zcripted.obx.feature.playerstate.service.GodModeManager;
import dev.zcripted.obx.feature.playerstate.service.KillModeManager;

/**
 * Player-state feature: god/fly/speed/afk/heal/feed/vital + kill/butcher/smite/
 * tree/spawnmob/spawner/gamemode. Owns the god/kill/afk listeners and the
 * flight + afk services.
 */
public final class PlayerStateModule extends AbstractModule {

    @Override
    public String id() {
        return "playerstate";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        GodModeManager god = service(GodModeManager.class, new GodModeManager());
        KillModeManager kill = service(KillModeManager.class, new KillModeManager(plugin));
        dev.zcripted.obx.feature.playerstate.service.AfkServiceImpl afk =
                new dev.zcripted.obx.feature.playerstate.service.AfkServiceImpl(plugin);
        service(AfkService.class, afk);
        FlightStateService flight = service(FlightStateService.class, new FlightStateService(plugin));
        flight.load();

        listener(god);
        listener(kill);
        listener(afk);
        afk.start();
        onDisable(afk::stop);

        command("gamemode", new GamemodeCommand(plugin));
        command("kill", new KillCommand(plugin));
        command("heal", new HealCommand(plugin));
        command("feed", new FeedCommand(plugin));
        command("vital", new VitalCommand(plugin));
        command("god", new GodCommand(plugin));
        command("fly", new FlyCommand(plugin));
        command("flyspeed", new FlySpeedCommand(plugin));
        command("walkspeed", new WalkSpeedCommand(plugin));
        command("afk", new AfkCommand(plugin));
        command("butcher", new ButcherCommand(plugin));
        command("spawnmob", new SpawnMobCommand(plugin));
        command("spawner", new SpawnerCommand(plugin));
        command("smite", new SmiteCommand(plugin));
        command("tree", new TreeCommand(plugin));
    }
}
