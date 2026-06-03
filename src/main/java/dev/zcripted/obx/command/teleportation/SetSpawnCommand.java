package dev.zcripted.obx.command.teleportation;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.storage.DataService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand extends AbstractObxCommand {

    private final DataService dataService;

    public SetSpawnCommand(Main plugin) {
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
        if (!player.hasPermission("obx.spawn.set")) {
            languages.send(player, "core.no-permission");
            return true;
        }

        Location location = player.getLocation();
        dataService.setSpawn(location, player.getUniqueId(), player.getName(), java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.now()));
        languages.send(player, "teleport.spawn.set");
        return true;
    }
}
