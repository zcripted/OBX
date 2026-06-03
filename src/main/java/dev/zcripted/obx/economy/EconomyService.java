package dev.zcripted.obx.economy;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.storage.SqliteDataStore;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

public class EconomyService {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");

    public static final class BalanceEntry {
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

    private final OBX plugin;
    private final SqliteDataStore store;

    public EconomyService(OBX plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            plugin.getLogger().warning("EconomyService disabled — SQLite store unavailable.");
            return;
        }
        store.execute("CREATE TABLE IF NOT EXISTS economy (" +
                "uuid TEXT PRIMARY KEY," +
                "name TEXT," +
                "balance REAL NOT NULL DEFAULT 0" +
                ")");
        migrateLegacyYaml();
    }

    public void reload() { load(); }
    public void save() { /* SQLite auto-commit on each statement */ }

    private void migrateLegacyYaml() {
        File legacy = new File(plugin.getDataFolder(), "economy.yml");
        if (!legacy.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection balances = data.getConfigurationSection("balances");
        ConfigurationSection names = data.getConfigurationSection("names");
        if (balances == null) {
            renameMigrated(legacy);
            return;
        }
        int migrated = 0;
        for (String key : balances.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                double balance = balances.getDouble(key);
                String name = names == null ? null : names.getString(key);
                setBalance(uuid, name, balance);
                migrated++;
            } catch (IllegalArgumentException ignored) { /* skip */ }
        }
        plugin.getLogger().info("Migrated " + migrated + " balance row(s) from economy.yml.");
        renameMigrated(legacy);
    }

    private void renameMigrated(File legacy) {
        File renamed = new File(legacy.getParentFile(), legacy.getName() + ".migrated");
        if (!legacy.renameTo(renamed)) {
            plugin.getLogger().warning("Could not rename " + legacy.getName() + " after migration.");
        }
    }

    public String getCurrencySymbol() {
        return plugin.getConfig().getString("economy.symbol", "$");
    }

    public String getCurrencyName() {
        return plugin.getConfig().getString("economy.currency-name", "Coin");
    }

    public String getCurrencyNamePlural() {
        return plugin.getConfig().getString("economy.currency-name-plural", "Coins");
    }

    public double getStartingBalance() {
        return plugin.getConfig().getDouble("economy.starting-balance", 100.0);
    }

    public double getBalance(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return 0.0;
        return store.queryFirst("SELECT balance FROM economy WHERE uuid = ?",
                rs -> rs.getDouble("balance"), uuid).orElseGet(this::getStartingBalance);
    }

    /** Hard upper bound so a crafted/overflowing amount can't poison the column. */
    public static final double MAX_BALANCE = 1_000_000_000_000.0; // 1 trillion

    /** Clamp to [0, MAX_BALANCE], drop NaN/Infinity, and round to 2 decimals. */
    public static double sanitize(double value) {
        if (Double.isNaN(value) || value <= 0.0) {
            return 0.0;
        }
        if (value > MAX_BALANCE) { // also catches +Infinity
            return MAX_BALANCE;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    /** Ensures a row exists (seeded at the starting balance) so atomic UPDATEs apply. */
    private void ensureRow(UUID uuid, String name) {
        store.executeUpdate(
                "INSERT OR IGNORE INTO economy (uuid, name, balance) VALUES (?, ?, ?)",
                uuid, name, sanitize(getStartingBalance()));
    }

    public void setBalance(UUID uuid, String name, double value) {
        if (uuid == null || !store.isAvailable()) return;
        store.executeUpdate(
                "INSERT INTO economy (uuid, name, balance) VALUES (?, ?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance," +
                        " name = COALESCE(excluded.name, economy.name)",
                uuid, name, sanitize(value));
    }

    public void deposit(UUID uuid, String name, double amount) {
        if (uuid == null || !store.isAvailable()) return;
        double amt = sanitize(amount);
        if (amt <= 0.0) return;
        ensureRow(uuid, name);
        // Atomic add, capped — no read-modify-write race.
        store.executeUpdate(
                "UPDATE economy SET balance = MIN(balance + ?, ?), name = COALESCE(?, name) WHERE uuid = ?",
                amt, MAX_BALANCE, name, uuid);
    }

    public boolean withdraw(UUID uuid, String name, double amount) {
        if (uuid == null || !store.isAvailable()) return false;
        double amt = sanitize(amount);
        if (amt <= 0.0) {
            return true; // nothing to take — no-op success (unchanged behaviour)
        }
        ensureRow(uuid, name);
        // Atomic debit guarded by sufficient balance: 0 rows updated = insufficient funds.
        int rows = store.executeUpdateRows(
                "UPDATE economy SET balance = balance - ?, name = COALESCE(?, name) WHERE uuid = ? AND balance >= ?",
                amt, name, uuid, amt);
        return rows > 0;
    }

    public boolean transfer(UUID from, String fromName, UUID to, String toName, double amount) {
        if (from == null || to == null || !store.isAvailable()) return false;
        if (from.equals(to)) return false;
        final double amt = sanitize(amount);
        if (amt <= 0.0) return false;
        ensureRow(from, fromName);
        ensureRow(to, toName);
        // Single transaction: guarded debit then credit, all-or-nothing. If the
        // sender lacks funds the guarded UPDATE affects 0 rows and we throw to roll
        // back, so the recipient is never credited from an empty account.
        return store.transaction(conn -> {
            try (java.sql.PreparedStatement debit = conn.prepareStatement(
                    "UPDATE economy SET balance = balance - ?, name = COALESCE(?, name) WHERE uuid = ? AND balance >= ?")) {
                store.bind(debit, amt, fromName, from, amt);
                if (debit.executeUpdate() == 0) {
                    throw new java.sql.SQLException("insufficient funds");
                }
            }
            try (java.sql.PreparedStatement credit = conn.prepareStatement(
                    "UPDATE economy SET balance = MIN(balance + ?, ?), name = COALESCE(?, name) WHERE uuid = ?")) {
                store.bind(credit, amt, MAX_BALANCE, toName, to);
                credit.executeUpdate();
            }
        });
    }

    public void resetBalance(UUID uuid, String name) {
        setBalance(uuid, name, getStartingBalance());
    }

    public String format(double value) {
        return getCurrencySymbol() + FORMAT.format(value);
    }

    public List<BalanceEntry> topBalances(int limit) {
        String sql = "SELECT uuid, name, balance FROM economy ORDER BY balance DESC" +
                (limit > 0 ? " LIMIT " + limit : "");
        return store.queryAll(sql, rs -> {
            UUID uuid;
            try { uuid = UUID.fromString(rs.getString("uuid")); } catch (IllegalArgumentException ex) { return null; }
            String name = rs.getString("name");
            if (name == null) {
                OfflinePlayer offline = plugin.getServer().getOfflinePlayer(uuid);
                name = offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
            }
            return new BalanceEntry(uuid, name, rs.getDouble("balance"));
        });
    }
}
