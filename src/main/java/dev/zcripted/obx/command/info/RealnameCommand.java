package dev.zcripted.obx.command.info;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class RealnameCommand extends AbstractObxCommand implements TabCompleter {


    public RealnameCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.realname")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "info.realname.usage");
            return true;
        }
        String needle = ChatColor.stripColor(args[0]).toLowerCase();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            String real = online.getName();
            String display = ChatColor.stripColor(online.getDisplayName());
            String customName = ChatColor.stripColor(online.getCustomName() == null ? "" : online.getCustomName());
            if (real.equalsIgnoreCase(needle) || display.equalsIgnoreCase(needle) || customName.equalsIgnoreCase(needle)) {
                languages.send(sender, "info.realname.match",
                        Placeholders.with("display", online.getDisplayName(), "real", real));
                return true;
            }
        }
        languages.send(sender, "info.realname.no-match", Placeholders.with("player", args[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = ChatColor.stripColor(args[0]).toLowerCase();
        java.util.Set<String> matches = new java.util.LinkedHashSet<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            String real = online.getName();
            String display = ChatColor.stripColor(online.getDisplayName() == null ? real : online.getDisplayName());
            if (real.toLowerCase().startsWith(prefix)) matches.add(real);
            if (display != null && !display.equalsIgnoreCase(real) && display.toLowerCase().startsWith(prefix)) {
                matches.add(display);
            }
        }
        return new java.util.ArrayList<>(matches);
    }
}
