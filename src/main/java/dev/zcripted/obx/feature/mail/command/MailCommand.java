package dev.zcripted.obx.feature.mail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.mail.mail.MailService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

public class MailCommand extends AbstractObxCommand implements TabCompleter {

    private final MailService messageService;

    public MailCommand(ObxPlugin plugin) {
        super(plugin);
        this.messageService = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.mail.MailService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.mail")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length == 0) {
            languages.send(player, "messaging.mail.usage");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "read":
                return read(player);
            case "clear":
                return clear(player);
            case "send":
                return send(player, args);
            case "list":
                return read(player);
            default:
                languages.send(player, "messaging.mail.usage");
                return true;
        }
    }

    private boolean read(Player player) {
        List<MailService.MailEntry> mail = messageService.readMail(player.getUniqueId());
        if (mail.isEmpty()) {
            languages.send(player, "messaging.mail.empty");
            return true;
        }
        languages.send(player, "messaging.mail.header", Placeholders.with("count", mail.size()));
        int index = 1;
        for (MailService.MailEntry entry : mail) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("index", String.valueOf(index++));
            placeholders.put("from", entry.getFrom() == null ? "Unknown" : entry.getFrom());
            placeholders.put("sentAt", entry.getSentAt() == null ? "" : entry.getSentAt());
            placeholders.put("body", entry.getBody() == null ? "" : entry.getBody());
            languages.send(player, "messaging.mail.entry", placeholders);
        }
        languages.send(player, "messaging.mail.footer");
        return true;
    }

    private boolean clear(Player player) {
        int cleared = messageService.clearMail(player.getUniqueId());
        languages.send(player, "messaging.mail.cleared", Placeholders.with("count", cleared));
        return true;
    }

    private boolean send(Player player, String[] args) {
        if (args.length < 3) {
            languages.send(player, "messaging.mail.send-usage");
            return true;
        }
        OfflinePlayer recipient = Bukkit.getOfflinePlayer(args[1]);
        UUID recipientUuid = recipient.getUniqueId();
        String recipientName = recipient.getName();
        if (recipientName == null && recipient.getFirstPlayed() == 0L) {
            languages.send(player, "messaging.mail.unknown-recipient", Placeholders.with("player", args[1]));
            return true;
        }
        StringBuilder body = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) body.append(' ');
            body.append(args[i]);
        }
        MailService.MailResult result = messageService.sendMail(
                recipientUuid, recipientName, player.getUniqueId(), player.getName(), body.toString());
        if (result == MailService.MailResult.MAILBOX_FULL) {
            languages.send(player, "messaging.mail.full", Placeholders.with("player",
                    recipientName == null ? args[1] : recipientName));
            return true;
        }
        languages.send(player, "messaging.mail.sent", Placeholders.with("player",
                recipientName == null ? args[1] : recipientName));
        Player onlineRecipient = recipientUuid == null ? null : Bukkit.getPlayer(recipientUuid);
        if (onlineRecipient != null) {
            languages.send(onlineRecipient, "messaging.mail.notify-receiver",
                    Placeholders.with("player", player.getName()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("read", "send", "clear", "list"), args[0]);
        }
        if (args.length == 2 && "send".equalsIgnoreCase(args[0])) {
            List<String> names = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return java.util.Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String value : options) {
            if (value.toLowerCase().startsWith(lower)) matches.add(value);
        }
        return matches;
    }
}
