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

    void setBalance(UUID uuid, String name, double value);

    void deposit(UUID uuid, String name, double amount);

    boolean withdraw(UUID uuid, String name, double amount);

    boolean transfer(UUID from, String fromName, UUID to, String toName, double amount);

    void resetBalance(UUID uuid, String name);

    String format(double value);

    List<BalanceEntry> topBalances(int limit);
}
