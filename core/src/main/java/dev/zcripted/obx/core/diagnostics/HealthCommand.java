package dev.zcripted.obx.core.diagnostics;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.command.AbstractObxCommand;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * {@code /health} (and {@code /obx health}) — staff-only ({@code obx.admin.health}) full
 * server health check, rendered as one clean box-style report:
 *
 * <ul>
 *   <li><b>Performance</b> — TPS (1m/5m/15m), tick time vs the 50 ms budget, process/system
 *       CPU load + core count, heap memory.</li>
 *   <li><b>World</b> — live entities and loaded chunks across all worlds.</li>
 *   <li><b>Network &amp; players</b> — online/max players and average connection ping.</li>
 *   <li><b>Scheduling &amp; storage</b> — pending sync tasks, active async workers, SQLite
 *       store state + size, and server disk capacity.</li>
 *   <li><b>Per player</b> — average entities/ping per player and the per-player share of
 *       tick time and memory.</li>
 * </ul>
 *
 * <p>Every row carries a hover tooltip explaining the metric and its thresholds; key rows
 * and the footer buttons carry click actions ({@code /tps}, {@code /obx diagnostics full},
 * {@code /obx config validate}, re-run). All values are colour-graded
 * (green/yellow/red) so problems stand out at a glance. Console senders receive the same
 * report as plain text via the {@link ComponentMessenger} fallback.
 *
 * <p>All data is gathered from the calling thread with reflection-guarded probes
 * (Paper {@code getEntityCount}, {@code Player#getPing}, {@code com.sun.management} CPU
 * loads), each degrading to {@code n/a} rather than failing — including on Folia, where
 * cross-region entity lists are not directly readable.
 */
public class HealthCommand extends AbstractObxCommand {

    public static final String PERMISSION = "obx.admin.health";

    // ThreadLocal: DecimalFormat is not thread-safe; the report runs on the main
    // thread today, but this removes the latent hazard for free.
    private static final ThreadLocal<DecimalFormat> ONE_DECIMAL =
            ThreadLocal.withInitial(() -> new DecimalFormat("0.0"));
    private static final long MB = 1024L * 1024L;
    private static final long GB = 1024L * MB;

    public HealthCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePermission(sender, PERMISSION)) {
            return true;
        }
        report(sender);
        return true;
    }

    /** Gathers every metric and renders the full box report to {@code sender}. */
    public void report(CommandSender sender) {
        languages.send(sender, "health.header");

        // ── Performance ──────────────────────────────────────────────────────
        section(sender, "health.section.performance");
        TpsService tps = plugin.getTpsService();
        double tps1 = tps == null ? 20.0 : tps.tpsForWindow(TimeUnit.MINUTES.toNanos(1));
        double tps5 = tps == null ? 20.0 : tps.tpsForWindow(TimeUnit.MINUTES.toNanos(5));
        double tps15 = tps == null ? 20.0 : tps.tpsForWindow(TimeUnit.MINUTES.toNanos(15));
        double mspt = tps == null ? 0.0 : tps.mspt();
        row(sender, "health.row.tps", Placeholders.with(
                "tps1", gradeTps(tps1), "tps5", gradeTps(tps5), "tps15", gradeTps(tps15)),
                "health.hover.tps", "/tps");
        row(sender, "health.row.tick", Placeholders.with("mspt", gradeMspt(mspt)),
                "health.hover.tick", "/tps");
        double processCpu = cpuLoad("getProcessCpuLoad");
        double systemCpu = cpuLoad("getSystemCpuLoad");
        if (systemCpu < 0) {
            systemCpu = cpuLoad("getCpuLoad"); // JDK 14+ rename
        }
        row(sender, "health.row.cpu", Placeholders.with(
                "process", gradePercent(processCpu, 0.60, 0.85),
                "system", gradePercent(systemCpu, 0.60, 0.85),
                "cores", String.valueOf(Runtime.getRuntime().availableProcessors())),
                "health.hover.cpu", null);
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryFraction = maxMemory <= 0 ? 0 : (double) usedMemory / maxMemory;
        row(sender, "health.row.memory", Placeholders.with(
                "used", bytes(usedMemory), "max", bytes(maxMemory),
                "percent", gradePercent(memoryFraction, 0.70, 0.85)),
                "health.hover.memory", "/obx diagnostics full");

        // ── World ────────────────────────────────────────────────────────────
        section(sender, "health.section.world");
        long entities = countEntities();
        int worlds = Bukkit.getWorlds().size();
        long chunks = countChunks();
        row(sender, "health.row.entities", Placeholders.with(
                "count", count(entities), "worlds", String.valueOf(worlds)),
                "health.hover.entities", null);
        row(sender, "health.row.chunks", Placeholders.with("count", count(chunks)),
                "health.hover.chunks", null);

        // ── Network & players ────────────────────────────────────────────────
        section(sender, "health.section.network");
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        long pingSum = 0;
        int pingCount = 0;
        int worstPing = -1;
        for (Player player : Bukkit.getOnlinePlayers()) {
            int ping = ping(player);
            if (ping >= 0) {
                pingSum += ping;
                pingCount++;
                worstPing = Math.max(worstPing, ping);
            }
        }
        long avgPing = pingCount == 0 ? -1 : pingSum / pingCount;
        row(sender, "health.row.players", Placeholders.with(
                "online", String.valueOf(online), "max", String.valueOf(max)),
                "health.hover.players", null);
        row(sender, "health.row.ping", Placeholders.with(
                "ping", gradePing(avgPing), "worst", gradePing(worstPing)),
                "health.hover.ping", null);

        // ── Scheduling & storage ─────────────────────────────────────────────
        section(sender, "health.section.system");
        int pendingSync = -1;
        int asyncWorkers = -1;
        try {
            pendingSync = Bukkit.getScheduler().getPendingTasks().size();
        } catch (Throwable folia) {
            // Folia routes work through region schedulers; the legacy queue is n/a.
        }
        try {
            asyncWorkers = Bukkit.getScheduler().getActiveWorkers().size();
        } catch (Throwable folia) {
            // same as above
        }
        row(sender, "health.row.sync", Placeholders.with(
                "count", pendingSync < 0 ? na(sender) : gradeCount(pendingSync, 100, 500)),
                "health.hover.sync", null);
        row(sender, "health.row.async", Placeholders.with(
                "count", asyncWorkers < 0 ? na(sender) : "&f" + asyncWorkers),
                "health.hover.async", null);
        boolean dbUp = plugin.getDataStore() != null && plugin.getDataStore().isAvailable();
        long dbSize = dbUp ? plugin.getDataStore().getDatabaseFile().length() : 0L;
        row(sender, "health.row.database", Placeholders.with(
                "state", languages.get(sender, dbUp ? "health.value.db-ok" : "health.value.db-down"),
                "file", dbUp ? plugin.getDataStore().getDatabaseFile().getName() : "obx.db",
                "size", dbUp ? bytes(dbSize) : "0 MB"),
                "health.hover.database", "/obx config validate");
        File dataRoot = plugin.getDataFolder().getAbsoluteFile();
        long diskTotal = dataRoot.getTotalSpace();
        long diskFree = dataRoot.getUsableSpace();
        double freeFraction = diskTotal <= 0 ? 1.0 : (double) diskFree / diskTotal;
        String freeColor = freeFraction > 0.25 ? "&a" : freeFraction > 0.10 ? "&e" : "&c";
        row(sender, "health.row.disk", Placeholders.with(
                "free", freeColor + bytes(diskFree),
                "total", bytes(diskTotal),
                "used", percent(1.0 - freeFraction)),
                "health.hover.disk", null);

        // ── Per player ───────────────────────────────────────────────────────
        section(sender, "health.section.perplayer");
        String avgEntities = online > 0 && entities >= 0 ? "&f" + (entities / online) : na(sender);
        String avgTick = online > 0 ? "&f" + ONE_DECIMAL.get().format(mspt / online) + " ms" : na(sender);
        String avgMemory = online > 0 ? "&f" + bytes(usedMemory / online) : na(sender);
        row(sender, "health.row.load", Placeholders.with(
                "entities", avgEntities, "ping", gradePing(avgPing)),
                "health.hover.load", null);
        row(sender, "health.row.share", Placeholders.with(
                "tick", avgTick, "memory", avgMemory),
                "health.hover.share", null);

        // ── Footer: divider + action buttons ─────────────────────────────────
        languages.send(sender, "health.divider");
        List<InteractiveMessagePart> buttons = new ArrayList<>();
        buttons.add(InteractiveMessagePart.plain("  "));
        buttons.add(InteractiveMessagePart.interactive(
                languages.get(sender, "health.button.rerun"),
                languages.list(sender, "health.button.rerun.hover", Collections.<String, String>emptyMap()),
                "/health", true));
        buttons.add(InteractiveMessagePart.plain("  "));
        buttons.add(InteractiveMessagePart.interactive(
                languages.get(sender, "health.button.tps"),
                languages.list(sender, "health.button.tps.hover", Collections.<String, String>emptyMap()),
                "/tps", true));
        buttons.add(InteractiveMessagePart.plain("  "));
        buttons.add(InteractiveMessagePart.interactive(
                languages.get(sender, "health.button.diag"),
                languages.list(sender, "health.button.diag.hover", Collections.<String, String>emptyMap()),
                "/obx diagnostics full", true));
        ComponentMessenger.sendJoinedHoverMessages(sender, buttons);
        sender.sendMessage(" ");
    }

    // ── rendering helpers ────────────────────────────────────────────────────

    private void section(CommandSender sender, String key) {
        languages.send(sender, key);
    }

    /** One report row: localized label+value template with hover tooltip and optional click. */
    private void row(CommandSender sender, String rowKey, Map<String, String> placeholders,
                     String hoverKey, String clickCommand) {
        String line = languages.get(sender, rowKey, placeholders);
        List<String> hover = languages.list(sender, hoverKey, placeholders);
        ComponentMessenger.sendHoverMessage(sender, line, hover, clickCommand, true);
    }

    private String na(CommandSender sender) {
        return languages.get(sender, "health.value.na");
    }

    // ── grading / formatting ─────────────────────────────────────────────────

    private static String gradeTps(double tps) {
        double capped = Math.min(tps, 20.0);
        String color = capped >= 18.0 ? "&a" : capped >= 15.0 ? "&e" : "&c";
        return color + ONE_DECIMAL.get().format(capped);
    }

    private static String gradeMspt(double mspt) {
        String color = mspt < 40.0 ? "&a" : mspt <= 50.0 ? "&e" : "&c";
        return color + ONE_DECIMAL.get().format(mspt) + " ms";
    }

    /** Colour-grades a 0..1 fraction (or {@code <0} = unknown) against warn/critical bounds. */
    private String gradePercent(double fraction, double warn, double critical) {
        if (fraction < 0) {
            return "&8n/a";
        }
        String color = fraction < warn ? "&a" : fraction < critical ? "&e" : "&c";
        return color + percent(fraction);
    }

    private static String gradePing(long ping) {
        if (ping < 0) {
            return "&8n/a";
        }
        String color = ping < 80 ? "&a" : ping < 150 ? "&e" : "&c";
        return color + ping + " ms";
    }

    private static String gradeCount(int value, int warn, int critical) {
        String color = value < warn ? "&a" : value < critical ? "&e" : "&c";
        return color + value;
    }

    private static String percent(double fraction) {
        return Math.round(Math.max(0, Math.min(1, fraction)) * 100) + "%";
    }

    private static String count(long value) {
        return value < 0 ? "&8n/a" : "&f" + String.format(Locale.ENGLISH, "%,d", value);
    }

    private static String bytes(long bytes) {
        if (bytes >= 10L * GB) {
            return (bytes / GB) + " GB";
        }
        if (bytes >= GB) {
            return ONE_DECIMAL.get().format((double) bytes / GB) + " GB";
        }
        if (bytes >= MB) {
            return (bytes / MB) + " MB";
        }
        return Math.max(0, bytes / 1024) + " KB";
    }

    // ── reflection-guarded probes ────────────────────────────────────────────

    /** Live entity total across worlds; Paper's Folia-safe counter first, {@code -1} when unreadable. */
    private static long countEntities() {
        long total = 0;
        int readable = 0;
        for (World world : Bukkit.getWorlds()) {
            try {
                Method counter = world.getClass().getMethod("getEntityCount");
                counter.setAccessible(true);
                total += ((Number) counter.invoke(world)).longValue();
                readable++;
            } catch (Throwable noPaperCounter) {
                try {
                    total += world.getEntities().size();
                    readable++;
                } catch (Throwable foliaRegioned) {
                    // unreadable from this thread — skip
                }
            }
        }
        return readable == 0 ? -1 : total;
    }

    private static long countChunks() {
        long total = 0;
        int readable = 0;
        for (World world : Bukkit.getWorlds()) {
            try {
                total += world.getLoadedChunks().length;
                readable++;
            } catch (Throwable unreadable) {
                // skip
            }
        }
        return readable == 0 ? -1 : total;
    }

    /** {@link Player#getPing} (1.17+/Paper 1.16.5) → NMS {@code ping} field → {@code -1}. */
    private static int ping(Player player) {
        try {
            Method getPing = player.getClass().getMethod("getPing");
            return ((Number) getPing.invoke(player)).intValue();
        } catch (Throwable noApi) {
            // older servers: CraftPlayer#getHandle().ping
        }
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (Throwable noField) {
            return -1;
        }
    }

    /** {@code com.sun.management.OperatingSystemMXBean} load (0..1) via reflection; {@code -1} unknown. */
    private static double cpuLoad(String methodName) {
        try {
            Object os = ManagementFactory.getOperatingSystemMXBean();
            Method method = os.getClass().getMethod(methodName);
            method.setAccessible(true);
            double value = ((Number) method.invoke(os)).doubleValue();
            return value >= 0 && value <= 1 ? value : -1;
        } catch (Throwable unavailable) {
            return -1;
        }
    }
}
