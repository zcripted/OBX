package dev.zcripted.obx.command.teleportation;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.storage.DataService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HomeCommand extends AbstractObxCommand implements TabCompleter {

    private final DataService dataService;

    public HomeCommand(OBX plugin) {
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
        if (!player.hasPermission("obx.home")) {
            languages.send(player, "core.no-permission");
            return true;
        }

        String homeName = args.length > 0 ? args[0] : "home";
        Location home = dataService.getHome(player.getUniqueId(), homeName);
        if (home == null) {
            languages.send(player, "teleport.home.not-found", Placeholders.with("home", homeName));
            return true;
        }

        dataService.setBack(player.getUniqueId(), player.getLocation());
        plugin.getTeleportManager().teleportPlayer(player, home, "teleport.home.teleporting", Placeholders.with("home", homeName));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 1) {
            return new ArrayList<String>();
        }
        Player player = (Player) sender;
        String prefix = args[0].toLowerCase();
        List<String> matches = new ArrayList<String>();
        for (String home : dataService.getHomes(player.getUniqueId())) {
            if (home.toLowerCase().startsWith(prefix)) matches.add(home);
        }
        return matches;
    }
}
