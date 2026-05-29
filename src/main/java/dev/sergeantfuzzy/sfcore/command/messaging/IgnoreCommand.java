package dev.sergeantfuzzy.sfcore.command.messaging;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.messaging.MessageService;
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
import java.util.UUID;

public class IgnoreCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final MessageService messageService;

    public IgnoreCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.messageService = plugin.getMailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.ignore")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length == 0) {
            List<UUID> ignored = messageService.getIgnoredUuids(player.getUniqueId());
            if (ignored.isEmpty()) {
                languages.send(player, "messaging.ignore.empty");
                return true;
            }
            StringBuilder names = new StringBuilder();
            int i = 0;
            for (UUID id : ignored) {
                if (i++ > 0) names.append(", ");
                OfflinePlayer entry = Bukkit.getOfflinePlayer(id);
                names.append(entry.getName() == null ? id.toString() : entry.getName());
            }
            languages.send(player, "messaging.ignore.list", Placeholders.with("players", names.toString()));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID targetUuid = target.getUniqueId();
        if (target.getName() == null && target.getFirstPlayed() == 0L) {
            languages.send(player, "messaging.ignore.unknown", Placeholders.with("player", args[0]));
            return true;
        }
        if (targetUuid.equals(player.getUniqueId())) {
            languages.send(player, "messaging.ignore.self");
            return true;
        }
        if (messageService.isIgnoring(player.getUniqueId(), targetUuid)) {
            messageService.removeIgnore(player.getUniqueId(), targetUuid);
            languages.send(player, "messaging.ignore.removed", Placeholders.with("player", target.getName()));
        } else {
            messageService.addIgnore(player.getUniqueId(), targetUuid);
            languages.send(player, "messaging.ignore.added", Placeholders.with("player", target.getName()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && online.getUniqueId().equals(((Player) sender).getUniqueId())) continue;
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
