package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.nickname.NicknameService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NickCommand extends AbstractObxCommand implements TabCompleter {

    private final NicknameService nicknames;

    public NickCommand(OBX plugin) {
        super(plugin);
        this.nicknames = plugin.getNicknameService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            languages.send(sender, "nickname.usage");
            return true;
        }
        String desired = args[0];
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("obx.nick.others")) {
                languages.send(sender, "core.no-permission");
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !target.isOnline()) {
                languages.send(sender, "teleport.tp.not-online", Placeholders.with("player", args[1]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("obx.nick")) {
                languages.send(target, "core.no-permission");
                return true;
            }
        }
        if (desired.equalsIgnoreCase("off") || desired.equalsIgnoreCase("reset") || desired.equalsIgnoreCase("clear")) {
            nicknames.clearNickname(target);
            languages.send(target, "nickname.cleared-self");
            if (!sender.equals(target)) {
                languages.send(sender, "nickname.cleared-other", Placeholders.with("player", target.getName()));
            }
            return true;
        }
        boolean allowColor = sender.hasPermission("obx.nick.color");
        if (!allowColor) {
            desired = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', desired));
        }
        if (desired.length() < 2 || desired.length() > 32) {
            languages.send(sender, "nickname.length");
            return true;
        }
        nicknames.setNickname(target, desired, allowColor);
        languages.send(target, "nickname.applied-self",
                Placeholders.with("nickname", ChatColor.translateAlternateColorCodes('&', desired)));
        if (!sender.equals(target)) {
            languages.send(sender, "nickname.applied-other",
                    Placeholders.with("player", target.getName(),
                            "nickname", ChatColor.translateAlternateColorCodes('&', desired)));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("off");
            return options;
        }
        if (args.length == 2 && sender.hasPermission("obx.nick.others")) {
            List<String> names = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
