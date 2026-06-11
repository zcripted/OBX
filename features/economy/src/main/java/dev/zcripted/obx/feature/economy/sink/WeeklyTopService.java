package dev.zcripted.obx.feature.economy.sink;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Manages weekly balance-top snapshots. On schedule (every Monday), the top N
 * balances are frozen into a separate table so players can see "who had the most
 * money this week" even after the leaderboard shifts.
 *
 * <p>Config: {@code economy.sinks.weekly-top.enabled}, {@code economy.sinks.weekly-top.top-size} (default 50).
 */
public final class WeeklyTopService {

    private static final String TABLE = "economy_baltop_weekly";

    private final ObxPlugin plugin;
    private final SqliteDataStore store;

    public WeeklyTopService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            return;
        }
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "week_key TEXT NOT NULL, rank INTEGER NOT NULL,"
                + " player_uuid TEXT, player_name TEXT, balance REAL NOT NULL,"
                + " PRIMARY KEY (week_key, rank))");
    }

    /**
     * Takes a snapshot of the current top N balances keyed by the current ISO week.
     */
    public void snapshot() {
        if (!store.isAvailable()) {
            return;
        }
        EconomyService economy = plugin.getEconomyService();
        if (economy == null) {
            return;
        }
        String weekKey = weekKey();
        int topSize = Math.max(10, plugin.getConfig().getInt("economy.sinks.weekly-top.top-size", 50));
        List<EconomyService.BalanceEntry> top = economy.topBalances(topSize);
        store.executeUpdate("DELETE FROM " + TABLE + " WHERE week_key = ?", weekKey);
        int rank = 1;
        for (EconomyService.BalanceEntry entry : top) {
            store.executeUpdate("INSERT INTO " + TABLE
                    + " (week_key, rank, player_uuid, player_name, balance) VALUES (?, ?, ?, ?, ?)",
                    weekKey, rank++, entry.getUuid(), entry.getName(), entry.getBalance());
        }
    }

    /**
     * Returns the snapshot for a given week key, or empty list if none exists.
     */
    public List<WeeklyEntry> getWeek(String weekKey) {
        if (!store.isAvailable()) {
            return java.util.Collections.emptyList();
        }
        return store.queryAll(
                "SELECT rank, player_uuid, player_name, balance FROM " + TABLE
                        + " WHERE week_key = ? ORDER BY rank ASC",
                rs -> new WeeklyEntry(
                        rs.getInt("rank"),
                        rs.getString("player_uuid"),
                        rs.getString("player_name"),
                        rs.getDouble("balance")),
                weekKey);
    }

    /** Returns the current ISO week key (e.g. "2026-W23"). */
    public static String weekKey() {
        return new SimpleDateFormat("yyyy-'W'ww").format(new Date());
    }

    public static final class WeeklyEntry {
        private final int rank;
        private final String playerUuid;
        private final String playerName;
        private final double balance;

        WeeklyEntry(int rank, String playerUuid, String playerName, double balance) {
            this.rank = rank;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.balance = balance;
        }

        public int rank() { return rank; }
        public String playerUuid() { return playerUuid; }
        public String playerName() { return playerName; }
        public double balance() { return balance; }
    }
}