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
import java.util.List;
import java.util.UUID;

public class UnjailCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final JailService jailService;

    public UnjailCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.jailService = plugin.getJailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.unjail")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "jail.unjail.usage");
            return true;
        }
        UUID uuid = jailService.resolveOfflineUuid(args[0]);
        if (uuid == null || !jailService.isJailed(uuid)) {
            languages.send(sender, "jail.unjail.not-jailed", Placeholders.with("player", args[0]));
            return true;
        }
        jailService.clearState(uuid);
        Player target = Bukkit.getPlayer(uuid);
        if (target != null && target.isOnline()) {
            languages.send(target, "jail.unjail.released-target");
        }
        languages.send(sender, "jail.unjail.released", Placeholders.with("player", args[0]));
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
