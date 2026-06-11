package dev.zcripted.obx.core.console;

import dev.zcripted.obx.api.chat.ChatService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.message.ConsoleTimestamp;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Replaces the vanilla {@code <name> issued server command: /cmd} console line with a
 * hardcoded, styled OBX log line (ANSI truecolor via the direct console writer):
 *
 * <pre>
 * [23:13:21 INFO]: [COMMAND] SergeantFuzzy used command /baltop.
 *                  World: world · Date: Sunday, June 7, 2026 · 2026-06-07 · Time: 11:13 PM EST
 * </pre>
 *
 * <p><b>Custom line</b> — a MONITOR-priority {@link PlayerCommandPreprocessEvent} hook
 * renders the hardcoded format lines (placeholders {player}, {command}, {world}, {date},
 * {date-iso}, {time}) and writes them through {@code OBX.writeConsoleLine} with the
 * shared {@link ConsoleTimestamp} prefix. Cancelled commands are skipped — vanilla
 * doesn't log those either.
 *
 * <p><b>Vanilla suppression</b> — the server logs "issued server command" internally
 * (not via Bukkit), so it can't be cancelled through the API. A log4j2 {@code Filter}
 * is attached to the root logger that DENYs messages containing that marker. The filter
 * is built as a {@link Proxy dynamic proxy} so there is no compile-time log4j
 * dependency and it tolerates every log4j 2.x interface revision shipped between 1.8.8
 * and current Paper (unknown interface methods get safe defaults). The filter consults
 * the live service instance, so plugin disable makes it inert (it stays attached —
 * log4j offers no clean detach — but answers NEUTRAL).
 */
public final class PlayerCommandConsoleLog implements Listener {

    private static final String VANILLA_MARKER = " issued server command: ";
    private static final List<String> DEFAULT_FORMAT = Arrays.asList(
            "&f[&dCOMMAND&f] &d{player} &7used command &d/{command}&7.",
            "&7World: &d{world} &8· &7Date: &d{date} &8· &d{date-iso} &8· &7Time: &d{time}");

    /** The live instance the (static, once-installed) log4j filter consults. */
    private static volatile PlayerCommandConsoleLog active;
    private static volatile boolean filterInstalled;

    private final ObxPlugin plugin;

    public PlayerCommandConsoleLog(ObxPlugin plugin) {
        this.plugin = plugin;
        active = this;
        installVanillaFilter(plugin);
    }

    /** Detach hook for plugin disable: the filter stays registered but goes inert. */
    public void shutdown() {
        if (active == this) {
            active = null;
        }
    }

    private boolean enabled() {
        return true;
    }

    private boolean suppressVanilla() {
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled()) {
            return;
        }
        Player player = event.getPlayer();
        String command = event.getMessage();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        ZoneId zone = ZoneId.of("America/Detroit");
        ZonedDateTime now = ZonedDateTime.now(zone);
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("command", command);
        placeholders.put("world", player.getWorld() == null ? "?" : player.getWorld().getName());
        placeholders.put("date", now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)));
        placeholders.put("date-iso", now.format(DateTimeFormatter.ISO_LOCAL_DATE));
        placeholders.put("time", now.format(DateTimeFormatter.ofPattern("hh:mm a zzz", Locale.ENGLISH)));

        List<String> format = DEFAULT_FORMAT;
        ChatService chat = plugin.getChatService();
        String prefix = ConsoleTimestamp.prefix(plugin, chat == null || chat.isConsoleTimestampEnabled(),
                chat == null ? null : chat.getConsoleTimestampFormat());
        for (int i = 0; i < format.size(); i++) {
            String resolved = format.get(i);
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            String line = ChatColor.translateAlternateColorCodes('&', resolved);
            plugin.writeConsoleLine(i == 0 ? prefix + line : line);
        }
    }

    // ── Vanilla "issued server command" suppression (reflective log4j2 filter) ────

    /**
     * Attaches a DENY filter for the vanilla line to log4j's root logger. Installed at
     * most once per classloader; failures (exotic logging setups) are logged once and
     * the feature degrades to "custom line + vanilla line both shown".
     */
    private static synchronized void installVanillaFilter(ObxPlugin plugin) {
        if (filterInstalled) {
            return;
        }
        try {
            Class<?> filterClass = Class.forName("org.apache.logging.log4j.core.Filter");
            Class<?> resultClass = Class.forName("org.apache.logging.log4j.core.Filter$Result");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object deny = Enum.valueOf((Class<Enum>) resultClass.asSubclass(Enum.class), "DENY");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object neutral = Enum.valueOf((Class<Enum>) resultClass.asSubclass(Enum.class), "NEUTRAL");
            final Class<?> messageClass = Class.forName("org.apache.logging.log4j.message.Message");
            Class<?> logEventClass;
            try {
                logEventClass = Class.forName("org.apache.logging.log4j.core.LogEvent");
            } catch (ClassNotFoundException missing) {
                logEventClass = null;
            }
            final Class<?> eventClass = logEventClass;

            InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                if ("filter".equals(name)) {
                    return shouldDeny(args, messageClass, eventClass) ? deny : neutral;
                }
                if ("getOnMatch".equals(name) || "getOnMismatch".equals(name)) {
                    return neutral;
                }
                if ("isStarted".equals(name)) {
                    return Boolean.TRUE;
                }
                if ("isStopped".equals(name)) {
                    return Boolean.FALSE;
                }
                if ("getState".equals(name)) {
                    Class<?> state = Class.forName("org.apache.logging.log4j.core.LifeCycle$State");
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object started = Enum.valueOf((Class<Enum>) state.asSubclass(Enum.class), "STARTED");
                    return started;
                }
                if ("toString".equals(name)) {
                    return "OBXPlayerCommandLogFilter";
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(name)) {
                    return args != null && args.length == 1 && proxy == args[0];
                }
                return null; // start/stop/initialize and any future void/Object methods
            };
            Object filter = Proxy.newProxyInstance(filterClass.getClassLoader(),
                    new Class<?>[]{filterClass}, handler);
            Object rootLogger = Class.forName("org.apache.logging.log4j.LogManager")
                    .getMethod("getRootLogger").invoke(null);
            rootLogger.getClass().getMethod("addFilter", filterClass).invoke(rootLogger, filter);
            filterInstalled = true;
        } catch (Throwable unsupported) {
            plugin.getLogger().warning("Could not attach the console command-log filter ("
                    + unsupported.getClass().getSimpleName()
                    + ") — the vanilla 'issued server command' line will still appear.");
        }
    }

    /** True when this log event carries the vanilla line AND suppression is currently on. */
    private static boolean shouldDeny(Object[] args, Class<?> messageClass, Class<?> eventClass) {
        PlayerCommandConsoleLog current = active;
        if (current == null || !current.suppressVanilla() || args == null) {
            return false;
        }
        for (Object arg : args) {
            String text = extractText(arg, messageClass, eventClass);
            if (text != null && text.contains(VANILLA_MARKER)) {
                return true;
            }
        }
        return false;
    }

    /** Pulls loggable text out of a filter argument (String / Message / LogEvent). */
    private static String extractText(Object arg, Class<?> messageClass, Class<?> eventClass) {
        if (arg == null) {
            return null;
        }
        try {
            if (arg instanceof String) {
                return (String) arg;
            }
            // Look methods up on the PUBLIC interfaces — implementation classes may be
            // package-private, and Method.invoke would throw IllegalAccessException.
            if (messageClass.isInstance(arg)) {
                Object formatted = messageClass.getMethod("getFormattedMessage").invoke(arg);
                return formatted == null ? null : formatted.toString();
            }
            if (eventClass != null && eventClass.isInstance(arg)) {
                Object message = eventClass.getMethod("getMessage").invoke(arg);
                if (message != null && messageClass.isInstance(message)) {
                    Object formatted = messageClass.getMethod("getFormattedMessage").invoke(message);
                    return formatted == null ? null : formatted.toString();
                }
            }
        } catch (Throwable ignored) {
            // Unexpected log4j surface — treat as non-matching rather than break logging.
        }
        return null;
    }
}