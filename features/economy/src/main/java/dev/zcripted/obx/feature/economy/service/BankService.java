package dev.zcripted.obx.feature.economy.service;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
     * refused outright (never silently truncated). The cap check and credit are
     * atomic in a single SQL UPDATE, eliminating the TOCTOU between the balance
     * pre-check and the actual credit.
     */
    public boolean deposit(UUID uuid, String name, double amount) {
        double value = EconomyService.sanitize(amount);
        EconomyService economy = plugin.getEconomyService();
        if (value <= 0.0 || uuid == null || economy == null || !store.isAvailable()) {
            return false;
        }
        accrue(uuid, name);
        if (!economy.withdraw(uuid, name, value)) {
            return false;
        }
        ensureRow(uuid, name);
        // Atomic credit with cap guard — the WHERE clause prevents crediting past
        // max-balance, eliminating the TOCTOU between the old pre-check and the update.
        int rows = store.executeUpdateRows(
                "UPDATE " + TABLE + " SET balance = MIN(balance + ?, ?) WHERE uuid = ? AND balance + ? <= ?",
                value, bankMax(), uuid, value, bankMax());
        if (rows <= 0) {
            // Cap would be exceeded or DB hiccup — restore the wallet.
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
        Object[] row = store.queryFirst(
                "SELECT balance, last_interest FROM " + TABLE + " WHERE uuid = ?",
                rs -> new Object[]{rs.getDouble("balance"), rs.getLong("last_interest")}, uuid).orElse(null);
        if (row == null) {
            return;
        }
        double balance = (Double) row[0];
        double rate = effectiveRate(balance, uuid);
        if (rate <= 0.0) {
            return;
        }
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

    /**
     * A single bank-related transaction for the GUI history panel.
     */
    public static final class BankTransaction {
        public final String type;
        public final double amount;
        public final double balanceAfter;
        public final String timestamp;

        BankTransaction(String type, double amount, double balanceAfter, String timestamp) {
            this.type = type;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
            this.timestamp = timestamp;
        }
    }

    /**
     * Returns paginated bank transactions (deposits, withdrawals, interest) for the
     * given player from the audit log, newest first.
     */
    public List<BankTransaction> history(UUID uuid, int offset, int limit) {
        if (uuid == null || !store.isAvailable()) {
            return Collections.emptyList();
        }
        final java.text.SimpleDateFormat date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return store.queryAll(
                "SELECT action, amount, balance_after, ts FROM economy_log"
                        + " WHERE target_uuid = ?"
                        + " AND action IN ('BANK_DEPOSIT','BANK_WITHDRAW','BANK_INTEREST')"
                        + " ORDER BY id DESC LIMIT ? OFFSET ?",
                rs -> new BankTransaction(
                        rs.getString("action"),
                        rs.getDouble("amount"),
                        rs.getDouble("balance_after"),
                        date.format(new java.util.Date(rs.getLong("ts")))),
                uuid, Math.max(1, limit), Math.max(0, offset));
    }

    /**
     * Resolves the effective daily interest rate for a given banked balance, based
     * on optional tiered config under {@code economy.bank.tiers}. Each tier has a
     * {@code rate} plus either {@code min-balance} (balance tier) or
     * {@code permission} (rank tier — matched while the owner is online; lazy
     * accrual only ever runs when the owner touches their own account, so the
     * permission check sees the right player). The highest matching rate wins;
     * falls back to the flat {@code economy.bank.interest-percent-daily}.
     */
    public double effectiveRate(double bankedBalance, UUID owner) {
        List<?> tiers = plugin.getConfig().getMapList("economy.bank.tiers");
        if (tiers == null || tiers.isEmpty()) {
            return Math.max(0.0, plugin.getConfig().getDouble("economy.bank.interest-percent-daily", 0.5));
        }
        double best = Math.max(0.0, plugin.getConfig().getDouble("economy.bank.interest-percent-daily", 0.5));
        org.bukkit.entity.Player online = owner == null ? null : plugin.getServer().getPlayer(owner);
        for (Object raw : tiers) {
            if (!(raw instanceof java.util.Map)) continue;
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> tier = (java.util.Map<String, Object>) raw;
            Object rateObj = tier.get("rate");
            double rate = rateObj instanceof Number ? ((Number) rateObj).doubleValue() : 0;
            if (rate <= best) {
                continue;
            }
            Object permission = tier.get("permission");
            if (permission != null) {
                // Rank tier — requires the owner online and holding the node.
                if (online != null && online.hasPermission(String.valueOf(permission))) {
                    best = rate;
                }
                continue;
            }
            Object minObj = tier.get("min-balance");
            double min = minObj instanceof Number ? ((Number) minObj).doubleValue() : 0;
            if (bankedBalance >= min) {
                best = rate;
            }
        }
        return best;
    }

    /** Balance-tier-only resolution (no rank tiers — kept for callers without an owner). */
    public double effectiveRate(double bankedBalance) {
        return effectiveRate(bankedBalance, null);
    }

    private void ensureRow(UUID uuid, String name) {
        store.executeUpdate(
                "INSERT OR IGNORE INTO " + TABLE + " (uuid, name, balance, last_interest) VALUES (?, ?, 0, ?)",
                uuid, name, System.currentTimeMillis());
    }
}