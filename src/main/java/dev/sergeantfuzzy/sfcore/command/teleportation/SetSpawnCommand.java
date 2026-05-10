package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final DataService dataService;
    private final LanguageManager languages;

    public SetSpawnCommand(Main plugin) {
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
        if (!player.hasPermission("sfcore.spawn.set")) {
            languages.send(player, "core.no-permission");
            return true;
        }

        Location location = player.getLocation();
        dataService.setSpawn(location, player.getUniqueId(), player.getName(), java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.now()));
        languages.send(player, "teleport.spawn.set");
        return true;
    }
}
