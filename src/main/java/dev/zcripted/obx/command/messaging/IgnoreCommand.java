package dev.zcripted.obx.command.messaging;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.messaging.MessageService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class IgnoreCommand extends AbstractObxCommand implements TabCompleter {

    private final MessageService messageService;

    public IgnoreCommand(OBX plugin) {
        super(plugin);
        this.messageService = plugin.getMailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.ignore")) {
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
