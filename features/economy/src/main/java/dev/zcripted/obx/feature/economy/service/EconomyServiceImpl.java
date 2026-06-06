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
        // Audit trail for admin / payment / shop money movement (see logTransaction).
        store.execute("CREATE TABLE IF NOT EXISTS economy_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ts INTEGER NOT NULL," +
                "actor TEXT," +
                "target_uuid TEXT," +
                "target_name TEXT," +
                "action TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "balance_after REAL NOT NULL" +
                ")");
        migrateLegacyYaml();
        pruneTransactionLog();
    }

    /**
     * Deletes audit rows older than {@code economy.log-retention-days} (default 90;
     * {@code 0} disables pruning and keeps the log forever). Runs on every load/reload.
     */
    private void pruneTransactionLog() {
        int days = plugin.getConfig().getInt("economy.log-retention-days", 90);
        if (days <= 0) return;
        long cutoff = System.currentTimeMillis() - days * 86_400_000L;
        store.executeUpdateAsync("DELETE FROM economy_log WHERE ts < ?", cutoff);
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
        // Report the real stored balance; an account with no row (never seeded) is 0, not a
        // fabricated starting balance — players are seeded on join, so real accounts have a
        // row and appear consistently in /balance and /baltop.
        return store.queryFirst("SELECT balance FROM economy WHERE uuid = ?",
                rs -> rs.getDouble("balance"), uuid).orElse(0.0);
    }

    @Override
    public void ensureAccount(UUID uuid, String name) {
        if (uuid == null || !store.isAvailable()) return;
        ensureRow(uuid, name);
        if (name != null && !name.isEmpty()) {
            // The joining player owns this name NOW. Mojang frees names for reuse, so
            // clear it from any OLDER account that recorded it historically — offline
            // name lookups (findAccount) must never resolve to a previous owner.
            store.executeUpdateAsync(
                    "UPDATE economy SET name = NULL WHERE LOWER(name) = LOWER(?) AND uuid != ?", name, uuid);
            store.executeUpdateAsync(
                    "UPDATE economy SET name = ? WHERE uuid = ?", name, uuid);
        }
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
    public boolean depositStrict(UUID uuid, String name, double amount) {
        if (uuid == null || !store.isAvailable()) return false;
        double amt = sanitize(amount);
        if (amt <= 0.0) return true;
        ensureRow(uuid, name);
        // Guarded credit: 0 rows = the deposit would breach MAX_BALANCE — refuse
        // rather than silently clamping the player's earnings away.
        int rows = store.executeUpdateRows(
                "UPDATE economy SET balance = balance + ?, name = COALESCE(?, name)"
                        + " WHERE uuid = ? AND balance + ? <= ?",
                amt, name, uuid, amt, MAX_BALANCE);
        return rows > 0;
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
            // Guarded credit: add the FULL amount only when the recipient has headroom.
            // If crediting would exceed MAX_BALANCE the UPDATE matches 0 rows and we abort
            // the transaction, rolling back the debit so the sender's money is never silently
            // clamped away (the old MIN(...) cap destroyed the excess on a successful commit).
            try (java.sql.PreparedStatement credit = conn.prepareStatement(
                    "UPDATE economy SET balance = balance + ?, name = COALESCE(?, name)" +
                            " WHERE uuid = ? AND balance + ? <= ?")) {
                store.bind(credit, amt, toName, to, amt, MAX_BALANCE);
                if (credit.executeUpdate() == 0) {
                    throw new java.sql.SQLException("recipient at maximum balance");
                }
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

    @Override
    public int accountCount() {
        if (!store.isAvailable()) return -1;
        return store.queryFirst("SELECT COUNT(*) AS n FROM economy",
                rs -> rs.getInt("n")).orElse(-1);
    }

    @Override
    public double totalSupply() {
        if (!store.isAvailable()) return -1;
        return store.queryFirst("SELECT COALESCE(SUM(balance), 0) AS s FROM economy",
                rs -> rs.getDouble("s")).orElse(-1.0);
    }

    @Override
    public java.util.Optional<BalanceEntry> findAccount(String name) {
        if (name == null || name.trim().isEmpty() || !store.isAvailable()) {
            return java.util.Optional.empty();
        }
        // rowid DESC: when two players historically shared a name, the NEWER account
        // wins (Mojang name reuse means the most recent holder owns the name) — and
        // ensureAccount additionally NULLs stale duplicates on every join.
        return store.queryFirst(
                "SELECT uuid, name, balance FROM economy WHERE LOWER(name) = LOWER(?) ORDER BY rowid DESC LIMIT 1",
                rs -> {
                    try {
                        return new BalanceEntry(UUID.fromString(rs.getString("uuid")),
                                rs.getString("name"), rs.getDouble("balance"));
                    } catch (IllegalArgumentException malformed) {
                        return null;
                    }
                }, name.trim());
    }

    @Override
    public void logTransaction(String actor, UUID targetUuid, String targetName,
                               String action, double amount, double balanceAfter) {
        if (!store.isAvailable() || action == null) return;
        store.executeUpdateAsync(
                "INSERT INTO economy_log (ts, actor, target_uuid, target_name, amount, action, balance_after)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                System.currentTimeMillis(), actor, targetUuid, targetName, amount, action, balanceAfter);
    }

    @Override
    public List<TransactionEntry> recentTransactions(UUID target, int limit) {
        return recentTransactions(target, null, limit, 0);
    }

    @Override
    public List<TransactionEntry> recentTransactions(UUID target, String action, int limit, int offset) {
        if (!store.isAvailable()) return java.util.Collections.emptyList();
        int capped = Math.max(1, Math.min(limit, 100));
        int skip = Math.max(0, offset);
        StringBuilder sql = new StringBuilder(
                "SELECT ts, actor, target_uuid, target_name, action, amount, balance_after FROM economy_log");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (target != null) {
            sql.append(" WHERE target_uuid = ?");
            params.add(target);
        }
        if (action != null && !action.trim().isEmpty()) {
            sql.append(target != null ? " AND" : " WHERE").append(" action = ?");
            params.add(action.trim().toUpperCase(java.util.Locale.ENGLISH));
        }
        sql.append(" ORDER BY id DESC LIMIT ").append(capped).append(" OFFSET ").append(skip);
        SqliteDataStore.RowMapper<TransactionEntry> mapper = rs -> {
            UUID uuid = null;
            String raw = rs.getString("target_uuid");
            if (raw != null) {
                try { uuid = UUID.fromString(raw); } catch (IllegalArgumentException ignored) { /* keep null */ }
            }
            return new TransactionEntry(rs.getLong("ts"), rs.getString("actor"), uuid,
                    rs.getString("target_name"), rs.getString("action"),
                    rs.getDouble("amount"), rs.getDouble("balance_after"));
        };
        return store.queryAll(sql.toString(), mapper, params.toArray());
    }
}
