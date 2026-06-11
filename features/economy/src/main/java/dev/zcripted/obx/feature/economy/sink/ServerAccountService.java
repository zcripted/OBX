package dev.zcripted.obx.feature.economy.sink;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The visible "server account": every economy sink (auction sales tax +
 * listing fees, anvil repair fees, claim upkeep) deposits here instead of
 * burning the money, so admins can SEE what the sinks collect and recover it
 * (event prize pools, lotteries, …) via {@code /eco server withdraw}.
 *
 * <p>Deliberately NOT a row in the {@code economy} table — the account must
 * never appear in {@code /baltop} or inflate {@code totalSupply()}. It lives
 * in its own single-row table; every deposit additionally writes an
 * {@code economy_log} audit entry targeting {@link #SERVER_UUID}, which is
 * what {@code /eco server} and the weekly digest use to break revenue down
 * by source.
 */
public final class ServerAccountService {

    /** The nil UUID marks server-account rows in the audit log. */
    public static final UUID SERVER_UUID = new UUID(0L, 0L);
    public static final String SERVER_NAME = "SERVER";

    private static final String TABLE = "economy_server_account";

    private final ObxPlugin plugin;
    private final SqliteDataStore store;

    public ServerAccountService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            return;
        }
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "id INTEGER PRIMARY KEY CHECK (id = 1), balance REAL NOT NULL DEFAULT 0)");
        store.executeUpdate("INSERT OR IGNORE INTO " + TABLE + " (id, balance) VALUES (1, 0)");
    }

    /** The server account's current balance ({@code 0} when storage is down). */
    public double balance() {
        if (!store.isAvailable()) {
            return 0.0;
        }
        Double balance = store.queryFirst("SELECT balance FROM " + TABLE + " WHERE id = 1",
                rs -> rs.getDouble("balance")).orElse(null);
        return balance == null ? 0.0 : balance;
    }

    /**
     * Credits sink revenue to the server account and writes the audit entry
     * that makes it attributable ({@code action} = source, e.g. {@code AH_TAX},
     * {@code AH_LISTING_FEE}, {@code ANVIL_REPAIR}, {@code CLAIM_UPKEEP};
     * {@code actor} = the paying player).
     */
    public void deposit(String actor, String action, double amount) {
        double value = EconomyService.sanitize(amount);
        if (value <= 0.0 || !store.isAvailable()) {
            return;
        }
        store.executeUpdate("UPDATE " + TABLE + " SET balance = balance + ? WHERE id = 1", value);
        EconomyService economy = plugin.getEconomyService();
        if (economy != null) {
            economy.logTransaction(actor, SERVER_UUID, SERVER_NAME, action, value, balance());
        }
    }

    /**
     * Moves {@code amount} server account → {@code admin}'s wallet. The account
     * debit is a guarded UPDATE (refuses overdraw atomically); a wallet credit
     * refusal (MAX_BALANCE headroom) rolls the debit back.
     */
    public boolean withdrawTo(Player admin, double amount) {
        double value = EconomyService.sanitize(amount);
        EconomyService economy = plugin.getEconomyService();
        if (value <= 0.0 || economy == null || !store.isAvailable()) {
            return false;
        }
        int rows = store.executeUpdateRows(
                "UPDATE " + TABLE + " SET balance = balance - ? WHERE id = 1 AND balance >= ?",
                value, value);
        if (rows <= 0) {
            return false;
        }
        if (!economy.depositStrict(admin.getUniqueId(), admin.getName(), value)) {
            store.executeUpdate("UPDATE " + TABLE + " SET balance = balance + ? WHERE id = 1", value);
            return false;
        }
        economy.logTransaction(admin.getName(), SERVER_UUID, SERVER_NAME,
                "SERVER_WITHDRAW", value, balance());
        return true;
    }

    /** One revenue source aggregated from the audit log. */
    public static final class SourceTotal {
        public final String action;
        public final double total;

        SourceTotal(String action, double total) {
            this.action = action;
            this.total = total;
        }
    }

    /**
     * Revenue per source since {@code sinceTs} (largest first), read from the
     * audit rows that {@link #deposit} writes. Withdrawals are excluded.
     */
    public List<SourceTotal> sourceTotals(long sinceTs) {
        if (!store.isAvailable()) {
            return Collections.emptyList();
        }
        return store.queryAll(
                "SELECT action, SUM(amount) AS total FROM economy_log"
                        + " WHERE target_uuid = ? AND ts >= ? AND action != 'SERVER_WITHDRAW'"
                        + " GROUP BY action ORDER BY total DESC",
                rs -> new SourceTotal(rs.getString("action"), rs.getDouble("total")),
                SERVER_UUID, sinceTs);
    }
}