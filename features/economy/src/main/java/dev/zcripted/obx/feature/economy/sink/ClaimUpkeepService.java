package dev.zcripted.obx.feature.economy.sink;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

/**
 * Daily plot/claim upkeep (money sink): every 24h each ONLINE claim owner is
 * charged {@code fee-per-claim} per claim they own; proceeds flow to the
 * visible {@link ServerAccountService server account} as {@code CLAIM_UPKEEP}.
 *
 * <p><b>Assumption (documented):</b> OBX itself has no land-claim module, so
 * upkeep integrates with an external claims plugin — currently
 * <b>GriefPrevention</b>, probed via reflection (compileOnly-free; absent
 * plugin = the sink idles with a console note). More providers can be added in
 * {@link #countClaims}.
 *
 * <p>Design choices, erring non-destructive:
 * <ul>
 *   <li>Only online players are charged (their {@code last_charged} anchor
 *       advances by whole days, so log-outs don't dodge fees forever — the
 *       backlog, capped at 7 days, is collected on their next online day).</li>
 *   <li>A player who can't afford the full fee is charged nothing and warned;
 *       OBX never unclaims land it doesn't manage.</li>
 *   <li>{@code obx.upkeep.exempt} skips a player entirely.</li>
 * </ul>
 *
 * <p>Config ({@code economy.sinks.claim-upkeep.*}): {@code enabled} (default
 * false), {@code fee-per-claim}.
 */
public final class ClaimUpkeepService {

    private static final String TABLE = "economy_upkeep";
    private static final long DAY_MILLIS = 86_400_000L;
    private static final int MAX_BACKLOG_DAYS = 7;

    private final ObxPlugin plugin;
    private final SqliteDataStore store;
    private boolean providerMissingLogged;

    public ClaimUpkeepService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            return;
        }
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "uuid TEXT PRIMARY KEY, last_charged INTEGER NOT NULL)");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("economy.sinks.claim-upkeep.enabled", false)
                && feePerClaim() > 0.0 && store.isAvailable();
    }

    private double feePerClaim() {
        return Math.max(0.0, plugin.getConfig().getDouble("economy.sinks.claim-upkeep.fee-per-claim", 25.0));
    }

    /** Hourly sweep: charges every online claim owner whose daily anchor is due. */
    public void sweep() {
        if (!isEnabled()) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            chargeIfDue(player);
        }
    }

    private void chargeIfDue(Player player) {
        if (player.hasPermission("obx.upkeep.exempt")) {
            return;
        }
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        Long anchor = store.queryFirst("SELECT last_charged FROM " + TABLE + " WHERE uuid = ?",
                rs -> rs.getLong("last_charged"), uuid).orElse(null);
        if (anchor == null) {
            // First sight: start the clock now — no surprise back-charge on install day.
            store.executeUpdate("INSERT OR IGNORE INTO " + TABLE
                    + " (uuid, last_charged) VALUES (?, ?)", uuid, now);
            return;
        }
        int days = (int) Math.min(MAX_BACKLOG_DAYS, (now - anchor) / DAY_MILLIS);
        if (days <= 0) {
            return;
        }
        int claims = countClaims(uuid);
        if (claims < 0) {
            return; // no claims provider installed — sink idles (logged once)
        }
        // Anchor advances by exactly the days charged so partial days carry over —
        // claim-less players advance too (nothing owed for those days).
        long newAnchor = anchor + days * DAY_MILLIS;
        if (claims == 0) {
            store.executeUpdate("UPDATE " + TABLE + " SET last_charged = ? WHERE uuid = ?", newAnchor, uuid);
            return;
        }
        EconomyService economy = plugin.getEconomyService();
        if (economy == null) {
            return;
        }
        double fee = EconomyService.sanitize(feePerClaim() * claims * days);
        if (fee <= 0.0) {
            store.executeUpdate("UPDATE " + TABLE + " SET last_charged = ? WHERE uuid = ?", newAnchor, uuid);
            return;
        }
        if (!economy.withdraw(uuid, player.getName(), fee)) {
            // Can't pay: charge nothing, warn, and still advance the anchor — upkeep
            // is a sink, not a debt collector, and OBX won't unclaim foreign land.
            store.executeUpdate("UPDATE " + TABLE + " SET last_charged = ? WHERE uuid = ?", newAnchor, uuid);
            plugin.getLanguageManager().send(player, "economy.sink.upkeep.cant-afford", Placeholders.with(
                    "amount", economy.format(fee), "claims", claims));
            return;
        }
        store.executeUpdate("UPDATE " + TABLE + " SET last_charged = ? WHERE uuid = ?", newAnchor, uuid);
        economy.logTransaction(player.getName(), uuid, player.getName(),
                "CLAIM_UPKEEP", fee, economy.getBalance(uuid));
        ServerAccountService account = plugin.getServiceRegistry().get(ServerAccountService.class);
        if (account != null) {
            account.deposit(player.getName(), "CLAIM_UPKEEP", fee);
        }
        plugin.getLanguageManager().send(player, "economy.sink.upkeep.charged", Placeholders.with(
                "amount", economy.format(fee), "claims", claims));
    }

    /**
     * Claims owned by {@code uuid} via the installed provider, or {@code -1}
     * when no supported claims plugin is present.
     *
     * <p>GriefPrevention (reflective — no compile-time dependency):
     * {@code GriefPrevention.dataStore.getPlayerData(uuid).getClaims().size()}.
     */
    int countClaims(UUID uuid) {
        Plugin griefPrevention = plugin.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (griefPrevention != null && griefPrevention.isEnabled()) {
            try {
                Object dataStore = griefPrevention.getClass().getField("dataStore").get(griefPrevention);
                Method getPlayerData = dataStore.getClass().getMethod("getPlayerData", UUID.class);
                Object playerData = getPlayerData.invoke(dataStore, uuid);
                Method getClaims = playerData.getClass().getMethod("getClaims");
                Object claims = getClaims.invoke(playerData);
                if (claims instanceof Collection) {
                    return ((Collection<?>) claims).size();
                }
            } catch (Throwable incompatible) {
                logProviderMissing("GriefPrevention found but its API didn't match: " + incompatible);
                return -1;
            }
        }
        logProviderMissing("claim-upkeep is enabled but no supported claims plugin"
                + " (GriefPrevention) is installed — the sink is idle.");
        return -1;
    }

    private void logProviderMissing(String message) {
        if (!providerMissingLogged) {
            providerMissingLogged = true;
            dev.zcripted.obx.util.message.ConsoleLog.info(plugin, "Economy", message);
        }
    }
}