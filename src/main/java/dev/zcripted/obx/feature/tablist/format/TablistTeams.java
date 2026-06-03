package dev.zcripted.obx.feature.tablist.format;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Groups staff (OP players) above everyone else in the TAB player list by
 * putting them on scoreboard teams whose names sort first. The Minecraft client
 * orders the player list by team name, so members of {@code obx.0staff}
 * always appear before members of {@code obx.1players} — staff are kept
 * together at the top, separated from regular players.
 *
 * <p>The teams also color the name shown above each player's head: staff (OP)
 * names render light red and regular players light yellow. Coloring is done via
 * the team prefix (a trailing color code the following name inherits) so it works
 * on 1.8 → 1.21; on 1.13+ {@code Team#setColor} is also set reflectively for
 * consistency. The visible tablist name (including the staff tag) is still applied
 * separately via {@code setPlayerListName} in {@link TablistRenderer}.
 *
 * <p>The teams live on the server's main scoreboard so every viewer shares the
 * same ordering. Players are only moved when their staff state actually changes
 * (tracked in {@link #lastState}), so the periodic tablist refresh doesn't spam
 * scoreboard packets.
 */
public final class TablistTeams {

    private static final String STAFF_TEAM = "obx.0staff";
    private static final String PLAYER_TEAM = "obx.1players";

    private static final Map<UUID, Boolean> lastState = new ConcurrentHashMap<>();

    private TablistTeams() {
    }

    /** Puts {@code player} on the staff or players team — a no-op if unchanged. */
    public static void assign(Player player, boolean staff) {
        if (player == null) {
            return;
        }
        Boolean previous = lastState.get(player.getUniqueId());
        if (previous != null && previous == staff) {
            return;
        }
        Scoreboard board = mainScoreboard();
        if (board == null) {
            return;
        }
        try {
            Team team = ensureTeam(board, staff ? STAFF_TEAM : PLAYER_TEAM);
            if (team != null) {
                // addEntry moves the player here, removing them from the other team.
                team.addEntry(player.getName());
                lastState.put(player.getUniqueId(), staff);
            }
        } catch (Throwable ignored) {
            // Scoreboard/team API unavailable on this fork — grouping degrades to
            // the client's default ordering.
        }
    }

    /** Removes {@code player} from the OBX tablist teams (call on quit). */
    public static void remove(Player player) {
        if (player == null) {
            return;
        }
        lastState.remove(player.getUniqueId());
        Scoreboard board = mainScoreboard();
        if (board == null) {
            return;
        }
        for (String name : new String[]{STAFF_TEAM, PLAYER_TEAM}) {
            try {
                Team team = board.getTeam(name);
                if (team != null) {
                    team.removeEntry(player.getName());
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /** Unregisters the OBX tablist teams entirely (call on disable / when off). */
    public static void reset() {
        lastState.clear();
        Scoreboard board = mainScoreboard();
        if (board == null) {
            return;
        }
        for (String name : new String[]{STAFF_TEAM, PLAYER_TEAM}) {
            try {
                Team team = board.getTeam(name);
                if (team != null) {
                    team.unregister();
                }
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
            }
        }
        if (team != null) {
            applyColor(team, name);
        }
        return team;
    }

    /** Colors the team's nametags: staff light red, players light yellow. */
    private static void applyColor(Team team, String name) {
        ChatColor color = STAFF_TEAM.equals(name) ? ChatColor.RED : ChatColor.YELLOW;
        try {
            // The trailing color code in the prefix carries onto the player's name
            // (works on every version, including 1.8–1.12 which lack Team#setColor).
            String prefix = color.toString();
            if (!prefix.equals(team.getPrefix())) {
                team.setPrefix(prefix);
            }
        } catch (Throwable ignored) {
            // Prefix unsupported on this fork — leave nametags uncolored.
        }
        try {
            // 1.13+: also set the proper team color so vanilla name coloring matches.
            Team.class.getMethod("setColor", ChatColor.class).invoke(team, color);
        } catch (Throwable ignored) {
            // setColor absent (≤1.12) — the prefix above already colors the name.
        }
    }

    private static Scoreboard mainScoreboard() {
        try {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            return manager == null ? null : manager.getMainScoreboard();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
