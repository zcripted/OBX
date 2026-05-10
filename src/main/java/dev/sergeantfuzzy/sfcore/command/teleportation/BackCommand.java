package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand implements CommandExecutor {

    private final Main plugin;
    private final DataService dataService;
    private final LanguageManager languages;

    public BackCommand(Main plugin) {
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
        if (!player.hasPermission("sfcore.back")) {
            languages.send(player, "core.no-permission");
            return true;
        }

        Location back = dataService.getBack(player.getUniqueId());
        if (back == null) {
            languages.send(player, "teleport.back.missing");
            return true;
        }

        dataService.setBack(player.getUniqueId(), player.getLocation());
        plugin.getTeleportManager().teleportPlayer(player, back, "teleport.back.teleporting", null);
        return true;
    }
}
