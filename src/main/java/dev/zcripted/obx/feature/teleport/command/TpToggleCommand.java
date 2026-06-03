package dev.zcripted.obx.feature.teleport.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.teleport.service.TpaService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TpToggleCommand extends AbstractObxCommand implements TabCompleter {

    private final TpaService tpaService;

    public TpToggleCommand(OBX plugin) {
        super(plugin);
        this.tpaService = plugin.getTpaService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.tptoggle")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        boolean nowAccepting = tpaService.toggle(player.getUniqueId());
        languages.send(player, nowAccepting ? "tpa.toggle-on" : "tpa.toggle-off");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
