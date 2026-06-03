package dev.zcripted.obx.command.teleportation;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.util.teleport.TpaService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TpCancelCommand extends AbstractObxCommand implements TabCompleter {

    private final TpaService tpaService;

    public TpCancelCommand(Main plugin) {
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
        if (!player.hasPermission("obx.tpcancel")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        TpaService.Request request = tpaService.cancelOutgoing(player.getUniqueId());
        if (request == null) {
            languages.send(player, "tpa.cancel-none");
            return true;
        }
        languages.send(player, "tpa.cancelled-sender", Placeholders.with("player", request.getReceiverName()));
        Player receiver = Bukkit.getPlayer(request.getReceiverUuid());
        if (receiver != null && receiver.isOnline()) {
            languages.send(receiver, "tpa.cancelled-receiver", Placeholders.with("player", player.getName()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
