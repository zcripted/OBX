package dev.zcripted.obx.api.economy;

import java.util.List;
import java.util.UUID;

/**
 * Public economy API. Implemented by {@code feature.economy.service.EconomyServiceImpl}
 * and exposed to other plugins via {@link VaultEconomyProvider}. Balances are
 * stored in SQLite; all mutations are sanitised (see {@link #sanitize(double)}).
 */
public interface EconomyService {

    /** Hard upper bound so a crafted/overflowing amount can't poison the column. */
    double MAX_BALANCE = 1_000_000_000_000.0; // 1 trillion

    /** Clamp to [0, MAX_BALANCE], drop NaN/Infinity, and round to 2 decimals. */
    static double sanitize(double value) {
        if (Double.isNaN(value) || value <= 0.0) {
            return 0.0;
        }
        if (value > MAX_BALANCE) { // also catches +Infinity
            return MAX_BALANCE;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    /** An entry in the balance leaderboard. */
    final class BalanceEntry {
        private final UUID uuid;
        private final String name;
        private final double balance;

        public BalanceEntry(UUID uuid, String name, double balance) {
            this.uuid = uuid;
            this.name = name;
            this.balance = balance;
        }

        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public double getBalance() { return balance; }
    }

    void load();

    void reload();

    void save();

    String getCurrencySymbol();

    String getCurrencyName();

    String getCurrencyNamePlural();

    double getStartingBalance();

    double getBalance(UUID uuid);

    /** Ensures an account row exists (seeded at the starting balance) for {@code uuid}. */
    void ensureAccount(UUID uuid, String name);

    void setBalance(UUID uuid, String name, double value);

    void deposit(UUID uuid, String name, double amount);

    /**
     * Like {@link #deposit} but REFUSES (returns {@code false}) when crediting would
     * exceed {@link #MAX_BALANCE}, instead of silently clamping the overflow away.
     * Use for player-earned money (sells, redemptions) where destroying value is wrong.
     * Default falls back to the clamping deposit for third-party implementors.
     */
    default boolean depositStrict(UUID uuid, String name, double amount) {
        deposit(uuid, name, amount);
        return true;
    }

    boolean withdraw(UUID uuid, String name, double amount);

    boolean transfer(UUID from, String fromName, UUID to, String toName, double amount);

    void resetBalance(UUID uuid, String name);

    String format(double value);

    List<BalanceEntry> topBalances(int limit);

    // ── Stats & lookups (default-implemented so the additions never break an
    //    existing third-party implementor — the SQLite impl overrides them) ────

    /** Number of economy accounts, or {@code -1} when the backing store can't say. */
    default int accountCount() {
        return -1;
    }

    /** Sum of every account balance (total money supply), or {@code -1} when unknown. */
    default double totalSupply() {
        return -1;
    }

    /**
     * Looks up an account by its last-known player name (case-insensitive) — lets
     * admin actions target players who are OFFLINE and unknown to the server's
     * usercache, as long as they ever held an account.
     */
    default java.util.Optional<BalanceEntry> findAccount(String name) {
        return java.util.Optional.empty();
    }

    // ── Transaction log (audit trail for admin/payment/shop money movement) ───

    /** One audited money movement. */
    final class TransactionEntry {
        private final long time;
        private final String actor;
        private final UUID targetUuid;
        private final String targetName;
        private final String action;
        private final double amount;
        private final double balanceAfter;

        public TransactionEntry(long time, String actor, UUID targetUuid, String targetName,
                                String action, double amount, double balanceAfter) {
            this.time = time;
            this.actor = actor;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.action = action;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
        }

        /** Epoch millis when the movement was recorded. */
        public long getTime() { return time; }
        /** Who caused it (player/staff name, or {@code SHOP} / {@code CONSOLE}). */
        public String getActor() { return actor; }
        public UUID getTargetUuid() { return targetUuid; }
        public String getTargetName() { return targetName; }
        /** Movement kind: GIVE / TAKE / SET / RESET / PAY / RECEIVE / SELL / SHOP_BUY / SHOP_SELL. */
        public String getAction() { return action; }
        public double getAmount() { return amount; }
        /** The target's balance immediately after the movement (best-effort snapshot). */
        public double getBalanceAfter() { return balanceAfter; }
    }

    /**
     * Records one money movement in the audit log. Default no-op so third-party
     * implementors are unaffected; the SQLite impl persists to {@code economy_log}.
     */
    default void logTransaction(String actor, UUID targetUuid, String targetName,
                                String action, double amount, double balanceAfter) {
    }

    /**
     * Most-recent audited movements, newest first — for the whole economy when
     * {@code target} is {@code null}, otherwise for that player only.
     */
    default List<TransactionEntry> recentTransactions(UUID target, int limit) {
        return java.util.Collections.emptyList();
    }

    /**
     * Paginated/filtered audit query: newest first, optionally restricted to one
     * {@code target} and/or one {@code action} (e.g. {@code GIVE}, {@code SHOP_BUY}).
     * {@code offset} skips already-shown rows for pagination.
     */
    default List<TransactionEntry> recentTransactions(UUID target, String action, int limit, int offset) {
        return offset > 0 ? java.util.Collections.<TransactionEntry>emptyList()
                : recentTransactions(target, limit);
    }
}
