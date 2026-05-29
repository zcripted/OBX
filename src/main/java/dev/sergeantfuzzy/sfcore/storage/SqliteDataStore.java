package dev.sergeantfuzzy.sfcore.storage;

import dev.sergeantfuzzy.sfcore.Main;

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
 * to acquire the shared {@link Connection} or to dispatch fire-and-forget
 * writes onto the async scheduler. SQLite's serialized mode (the default) is
 * thread-safe for a single connection, so we don't need a pool.
 *
 * <p>If the SQLite JDBC driver is missing, {@link #open()} marks the store
 * unavailable and logs a clear error. Services check {@link #isAvailable()}
 * before reading/writing and degrade silently rather than crashing the plugin.
 */
public final class SqliteDataStore {

    private static final String DEFAULT_DRIVER = "org.sqlite.JDBC";

    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private final Main plugin;
    private final File databaseFile;
    private Connection connection;
    private boolean available;

    public SqliteDataStore(Main plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "sf-core.db");
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
            plugin.getLogger().severe("[SF-Core] SQLite JDBC driver (" + DEFAULT_DRIVER + ") is not on the classpath.");
            plugin.getLogger().severe("[SF-Core] On Paper / Folia 1.16.5+ this is declared via plugin.yml libraries.");
            plugin.getLogger().severe("[SF-Core] On older Spigot/PurPur builds, place sqlite-jdbc-*.jar in the server's libraries/ folder.");
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
            available = true;
            plugin.getLogger().info("[SF-Core] SQLite store opened at " + databaseFile.getName());
        } catch (SQLException exception) {
            plugin.getLogger().severe("[SF-Core] Failed to open SQLite database: " + exception.getMessage());
            available = false;
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                plugin.getLogger().warning("[SF-Core] Error closing SQLite connection: " + exception.getMessage());
            }
        }
        connection = null;
        available = false;
    }

    public void execute(String sql) {
        if (!isAvailable()) return;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException exception) {
            plugin.getLogger().warning("[SF-Core] SQL execute failed: " + exception.getMessage());
        }
    }

    public void executeUpdate(String sql, Object... params) {
        if (!isAvailable()) return;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("[SF-Core] SQL update failed (" + truncate(sql) + "): " + exception.getMessage());
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

    public <T> Optional<T> queryFirst(String sql, RowMapper<T> mapper, Object... params) {
        if (!isAvailable()) return Optional.empty();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(mapper.map(rs));
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[SF-Core] SQL queryFirst failed (" + truncate(sql) + "): " + exception.getMessage());
        }
        return Optional.empty();
    }

    public <T> List<T> queryAll(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();
        if (!isAvailable()) return results;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    T row = mapper.map(rs);
                    if (row != null) results.add(row);
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[SF-Core] SQL queryAll failed (" + truncate(sql) + "): " + exception.getMessage());
        }
        return results;
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
