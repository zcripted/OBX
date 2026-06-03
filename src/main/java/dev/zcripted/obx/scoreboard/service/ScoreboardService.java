package dev.zcripted.obx.scoreboard.service;

import dev.zcripted.obx.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads, persists, and exposes the sidebar scoreboard configuration stored in
 * {@code systems/scoreboard.yml}. Mirrors {@code TablistService}: sensible
 * defaults are returned whenever the YAML is missing or a key is unset, and a
 * {@code /obx reload} re-reads the file so title/lines/interval changes apply
 * live.
 */
public final class ScoreboardService {

    private static final String RESOURCE_PATH = "systems/scoreboard.yml";

    private final Main plugin;
    private final File configFile;
    private YamlConfiguration config;

    public ScoreboardService(Main plugin) {
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
                // Resource not bundled — fall through and load empty defaults.
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        load();
    }

    public boolean isEnabled() {
        return config != null && config.getBoolean("enabled", true);
    }

    public int getRefreshIntervalTicks() {
        if (config == null) {
            return 20;
        }
        return config.getInt("refresh-interval-ticks", 20);
    }

    /** Sidebar title template (defaults to the plugin name via {plugin}). */
    public String getTitle() {
        if (config == null) {
            return "{plugin}";
        }
        String value = config.getString("title", "{plugin}");
        return value == null ? "{plugin}" : value;
    }

    public String getServerIp() {
        if (config == null) {
            return "play.example.net";
        }
        String value = config.getString("server-ip", "play.example.net");
        return value == null ? "play.example.net" : value;
    }

    public String getServerWebsite() {
        if (config == null) {
            return "example.net";
        }
        String value = config.getString("server-website", "example.net");
        return value == null ? "example.net" : value;
    }

    /** The sidebar line templates, top to bottom. */
    public List<String> getLines() {
        if (config == null) {
            return Collections.emptyList();
        }
        if (config.isList("lines")) {
            List<String> raw = config.getStringList("lines");
            return raw == null ? Collections.<String>emptyList() : new ArrayList<>(raw);
        }
        return Collections.emptyList();
    }
}
