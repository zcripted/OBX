package dev.zcripted.obx.command.admin;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.language.LanguageManager;
import dev.zcripted.obx.util.message.AdventureMessageUtil;
import dev.zcripted.obx.util.perf.TpsService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Replaces the vanilla / Paper {@code /tps} command with a styled report.
 *
 * <p>Players receive the message through {@link AdventureMessageUtil#send} so the
 * MiniMessage {@code <hover:show_text:'…'>} tags embedded in the language template
 * render as in-chat tooltips on each TPS / MSPT value. The console receives the
 * same content rendered to ANSI through OBX's existing console writer (no
 * tooltips on the terminal — just the inline text and color).
 */
public final class TpsCommand extends AbstractObxCommand {

    private static final String BEST_TPS = "&a20.00";
    private static final String BUDGET_MSPT = "&f50.00ms";
    private static final String BEST_MSPT = "&a0.00ms";


    public TpsCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.tps")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        sendReport(plugin, sender);
        return true;
    }

    /**
     * Renders the styled {@code /tps} report to {@code sender}. Public + static so
     * other entry points (e.g. the Performance + Health GUI's "View TPS" item)
     * produce the exact same output without duplicating the layout.
     */
    public static void sendReport(Main plugin, CommandSender sender) {
        LanguageManager languages = plugin.getLanguageManager();
        TpsService service = plugin.getTpsService();
        Map<String, String> placeholders = buildPlaceholders(service);
        List<String> lines = languages.list(sender, "commands.tps.report", placeholders);
        if (sender instanceof ConsoleCommandSender) {
            // Console terminals can't show hover popups, so we strip the MiniMessage
            // hover wrappers before printing the line through the ANSI writer.
            for (String line : lines) {
                String stripped = AdventureMessageUtil.preview(line, Collections.<String, String>emptyMap());
                plugin.writeConsoleLine(ChatColor.translateAlternateColorCodes('&', stripped));
            }
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            for (String line : lines) {
                AdventureMessageUtil.send(player, line, Collections.<String, String>emptyMap());
            }
        } else {
            for (String line : lines) {
                String stripped = AdventureMessageUtil.preview(line, Collections.<String, String>emptyMap());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', stripped));
            }
        }
    }

    private static Map<String, String> buildPlaceholders(TpsService service) {
        double tps1 = service == null ? 20.0 : service.tpsForWindow(TimeUnit.MINUTES.toNanos(1));
        double tps5 = service == null ? 20.0 : service.tpsForWindow(TimeUnit.MINUTES.toNanos(5));
        double tps15 = service == null ? 20.0 : service.tpsForWindow(TimeUnit.MINUTES.toNanos(15));
        double mspt = service == null ? 0.0 : service.mspt();
        boolean realMspt = service != null && service.isMsptFromTickProcessing();
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        String tps1Color = colorForTps(tps1);
        String tps5Color = colorForTps(tps5);
        String tps15Color = colorForTps(tps15);
        String msptColor = colorForMspt(mspt, realMspt);

        String tps1Formatted = formatTps(tps1);
        String tps5Formatted = formatTps(tps5);
        String tps15Formatted = formatTps(tps15);
        String msptFormatted = String.format(Locale.ENGLISH, "%.2f", mspt);

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("tps_1m", tps1Formatted);
        placeholders.put("tps_5m", tps5Formatted);
        placeholders.put("tps_15m", tps15Formatted);
        placeholders.put("tps_1m_color", tps1Color);
        placeholders.put("tps_5m_color", tps5Color);
        placeholders.put("tps_15m_color", tps15Color);
        placeholders.put("mspt", msptFormatted);
        placeholders.put("mspt_color", msptColor);
        placeholders.put("tps_1m_tooltip", buildTpsTooltip(tps1, tps1Color, tps1Formatted, "1 minute"));
        placeholders.put("tps_5m_tooltip", buildTpsTooltip(tps5, tps5Color, tps5Formatted, "5 minutes"));
        placeholders.put("tps_15m_tooltip", buildTpsTooltip(tps15, tps15Color, tps15Formatted, "15 minutes"));
        placeholders.put("mspt_tooltip", buildMsptTooltip(mspt, msptColor, msptFormatted, realMspt));
        placeholders.put("players", Integer.toString(Bukkit.getOnlinePlayers().size()));
        placeholders.put("max_players", Integer.toString(Bukkit.getMaxPlayers()));
        placeholders.put("uptime", formatUptime(uptimeMs));
        placeholders.put("ready", service != null && service.isReady() ? "true" : "false");
        return placeholders;
    }

    /**
     * Builds the multi-line MiniMessage hover tooltip body for a TPS value.
     * Lines are joined with the literal {@code \n} sequence (two characters)
     * so the downstream {@code splitHoverLines} parser breaks them into a
     * proper multi-line tooltip. Every visible line is kept under 45 characters
     * so the tooltip popup never trims words.
     */
    private static String buildTpsTooltip(double tps, String color, String formatted, String window) {
        StringBuilder builder = new StringBuilder(192);
        builder.append("&5TPS &8› &fPerformance");
        builder.append("\\n").append("&8──────────────────────────");
        builder.append("\\n").append("&7Current  &8›  ").append(color).append(formatted);
        builder.append("\\n").append("&7Best     &8›  ").append(BEST_TPS);
        builder.append("\\n").append("&7Window   &8›  &f").append(window);
        builder.append("\\n").append("&7Status   &8›  ").append(tpsStatusText(tps));
        return builder.toString();
    }

    /**
     * Builds the multi-line MiniMessage hover tooltip body for the MSPT value.
     *
     * <p>On Paper / Folia / PurPur the tooltip surfaces the real tick processing
     * time alongside the 50 ms tick budget and remaining headroom. On Spigot /
     * CraftBukkit (where we fall back to the inter-tick drift estimate) the
     * tooltip shows the drift instead — same "lower is better" semantic, but
     * different reference numbers, and no headroom percentage because the value
     * isn't a fraction of any budget.
     */
    private static String buildMsptTooltip(double mspt, String color, String formatted, boolean realProcessingTime) {
        StringBuilder builder = new StringBuilder(224);
        if (realProcessingTime) {
            double clamped = Math.max(0.0, Math.min(50.0, mspt));
            double headroom = (1.0 - clamped / 50.0) * 100.0;
            builder.append("&5MSPT &8› &fTick Time");
            builder.append("\\n").append("&8──────────────────────────");
            builder.append("\\n").append("&7Current  &8›  ").append(color).append(formatted).append("ms");
            builder.append("\\n").append("&7Best     &8›  ").append(BEST_MSPT);
            builder.append("\\n").append("&7Budget   &8›  ").append(BUDGET_MSPT);
            builder.append("\\n").append("&7Headroom &8›  ").append(headroomColor(headroom)).append(String.format(Locale.ENGLISH, "%.1f", headroom)).append("%");
            builder.append("\\n").append("&7Status   &8›  ").append(msptStatusTextProcessing(mspt));
        } else {
            builder.append("&5MSPT &8› &fTick Drift");
            builder.append("\\n").append("&8──────────────────────────");
            builder.append("\\n").append("&7Current  &8›  ").append(color).append(formatted).append("ms behind");
            builder.append("\\n").append("&7Best     &8›  ").append(BEST_MSPT).append(" (on time)");
            builder.append("\\n").append("&7Status   &8›  ").append(msptStatusTextDrift(mspt));
        }
        return builder.toString();
    }

    private static String tpsStatusText(double tps) {
        if (tps >= 19.95) return "&aPerfect";
        if (tps >= 19.5) return "&aHealthy";
        if (tps >= 17.0) return "&eMild lag";
        if (tps >= 14.0) return "&cLagging";
        return "&cSevere lag";
    }

    private static String msptStatusTextProcessing(double mspt) {
        if (mspt < 10) return "&aHealthy";
        if (mspt < 20) return "&aGood";
        if (mspt < 30) return "&eBusy";
        if (mspt < 40) return "&eHeavy";
        if (mspt < 50) return "&cSaturated";
        return "&cOver budget";
    }

    private static String msptStatusTextDrift(double mspt) {
        if (mspt <= 0) return "&aOn schedule";
        if (mspt < 5) return "&aOn schedule";
        if (mspt < 10) return "&eSlight drift";
        if (mspt < 30) return "&eBehind";
        return "&cLagging";
    }

    private static String headroomColor(double pct) {
        if (pct >= 60) return "&a";
        if (pct >= 30) return "&e";
        return "&c";
    }

    private static String formatTps(double tps) {
        return String.format(Locale.ENGLISH, "%.2f", Math.min(20.0, tps));
    }

    private static String colorForTps(double tps) {
        if (tps >= 19.5) {
            return "&a";
        }
        if (tps >= 17.0) {
            return "&e";
        }
        return "&c";
    }

    /**
     * Tick-time color thresholds.
     *
     * <p>On Paper-derived forks the value is the genuine processing time: 0–20 ms is
     * healthy (green), 20–40 ms is creeping toward the 50 ms tick budget (yellow),
     * and ≥ 40 ms means the server is essentially saturated (red).
     *
     * <p>On Spigot / CraftBukkit fallback the value is the <em>excess</em> over the
     * 50 ms tick budget — i.e., milliseconds behind schedule. A healthy server
     * reports 0; we treat 0–10 ms drift as green, 10–30 ms as yellow, and ≥ 30 ms as
     * red because that's serious tick lag on a server that should be ticking on time.
     */
    private static String colorForMspt(double mspt, boolean realProcessingTime) {
        if (realProcessingTime) {
            if (mspt < 0) {
                return "&8";
            }
            if (mspt < 20) {
                return "&a";
            }
            if (mspt < 40) {
                return "&e";
            }
            return "&c";
        }
        if (mspt <= 0) {
            return "&a";
        }
        if (mspt < 10) {
            return "&a";
        }
        if (mspt < 30) {
            return "&e";
        }
        return "&c";
    }

    private static String formatUptime(long millis) {
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / 86400L;
        totalSeconds %= 86400L;
        long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            builder.append(minutes).append("m ");
        }
        builder.append(seconds).append("s");
        return builder.toString();
    }
}
