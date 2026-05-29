package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
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

public class TpDenyCommand implements CommandExecutor, TabCompleter {

    private final TpaService tpaService;
    private final LanguageManager languages;

    public TpDenyCommand(Main plugin) {
        this.tpaService = plugin.getTpaService();
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player receiver = (Player) sender;
        if (!receiver.hasPermission("sfcore.tpdeny")) {
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
