package dev.sergeantfuzzy.sfcore.scoreboard.format;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.scoreboard.service.ScoreboardService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders SF-Core's FeatherBoard-style sidebar onto each player's own scoreboard.
 *
 * <p>Lines are written via per-line scoreboard teams: each line gets a unique,
 * invisible color-token entry and a team whose prefix/suffix carries the actual
 * text (the classic technique that works on 1.8 → 1.21 and allows long lines on
 * legacy servers). Because the sidebar needs per-player content, every player is
 * put on their own scoreboard — so the OP-red / player-yellow nametag teams are
 * replicated onto that board too (populated with all online players) to keep the
 * name colors and TAB grouping working alongside the sidebar.
 */
public final class ScoreboardRenderer {

    private static final String OBJECTIVE = "sfcore_sb";
    private static final int MAX_LINES = 15;
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");
    private static final Pattern HEX = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final char FULL_HEART = (char) 0x2764; // ❤
    private static final char EMPTY_HEART = (char) 0x2661; // ♡

    private ScoreboardRenderer() {
    }

    public static void apply(Main plugin, ScoreboardService service, Player player) {
        if (plugin == null || service == null || player == null || !service.isEnabled()) {
            return;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        Scoreboard board = boardFor(player, manager);
        Map<String, String> placeholders = buildPlaceholders(plugin, service, player);
        Objective objective = ensureObjective(board, colorize(applyPlaceholders(service.getTitle(), placeholders)));
        if (objective == null) {
            return;
        }
        List<String> raw = service.getLines();
        List<String> lines = new ArrayList<>(raw.size());
        for (String template : raw) {
            lines.add(colorize(applyPlaceholders(template, placeholders)));
        }
        renderLines(board, objective, lines);
        applyNameTeams(board);
        if (player.getScoreboard() != board) {
            try {
                player.setScoreboard(board);
            } catch (Throwable ignored) {
                // setScoreboard can be region-restricted on Folia — leave as-is.
            }
        }
    }

    /** Restores the player to the server's main scoreboard (called on quit / disable / when off). */
    public static void clear(Player player) {
        if (player == null) {
            return;
        }
        try {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager != null) {
                player.setScoreboard(manager.getMainScoreboard());
            }
        } catch (Throwable ignored) {
            // best effort
        }
    }

    private static Scoreboard boardFor(Player player, ScoreboardManager manager) {
        Scoreboard current = player.getScoreboard();
        if (current != null && current != manager.getMainScoreboard() && current.getObjective(OBJECTIVE) != null) {
            return current; // already an SF-Core board — reuse it (no per-tick churn)
        }
        return manager.getNewScoreboard();
    }

    @SuppressWarnings("deprecation")
    private static Objective ensureObjective(Scoreboard board, String title) {
        Objective objective = board.getObjective(OBJECTIVE);
        if (objective == null) {
            try {
                objective = board.registerNewObjective(OBJECTIVE, "dummy");
            } catch (Throwable cannotCreate) {
                return null;
            }
        }
        try {
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } catch (Throwable ignored) {
            // slot already set
        }
        try {
            objective.setDisplayName(truncate(title, 32));
        } catch (Throwable ignored) {
            // display name limits vary by version
        }
        return objective;
    }

    private static void renderLines(Scoreboard board, Objective objective, List<String> lines) {
        int count = Math.min(lines.size(), MAX_LINES);
        for (int i = 0; i < MAX_LINES; i++) {
            String entry = token(i);
            String teamName = "sb_" + i;
            if (i < count) {
                setTeamText(board, teamName, entry, lines.get(i));
                try {
                    objective.getScore(entry).setScore(count - i); // first line = highest score = top
                } catch (Throwable ignored) {
                    // off-region on Folia — skip this tick
                }
            } else {
                Team team = board.getTeam(teamName);
                if (team != null) {
                    try {
                        team.unregister();
                    } catch (Throwable ignored) {
                    }
                }
                try {
                    board.resetScores(entry);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** A unique, invisible sidebar entry per line index (the 16 color codes render blank). */
    private static String token(int index) {
        ChatColor[] values = ChatColor.values();
        return values[index % 16].toString();
    }

    private static void setTeamText(Scoreboard board, String teamName, String entry, String text) {
        Team team = ensureTeam(board, teamName);
        if (team == null) {
            return;
        }
        if (!team.getEntries().contains(entry)) {
            team.addEntry(entry);
        }
        String prefix;
        String suffix;
        if (text.length() <= 16) {
            prefix = text;
            suffix = "";
        } else {
            int split = 16;
            if (text.charAt(15) == ChatColor.COLOR_CHAR) {
                split = 15; // don't split in the middle of a color code
            }
            prefix = text.substring(0, split);
            suffix = ChatColor.getLastColors(prefix) + text.substring(split);
        }
        try {
            team.setPrefix(prefix);
        } catch (Throwable ignored) {
        }
        try {
            team.setSuffix(suffix);
        } catch (Throwable tooLong) {
            try {
                team.setSuffix(suffix.substring(0, Math.min(16, suffix.length())));
            } catch (Throwable ignored) {
            }
        }
    }

    /** Replicates the OP-red / player-yellow nametag teams on this board (online players only). */
    private static void applyNameTeams(Scoreboard board) {
        Team op = ensureTeam(board, "np_0op");
        Team def = ensureTeam(board, "np_1players");
        if (op == null || def == null) {
            return;
        }
        colorTeam(op, ChatColor.RED);
        colorTeam(def, ChatColor.YELLOW);
        clearEntries(op);
        clearEntries(def);
        for (Player online : Bukkit.getOnlinePlayers()) {
            try {
                (online.isOp() ? op : def).addEntry(online.getName());
            } catch (Throwable ignored) {
            }
        }
    }

    private static void colorTeam(Team team, ChatColor color) {
        try {
            String prefix = color.toString();
            if (!prefix.equals(team.getPrefix())) {
                team.setPrefix(prefix);
            }
        } catch (Throwable ignored) {
        }
        try {
            Team.class.getMethod("setColor", ChatColor.class).invoke(team, color);
        } catch (Throwable ignored) {
            // setColor absent (≤1.12) — the prefix already colors the name
        }
    }

    private static void clearEntries(Team team) {
        for (String entry : new HashSet<>(team.getEntries())) {
            try {
                team.removeEntry(entry);
            } catch (Throwable ignored) {
            }
        }
    }

    private static Team ensureTeam(Scoreboard board, String name) {
        Team team = board.getTeam(name);
        if (team == null) {
            try {
                team = board.registerNewTeam(name);
            } catch (IllegalArgumentException alreadyExists) {
                team = board.getTeam(name);
            } catch (Throwable other) {
                return null;
            }
        }
        return team;
    }

    private static Map<String, String> buildPlaceholders(Main plugin, ScoreboardService service, Player player) {
        Map<String, String> placeholders = new HashMap<>();
        Server server = Bukkit.getServer();
        double health = safeHealth(player);
        double maxHealth = safeMaxHealth(player);
        int percent = maxHealth <= 0 ? 0 : (int) Math.round(health / maxHealth * 100.0);
        placeholders.put("plugin", plugin.getDescription().getName());
        placeholders.put("player", player.getName());
        placeholders.put("displayname", player.getDisplayName());
        placeholders.put("world", player.getWorld() == null ? "" : player.getWorld().getName());
        placeholders.put("online", Integer.toString(server.getOnlinePlayers().size()));
        placeholders.put("max", Integer.toString(server.getMaxPlayers()));
        placeholders.put("health", Integer.toString((int) Math.round(health)));
        placeholders.put("max_health", Integer.toString((int) Math.round(maxHealth)));
        placeholders.put("health_percent", Integer.toString(Math.max(0, Math.min(100, percent))));
        placeholders.put("hearts", heartBar(health, maxHealth));
        placeholders.put("ip", service.getServerIp());
        placeholders.put("website", service.getServerWebsite());
        return placeholders;
    }

    /** A 10-icon heart bar: filled red ❤ proportional to health, the rest gray ♡. */
    private static String heartBar(double health, double maxHealth) {
        int icons = 10;
        int filled = maxHealth <= 0 ? 0 : (int) Math.round(health / maxHealth * icons);
        if (filled < 0) {
            filled = 0;
        }
        if (filled > icons) {
            filled = icons;
        }
        StringBuilder builder = new StringBuilder("&c");
        for (int i = 0; i < filled; i++) {
            builder.append(FULL_HEART);
        }
        builder.append("&7");
        for (int i = filled; i < icons; i++) {
            builder.append(EMPTY_HEART);
        }
        return builder.toString();
    }

    @SuppressWarnings("deprecation")
    private static double safeHealth(Player player) {
        try {
            return player.getHealth();
        } catch (Throwable ignored) {
            return 20.0;
        }
    }

    @SuppressWarnings("deprecation")
    private static double safeMaxHealth(Player player) {
        try {
            return player.getMaxHealth();
        } catch (Throwable ignored) {
            return 20.0;
        }
    }

    private static String applyPlaceholders(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty() || template.indexOf('{') < 0) {
            return template == null ? "" : template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer buffer = new StringBuffer(template.length());
        while (matcher.find()) {
            String replacement = placeholders.get(matcher.group(1));
            if (replacement == null) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /** Legacy colorize: {@code &#RRGGBB} → §x sequence (1.16+), then {@code &} codes. */
    private static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX.matcher(input);
        StringBuffer buffer = new StringBuffer();
        char section = ChatColor.COLOR_CHAR;
        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder().append(section).append('x');
            for (char c : matcher.group(1).toCharArray()) {
                replacement.append(section).append(c);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
