package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.control.FlightStateService;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FlySpeedCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final FlightStateService flight;

    public FlySpeedCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.flight = plugin.getFlightStateService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.flyspeed")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "flight.speed.usage-fly");
            return true;
        }
        double value;
        try { value = Double.parseDouble(args[0]); }
        catch (NumberFormatException ignored) {
            languages.send(player, "flight.speed.invalid", Placeholders.with("input", args[0]));
            return true;
        }
        if (value < 0.0 || value > 10.0) {
            languages.send(player, "flight.speed.range");
            return true;
        }
        flight.setFlySpeed(player, flight.scaleFromInput(value));
        languages.send(player, "flight.speed.set-fly", Placeholders.with("value", value));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> matches = new java.util.ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            String s = String.valueOf(i);
            if (s.startsWith(prefix)) matches.add(s);
        }
        return matches;
    }
}
