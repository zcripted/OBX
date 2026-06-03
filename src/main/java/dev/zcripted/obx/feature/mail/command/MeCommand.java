package dev.zcripted.obx.feature.mail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeCommand extends AbstractObxCommand implements TabCompleter {


    public MeCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.me")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "messaging.me.usage");
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
            languages.send(online, "messaging.me.broadcast", placeholders);
        }
        if (!(sender instanceof Player)) {
            languages.send(sender, "messaging.me.broadcast", placeholders);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
