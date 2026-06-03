package dev.zcripted.obx.command.teleportation;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.storage.DataService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand extends AbstractObxCommand {

    private final DataService dataService;

    public BackCommand(OBX plugin) {
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
        if (!player.hasPermission("obx.back")) {
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
