package dev.zcripted.obx.feature.teleport.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.DataService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.StringJoiner;

public class HomesCommand extends AbstractObxCommand {

    private final DataService dataService;

    public HomesCommand(ObxPlugin plugin) {
        super(plugin);
        this.dataService = plugin.getDataService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.home.list")) {
            languages.send(player, "core.no-permission");
            return true;
        }

        Set<String> homes = dataService.getHomes(player.getUniqueId());
        StringJoiner joiner = new StringJoiner(", ");
        for (String home : homes) {
            joiner.add(home);
        }
        String list = joiner.length() == 0 ? languages.get(player, "teleport.homes.none") : joiner.toString();
        languages.send(player, "teleport.homes.list", Placeholders.with("homes", list));
        return true;
    }
}