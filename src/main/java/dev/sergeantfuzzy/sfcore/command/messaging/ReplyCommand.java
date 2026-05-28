package dev.sergeantfuzzy.sfcore.command.messaging;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.messaging.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ReplyCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final LanguageManager languages;
    private final MessageService messageService;

    public ReplyCommand(Main plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.msg")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "messaging.reply.usage");
            return true;
        }
        UUID partner = messageService.getLastRecipient(player.getUniqueId());
        if (partner == null) {
            languages.send(player, "messaging.reply.none");
            return true;
        }
        Player target = Bukkit.getPlayer(partner);
        if (target == null || !target.isOnline()) {
            languages.send(player, "messaging.reply.offline");
            return true;
        }
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) body.append(' ');
            body.append(args[i]);
        }
        String[] forwarded = new String[]{ target.getName() };
        String[] msg = new String[args.length + 1];
        msg[0] = target.getName();
        System.arraycopy(args, 0, msg, 1, args.length);
        new MsgCommand(plugin).onCommand(sender, command, "msg", msg);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
