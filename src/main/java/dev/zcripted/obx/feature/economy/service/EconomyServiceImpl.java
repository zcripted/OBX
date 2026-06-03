package dev.zcripted.obx.feature.economy.service;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.api.economy.EconomyService.BalanceEntry;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

import static dev.zcripted.obx.api.economy.EconomyService.MAX_BALANCE;
import static dev.zcripted.obx.api.economy.EconomyService.sanitize;

/** SQLite-backed {@link EconomyService} implementation. */
public class EconomyServiceImpl implements EconomyService {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");

    private final ObxPlugin plugin;
    private final SqliteDataStore store;

    public EconomyServiceImpl(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    @Override
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

    @Override
    public void reload() { load(); }

    @Override
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

    @Override
    public String getCurrencySymbol() {
        return plugin.getConfig().getString("economy.symbol", "$");
    }

    @Override
    public String getCurrencyName() {
        return plugin.getConfig().getString("economy.currency-name", "Coin");
    }

    @Override
    public String getCurrencyNamePlural() {
        return plugin.getConfig().getString("economy.currency-name-plural", "Coins");
    }

    @Override
    public double getStartingBalance() {
        return plugin.getConfig().getDouble("economy.starting-balance", 100.0);
    }

    @Override
    public double getBalance(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return 0.0;
        return store.queryFirst("SELECT balance FROM economy WHERE uuid = ?",
                rs -> rs.getDouble("balance"), uuid).orElseGet(this::getStartingBalance);
    }

    /** Ensures a row exists (seeded at the starting balance) so atomic UPDATEs apply. */
    private void ensureRow(UUID uuid, String name) {
        store.executeUpdate(
                "INSERT OR IGNORE INTO economy (uuid, name, balance) VALUES (?, ?, ?)",
                uuid, name, sanitize(getStartingBalance()));
    }

    @Override
    public void setBalance(UUID uuid, String name, double value) {
        if (uuid == null || !store.isAvailable()) return;
        store.executeUpdate(
                "INSERT INTO economy (uuid, name, balance) VALUES (?, ?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance," +
                        " name = COALESCE(excluded.name, economy.name)",
                uuid, name, sanitize(value));
    }

    @Override
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

    @Override
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

    @Override
    public boolean transfer(UUID from, String fromName, UUID to, String toName, double amount) {
        if (from == null || to == null || !store.isAvailable()) return false;
        if (from.equals(to)) return false;
        final double amt = sanitize(amount);
        if (amt <= 0.0) return false;
        ensureRow(from, fromName);
        ensureRow(to, toName);
        // Single transaction: guarded debit then credit, all-or-nothing.
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

    @Override
    public void resetBalance(UUID uuid, String name) {
        setBalance(uuid, name, getStartingBalance());
    }

    @Override
    public String format(double value) {
        return getCurrencySymbol() + FORMAT.format(value);
    }

    @Override
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
