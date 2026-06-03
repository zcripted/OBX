package dev.zcripted.obx.feature.world;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.world.command.DayCommand;
import dev.zcripted.obx.feature.world.command.PTimeCommand;
import dev.zcripted.obx.feature.world.command.PWeatherCommand;
import dev.zcripted.obx.feature.world.command.TimeCommand;
import dev.zcripted.obx.feature.world.command.WeatherCommand;
import dev.zcripted.obx.feature.world.listener.JoinLockListener;
import dev.zcripted.obx.feature.world.listener.RedstoneControlListener;
import dev.zcripted.obx.feature.world.service.PerPlayerTimeService;

/**
 * World/server-control feature: time/day/weather, per-player time + weather,
 * redstone freeze, and join-lock enforcement.
 */
public final class WorldModule extends AbstractModule {

    @Override
    public String id() {
        return "world";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        PerPlayerTimeService time = service(PerPlayerTimeService.class, new PerPlayerTimeService(plugin));
        time.load();
        command("time", new TimeCommand(plugin));
        command("day", new DayCommand(plugin, 1000L));
        command("night", new DayCommand(plugin, 13000L));
        command("sun", new DayCommand(plugin, 6000L));
        command("weather", new WeatherCommand(plugin));
        command("ptime", new PTimeCommand(plugin));
        command("pweather", new PWeatherCommand(plugin));
        listener(new RedstoneControlListener());
        listener(new JoinLockListener(plugin.getLanguageManager()));
    }
}
