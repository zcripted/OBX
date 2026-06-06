package dev.zcripted.obx.feature.economy.papi;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * PlaceholderAPI expansion ({@code %obx_...%}) for the OBX economy — feeds the same
 * numbers the scoreboard/tablist render into any PAPI-aware plugin:
 *
 * <ul>
 *   <li>{@code %obx_balance%} / {@code %obx_balance_formatted%}</li>
 *   <li>{@code %obx_baltop_<n>_name%} / {@code %obx_baltop_<n>_balance%} (n = 1–10)</li>
 *   <li>{@code %obx_accounts%} / {@code %obx_supply%}</li>
 * </ul>
 *
 * <p>PAPI resolves placeholders on hot paths (chat, scoreboards), so leaderboard and
 * stats reads go through a 3-second snapshot cache instead of per-request SQL.
 *
 * <p>This class is only ever LOADED when the PlaceholderAPI plugin is present
 * (guarded by a {@code Class.forName} probe in EconomyModule) — the dependency is
 * compileOnly and never shaded.
 */
public final class ObxEconomyExpansion extends PlaceholderExpansion {

    private static final long CACHE_TTL_MS = 3000L;
    private static final long BALANCE_TTL_MS = 2000L;

    private final ObxPlugin plugin;
    private volatile List<EconomyService.BalanceEntry> topCache = Collections.emptyList();
    private volatile int accountsCache = -1;
    private volatile double supplyCache = -1;
    private volatile long cachedAt;
    /** Per-player balance snapshot — keeps %obx_balance% off the main thread on PAPI's
     *  per-tick render path (scoreboards/tablist) instead of a synchronous SQLite read. */
    private final java.util.Map<java.util.UUID, double[]> balanceCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    public ObxEconomyExpansion(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "obx";
    }

    @Override
    public String getAuthor() {
        return "zcripted";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription() == null ? "1.0.0" : plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // survive PAPI reloads — OBX re-registers on its own lifecycle
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        EconomyService economy = plugin.getEconomyService();
        if (economy == null || params == null) {
            return "";
        }
        String key = params.toLowerCase(Locale.ENGLISH);
        switch (key) {
            case "balance":
            case "balance_formatted":
                if (player == null) {
                    return "";
                }
                double balance = cachedBalance(economy, player.getUniqueId());
                return key.equals("balance")
                        ? String.valueOf(balance)
                        : economy.format(balance);
            case "accounts":
                refreshCache(economy);
                return accountsCache < 0 ? "?" : String.valueOf(accountsCache);
            case "supply":
                refreshCache(economy);
                return supplyCache < 0 ? "?" : economy.format(supplyCache);
            default:
                break;
        }
        if (key.startsWith("baltop_")) {
            return baltop(economy, key.substring("baltop_".length()));
        }
        return null; // unknown sub-placeholder — let PAPI report it
    }

    /** {@code <n>_name} or {@code <n>_balance} (1-based rank, capped at the top 10). */
    private String baltop(EconomyService economy, String spec) {
        int split = spec.indexOf('_');
        if (split <= 0) {
            return "";
        }
        int rank;
        try {
            rank = Integer.parseInt(spec.substring(0, split));
        } catch (NumberFormatException ignored) {
            return "";
        }
        if (rank < 1 || rank > 10) {
            return "";
        }
        refreshCache(economy);
        if (rank > topCache.size()) {
            return "";
        }
        EconomyService.BalanceEntry entry = topCache.get(rank - 1);
        String field = spec.substring(split + 1);
        if (field.equals("name")) {
            return entry.getName() == null ? "?" : entry.getName();
        }
        if (field.equals("balance")) {
            return economy.format(entry.getBalance());
        }
        return "";
    }

    /** {@link EconomyService#getBalance} behind a short per-UUID TTL (PAPI hot path). */
    private double cachedBalance(EconomyService economy, java.util.UUID uuid) {
        long now = System.currentTimeMillis();
        double[] cached = balanceCache.get(uuid);
        if (cached != null && now - (long) cached[1] < BALANCE_TTL_MS) {
            return cached[0];
        }
        double value = economy.getBalance(uuid);
        balanceCache.put(uuid, new double[]{value, now});
        return value;
    }

    private void refreshCache(EconomyService economy) {
        long now = System.currentTimeMillis();
        if (now - cachedAt < CACHE_TTL_MS) {
            return;
        }
        synchronized (this) {
            if (now - cachedAt < CACHE_TTL_MS) {
                return;
            }
            topCache = economy.topBalances(10);
            accountsCache = economy.accountCount();
            supplyCache = economy.totalSupply();
            cachedAt = now;
        }
    }
}
