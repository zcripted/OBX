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

import java.util.Collections;
import java.util.List;

public class TpCancelCommand implements CommandExecutor, TabCompleter {

    private final TpaService tpaService;
    private final LanguageManager languages;

    public TpCancelCommand(Main plugin) {
        this.tpaService = plugin.getTpaService();
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.tpcancel")) {
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
