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

public class EnderchestCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;

    public EnderchestCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player viewer = (Player) sender;
        if (args.length >= 1) {
            if (!viewer.hasPermission("sfcore.enderchest.others")) {
                languages.send(viewer, "core.no-permission");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                languages.send(viewer, "tpa.target-not-online", Placeholders.with("player", args[0]));
                return true;
            }
            viewer.openInventory(target.getEnderChest());
            languages.send(viewer, "inventory.enderchest.other", Placeholders.with("player", target.getName()));
            return true;
        }
        if (!viewer.hasPermission("sfcore.enderchest")) {
            languages.send(viewer, "core.no-permission");
            return true;
        }
        viewer.openInventory(viewer.getEnderChest());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("sfcore.enderchest.others")) {
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
