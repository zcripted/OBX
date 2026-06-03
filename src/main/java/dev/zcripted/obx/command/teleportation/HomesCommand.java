package dev.zcripted.obx.command.teleportation;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.storage.DataService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.StringJoiner;

public class HomesCommand extends AbstractObxCommand {

    private final DataService dataService;

    public HomesCommand(OBX plugin) {
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
