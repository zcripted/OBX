package dev.sergeantfuzzy.sfcore.command.teleportation;

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
import java.util.List;

public class TpHereCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final LanguageManager languages;

    public TpHereCommand(Main plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player staff = (Player) sender;
        if (!staff.hasPermission("sfcore.tphere")) {
            languages.send(staff, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(staff, "teleport.tphere.usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            languages.send(staff, "tpa.target-not-online", Placeholders.with("player", args[0]));
            return true;
        }
        plugin.getDataService().setBack(target.getUniqueId(), target.getLocation());
        plugin.getTeleportManager().teleportPlayer(target, staff.getLocation(),
                "teleport.tphere.target-message", Placeholders.with("player", staff.getName()));
        languages.send(staff, "teleport.tphere.success", Placeholders.with("player", target.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> names = new ArrayList<>();
        if (args.length != 1) {
            return names;
        }
        String prefix = args[0].toLowerCase();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && online.getUniqueId().equals(((Player) sender).getUniqueId())) {
                continue;
            }
            if (online.getName().toLowerCase().startsWith(prefix)) {
                names.add(online.getName());
            }
        }
        return names;
    }
}
