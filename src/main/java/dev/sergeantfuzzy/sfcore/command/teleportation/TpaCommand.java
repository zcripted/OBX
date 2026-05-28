package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.teleport.TpaService;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger.InteractiveMessagePart;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TpaCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final TpaService tpaService;
    private final LanguageManager languages;
    private final TpaService.RequestType type;

    public TpaCommand(Main plugin, TpaService.RequestType type) {
        this.plugin = plugin;
        this.tpaService = plugin.getTpaService();
        this.languages = plugin.getLanguageManager();
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        String permission = type == TpaService.RequestType.HERE ? "sfcore.tpahere" : "sfcore.tpa";
        if (!player.hasPermission(permission)) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, type == TpaService.RequestType.HERE ? "tpa.usage-here" : "tpa.usage-to");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            languages.send(player, "tpa.target-not-online", Placeholders.with("player", args[0]));
            return true;
        }
        TpaService.SendResult result = tpaService.send(player, target, type);
        switch (result) {
            case SAME_PLAYER:
                languages.send(player, "tpa.same-player");
                return true;
            case ALREADY_PENDING:
                languages.send(player, "tpa.already-pending");
                return true;
            case RECEIVER_OFF:
                languages.send(player, "tpa.receiver-off", Placeholders.with("player", target.getName()));
                return true;
            case OK:
            default:
                int expirySeconds = tpaService.getExpirySeconds();
                String senderKey = type == TpaService.RequestType.HERE ? "tpa.sent-here-sender" : "tpa.sent-to-sender";
                languages.send(player, senderKey, Placeholders.with("player", target.getName(), "seconds", expirySeconds));
                sendInteractivePrompt(target, player, expirySeconds);
                return true;
        }
    }

    private void sendInteractivePrompt(Player receiver, Player senderPlayer, int seconds) {
        String prefixKey = type == TpaService.RequestType.HERE ? "tpa.received-here-prefix" : "tpa.received-to-prefix";
        String prefix = languages.get(receiver, prefixKey,
                Placeholders.with("player", senderPlayer.getName(), "seconds", seconds));
        String accept = languages.get(receiver, "tpa.button.accept");
        String deny = languages.get(receiver, "tpa.button.deny");
        String separator = languages.get(receiver, "tpa.button.separator");
        List<String> acceptHover = languages.list(receiver, "tpa.button.accept-hover",
                Placeholders.with("player", senderPlayer.getName()));
        List<String> denyHover = languages.list(receiver, "tpa.button.deny-hover",
                Placeholders.with("player", senderPlayer.getName()));

        List<InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(InteractiveMessagePart.plain(prefix));
        parts.add(InteractiveMessagePart.interactive(accept, acceptHover, "/tpaccept " + senderPlayer.getName(), true));
        parts.add(InteractiveMessagePart.plain(separator));
        parts.add(InteractiveMessagePart.interactive(deny, denyHover, "/tpdeny " + senderPlayer.getName(), true));
        ComponentMessenger.sendJoinedHoverMessages(receiver, parts);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && online.getUniqueId().equals(((Player) sender).getUniqueId())) {
                    continue;
                }
                if (online.getName().toLowerCase().startsWith(prefix)) {
                    names.add(online.getName());
                }
            }
            return names;
        }
        return new ArrayList<>(Arrays.asList());
    }
}
