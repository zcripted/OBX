package dev.zcripted.obx.feature.mail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BroadcastCommand extends AbstractObxCommand implements TabCompleter {

    /** Caps the broadcast body so one command can't flood chat. */
    private static final int MAX_LENGTH = 512;

    public BroadcastCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.broadcast")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "messaging.broadcast.usage");
            return true;
        }
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) body.append(' ');
            body.append(args[i]);
        }
        String text = body.toString();
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH);
        }
        java.util.Map<String, String> placeholders = Placeholders.with("message",
                dev.zcripted.obx.util.text.MessageSanitizer.sanitize(sender, text));
        for (Player online : Bukkit.getOnlinePlayers()) {
            languages.send(online, "messaging.broadcast.line", placeholders);
        }
        languages.send(Bukkit.getConsoleSender(), "messaging.broadcast.line", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
