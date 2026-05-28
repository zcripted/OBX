package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.util.teleport.TpaService;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TpAcceptCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final TpaService tpaService;
    private final LanguageManager languages;
    private final DataService dataService;

    public TpAcceptCommand(Main plugin) {
        this.plugin = plugin;
        this.tpaService = plugin.getTpaService();
        this.languages = plugin.getLanguageManager();
        this.dataService = plugin.getDataService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player receiver = (Player) sender;
        if (!receiver.hasPermission("sfcore.tpaccept")) {
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
        Player senderPlayer = Bukkit.getPlayer(request.getSenderUuid());
        if (senderPlayer == null || !senderPlayer.isOnline()) {
            languages.send(receiver, "tpa.sender-offline", Placeholders.with("player", request.getSenderName()));
            return true;
        }

        Player traveler = request.getType() == TpaService.RequestType.HERE ? receiver : senderPlayer;
        Player destinationHolder = request.getType() == TpaService.RequestType.HERE ? senderPlayer : receiver;

        dataService.setBack(traveler.getUniqueId(), traveler.getLocation());
        languages.send(senderPlayer, "tpa.accepted-sender", Placeholders.with("player", receiver.getName()));
        languages.send(receiver, "tpa.accepted-receiver", Placeholders.with("player", senderPlayer.getName()));
        plugin.getTeleportManager().teleportPlayer(traveler, destinationHolder.getLocation(),
                "tpa.teleporting", Placeholders.with("player", destinationHolder.getName()));
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
