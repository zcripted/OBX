package dev.zcripted.obx.feature.playerinfo.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.command.AbstractObxCommand;
import dev.zcripted.obx.feature.playerinfo.service.PlaytimeService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /topplaytime} — a box-style leaderboard of the top 10 players by total playtime.
 * Open to everyone by default. Ranks 1–3 get medal badges; the viewer's own total is shown below.
 */
public class TopPlaytimeCommand extends AbstractObxCommand {

    private static final int TOP_N = 10;
    /** Circled digits ①–⑩ (U+2460..U+2469) for clean rank badges. */
    private static final char[] CIRCLED = {'①', '②', '③', '④', '⑤',
            '⑥', '⑦', '⑧', '⑨', '⑩'};

    private final PlaytimeService playtime;

    public TopPlaytimeCommand(ObxPlugin plugin) {
        super(plugin);
        this.playtime = plugin.getServiceRegistry().get(PlaytimeService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.topplaytime")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        List<PlaytimeService.PlaytimeEntry> top = playtime.topPlaytimes(TOP_N);
        for (String line : languages.list(sender, "info.topplaytime.header", java.util.Collections.<String, String>emptyMap())) {
            sender.sendMessage(line);
        }
        if (top.isEmpty()) {
            languages.send(sender, "info.topplaytime.empty");
            sender.sendMessage("");
            return true;
        }
        for (int i = 0; i < top.size(); i++) {
            PlaytimeService.PlaytimeEntry entry = top.get(i);
            Map<String, String> ph = new LinkedHashMap<>();
            ph.put("rank", rankBadge(i + 1));
            ph.put("player", entry.getName());
            ph.put("time", playtime.formatDuration(entry.getTotalSeconds()));
            languages.send(sender, "info.topplaytime.entry", ph);
        }
        if (sender instanceof Player) {
            long mine = playtime.getTotalPlaytimeSeconds(((Player) sender).getUniqueId());
            languages.send(sender, "info.topplaytime.you",
                    Placeholders.with("your_total", playtime.formatDuration(mine)));
        }
        sender.sendMessage("");
        return true;
    }

    /** Colored medal/rank badge: 1 gold, 2 silver, 3 bronze, 4-10 gray circled digits. */
    private static String rankBadge(int rank) {
        char glyph = (rank >= 1 && rank <= CIRCLED.length) ? CIRCLED[rank - 1] : '#';
        String color;
        switch (rank) {
            case 1: color = "&6&l"; break;
            case 2: color = "&f&l"; break;
            case 3: color = "&c&l"; break;
            default: color = "&8"; break;
        }
        return color + glyph;
    }
}