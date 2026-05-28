package dev.sergeantfuzzy.sfcore.command.messaging;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.messaging.MessageService;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MsgCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final LanguageManager languages;
    private final MessageService messageService;

    public MsgCommand(Main plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("sfcore.msg")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "messaging.msg.usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            languages.send(sender, "messaging.target-offline", Placeholders.with("player", args[0]));
            return true;
        }
        StringBuilder body = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) body.append(' ');
            body.append(args[i]);
        }
        String message = body.toString();
        deliver(sender, target, message);
        return true;
    }

    private void deliver(CommandSender sender, Player target, String body) {
        UUID senderUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        String senderName = sender instanceof Player ? ((Player) sender).getName()
                : (sender instanceof ConsoleCommandSender ? "Console" : sender.getName());

        if (senderUuid != null && messageService.isIgnoring(target.getUniqueId(), senderUuid)) {
            // silently swallow on receiver — sender still gets the outgoing line so they don't probe
            languages.send(sender, "messaging.msg.outgoing",
                    Placeholders.with("player", target.getName(), "message", body));
            return;
        }
        if (senderUuid != null && messageService.isIgnoring(senderUuid, target.getUniqueId())) {
            languages.send(sender, "messaging.msg.you-are-ignoring",
                    Placeholders.with("player", target.getName()));
            return;
        }

        Map<String, String> out = new HashMap<>();
        out.put("player", target.getName());
        out.put("message", body);
        languages.send(sender, "messaging.msg.outgoing", out);

        Map<String, String> in = new HashMap<>();
        in.put("player", senderName);
        in.put("message", body);
        languages.send(target, "messaging.msg.incoming", in);

        if (senderUuid != null) {
            messageService.setLastRecipient(senderUuid, target.getUniqueId());
        }
        // Console sender: no /r partner is stored; the receiver's existing /r partner is preserved.

        // social-spy fan-out
        Map<String, String> spy = new HashMap<>();
        spy.put("from", senderName);
        spy.put("to", target.getName());
        spy.put("message", body);
        for (UUID spyUuid : messageService.getSocialSpies()) {
            if (senderUuid != null && spyUuid.equals(senderUuid)) continue;
            if (spyUuid.equals(target.getUniqueId())) continue;
            Player spyPlayer = Bukkit.getPlayer(spyUuid);
            if (spyPlayer != null && spyPlayer.isOnline()) {
                languages.send(spyPlayer, "messaging.socialspy.line", spy);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && online.getUniqueId().equals(((Player) sender).getUniqueId())) continue;
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
