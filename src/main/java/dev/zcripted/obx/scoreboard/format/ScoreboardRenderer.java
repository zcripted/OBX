package dev.zcripted.obx.scoreboard.format;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.scoreboard.service.ScoreboardService;
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
 * Renders OBX's FeatherBoard-style sidebar onto each player's own scoreboard.
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

    private static final String OBJECTIVE = "obx_sb";
    private static final int MAX_LINES = 15;
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");
    private static final Pattern HEX = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final char FULL_HEART = (char) 0x2764; // heart
    private static final char EMPTY_HEART = (char) 0x2661; // hollow heart
    /**
     * Sentinel left in a colorised line by {@link #applyPlaceholders} when the
     * template contains {@code {divider}}. Substituted in a second pass once
     * we know how wide the widest non-divider line is. The {@code SFDIV} token
     * is bracketed by the {@code OBX_} prefix / suffix so it can't collide
     * with anything a legitimate placeholder value or player name produces.
     */
    private static final String DIVIDER_SENTINEL = "OBX_SBDIV_OBX";
    private static final int DIVIDER_MIN_SPACES = 1;

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
        // Resolve {divider} → strikethrough run sized to the widest content
        // line currently on the board. Done after all other placeholders so
        // live fields (heart bar, online count, etc.) are already in their
        // final form when their visible width is measured.
        substituteDividers(lines);
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
            return current; // already an OBX board — reuse it (no per-tick churn)
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
        // Incremental membership: only move a player when their team is actually
        // wrong (addEntry moves them off the other team), and drop entries for
        // players no longer online. Steady-state renders send zero packets —
        // avoids the per-render clear+re-add storm that scaled O(N^2).
        java.util.Set<String> onlineNames = new java.util.HashSet<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            String name = online.getName();
            onlineNames.add(name);
            Team want = online.isOp() ? op : def;
            try {
                if (!want.hasEntry(name)) {
                    want.addEntry(name);
                }
            } catch (Throwable ignored) {
            }
        }
        removeStaleEntries(op, onlineNames);
        removeStaleEntries(def, onlineNames);
    }

    private static void removeStaleEntries(Team team, java.util.Set<String> online) {
        for (String entry : new java.util.ArrayList<>(team.getEntries())) {
            if (!online.contains(entry)) {
                try {
                    team.removeEntry(entry);
                } catch (Throwable ignored) {
                }
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
        // Full economy balance with the configured currency symbol (e.g. "$1,250.00").
        dev.zcripted.obx.economy.EconomyService economy = plugin.getEconomyService();
        String balance = economy == null ? "0" : economy.format(economy.getBalance(player.getUniqueId()));
        placeholders.put("balance", balance);
        placeholders.put("bank", balance);
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
            String key = matcher.group(1);
            String replacement;
            if ("divider".equalsIgnoreCase(key)) {
                // Leave a sentinel — substituteDividers() resolves it after
                // every other line is in its final form.
                replacement = DIVIDER_SENTINEL;
            } else {
                replacement = placeholders.get(key);
                if (replacement == null) {
                    replacement = matcher.group(0);
                }
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Replaces every {@link #DIVIDER_SENTINEL} occurrence with a dark-gray
     * strikethrough run sized to the widest visible content line on the
     * board. Color codes count as zero width.
     */
    private static void substituteDividers(List<String> lines) {
        int maxWidth = 0;
        for (String line : lines) {
            if (line == null || line.contains(DIVIDER_SENTINEL)) {
                continue;
            }
            int width = visibleLength(line);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        if (maxWidth < DIVIDER_MIN_SPACES) {
            maxWidth = DIVIDER_MIN_SPACES;
        }
        String divider = buildDivider(maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line != null && line.contains(DIVIDER_SENTINEL)) {
                lines.set(i, line.replace(DIVIDER_SENTINEL, divider));
            }
        }
    }

    /**
     * Builds the strikethrough divider: {@code §8§m} followed by {@code width}
     * spaces. Each color/format token contributes 0 visible chars, so the
     * resulting line renders as a strikethrough bar exactly {@code width} wide.
     */
    private static String buildDivider(int width) {
        StringBuilder builder = new StringBuilder(4 + width);
        builder.append(ChatColor.DARK_GRAY).append(ChatColor.STRIKETHROUGH);
        for (int i = 0; i < width; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

    /**
     * Counts visible characters in a legacy-colored string. Every
     * {@code §X} formatting pair is skipped (covers both single codes like
     * {@code §c} and the {@code §x §R §R §G §G §B §B} hex sequence the same
     * way — each pair contributes zero width).
     */
    private static int visibleLength(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int len = 0;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ChatColor.COLOR_CHAR && i + 1 < s.length()) {
                i += 2; // skip the color/format pair
                continue;
            }
            len++;
            i++;
        }
        return len;
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
