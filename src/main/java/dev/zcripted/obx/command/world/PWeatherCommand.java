package dev.zcripted.obx.command.world;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PWeatherCommand extends AbstractObxCommand implements TabCompleter {


    public PWeatherCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.pweather")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "world.pweather.usage");
            return true;
        }
        String mode = args[0].toLowerCase();
        if (mode.equals("reset")) {
            player.resetPlayerWeather();
            languages.send(player, "world.pweather.reset");
            return true;
        }
        WeatherType weather;
        switch (mode) {
            case "clear":
            case "sun":
                weather = WeatherType.CLEAR;
                break;
            case "rain":
            case "downfall":
            case "thunder":
            case "storm":
                weather = WeatherType.DOWNFALL;
                break;
            default:
                languages.send(player, "world.pweather.usage");
                return true;
        }
        player.setPlayerWeather(weather);
        languages.send(player, "world.pweather.set", Placeholders.with("weather", mode));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return new ArrayList<>(Arrays.asList("clear", "rain", "thunder", "reset"));
        return Collections.emptyList();
    }
}
