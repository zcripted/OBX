package dev.zcripted.obx.command.world;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.control.PerPlayerTimeService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PTimeCommand extends AbstractObxCommand implements TabCompleter {

    private final PerPlayerTimeService timeService;

    public PTimeCommand(OBX plugin) {
        super(plugin);
        this.timeService = plugin.getPerPlayerTimeService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.ptime")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "world.ptime.usage");
            return true;
        }
        String input = args[0].toLowerCase();
        if (input.equals("reset")) {
            timeService.reset(player);
            languages.send(player, "world.ptime.reset");
            return true;
        }
        Long parsed = parseTime(input);
        if (parsed == null) {
            languages.send(player, "world.time.invalid", Placeholders.with("input", input));
            return true;
        }
        timeService.setTime(player, parsed, false);
        languages.send(player, "world.ptime.set", Placeholders.with("time", parsed));
        return true;
    }

    private Long parseTime(String value) {
        switch (value) {
            case "day": case "morning": return 1000L;
            case "noon": return 6000L;
            case "night": case "evening": return 13000L;
            case "midnight": return 18000L;
            default:
                try { return Long.parseLong(value); }
                catch (NumberFormatException ignored) { return null; }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return new ArrayList<>(Arrays.asList("day", "noon", "night", "midnight", "reset"));
        return Collections.emptyList();
    }
}
