package dev.zcripted.obx.command.teleportation;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.teleport.TpaService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TpDenyCommand extends AbstractObxCommand implements TabCompleter {

    private final TpaService tpaService;

    public TpDenyCommand(OBX plugin) {
        super(plugin);
        this.tpaService = plugin.getTpaService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player receiver = (Player) sender;
        if (!receiver.hasPermission("obx.tpdeny")) {
            languages.send(receiver, "core.no-permission");
            return true;
        }

        TpaService.Request request;
        if (args.length >= 1) {
            Player senderPlayer = Bukkit.getPlayerExact(args[0]);
            if (senderPlayer == null) {
                request = tpaService.popIncomingFromSender(receiver.getUniqueId(),
                        Bukkit.getOfflinePlayer(args[0]).getUniqueId());
            } else {
                request = tpaService.popIncomingFromSender(receiver.getUniqueId(), senderPlayer.getUniqueId());
            }
        } else {
            request = tpaService.popLatestIncomingFor(receiver.getUniqueId());
        }

        if (request == null) {
            languages.send(receiver, "tpa.no-pending");
            return true;
        }
        languages.send(receiver, "tpa.denied-receiver", Placeholders.with("player", request.getSenderName()));
        Player senderPlayer = Bukkit.getPlayer(request.getSenderUuid());
        if (senderPlayer != null && senderPlayer.isOnline()) {
            languages.send(senderPlayer, "tpa.denied-sender", Placeholders.with("player", receiver.getName()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> names = new ArrayList<>();
        if (!(sender instanceof Player) || args.length != 1) {
            return names;
        }
        Player receiver = (Player) sender;
        String prefix = args[0].toLowerCase();
        for (TpaService.Request request : tpaService.incomingFor(receiver.getUniqueId())) {
            String name = request.getSenderName();
            if (name != null && name.toLowerCase().startsWith(prefix)) {
                names.add(name);
            }
        }
        return names;
    }
}
