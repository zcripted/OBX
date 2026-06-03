package dev.zcripted.obx.tablist.service;

import dev.zcripted.obx.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads, persists, and exposes the tablist configuration stored in
 * {@code systems/tablist.yml}. Defaults are returned whenever the YAML is
 * missing or a key is unset so the tablist always renders something
 * sensible.
 */
public final class TablistService {

    private static final String RESOURCE_PATH = "systems/tablist.yml";

    private final Main plugin;
    private final File configFile;
    private YamlConfiguration config;

    public TablistService(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), RESOURCE_PATH);
    }

    public void load() {
        if (!configFile.exists()) {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try {
                plugin.saveResource(RESOURCE_PATH, false);
            } catch (IllegalArgumentException ignored) {
                // Resource not bundled - fall through and load empty defaults.
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        load();
    }

    public boolean isEnabled() {
        return config == null || config.getBoolean("enabled", true);
    }

    public int getRefreshIntervalTicks() {
        if (config == null) {
            return 40;
        }
        return config.getInt("refresh-interval-ticks", 40);
    }

    public List<String> getHeaderLines() {
        return readLines("header");
    }

    public List<String> getFooterLines() {
        return readLines("footer");
    }

    public String getPlayerFormat() {
        if (config == null) {
            return "";
        }
        String value = config.getString("player-format", "");
        return value == null ? "" : value;
    }

    /** Whether OP/staff are grouped to the top of the tablist (own block). */
    public boolean isStaffGroupingEnabled() {
        return config == null || config.getBoolean("staff-grouping.enabled", true);
    }

    /**
     * Tablist entry name used for OP/staff when grouping is on. Default prepends
     * the red/bold staff tag (ѕᴛᴀꜰꜰ┃) to the normal gray name. The characters are
     * U+0455 U+1D1B U+1D00 U+A730 U+A730 U+2503; this file is compiled as UTF-8.
     */
    public String getStaffPlayerFormat() {
        String fallback = "<red><bold>ѕᴛᴀꜰꜰ ┃ </bold></red><red>{player}</red>";
        if (config == null) {
            return fallback;
        }
        String value = config.getString("staff-grouping.player-format", fallback);
        return value == null ? fallback : value;
    }

    private List<String> readLines(String path) {
        if (config == null) {
            return Collections.emptyList();
        }
        if (config.isList(path)) {
            List<String> raw = config.getStringList(path);
            return raw == null ? Collections.<String>emptyList() : new ArrayList<>(raw);
        }
        String single = config.getString(path);
        if (single == null || single.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        Collections.addAll(lines, single.split("\n", -1));
        return lines;
    }
}
