package dev.zcripted.obx.feature.playerinfo.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.playerinfo.service.PlaytimeService;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart;
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

public class InfoCommand extends AbstractObxCommand implements TabCompleter {

    private final PlaytimeService playtime;

    public InfoCommand(ObxPlugin plugin) {
        super(plugin);
        this.playtime = plugin.getPlaytimeService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.info.player")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "info.info.usage");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = target.getUniqueId();
        if (target.getName() == null && target.getFirstPlayed() == 0L && !playtime.hasSeen(uuid)) {
            languages.send(sender, "info.unknown-player", Placeholders.with("player", args[0]));
            return true;
        }
        String name = target.getName() == null ? args[0] : target.getName();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", name);
        placeholders.put("uuid", uuid.toString());
        placeholders.put("firstSeen", playtime.getFirstSeen(uuid) == null ? "—" : playtime.getFirstSeen(uuid));
        placeholders.put("lastSeen", playtime.getLastSeen(uuid) == null ? "—" : playtime.getLastSeen(uuid));
        placeholders.put("playtime", playtime.formatDuration(playtime.getTotalPlaytimeSeconds(uuid)));
        placeholders.put("online", target.isOnline() ? "online" : "offline");

        // Header line is plain. Action row below uses interactive parts so staff can
        // jump straight from /info to /seen, /playtime, /tpa, etc.
        languages.send(sender, "info.info.header", placeholders);
        languages.send(sender, "info.info.identity", placeholders);
        languages.send(sender, "info.info.timeline", placeholders);
        languages.send(sender, "info.info.playtime-line", placeholders);

        if (sender instanceof Player) {
            renderActionRow((Player) sender, name);
        }
        return true;
    }

    private void renderActionRow(Player viewer, String targetName) {
        // The action buttons are structured interactive messages
        // (info.info.action.*); {target} fills the click command + hover.
        Map<String, String> ph = Placeholders.with("target", targetName);
        String prefix = languages.get(viewer, "info.info.actions-prefix");
        String separator = languages.get(viewer, "info.info.actions-separator");

        List<InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(InteractiveMessagePart.plain(prefix));
        parts.add(languages.getInteractivePart(viewer, "info.info.action.seen", ph));
        parts.add(InteractiveMessagePart.plain(separator));
        parts.add(languages.getInteractivePart(viewer, "info.info.action.playtime", ph));
        parts.add(InteractiveMessagePart.plain(separator));
        parts.add(languages.getInteractivePart(viewer, "info.info.action.tpa", ph));
        parts.add(InteractiveMessagePart.plain(separator));
        parts.add(languages.getInteractivePart(viewer, "info.info.action.whois", ph));
        ComponentMessenger.sendJoinedHoverMessages(viewer, parts);
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
