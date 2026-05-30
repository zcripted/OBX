package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClearInvCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;

    public ClearInvCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("sfcore.clearinv.others")) {
                languages.send(sender, "core.no-permission");
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                languages.send(sender, "teleport.tp.not-online", Placeholders.with("player", args[0]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("sfcore.clearinv")) {
                languages.send(target, "core.no-permission");
                return true;
            }
        }
        target.getInventory().clear();
        languages.send(target, "inventory.clearinv.self");
        if (!sender.equals(target)) {
            languages.send(sender, "inventory.clearinv.other", Placeholders.with("player", target.getName()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("sfcore.clearinv.others")) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
