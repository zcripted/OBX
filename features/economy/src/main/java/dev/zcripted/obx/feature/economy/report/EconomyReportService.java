package dev.zcripted.obx.feature.economy.report;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import dev.zcripted.obx.feature.economy.EconomyModule;
import dev.zcripted.obx.feature.economy.service.BankService;
import dev.zcripted.obx.feature.economy.sink.ServerAccountService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Produces a weekly economy digest: total supply, top balances, top movers
 * (largest 7-day balance swings from the audit log), biggest sinks (server
 * account revenue per source), and bank statistics. Output goes to the
 * console and optionally to a Discord webhook (sent async — never blocks
 * the main thread).
 *
 * <p>Triggered by the weekly-top scheduler in {@link EconomyModule}; admins
 * can run one on demand with {@code /eco digest}.
 */
public final class EconomyReportService {

    private static final long WEEK_MILLIS = 7L * 86_400_000L;

    private final ObxPlugin plugin;

    public EconomyReportService(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    /** One player's net balance movement over the report window. */
    private static final class Mover {
        final String name;
        final double delta;

        Mover(String name, double delta) {
            this.name = name;
            this.delta = delta;
        }
    }

    /**
     * Generates and outputs the weekly digest.
     */
    public void generateDigest() {
        EconomyService economy = plugin.getEconomyService();
        if (economy == null) {
            return;
        }
        long since = System.currentTimeMillis() - WEEK_MILLIS;

        StringBuilder report = new StringBuilder();
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        report.append("§5§m━§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§5§m━");
        report.append("\n§5⚘ §dWeekly Economy Digest §8(").append(date).append("§8)");
        report.append("\n§5§m━§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§5§m━");

        // Total supply
        double totalSupply = economy.totalSupply();
        report.append("\n§7Total Supply: §f").append(economy.format(totalSupply));

        // Top 5 balances
        report.append("\n§8┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄");
        report.append("\n§7Top §f5 §7Balances:");
        List<EconomyService.BalanceEntry> top = economy.topBalances(5);
        int rank = 1;
        for (EconomyService.BalanceEntry entry : top) {
            report.append("\n  §8").append(rank++).append(". §f").append(entry.getName())
                    .append(" §8→ §a").append(economy.format(entry.getBalance()));
        }

        // Top movers: biggest 7-day balance swings, from the audit trail.
        List<Mover> movers = topMovers(since);
        if (!movers.isEmpty()) {
            report.append("\n§8┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄");
            report.append("\n§7Top Movers §8(§77d§8):");
            appendMovers(report, economy, movers, true);  // gainers
            appendMovers(report, economy, movers, false); // losers
        }

        // Biggest sinks: what the server account collected, per source.
        ServerAccountService account = plugin.getServiceRegistry().get(ServerAccountService.class);
        if (account != null) {
            List<ServerAccountService.SourceTotal> sinks = account.sourceTotals(since);
            if (!sinks.isEmpty()) {
                report.append("\n§8┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄");
                report.append("\n§7Biggest Sinks §8(§77d§8):");
                for (ServerAccountService.SourceTotal sink : sinks) {
                    report.append("\n  §8• §f").append(sink.action)
                            .append(" §8→ §c").append(economy.format(sink.total));
                }
            }
            report.append("\n§7Server Account: §f").append(economy.format(account.balance()));
        }

        // Bank statistics
        BankService bank = plugin.getServiceRegistry().get(BankService.class);
        if (bank != null && bank.isEnabled()) {
            double totalBanked = bank.totalBanked();
            report.append("\n§8┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄");
            report.append("\n§7Banked Total: §f").append(economy.format(totalBanked));
            double pct = totalSupply > 0 ? (totalBanked / totalSupply * 100.0) : 0;
            report.append(" §8(").append(String.format("%.1f", pct)).append("% of supply)");
        }

        report.append("\n§5§m━§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━§5§m━");

        // Console output (one writeConsoleLine per line for reliable rendering across Paper versions).
        for (String line : report.toString().split("\n")) {
            plugin.writeConsoleLine(line);
        }

        // Discord webhook (optional) — HTTP must never block the main thread.
        String webhookUrl = plugin.getConfig().getString("economy.reporting.discord-webhook", "");
        if (!webhookUrl.isEmpty()) {
            final String content = report.toString();
            plugin.getSchedulerAdapter().runAsync(() -> sendDiscordDigest(webhookUrl, content));
        }
    }

    /**
     * Net balance movement per player inside the window: last logged
     * {@code balance_after} minus the first. An approximation — the first
     * entry's own delta isn't included — but it needs no schema change and
     * the audit log records every money movement. The server account row is
     * excluded.
     */
    private List<Mover> topMovers(long since) {
        SqliteDataStore store = plugin.getDataStore();
        if (store == null || !store.isAvailable()) {
            return java.util.Collections.emptyList();
        }
        return store.queryAll(
                "SELECT MAX(target_name) AS name,"
                        + " (SELECT l2.balance_after FROM economy_log l2 WHERE l2.target_uuid = l.target_uuid"
                        + "   AND l2.ts >= ? ORDER BY l2.id DESC LIMIT 1)"
                        + " - (SELECT l3.balance_after FROM economy_log l3 WHERE l3.target_uuid = l.target_uuid"
                        + "   AND l3.ts >= ? ORDER BY l3.id ASC LIMIT 1) AS delta"
                        + " FROM economy_log l"
                        + " WHERE l.ts >= ? AND l.target_uuid IS NOT NULL AND l.target_uuid != ?"
                        + " GROUP BY l.target_uuid HAVING delta != 0",
                rs -> new Mover(rs.getString("name"), rs.getDouble("delta")),
                since, since, since, ServerAccountService.SERVER_UUID);
    }

    /** Appends the top three gainers (or losers) to the report. */
    private static void appendMovers(StringBuilder report, EconomyService economy,
                                     List<Mover> movers, boolean gainers) {
        List<Mover> sorted = new java.util.ArrayList<>(movers);
        sorted.sort((a, b) -> gainers
                ? Double.compare(b.delta, a.delta)
                : Double.compare(a.delta, b.delta));
        int shown = 0;
        for (Mover mover : sorted) {
            if (shown >= 3 || (gainers ? mover.delta <= 0 : mover.delta >= 0)) {
                break;
            }
            report.append("\n  §8").append(gainers ? "▲ §f" : "▼ §f")
                    .append(mover.name == null ? "?" : mover.name)
                    .append(" §8→ ").append(gainers ? "§a+" : "§c-")
                    .append(economy.format(Math.abs(mover.delta)));
            shown++;
        }
    }

    private void sendDiscordDigest(String webhookUrl, String content) {
        try {
            String stripped = org.bukkit.ChatColor.stripColor(content);
            String escaped = stripped.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
            String json = "{\"content\":\"" + escaped + "\"}";
            java.net.URL url = new java.net.URL(webhookUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) {
        }
    }
}