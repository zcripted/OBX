package dev.sergeantfuzzy.sfcore.economy;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.SqliteDataStore;
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

    private final Main plugin;
    private final SqliteDataStore store;

    public EconomyService(Main plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            plugin.getLogger().warning("[SF-Core] EconomyService disabled — SQLite store unavailable.");
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
        plugin.getLogger().info("[SF-Core] Migrated " + migrated + " balance row(s) from economy.yml.");
        renameMigrated(legacy);
    }

    private void renameMigrated(File legacy) {
        File renamed = new File(legacy.getParentFile(), legacy.getName() + ".migrated");
        if (!legacy.renameTo(renamed)) {
            plugin.getLogger().warning("[SF-Core] Could not rename " + legacy.getName() + " after migration.");
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

    public void setBalance(UUID uuid, String name, double value) {
        if (uuid == null || !store.isAvailable()) return;
        double clamped = Math.max(0.0, value);
        store.executeUpdate(
                "INSERT INTO economy (uuid, name, balance) VALUES (?, ?, ?)" +
                        " ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance," +
                        " name = COALESCE(excluded.name, economy.name)",
                uuid, name, clamped);
    }

    public void deposit(UUID uuid, String name, double amount) {
        if (amount <= 0.0) return;
        setBalance(uuid, name, getBalance(uuid) + amount);
    }

    public boolean withdraw(UUID uuid, String name, double amount) {
        if (amount <= 0.0) return true;
        double balance = getBalance(uuid);
        if (balance < amount) return false;
        setBalance(uuid, name, balance - amount);
        return true;
    }

    public boolean transfer(UUID from, String fromName, UUID to, String toName, double amount) {
        if (from == null || to == null || amount <= 0.0) return false;
        if (from.equals(to)) return false;
        if (!withdraw(from, fromName, amount)) return false;
        deposit(to, toName, amount);
        return true;
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
