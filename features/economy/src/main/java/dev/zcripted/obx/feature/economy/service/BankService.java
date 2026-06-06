package dev.zcripted.obx.feature.economy.service;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;

import java.util.UUID;

/**
 * Savings bank ({@code /bank}): a second, interest-bearing balance per player.
 * Money moves wallet ⇄ bank through the audited economy service; interest accrues
 * LAZILY (computed from elapsed days whenever the account is touched, compounding
 * daily) so no scheduler ticks are spent on offline players.
 *
 * <p>Config ({@code economy.bank.*}): {@code enabled}, {@code interest-percent-daily}
 * (e.g. 0.5 = +0.5%/day, applied to at most 365 banked days per touch), and
 * {@code max-balance} — the interest ceiling, so banked fortunes can't compound
 * to infinity.
 */
public final class BankService {

    private static final String TABLE = "economy_bank";
    private static final int MAX_ACCRUAL_DAYS = 365;

    private final ObxPlugin plugin;
    private final SqliteDataStore store;

    public BankService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            return;
        }
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "uuid TEXT PRIMARY KEY, name TEXT, balance REAL NOT NULL DEFAULT 0,"
                + " last_interest INTEGER NOT NULL)");
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("economy.bank.enabled", true);
    }

    private double interestPercentDaily() {
        return Math.max(0.0, plugin.getConfig().getDouble("economy.bank.interest-percent-daily", 0.5));
    }

    private double bankMax() {
        return Math.min(EconomyService.MAX_BALANCE,
                Math.max(0.0, plugin.getConfig().getDouble("economy.bank.max-balance", 1_000_000_000.0)));
    }

    /** The player's banked balance, with any pending interest applied first. */
    public double balance(UUID uuid, String name) {
        if (uuid == null || !store.isAvailable()) {
            return 0.0;
        }
        accrue(uuid, name);
        Double balance = store.queryFirst("SELECT balance FROM " + TABLE + " WHERE uuid = ?",
                rs -> rs.getDouble("balance"), uuid).orElse(null);
        return balance == null ? 0.0 : balance;
    }

    /**
     * Moves {@code amount} wallet → bank. Wallet is debited first (guarded); the
     * bank credit caps at {@code max-balance} — a deposit that would exceed it is
     * refused outright (never silently truncated).
     */
    public boolean deposit(UUID uuid, String name, double amount) {
        double value = EconomyService.sanitize(amount);
        EconomyService economy = plugin.getEconomyService();
        if (value <= 0.0 || uuid == null || economy == null || !store.isAvailable()) {
            return false;
        }
        if (balance(uuid, name) + value > bankMax()) {
            return false;
        }
        if (!economy.withdraw(uuid, name, value)) {
            return false;
        }
        ensureRow(uuid, name);
        // Credit the bank row, verifying it landed. If the UPDATE matches no rows (DB
        // hiccup), restore the wallet so the money is never silently lost between tables.
        int rows = store.executeUpdateRows(
                "UPDATE " + TABLE + " SET balance = balance + ? WHERE uuid = ?", value, uuid);
        if (rows <= 0) {
            economy.deposit(uuid, name, value);
            return false;
        }
        economy.logTransaction(name, uuid, name, "BANK_DEPOSIT", value, economy.getBalance(uuid));
        return true;
    }

    /**
     * Moves {@code amount} bank → wallet. The bank debit is a guarded UPDATE
     * (refuses overdraw atomically); the wallet credit is strict — if the wallet
     * lacks MAX_BALANCE headroom the bank debit is rolled back.
     */
    public boolean withdraw(UUID uuid, String name, double amount) {
        double value = EconomyService.sanitize(amount);
        EconomyService economy = plugin.getEconomyService();
        if (value <= 0.0 || uuid == null || economy == null || !store.isAvailable()) {
            return false;
        }
        accrue(uuid, name);
        int rows = store.executeUpdateRows(
                "UPDATE " + TABLE + " SET balance = balance - ? WHERE uuid = ? AND balance >= ?",
                value, uuid, value);
        if (rows <= 0) {
            return false;
        }
        if (!economy.depositStrict(uuid, name, value)) {
            // Wallet at cap — restore the banked money, refuse the move.
            store.executeUpdate("UPDATE " + TABLE + " SET balance = balance + ? WHERE uuid = ?", value, uuid);
            return false;
        }
        economy.logTransaction(name, uuid, name, "BANK_WITHDRAW", value, economy.getBalance(uuid));
        return true;
    }

    /**
     * Applies pending daily compound interest (lazy). Whole elapsed days only; the
     * anchor advances by exactly the days credited so partial days carry over.
     */
    private void accrue(UUID uuid, String name) {
        double rate = interestPercentDaily();
        if (rate <= 0.0) {
            return;
        }
        Object[] row = store.queryFirst(
                "SELECT balance, last_interest FROM " + TABLE + " WHERE uuid = ?",
                rs -> new Object[]{rs.getDouble("balance"), rs.getLong("last_interest")}, uuid).orElse(null);
        if (row == null) {
            return;
        }
        double balance = (Double) row[0];
        long anchor = (Long) row[1];
        long now = System.currentTimeMillis();
        int days = (int) Math.min(MAX_ACCRUAL_DAYS, (now - anchor) / 86_400_000L);
        if (days <= 0 || balance <= 0.0) {
            return;
        }
        double grown = EconomyService.sanitize(
                Math.min(bankMax(), balance * Math.pow(1.0 + rate / 100.0, days)));
        long newAnchor = anchor + days * 86_400_000L;
        store.executeUpdate("UPDATE " + TABLE + " SET balance = ?, last_interest = ? WHERE uuid = ?",
                grown, newAnchor, uuid);
        double earned = grown - balance;
        EconomyService economy = plugin.getEconomyService();
        if (earned > 0.0 && economy != null) {
            economy.logTransaction("BANK", uuid, name, "BANK_INTEREST", earned, economy.getBalance(uuid));
        }
    }

    /** Sum of every banked balance (panel/stats visibility). */
    public double totalBanked() {
        if (!store.isAvailable()) {
            return -1;
        }
        Double total = store.queryFirst("SELECT COALESCE(SUM(balance), 0) AS total FROM " + TABLE,
                rs -> rs.getDouble("total")).orElse(null);
        return total == null ? -1 : total;
    }

    private void ensureRow(UUID uuid, String name) {
        store.executeUpdate(
                "INSERT OR IGNORE INTO " + TABLE + " (uuid, name, balance, last_interest) VALUES (?, ?, 0, ?)",
                uuid, name, System.currentTimeMillis());
    }
}
