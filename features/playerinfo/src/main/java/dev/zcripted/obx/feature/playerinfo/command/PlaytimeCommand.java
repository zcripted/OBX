package dev.zcripted.obx.feature.playerinfo.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.playerinfo.service.PlaytimeService;
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

    public PlaytimeCommand(ObxPlugin plugin) {
        super(plugin);
        this.playtime = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.playerinfo.service.PlaytimeService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.playtime")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        java.util.UUID uuid;
        String targetName;
        if (args.length >= 1) {
            // Resolve without a blocking Mojang lookup: online exact first, else the playtime DB
            // (anyone with recorded playtime — online or offline — resolves here).
            Player online = Bukkit.getPlayerExact(args[0]);
            if (online != null) {
                uuid = online.getUniqueId();
                targetName = online.getName();
            } else {
                uuid = playtime.findUuidByName(args[0]);
                if (uuid == null) {
                    languages.send(sender, "info.unknown-player", Placeholders.with("player", args[0]));
                    return true;
                }
                String known = playtime.getLastKnownName(uuid);
                targetName = known != null ? known : args[0];
            }
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            uuid = ((Player) sender).getUniqueId();
            targetName = ((Player) sender).getName();
        }

        boolean online = Bukkit.getPlayer(uuid) != null;
        long totalSeconds = playtime.getTotalPlaytimeSeconds(uuid);
        long sessionSeconds = online ? playtime.getSessionSeconds(uuid) : 0L;
        PlaytimeService.LongestSession longest = playtime.getLongestSession(uuid);

        java.util.Map<String, String> ph = new java.util.HashMap<>();
        ph.put("player", targetName);
        ph.put("total", playtime.formatDuration(totalSeconds));
        ph.put("session", playtime.formatDuration(sessionSeconds));
        ph.put("longest", playtime.formatDuration(longest.seconds));
        ph.put("longest_at", playtime.formatSessionTimestamp(longest.atMillis));

        boolean self = (sender instanceof Player) && ((Player) sender).getUniqueId().equals(uuid);
        if (self) {
            languages.send(sender, "info.playtime.self", ph);
        } else if (online) {
            languages.send(sender, "info.playtime.other-online", ph);
        } else {
            languages.send(sender, "info.playtime.other", ph);
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