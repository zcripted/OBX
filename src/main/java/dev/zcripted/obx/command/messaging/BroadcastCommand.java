package dev.zcripted.obx.command.messaging;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BroadcastCommand extends AbstractObxCommand implements TabCompleter {


    public BroadcastCommand(OBX plugin) {
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
        java.util.Map<String, String> placeholders = Placeholders.with("message", body.toString());
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
