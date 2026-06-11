package dev.zcripted.obx.feature.mail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /msg <player> <message>} (aliases {@code /tell}, {@code /w}, {@code /pm}) — send a
 * private message. Online targets get it live (with a clickable reply); offline targets get
 * it queued in their inbox. Console may send "as Console"; players cannot message the console.
 */
public final class MsgCommand extends AbstractObxCommand implements TabCompleter {


    public MsgCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.message")) {
            plugin.getLanguageManager().send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.getLanguageManager().send(sender, "message.usage");
            return true;
        }
        String target = args[0];
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                message.append(' ');
            }
            message.append(args[i]);
        }
        plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.pm.PrivateMessageService.class).sendMessage(sender, target, message.toString());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            List<String> names = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.equals(sender) && player.getName().toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                    names.add(player.getName());
                }
            }
            return names;
        }
        if (args.length == 2) {
            // First word of the message body — offer a few family-friendly ice-breakers /
            // Minecraft-humor openers (localized). Multi-word suggestions fill the line.
            String prefix = args[1].toLowerCase(Locale.ENGLISH);
            List<String> out = new ArrayList<String>();
            for (String line : plugin.getLanguageManager().list(sender, "message.icebreakers", Collections.<String, String>emptyMap())) {
                if (line.toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                    out.add(line);
                }
            }
            return out;
        }
        return Collections.emptyList();
    }
}