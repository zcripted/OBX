package dev.sergeantfuzzy.sfcore.command.message;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /rply} (aliases {@code /reply}, {@code /r}) — reply to your most recent message
 * sender. With a message it sends directly; with no arguments it opens a 60-second reply
 * draft (type your reply in chat); {@code /rply cancel} aborts the draft.
 */
public final class ReplyCommand implements CommandExecutor {

    private final Main plugin;

    public ReplyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.message")) {
            plugin.getLanguageManager().send(sender, "core.no-permission");
            return true;
        }
        if (!(sender instanceof Player)) {
            // Console can't be messaged, so it never has a reply target.
            plugin.getLanguageManager().send(sender, "reply.no-target");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            plugin.getMessageService().startDraft(player);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            plugin.getMessageService().cancelDraft(player, true);
            return true;
        }
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                message.append(' ');
            }
            message.append(args[i]);
        }
        plugin.getMessageService().reply(player, message.toString());
        return true;
    }
}
