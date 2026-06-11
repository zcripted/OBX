package dev.zcripted.obx.core.storage;

import dev.zcripted.obx.core.ObxPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Central SQLite gateway. Each per-player data service (PlaytimeService,
 * EconomyService, NicknameService, …) owns its own tables and uses this class
 * to run statements. A single {@link Connection} is shared, and every statement
 * runs under {@link #lock} so concurrent callers — main-thread reads and the
 * async writers dispatched via {@link #executeUpdateAsync} — never interleave on
 * the one connection (avoids races / {@code SQLITE_BUSY}). Multi-statement
 * atomic work uses {@link #transaction(TxBody)}.
 *
 * <p>If the SQLite JDBC driver is missing, {@link #open()} marks the store
 * unavailable and logs a clear error. Services check {@link #isAvailable()}
 * before reading/writing and degrade silently rather than crashing the plugin.
 */
public final class SqliteDataStore {

    private static final String DEFAULT_DRIVER = "org.sqlite.JDBC";

    /** Bump when a shipped schema change needs a migration; see {@link #schemaVersion()}. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** A unit of work run inside a single transaction on the shared connection. */
    public interface TxBody {
        void run(Connection connection) throws SQLException;
    }

    private final ObxPlugin plugin;
    private final File databaseFile;
    // Serializes all access to the single shared connection. SQLite's serialized
    // mode is internally thread-safe per-statement, but transactions and
    // read-modify-write sequences still need an application-level guard.
    private final Object lock = new Object();
    private Connection connection;
    private boolean available;
    // Set during plugin disable: the server cancels the async pool on shutdown, so once
    // this flips, {@link #executeUpdateAsync} runs inline to guarantee final writes land.
    private volatile boolean shuttingDown;

    public SqliteDataStore(ObxPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "obx.db");
    }

    public boolean isAvailable() {
        return available && connection != null;
    }

    public Connection getConnection() {
        return connection;
    }

    public File getDatabaseFile() {
        return databaseFile;
    }

    public void open() {
        try {
            Class.forName(DEFAULT_DRIVER);
        } catch (ClassNotFoundException missing) {
            plugin.getLogger().severe("SQLite JDBC driver (" + DEFAULT_DRIVER + ") is not on the classpath.");
            plugin.getLogger().severe("On Paper / Folia 1.16.5+ this is declared via plugin.yml libraries.");
            plugin.getLogger().severe("On older Spigot/PurPur builds, place sqlite-jdbc-*.jar in the server's libraries/ folder.");
            available = false;
            return;
        }
        try {
            if (databaseFile.getParentFile() != null && !databaseFile.getParentFile().exists()) {
                databaseFile.getParentFile().mkdirs();
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            connection.setAutoCommit(true);
            try (PreparedStatement pragma = connection.prepareStatement("PRAGMA journal_mode=WAL;")) {
                pragma.execute();
            }
            // busy_timeout makes a contended write WAIT (up to 5s) for the lock instead of failing
            // immediately with SQLITE_BUSY when a checkpoint or another connection holds it; synchronous
            // NORMAL is the recommended, fast, crash-safe setting under WAL.
            try (PreparedStatement pragma = connection.prepareStatement("PRAGMA busy_timeout=5000;")) {
                pragma.execute();
            } catch (SQLException ignored) {
                // older sqlite builds may not honor it — best-effort
            }
            try (PreparedStatement pragma = connection.prepareStatement("PRAGMA synchronous=NORMAL;")) {
                pragma.execute();
            } catch (SQLException ignored) {
                // best-effort
            }
            // Schema-version table: the single source of truth for migrations.
            // Created here so it exists before any service loads.
            try (PreparedStatement st = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS obx_schema_version (id INTEGER PRIMARY KEY, version INTEGER NOT NULL)")) {
                st.execute();
            }
            // Stamp a fresh DB at the current version and forward-migrate an existing one.
            runMigrations();
            available = true;
            // Themed console line (purple [OBX] prefix + light-gray body via ConsoleLog), with
            // "SQLite" and the db file in light purple and "schema vN" in green.
            dev.zcripted.obx.util.message.ConsoleLog.info(plugin, "§dSQLite§7 store opened at §d"
                    + databaseFile.getName() + "§7 (§aschema v" + schemaVersion() + "§7)");
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to open SQLite database: " + exception.getMessage());
            available = false;
        }
    }

    /**
     * Switches the store into shutdown mode: subsequent {@link #executeUpdateAsync} calls
     * run synchronously (inline). Call at the very start of {@code onDisable}, before
     * modules save, so the final per-player writes are not dropped when the server cancels
     * the plugin's async task pool.
     */
    public void beginShutdown() {
        shuttingDown = true;
    }

    public void close() {
        synchronized (lock) {
            if (connection != null) {
                try {
                    // Flush the WAL back into the main db file so nothing is left only in
                    // obx.db-wal after a clean stop.
                    try (PreparedStatement checkpoint = connection.prepareStatement("PRAGMA wal_checkpoint(TRUNCATE);")) {
                        checkpoint.execute();
                    } catch (SQLException ignored) {
                        // Best-effort; close anyway.
                    }
                    connection.close();
                } catch (SQLException exception) {
                    plugin.getLogger().warning("Error closing SQLite connection: " + exception.getMessage());
                }
            }
            connection = null;
            available = false;
        }
    }

    /** Persisted schema version (0 if unreadable). Bump alongside migrations. */
    public int schemaVersion() {
        return queryFirst("SELECT version FROM obx_schema_version WHERE id = 1",
                rs -> rs.getInt("version")).orElse(0);
    }

    public void setSchemaVersion(int version) {
        executeUpdate("UPDATE obx_schema_version SET version = ? WHERE id = 1", version);
    }

    /**
     * Reads the persisted schema version and forward-migrates the database to
     * {@link #CURRENT_SCHEMA_VERSION}. Runs during {@link #open()} on the raw connection,
     * before {@code available} is set and before any service loads, so no other thread can
     * touch the store concurrently.
     *
     * <ul>
     *   <li><b>Fresh DB</b> (no version row): stamped at the current version — each service
     *       creates its tables at the current shape via {@code CREATE TABLE IF NOT EXISTS},
     *       so there is nothing to transform.</li>
     *   <li><b>Existing DB behind</b> the current version: each intermediate step from
     *       {@link #applyMigration(Connection, int)} runs in its own transaction and the
     *       version row is advanced only on a successful commit, so a crash mid-migration
     *       resumes cleanly from the last good version.</li>
     *   <li><b>Newer DB</b> (downgrade): left untouched, with a warning.</li>
     * </ul>
     */
    private void runMigrations() throws SQLException {
        Integer stored = null;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT version FROM obx_schema_version WHERE id = 1");
             ResultSet rs = query.executeQuery()) {
            if (rs.next()) {
                stored = rs.getInt(1);
            }
        }
        if (stored == null) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT OR IGNORE INTO obx_schema_version (id, version) VALUES (1, ?)")) {
                insert.setInt(1, CURRENT_SCHEMA_VERSION);
                insert.execute();
            }
            return;
        }
        if (stored >= CURRENT_SCHEMA_VERSION) {
            if (stored > CURRENT_SCHEMA_VERSION) {
                plugin.getLogger().warning("Database schema v" + stored + " is newer than this build (v"
                        + CURRENT_SCHEMA_VERSION + "). Running without migrating — update the plugin.");
            }
            return;
        }
        plugin.getLogger().info("Migrating OBX database schema v" + stored + " -> v" + CURRENT_SCHEMA_VERSION + "…");
        for (int target = stored + 1; target <= CURRENT_SCHEMA_VERSION; target++) {
            connection.setAutoCommit(false);
            try {
                applyMigration(connection, target);
                try (PreparedStatement up = connection.prepareStatement(
                        "UPDATE obx_schema_version SET version = ? WHERE id = 1")) {
                    up.setInt(1, target);
                    up.executeUpdate();
                }
                connection.commit();
            } catch (SQLException failure) {
                try { connection.rollback(); } catch (SQLException ignored) { /* nothing we can do */ }
                // Halt at the last good version and keep the store available so the plugin still
                // runs (degraded) rather than disabling all persistence on one bad step.
                plugin.getLogger().severe("Schema migration to v" + target + " failed, halting at v"
                        + (target - 1) + ": " + failure.getMessage());
                return;
            } finally {
                connection.setAutoCommit(true);
            }
        }
        plugin.getLogger().info("OBX database schema is now v" + CURRENT_SCHEMA_VERSION + ".");
    }

    /**
     * Applies the schema/data changes that upgrade the database <em>to</em> {@code targetVersion},
     * inside the migration transaction. Add a {@code case N:} here for every {@link #CURRENT_SCHEMA_VERSION}
     * bump. Steps must be safe to re-run (idempotent) since a crash can repeat the last step.
     */
    private void applyMigration(Connection conn, int targetVersion) throws SQLException {
        switch (targetVersion) {
            // Example of a future step (v1 -> v2):
            // case 2:
            //     try (PreparedStatement st = conn.prepareStatement(
            //             "ALTER TABLE economy ADD COLUMN last_seen INTEGER NOT NULL DEFAULT 0")) {
            //         st.execute();
            //     }
            //     break;
            default:
                // No migration registered for this version (e.g. the v1 baseline). The version
                // stamp alone advances; add a case above when a real schema change ships.
                break;
        }
    }

    public void execute(String sql) {
        if (!isAvailable()) return;
        synchronized (lock) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.execute();
            } catch (SQLException exception) {
                plugin.getLogger().warning("SQL execute failed: " + exception.getMessage());
            }
        }
    }

    public void executeUpdate(String sql, Object... params) {
        executeUpdateRows(sql, params);
    }

    /** Like {@link #executeUpdate} but returns the affected row count (0 on failure). */
    public int executeUpdateRows(String sql, Object... params) {
        if (!isAvailable()) return 0;
        synchronized (lock) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindParams(statement, params);
                return statement.executeUpdate();
            } catch (SQLException exception) {
                plugin.getLogger().warning("SQL update failed (" + truncate(sql) + "): " + exception.getMessage());
                return 0;
            }
        }
    }

    public void executeUpdateAsync(String sql, Object... params) {
        if (!isAvailable()) return;
        if (shuttingDown || plugin.getSchedulerAdapter() == null) {
            // During shutdown the async pool is being torn down — run inline so the write
            // actually completes before close().
            executeUpdate(sql, params);
            return;
        }
        plugin.getSchedulerAdapter().runAsync(() -> executeUpdate(sql, params));
    }

    /**
     * Runs {@code body} inside a single transaction on the shared connection,
     * committing on success and rolling back on any thrown {@link SQLException}.
     * Held under {@link #lock} so it is atomic against all other store access.
     *
     * @return {@code true} if the transaction committed.
     */
    public boolean transaction(TxBody body) {
        if (!isAvailable()) return false;
        synchronized (lock) {
            try {
                connection.setAutoCommit(false);
            } catch (SQLException setup) {
                plugin.getLogger().warning("SQL transaction setup failed: " + setup.getMessage());
                return false;
            }
            try {
                body.run(connection);
                connection.commit();
                return true;
            } catch (Throwable failure) {
                // Roll back on ANY throw (not just SQLException): an unchecked exception in
                // the body must NOT reach the finally with work still pending, because
                // setAutoCommit(true) there would COMMIT the partial transaction.
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    plugin.getLogger().warning("SQL rollback failed: " + rollbackError.getMessage());
                }
                plugin.getLogger().warning("SQL transaction rolled back: " + failure.getMessage());
                return false;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // best-effort restore
                }
            }
        }
    }

    public <T> Optional<T> queryFirst(String sql, RowMapper<T> mapper, Object... params) {
        if (!isAvailable()) return Optional.empty();
        synchronized (lock) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindParams(statement, params);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(mapper.map(rs));
                    }
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("SQL queryFirst failed (" + truncate(sql) + "): " + exception.getMessage());
            }
            return Optional.empty();
        }
    }

    public <T> List<T> queryAll(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();
        if (!isAvailable()) return results;
        synchronized (lock) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindParams(statement, params);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        T row = mapper.map(rs);
                        if (row != null) results.add(row);
                    }
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("SQL queryAll failed (" + truncate(sql) + "): " + exception.getMessage());
            }
            return results;
        }
    }

    /** Binds params on a caller-supplied statement (used inside {@link #transaction}). */
    public void bind(PreparedStatement statement, Object... params) throws SQLException {
        bindParams(statement, params);
    }

    private void bindParams(PreparedStatement statement, Object... params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value == null) {
                statement.setObject(i + 1, null);
            } else if (value instanceof java.util.UUID) {
                statement.setString(i + 1, value.toString());
            } else if (value instanceof Boolean) {
                statement.setInt(i + 1, ((Boolean) value) ? 1 : 0);
            } else {
                statement.setObject(i + 1, value);
            }
        }
    }

    private String truncate(String value) {
        if (value == null) return "";
        return value.length() < 80 ? value : value.substring(0, 80) + "...";
    }
}