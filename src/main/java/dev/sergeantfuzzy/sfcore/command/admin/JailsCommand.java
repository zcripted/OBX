package dev.sergeantfuzzy.sfcore.command.admin;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.jail.Jail;
import dev.sergeantfuzzy.sfcore.jail.JailService;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class JailsCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final JailService jailService;

    public JailsCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.jailService = plugin.getJailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.jails")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        java.util.Collection<Jail> all = jailService.getJails();
        if (all.isEmpty()) {
            languages.send(sender, "jail.jails.empty");
            return true;
        }
        StringBuilder names = new StringBuilder();
        int i = 0;
        for (Jail jail : all) {
            if (i++ > 0) names.append(", ");
            names.append(jail.getName());
        }
        languages.send(sender, "jail.jails.list",
                Placeholders.with("count", all.size(), "names", names.toString()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
