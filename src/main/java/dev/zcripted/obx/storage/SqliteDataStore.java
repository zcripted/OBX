package dev.zcripted.obx.storage;

import dev.zcripted.obx.OBX;

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

    private final OBX plugin;
    private final File databaseFile;
    // Serializes all access to the single shared connection. SQLite's serialized
    // mode is internally thread-safe per-statement, but transactions and
    // read-modify-write sequences still need an application-level guard.
    private final Object lock = new Object();
    private Connection connection;
    private boolean available;

    public SqliteDataStore(OBX plugin) {
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
            // Schema-version table: the single source of truth for future
            // migrations. Created here so it exists before any service loads.
            try (PreparedStatement st = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS obx_schema_version (id INTEGER PRIMARY KEY, version INTEGER NOT NULL)")) {
                st.execute();
            }
            try (PreparedStatement st = connection.prepareStatement(
                    "INSERT OR IGNORE INTO obx_schema_version (id, version) VALUES (1, ?)")) {
                st.setInt(1, CURRENT_SCHEMA_VERSION);
                st.execute();
            }
            available = true;
            plugin.getLogger().info("SQLite store opened at " + databaseFile.getName()
                    + " (schema v" + schemaVersion() + ")");
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to open SQLite database: " + exception.getMessage());
            available = false;
        }
    }

    public void close() {
        synchronized (lock) {
            if (connection != null) {
                try {
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
        if (plugin.getSchedulerAdapter() == null) {
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
            } catch (SQLException failure) {
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
