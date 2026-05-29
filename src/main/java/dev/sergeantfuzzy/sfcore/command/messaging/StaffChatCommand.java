package dev.sergeantfuzzy.sfcore.command.messaging;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffChatCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;

    public StaffChatCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.staffchat")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "messaging.staffchat.usage");
            return true;
        }
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) body.append(' ');
            body.append(args[i]);
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", sender instanceof Player ? ((Player) sender).getName() : sender.getName());
        placeholders.put("message", body.toString());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("sfcore.staffchat")) {
                languages.send(online, "messaging.staffchat.line", placeholders);
            }
        }
        languages.send(Bukkit.getConsoleSender(), "messaging.staffchat.line", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
