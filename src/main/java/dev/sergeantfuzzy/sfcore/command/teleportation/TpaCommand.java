package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Player teleport requests: {@code /tpa <player>} sends a request; {@code /tpaccept} and
 * {@code /tpdeny} respond (also driven by the clickable Accept/Deny words in the request
 * message). Default-allowed ({@code sfcore.teleport.request}). Players only — the console
 * has no position to request from or to.
 */
public final class TpaCommand implements CommandExecutor, TabCompleter {

    public enum Mode { REQUEST, ACCEPT, DENY }

    private final Main plugin;
    private final LanguageManager languages;
    private final Mode mode;

    public TpaCommand(Main plugin, Mode mode) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
        this.mode = mode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.teleport.request")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        switch (mode) {
            case ACCEPT:
                plugin.getTeleportRequestService().accept(player);
                return true;
            case DENY:
                plugin.getTeleportRequestService().deny(player);
                return true;
            case REQUEST:
            default:
                if (args.length < 1) {
                    languages.send(sender, "teleport.request.usage");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    languages.send(sender, "teleport.tp.not-online", Collections.singletonMap("player", args[0]));
                    return true;
                }
                plugin.getTeleportRequestService().send(player, target);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (mode != Mode.REQUEST || args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ENGLISH);
        List<String> names = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(sender) && player.getName().toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                names.add(player.getName());
            }
        }
        return names;
    }
}
