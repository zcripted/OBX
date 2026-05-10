package dev.sergeantfuzzy.sfcore.storage;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Centralised access to the join/leave broadcast and welcome MOTD configuration.
 *
 * <p>Two independent toggles live under {@code join-leave.*} and {@code join-motd.*} in
 * {@code config.yml}. Each can be flipped at runtime through the admin command without
 * a full plugin reload.
 *
 * <p>This service caches every config value it exposes so listeners on the player-join
 * hot path read pre-resolved volatile fields instead of re-walking the YAML tree every
 * connection. The cached snapshot is rebuilt on {@link #reload()} and on each toggle.
 */
public final class JoinLeaveService {

    private final Main plugin;

    private volatile boolean joinLeaveEnabled;
    private volatile boolean suppressVanilla;
    private volatile boolean firstJoinMessageEnabled;
    private volatile boolean joinMotdEnabled;
    private volatile boolean firstJoinMotdEnabled;

    private volatile String joinMessage = "";
    private volatile String leaveMessage = "";
    private volatile String firstJoinMessage = "";

    private volatile List<String> joinMotdLines = Collections.emptyList();
    private volatile List<String> firstJoinMotdLines = Collections.emptyList();

    public JoinLeaveService(Main plugin) {
        this.plugin = plugin;
        cacheSnapshot();
    }

    public void reload() {
        plugin.reloadConfig();
        cacheSnapshot();
    }

    private void cacheSnapshot() {
        FileConfiguration config = plugin.getConfig();
        joinLeaveEnabled = config.getBoolean("join-leave.enabled", true);
        suppressVanilla = config.getBoolean("join-leave.suppress-vanilla", true);
        firstJoinMessageEnabled = config.getBoolean("join-leave.first-join.enabled", true);
        joinMessage = orEmpty(config.getString("join-leave.join-message", ""));
        leaveMessage = orEmpty(config.getString("join-leave.leave-message", ""));
        firstJoinMessage = orEmpty(config.getString("join-leave.first-join.message", ""));

        joinMotdEnabled = config.getBoolean("join-motd.enabled", true);
        firstJoinMotdEnabled = config.getBoolean("join-motd.first-join.enabled", true);
        joinMotdLines = freezeList(readLines("join-motd.lines"));
        List<String> firstLines = readLines("join-motd.first-join.lines");
        firstJoinMotdLines = freezeList(firstLines.isEmpty() ? joinMotdLines : firstLines);
    }

    private static List<String> freezeList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(input));
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    // ---------------------------------------------------------------------
    // Join / leave broadcast
    // ---------------------------------------------------------------------

    public boolean isJoinLeaveEnabled() {
        return joinLeaveEnabled;
    }

    public void setJoinLeaveEnabled(boolean enabled) {
        plugin.getConfig().set("join-leave.enabled", enabled);
        plugin.saveConfig();
        joinLeaveEnabled = enabled;
    }

    public boolean suppressVanillaJoinMessage() {
        return suppressVanilla;
    }

    public boolean isFirstJoinMessageEnabled() {
        return firstJoinMessageEnabled;
    }

    public String getJoinMessage() {
        return joinMessage;
    }

    public String getLeaveMessage() {
        return leaveMessage;
    }

    public String getFirstJoinMessage() {
        return firstJoinMessage;
    }

    // ---------------------------------------------------------------------
    // In-game welcome MOTD
    // ---------------------------------------------------------------------

    public boolean isJoinMotdEnabled() {
        return joinMotdEnabled;
    }

    public void setJoinMotdEnabled(boolean enabled) {
        plugin.getConfig().set("join-motd.enabled", enabled);
        plugin.saveConfig();
        joinMotdEnabled = enabled;
    }

    public boolean isFirstJoinMotdEnabled() {
        return firstJoinMotdEnabled;
    }

    public List<String> getJoinMotdLines() {
        return joinMotdLines;
    }

    public List<String> getFirstJoinMotdLines() {
        return firstJoinMotdLines;
    }

    private List<String> readLines(String path) {
        FileConfiguration config = plugin.getConfig();
        if (config.isList(path)) {
            List<String> raw = config.getStringList(path);
            return raw == null ? Collections.<String>emptyList() : new ArrayList<>(raw);
        }
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section != null) {
            List<String> values = new ArrayList<>();
            for (String key : section.getKeys(false)) {
                Object raw = section.get(key);
                if (raw != null) {
                    values.add(String.valueOf(raw));
                }
            }
            return values;
        }
        String single = config.getString(path);
        if (single == null || single.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(java.util.Arrays.asList(single.split("\n", -1)));
    }
}
