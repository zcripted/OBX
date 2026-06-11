package dev.zcripted.obx.feature.tablist.format;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.tablist.TablistService;
import dev.zcripted.obx.util.message.AdventureMessageUtil;
import dev.zcripted.obx.core.diagnostics.TpsService;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Composes the per-player tablist header, footer, and entry name from the
 * {@link TablistService} configuration and pushes the result through
 * {@link AdventureMessageUtil}. All placeholders are resolved here so the
 * rest of the module deals exclusively with already-substituted strings.
 *
 * <p>This class is on the periodic refresh hot path. To minimize per-tick
 * overhead, reflection lookups for {@code getPing()} / {@code getHandle()} /
 * {@code getTPS()} are cached per concrete class and the OBX internal
 * {@link TpsService} is preferred over the Paper-only {@code Server.getTPS()}
 * when available — it never reflects.
 */
public final class TablistRenderer {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    // DecimalFormat is NOT thread-safe and the per-player refresh runs on region
    // threads on Folia — a shared instance corrupts output or throws under load.
    private static final ThreadLocal<DecimalFormat> TPS_FORMAT =
            ThreadLocal.withInitial(() -> new DecimalFormat("0.0"));

    private static final Method MISSING = sentinelMethod();
    private static final Field MISSING_FIELD = sentinelField();

    /** Tracks the scoreboard-owns-grouping state across refreshes to detect the handoff exactly once. */
    private static final java.util.concurrent.atomic.AtomicReference<Boolean> lastScoreboardOwns =
            new java.util.concurrent.atomic.AtomicReference<>();

    private static final ConcurrentHashMap<Class<?>, Method> PING_METHODS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> HANDLE_METHODS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Field> PING_FIELDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> TPS_METHODS = new ConcurrentHashMap<>();

    private TablistRenderer() {
    }

    public static void apply(ObxPlugin plugin, TablistService service, Player player) {
        if (plugin == null || service == null || player == null || !service.isEnabled()) {
            return;
        }
        Map<String, String> placeholders = buildPlaceholders(plugin, player);

        String header = joinLines(service.getHeaderLines());
        String footer = joinLines(service.getFooterLines());
        AdventureMessageUtil.applyTablist(player, header, footer, placeholders);

        // Group staff (OP) above regular players via scoreboard-team sort, and
        // give them the distinct staff name format.
        boolean grouping = service.isStaffGroupingEnabled();
        boolean staff = grouping && player.isOp();
        // Defer the grouping TEAMS to the scoreboard feature when it's active: it moves each
        // player onto a per-player board whose np_0op/np_1players teams already sort staff
        // first (0<1) and color names identically — so our main-board teams would have no
        // effect on those viewers and only fight the scoreboard's. (The staff name format
        // below still applies regardless.)
        boolean scoreboardOwns = scoreboardOwnsGrouping(plugin);
        if (grouping && !scoreboardOwns) {
            TablistTeams.assign(player, player.isOp());
            lastScoreboardOwns.set(Boolean.FALSE);
        } else if (grouping && scoreboardOwns) {
            // Scoreboard just took over grouping — drop our now-inert main-board teams once so stale
            // obx.0staff/obx.1players entries don't linger. Atomic: only the thread that transitions
            // the flag to TRUE runs the reset, so concurrent Folia region threads can't double-fire.
            if (!Boolean.TRUE.equals(lastScoreboardOwns.getAndSet(Boolean.TRUE))) {
                TablistTeams.reset();
            }
        }

        String nameTemplate = staff ? service.getStaffPlayerFormat() : service.getPlayerFormat();
        if (nameTemplate != null && !nameTemplate.isEmpty()) {
            AdventureMessageUtil.applyTablistName(player, nameTemplate, placeholders);
        }
    }

    /** Whether the scoreboard feature is active (it then owns per-player nametag/grouping teams). */
    private static boolean scoreboardOwnsGrouping(dev.zcripted.obx.core.ObxPlugin plugin) {
        try {
            dev.zcripted.obx.api.scoreboard.ScoreboardService sb =
                    plugin.getServiceRegistry().get(dev.zcripted.obx.api.scoreboard.ScoreboardService.class);
            return sb != null && sb.isEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void clear(Player player) {
        if (player == null) {
            return;
        }
        AdventureMessageUtil.applyTablist(player, "", "", java.util.Collections.<String, String>emptyMap());
    }

    private static String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        if (lines.size() == 1) {
            String only = lines.get(0);
            return only == null ? "" : only;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            String line = lines.get(i);
            builder.append(line == null ? "" : line);
        }
        return builder.toString();
    }

    private static Map<String, String> buildPlaceholders(ObxPlugin plugin, Player player) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        Server server = Bukkit.getServer();
        int online = server.getOnlinePlayers().size();
        int max = server.getMaxPlayers();

        placeholders.put("player", player.getName());
        // Neutralize tags: a display name set by a nick/3rd-party plugin could otherwise inject
        // MiniMessage (e.g. <click>/<hover>) into the tab list via the Adventure render path.
        placeholders.put("displayname", dev.zcripted.obx.util.text.MessageSanitizer.neutralizeTags(player.getDisplayName()));
        placeholders.put("world", player.getWorld() == null ? "" : player.getWorld().getName());
        placeholders.put("uuid", player.getUniqueId().toString());
        placeholders.put("online", Integer.toString(online));
        placeholders.put("max", Integer.toString(max));
        placeholders.put("ping", Integer.toString(resolvePing(player)));
        placeholders.put("time", LocalTime.now().format(TIME_FORMAT));
        placeholders.put("tps", resolveTps(plugin, server));
        placeholders.put("uptime", formatUptime());

        if (plugin.getMotdService() != null) {
            try {
                String firstMotdLine = plugin.getMotdService().buildMotd(online, max);
                if (firstMotdLine != null) {
                    int newline = firstMotdLine.indexOf('\n');
                    placeholders.put("motd-1", newline < 0 ? firstMotdLine : firstMotdLine.substring(0, newline));
                } else {
                    placeholders.put("motd-1", "");
                }
            } catch (Throwable ignored) {
                placeholders.put("motd-1", "");
            }
        } else {
            placeholders.put("motd-1", "");
        }
        return placeholders;
    }

    /**
     * Server uptime since JVM start, taken from the runtime MX bean (millisecond
     * resolution) so it ticks live in real time. Rendered "Dd HH:mm:ss", dropping
     * the day segment while under 24h (e.g. "04:21:07", then "3d 04:21:07").
     */
    private static String formatUptime() {
        long millis = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        if (millis < 0) {
            millis = 0;
        }
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        sb.append(two(hours)).append(':').append(two(minutes)).append(':').append(two(seconds));
        return sb.toString();
    }

    private static String two(long value) {
        return value < 10 ? "0" + value : Long.toString(value);
    }

    private static int resolvePing(Player player) {
        Class<?> playerClass = player.getClass();
        Method getPing = PING_METHODS.computeIfAbsent(playerClass, TablistRenderer::lookupGetPing);
        if (getPing != MISSING) {
            try {
                Object value = getPing.invoke(player);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            } catch (Throwable ignored) {
                // fall through to handle.ping
            }
        }
        try {
            Method getHandle = HANDLE_METHODS.computeIfAbsent(playerClass, TablistRenderer::lookupGetHandle);
            if (getHandle == MISSING) {
                return -1;
            }
            Object handle = getHandle.invoke(player);
            if (handle == null) {
                return -1;
            }
            Field pingField = PING_FIELDS.computeIfAbsent(handle.getClass(), TablistRenderer::lookupPingField);
            if (pingField == MISSING_FIELD) {
                return -1;
            }
            Object value = pingField.get(handle);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Throwable ignored) {
            // give up - cache already records the miss
        }
        return -1;
    }

    private static String resolveTps(ObxPlugin plugin, Server server) {
        TpsService tpsService = plugin.getTpsService();
        if (tpsService != null && tpsService.isReady()) {
            double tps = tpsService.tpsForWindow(TimeUnit.MINUTES.toNanos(1));
            return TPS_FORMAT.get().format(Math.min(20.0, tps));
        }
        Method getTps = TPS_METHODS.computeIfAbsent(server.getClass(), TablistRenderer::lookupGetTps);
        if (getTps != MISSING) {
            try {
                Object value = getTps.invoke(server);
                if (value instanceof double[]) {
                    double[] tps = (double[]) value;
                    if (tps.length > 0) {
                        return TPS_FORMAT.get().format(Math.min(20.0, tps[0]));
                    }
                }
            } catch (Throwable ignored) {
                // platform doesn't expose getTPS - fall through to default
            }
        }
        return "20.0";
    }

    private static Method lookupGetPing(Class<?> klass) {
        try {
            return klass.getMethod("getPing");
        } catch (NoSuchMethodException ignored) {
            return MISSING;
        }
    }

    private static Method lookupGetHandle(Class<?> klass) {
        try {
            return klass.getMethod("getHandle");
        } catch (NoSuchMethodException ignored) {
            return MISSING;
        }
    }

    private static Field lookupPingField(Class<?> klass) {
        try {
            return klass.getField("ping");
        } catch (NoSuchFieldException ignored) {
            return MISSING_FIELD;
        }
    }

    private static Method lookupGetTps(Class<?> klass) {
        try {
            return klass.getMethod("getTPS");
        } catch (NoSuchMethodException ignored) {
            return MISSING;
        }
    }

    private static Method sentinelMethod() {
        try {
            return Object.class.getMethod("toString");
        } catch (NoSuchMethodException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static Field sentinelField() {
        // Stable JDK field used purely as a unique non-null sentinel. Avoids
        // reflecting on one of THIS class's own fields by name, which is fragile
        // under obfuscation (our field names get renamed). java.lang.Integer's
        // MAX_VALUE is in the JDK, never obfuscated, and always present.
        try {
            return Integer.class.getDeclaredField("MAX_VALUE");
        } catch (NoSuchFieldException impossible) {
            throw new AssertionError(impossible);
        }
    }
}