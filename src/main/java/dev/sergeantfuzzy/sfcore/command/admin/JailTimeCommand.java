package dev.sergeantfuzzy.sfcore.command.admin;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.jail.JailService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JailTimeCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final JailService jailService;

    public JailTimeCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.jailService = plugin.getJailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.jailtime")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        UUID target;
        String displayName;
        if (args.length >= 1) {
            target = jailService.resolveOfflineUuid(args[0]);
            displayName = args[0];
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "jail.jailtime.usage");
                return true;
            }
            Player player = (Player) sender;
            target = player.getUniqueId();
            displayName = player.getName();
        }
        JailService.JailState state = jailService.getState(target);
        if (state == null) {
            languages.send(sender, "jail.jailtime.not-jailed", Placeholders.with("player", displayName));
            return true;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", displayName);
        placeholders.put("jail", state.getJailName());
        placeholders.put("remaining", jailService.formatDuration(state.getSecondsRemaining()));
        placeholders.put("reason", state.getReason() == null ? "" : state.getReason());
        languages.send(sender, "jail.jailtime.line", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
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
