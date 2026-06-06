package dev.zcripted.obx.feature.economy.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Per-player daily sell-earnings cap ({@code economy.sell.daily-cap}; {@code 0}
 * disables). Closes the "AFK farm + /sellall = unlimited income" faucet: every
 * sell path (/sell, /sellall, the shop sell clicks, and the Sell-Items GUI) asks
 * {@link #remaining(UUID)} before paying and {@link #record(UUID, double)} after.
 *
 * <p>Persisted in SQLite ({@code economy_sell_day}) so restarts can't reset the
 * day's total; rows from previous days are pruned on load.
 */
public final class SellLimitTracker {

    private static final String TABLE = "economy_sell_day";

    private final ObxPlugin plugin;
    private final SqliteDataStore store;

    public SellLimitTracker(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            return;
        }
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "uuid TEXT NOT NULL, day TEXT NOT NULL, sold REAL NOT NULL DEFAULT 0,"
                + " PRIMARY KEY (uuid, day))");
        // Only today's rows matter — drop the rest so the table stays tiny.
        store.executeUpdateAsync("DELETE FROM " + TABLE + " WHERE day != ?", today());
    }

    /** The configured daily cap; {@code <= 0} means unlimited. */
    public double dailyCap() {
        return plugin.getConfig().getDouble("economy.sell.daily-cap", 0.0);
    }

    /**
     * How much more {@code player} may earn from selling today.
     * {@link Double#MAX_VALUE} when the cap is disabled or the store is down
     * (fail-open: a DB hiccup must not block normal selling).
     */
    public double remaining(UUID player) {
        double cap = dailyCap();
        if (cap <= 0.0 || player == null || !store.isAvailable()) {
            return Double.MAX_VALUE;
        }
        Double sold = store.queryFirst(
                "SELECT sold FROM " + TABLE + " WHERE uuid = ? AND day = ?",
                rs -> rs.getDouble("sold"), player, today()).orElse(null);
        return Math.max(0.0, cap - (sold == null ? 0.0 : sold));
    }

    /** Records {@code amount} of sell earnings against today's total (async upsert). */
    public void record(UUID player, double amount) {
        if (player == null || amount <= 0.0 || dailyCap() <= 0.0 || !store.isAvailable()) {
            return;
        }
        store.executeUpdateAsync(
                "INSERT INTO " + TABLE + " (uuid, day, sold) VALUES (?, ?, ?)"
                        + " ON CONFLICT(uuid, day) DO UPDATE SET sold = sold + ?",
                player, today(), amount, amount);
    }

    private static String today() {
        return LocalDate.now().toString();
    }
}
