package dev.zcripted.obx.util.message;

import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Builds a Paper-style timestamp prefix for lines written through
 * {@code OBX.writeConsoleLine}. Used by both the chat mirror and the
 * join/leave broadcast mirror so the two share a single source of truth.
 *
 * <p>The pattern is a {@link DateTimeFormatter} format string. Wrap any
 * literal characters in single quotes (e.g. {@code 'INFO]'}) so they aren't
 * interpreted as pattern letters. The default mirrors Paper's standard
 * console log line — {@code [12:34:56 INFO]: }.
 */
public final class ConsoleTimestamp {

    public static final String DEFAULT_PATTERN = "'['HH:mm:ss' INFO]: '";

    private ConsoleTimestamp() {
    }

    /**
     * Resolves the prefix using the supplied toggle and pattern. Returns an
     * empty string when the toggle is off or the pattern is blank. If the
     * pattern is malformed a one-off warning is logged and the default
     * pattern is used instead so the operator sees a clear actionable error
     * rather than silent nothing.
     */
    public static String prefix(Plugin plugin, boolean enabled, String pattern) {
        if (!enabled) {
            return "";
        }
        if (pattern == null || pattern.isEmpty()) {
            return "";
        }
        try {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
        } catch (IllegalArgumentException invalid) {
            if (plugin != null && plugin.getLogger() != null) {
                plugin.getLogger().log(Level.WARNING,
                        "Invalid console timestamp pattern \"" + pattern + "\": "
                                + invalid.getMessage() + ". Falling back to default.");
            }
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DEFAULT_PATTERN));
        }
    }
}