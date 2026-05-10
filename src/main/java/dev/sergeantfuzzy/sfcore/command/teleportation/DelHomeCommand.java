package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DelHomeCommand implements CommandExecutor, TabCompleter {

    private final DataService dataService;
    private final LanguageManager languages;

    public DelHomeCommand(Main plugin) {
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
        if (!player.hasPermission("sfcore.home.delete")) {
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
        if (!(sender instanceof Player)) {
            return new ArrayList<String>();
        }
        Player player = (Player) sender;
        Set<String> homes = dataService.getHomes(player.getUniqueId());
        return new ArrayList<String>(homes);
    }
}
