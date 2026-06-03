package dev.zcripted.obx.command.info;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.perf.PlaytimeService;
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

public class PlaytimeCommand extends AbstractObxCommand implements TabCompleter {

    private final PlaytimeService playtime;

    public PlaytimeCommand(OBX plugin) {
        super(plugin);
        this.playtime = plugin.getPlaytimeService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.playtime")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        OfflinePlayer target;
        if (args.length >= 1) {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (target.getName() == null && target.getFirstPlayed() == 0L && !playtime.hasSeen(target.getUniqueId())) {
                languages.send(sender, "info.unknown-player", Placeholders.with("player", args[0]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            target = (Player) sender;
        }
        long totalSeconds = playtime.getTotalPlaytimeSeconds(target.getUniqueId());
        long sessionSeconds = target.isOnline() ? playtime.getSessionSeconds(target.getUniqueId()) : 0L;
        if (target.equals(sender)) {
            languages.send(sender, "info.playtime.self",
                    Placeholders.with("total", playtime.formatDuration(totalSeconds),
                            "session", playtime.formatDuration(sessionSeconds)));
        } else if (sessionSeconds > 0L) {
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("player", target.getName());
            placeholders.put("total", playtime.formatDuration(totalSeconds));
            placeholders.put("session", playtime.formatDuration(sessionSeconds));
            languages.send(sender, "info.playtime.other-online", placeholders);
        } else {
            languages.send(sender, "info.playtime.other",
                    Placeholders.with("player", target.getName(), "total", playtime.formatDuration(totalSeconds)));
        }
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
