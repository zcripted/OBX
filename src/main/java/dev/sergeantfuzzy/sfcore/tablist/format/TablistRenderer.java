package dev.sergeantfuzzy.sfcore.tablist.format;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.tablist.service.TablistService;
import dev.sergeantfuzzy.sfcore.util.message.AdventureMessageUtil;
import dev.sergeantfuzzy.sfcore.util.perf.TpsService;
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
 * {@code getTPS()} are cached per concrete class and the SF-Core internal
 * {@link TpsService} is preferred over the Paper-only {@code Server.getTPS()}
 * when available — it never reflects.
 */
public final class TablistRenderer {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DecimalFormat TPS_FORMAT = new DecimalFormat("0.0");

    private static final Method MISSING = sentinelMethod();
    private static final Field MISSING_FIELD = sentinelField();

    private static final ConcurrentHashMap<Class<?>, Method> PING_METHODS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> HANDLE_METHODS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Field> PING_FIELDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> TPS_METHODS = new ConcurrentHashMap<>();

    private TablistRenderer() {
    }

    public static void apply(Main plugin, TablistService service, Player player) {
        if (plugin == null || service == null || player == null || !service.isEnabled()) {
            return;
        }
        Map<String, String> placeholders = buildPlaceholders(plugin, player);

        String header = joinLines(service.getHeaderLines());
        String footer = joinLines(service.getFooterLines());
        AdventureMessageUtil.applyTablist(player, header, footer, placeholders);

        String nameTemplate = service.getPlayerFormat();
        if (nameTemplate != null && !nameTemplate.isEmpty()) {
            AdventureMessageUtil.applyTablistName(player, nameTemplate, placeholders);
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

    private static Map<String, String> buildPlaceholders(Main plugin, Player player) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        Server server = Bukkit.getServer();
        int online = server.getOnlinePlayers().size();
        int max = server.getMaxPlayers();

        placeholders.put("player", player.getName());
        placeholders.put("displayname", player.getDisplayName());
        placeholders.put("world", player.getWorld() == null ? "" : player.getWorld().getName());
        placeholders.put("uuid", player.getUniqueId().toString());
        placeholders.put("online", Integer.toString(online));
        placeholders.put("max", Integer.toString(max));
        placeholders.put("ping", Integer.toString(resolvePing(player)));
        placeholders.put("time", LocalTime.now().format(TIME_FORMAT));
        placeholders.put("tps", resolveTps(plugin, server));

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

    private static String resolveTps(Main plugin, Server server) {
        TpsService tpsService = plugin.getTpsService();
        if (tpsService != null && tpsService.isReady()) {
            double tps = tpsService.tpsForWindow(TimeUnit.MINUTES.toNanos(1));
            return TPS_FORMAT.format(Math.min(20.0, tps));
        }
        Method getTps = TPS_METHODS.computeIfAbsent(server.getClass(), TablistRenderer::lookupGetTps);
        if (getTps != MISSING) {
            try {
                Object value = getTps.invoke(server);
                if (value instanceof double[]) {
                    double[] tps = (double[]) value;
                    if (tps.length > 0) {
                        return TPS_FORMAT.format(Math.min(20.0, tps[0]));
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
        try {
            return TablistRenderer.class.getDeclaredField("MISSING");
        } catch (NoSuchFieldException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
