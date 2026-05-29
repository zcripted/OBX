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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DelJailCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final JailService jailService;

    public DelJailCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.jailService = plugin.getJailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.deljail")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "jail.deljail.usage");
            return true;
        }
        if (jailService.deleteJail(args[0])) {
            languages.send(sender, "jail.deljail.deleted", Placeholders.with("jail", args[0]));
        } else {
            languages.send(sender, "jail.unknown-jail", Placeholders.with("jail", args[0]));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Jail jail : jailService.getJails()) {
                if (jail.getName().toLowerCase().startsWith(prefix)) names.add(jail.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
