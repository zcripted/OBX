package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final DataService dataService;
    private final LanguageManager languages;

    public HomeCommand(Main plugin) {
        this.plugin = plugin;
        this.dataService = plugin.getDataService();
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.home")) {
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
        if (!(sender instanceof Player)) {
            return new ArrayList<String>();
        }
        Player player = (Player) sender;
        Set<String> homes = dataService.getHomes(player.getUniqueId());
        return new ArrayList<String>(homes);
    }
}
