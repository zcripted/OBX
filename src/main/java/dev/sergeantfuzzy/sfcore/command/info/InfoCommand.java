package dev.sergeantfuzzy.sfcore.command.info;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.perf.PlaytimeService;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger.InteractiveMessagePart;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InfoCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final LanguageManager languages;
    private final PlaytimeService playtime;

    public InfoCommand(Main plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
        this.playtime = plugin.getPlaytimeService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.info.player")) {
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
        String prefix = languages.get(viewer, "info.info.actions-prefix");
        String separator = languages.get(viewer, "info.info.actions-separator");
        String seenLabel = languages.get(viewer, "info.info.actions-seen");
        String playtimeLabel = languages.get(viewer, "info.info.actions-playtime");
        String tpaLabel = languages.get(viewer, "info.info.actions-tpa");
        String whoisLabel = languages.get(viewer, "info.info.actions-whois");

        List<String> seenHover = languages.list(viewer, "info.info.hover-seen",
                Placeholders.with("player", targetName));
        List<String> playtimeHover = languages.list(viewer, "info.info.hover-playtime",
                Placeholders.with("player", targetName));
        List<String> tpaHover = languages.list(viewer, "info.info.hover-tpa",
                Placeholders.with("player", targetName));
        List<String> whoisHover = languages.list(viewer, "info.info.hover-whois",
                Placeholders.with("player", targetName));

        List<InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(InteractiveMessagePart.plain(prefix));
        parts.add(InteractiveMessagePart.interactive(seenLabel, seenHover, "/seen " + targetName, true));
        parts.add(InteractiveMessagePart.plain(separator));
        parts.add(InteractiveMessagePart.interactive(playtimeLabel, playtimeHover, "/playtime " + targetName, true));
        parts.add(InteractiveMessagePart.plain(separator));
        parts.add(InteractiveMessagePart.interactive(tpaLabel, tpaHover, "/tpa " + targetName, true));
        parts.add(InteractiveMessagePart.plain(separator));
        parts.add(InteractiveMessagePart.interactive(whoisLabel, whoisHover, "/whois " + targetName, true));
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
