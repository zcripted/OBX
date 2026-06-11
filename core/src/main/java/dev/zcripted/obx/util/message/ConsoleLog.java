package dev.zcripted.obx.util.message;

import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Level;

/**
 * Centralized, themed console logger for OBX.
 *
 * <p>Every operational line the plugin prints to the server console is funnelled
 * through here so the output is consistent and free of the doubled
 * {@code [OBX] [OBX]} prefix that resulted from emitting a literal
 * {@code [OBX]} tag through the Bukkit {@code PluginLogger} (which already
 * prepends the plugin name itself). Output is written through the
 * {@link ConsoleCommandSender} instead of the plugin logger, so:
 *
 * <ul>
 *   <li>the prefix is rendered exactly once and in OBX's light-purple brand
 *       palette;</li>
 *   <li>the platform (Spigot / Paper / PurPur / Folia, 1.12 → 1.21) converts the
 *       legacy section-codes to ANSI escape sequences on a colour-capable
 *       terminal, and strips them cleanly when piped to a file;</li>
 *   <li>the standard server timestamp / level prefix
 *       ({@code [HH:mm:ss INFO]:}) is preserved because the line still travels
 *       the normal logging pipeline and lands in {@code logs/latest.log}.</li>
 * </ul>
 *
 * <p>If the console sender is unavailable for any reason the call degrades to the
 * plugin {@link java.util.logging.Logger} with the colour codes stripped, so a
 * message is never lost.
 */
public final class ConsoleLog {

    // ── OBX console palette (legacy section-codes) ──────────────────────────
    private static final char S = ChatColor.COLOR_CHAR;
    private static final String BRACKET = S + "8";  // dark grey     — [ ] frames
    private static final String BRAND   = S + "d";  // light purple  — OBX (brand)
    private static final String TAGGED  = S + "d";  // light purple  — subsystem tag
    private static final String BODY    = S + "7";  // grey       — info body
    private static final String OK      = S + "a";  // green      — success body
    private static final String WARNCOL = S + "e";  // yellow     — warning body
    private static final String ERRCOL  = S + "c";  // red        — error body
    private static final String ACCENT  = S + "f";  // white      — highlighted values
    private static final String RESET   = S + "r";

    private ConsoleLog() {
    }

    /** Info line with no subsystem tag: {@code [OBX] message}. */
    public static void info(Plugin plugin, String message) {
        send(plugin, Level.INFO, prefix(null) + BODY + message + RESET, message);
    }

    /** Info line tagged with a subsystem: {@code [OBX][Arcanum] message}. */
    public static void info(Plugin plugin, String tag, String message) {
        send(plugin, Level.INFO, prefix(tag) + BODY + message + RESET, message);
    }

    /** Success (green) line tagged with a subsystem. */
    public static void success(Plugin plugin, String tag, String message) {
        send(plugin, Level.INFO, prefix(tag) + OK + message + RESET, message);
    }

    /**
     * Emits a single combined message: a header line followed by an indented,
     * bulleted list of items. Used for first-run summaries (e.g. the default
     * files that were just generated) so the console shows one tidy block
     * instead of one noisy line per file.
     */
    public static void list(Plugin plugin, String header, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(prefix(null)).append(BODY).append(header);
        for (int i = 0; i < items.size(); i++) {
            builder.append(BRACKET).append(i == 0 ? "  " : ", ")
                    .append(ACCENT).append(items.get(i));
        }
        builder.append(RESET);
        StringBuilder plain = new StringBuilder(header).append(' ');
        for (int i = 0; i < items.size(); i++) {
            plain.append(i == 0 ? "" : ", ").append(items.get(i));
        }
        send(plugin, Level.INFO, builder.toString(), plain.toString());
    }

    private static String prefix(String tag) {
        StringBuilder builder = new StringBuilder();
        builder.append(BRACKET).append('[').append(BRAND).append("OBX").append(BRACKET).append(']');
        if (tag != null && !tag.isEmpty()) {
            builder.append('[').append(TAGGED).append(tag).append(BRACKET).append(']');
        }
        builder.append(' ');
        return builder.toString();
    }

    /**
     * Writes {@code coloured} through the console sender; if that path is
     * unavailable (very early boot, headless harness, etc.) falls back to the
     * plugin logger with colours stripped so the message is still recorded.
     */
    private static void send(Plugin plugin, Level level, String coloured, String plain) {
        if (plugin != null) {
            try {
                ConsoleCommandSender console = plugin.getServer().getConsoleSender();
                if (console != null) {
                    console.sendMessage(coloured);
                    return;
                }
            } catch (Throwable ignored) {
                // fall through to the logger
            }
            if (plugin.getLogger() != null) {
                plugin.getLogger().log(level, ChatColor.stripColor(plain));
            }
        }
    }
}