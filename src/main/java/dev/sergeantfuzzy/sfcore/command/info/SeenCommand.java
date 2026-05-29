package dev.sergeantfuzzy.sfcore.command.info;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.perf.PlaytimeService;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeenCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final PlaytimeService playtime;

    public SeenCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.playtime = plugin.getPlaytimeService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.seen")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "info.seen.usage");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null && target.getFirstPlayed() == 0L && !playtime.hasSeen(target.getUniqueId())) {
            languages.send(sender, "info.unknown-player", Placeholders.with("player", args[0]));
            return true;
        }
        if (target.isOnline()) {
            languages.send(sender, "info.seen.online", Placeholders.with("player", target.getName()));
            return true;
        }
        String lastSeen = playtime.getLastSeen(target.getUniqueId());
        if (lastSeen == null) {
            languages.send(sender, "info.seen.unknown", Placeholders.with("player", target.getName()));
            return true;
        }
        languages.send(sender, "info.seen.line",
                Placeholders.with("player", target.getName(), "lastSeen", lastSeen));
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
