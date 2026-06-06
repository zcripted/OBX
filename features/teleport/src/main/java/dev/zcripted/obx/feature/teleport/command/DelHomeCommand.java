package dev.zcripted.obx.feature.teleport.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.DataService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DelHomeCommand extends AbstractObxCommand implements TabCompleter {

    private final DataService dataService;

    public DelHomeCommand(ObxPlugin plugin) {
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
        if (!player.hasPermission("obx.home.delete")) {
            languages.send(player, "core.no-permission");
            return true;
        }

        if (args.length == 0) {
            languages.send(player, "teleport.home.delete-usage", Placeholders.with("command", label));
            return true;
        }

        String homeName = args[0];
        boolean removed = dataService.deleteHome(player.getUniqueId(), homeName);
        if (!removed) {
            languages.send(player, "teleport.home.not-found", Placeholders.with("home", homeName));
            return true;
        }
        languages.send(player, "teleport.home.removed", Placeholders.with("home", homeName));
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
