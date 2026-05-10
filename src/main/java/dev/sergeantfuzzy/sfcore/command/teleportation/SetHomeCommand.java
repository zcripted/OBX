package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHomeCommand implements CommandExecutor {

    private final Main plugin;
    private final DataService dataService;
    private final LanguageManager languages;

    public SetHomeCommand(Main plugin) {
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
        if (!player.hasPermission("sfcore.home.set")) {
            languages.send(player, "core.no-permission");
            return true;
        }

        String homeName = args.length > 0 ? args[0] : "home";
        int limit = plugin.getConfig().getInt("homes.max-per-player", 5);
        if (limit > 0 && dataService.countHomes(player.getUniqueId()) >= limit) {
            languages.send(player, "teleport.home.limit", Placeholders.with("limit", limit));
            return true;
        }

        Location location = player.getLocation();
        dataService.setHome(player.getUniqueId(), homeName, location);
        languages.send(player, "teleport.home.set", Placeholders.with("home", homeName));
        return true;
    }
}
