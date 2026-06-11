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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WhoisCommand extends AbstractObxCommand implements TabCompleter {

    private final PlaytimeService playtime;

    public WhoisCommand(ObxPlugin plugin) {
        super(plugin);
        this.playtime = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.playerinfo.service.PlaytimeService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.whois")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "info.whois.usage");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = target.getUniqueId();
        if (target.getName() == null && target.getFirstPlayed() == 0L && !playtime.hasSeen(uuid)) {
            languages.send(sender, "info.unknown-player", Placeholders.with("player", args[0]));
            return true;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName() == null ? args[0] : target.getName());
        placeholders.put("uuid", uuid == null ? "unknown" : uuid.toString());
        placeholders.put("online", target.isOnline() ? "yes" : "no");
        placeholders.put("firstSeen", optional(playtime.getFirstSeen(uuid)));
        placeholders.put("lastSeen", optional(playtime.getLastSeen(uuid)));
        placeholders.put("playtime", playtime.formatDuration(playtime.getTotalPlaytimeSeconds(uuid)));
        Player online = target.isOnline() ? target.getPlayer() : null;
        if (online != null) {
            placeholders.put("world", online.getWorld().getName());
            placeholders.put("gamemode", online.getGameMode().name().toLowerCase());
            placeholders.put("health", String.format("%.1f", online.getHealth()));
            placeholders.put("food", String.valueOf(online.getFoodLevel()));
            placeholders.put("xpLevel", String.valueOf(online.getLevel()));
            placeholders.put("vanished", plugin.getServiceRegistry().get(dev.zcripted.obx.api.staff.VanishApi.class) != null
                    && plugin.getServiceRegistry().get(dev.zcripted.obx.api.staff.VanishApi.class).isVanished(online.getUniqueId()) ? "yes" : "no");
            placeholders.put("afk", plugin.getAfkService() != null
                    && plugin.getAfkService().isAfk(online.getUniqueId()) ? "yes" : "no");
            languages.send(sender, "info.whois.online", placeholders);
        } else {
            placeholders.put("world", "—");
            placeholders.put("gamemode", "—");
            placeholders.put("health", "—");
            placeholders.put("food", "—");
            placeholders.put("xpLevel", "—");
            placeholders.put("vanished", "—");
            placeholders.put("afk", "—");
            languages.send(sender, "info.whois.offline", placeholders);
        }
        return true;
    }

    private String optional(String value) {
        return value == null ? "—" : value;
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