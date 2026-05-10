package dev.sergeantfuzzy.sfcore.chat.service;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Loads, persists, and exposes the chat management configuration stored in
 * {@code systems/chat_management.yml}. All accessors return defaults if the
 * config file is missing or malformed so that chat continues to work even
 * when a server owner ships a broken YAML.
 */
public final class ChatService {

    private static final String RESOURCE_PATH = "systems/chat_management.yml";

    private final Main plugin;
    private final File configFile;
    private YamlConfiguration config;

    public ChatService(Main plugin) {
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

    public boolean isConsoleMirror() {
        return config == null || config.getBoolean("console-mirror", true);
    }

    public boolean isConsoleTimestampEnabled() {
        return config == null || config.getBoolean("console-timestamp", true);
    }

    public String getConsoleTimestampFormat() {
        return getString("console-timestamp-format", "'['HH:mm:ss' INFO]: '");
    }

    public boolean allowFormattingInMessages() {
        return config != null && config.getBoolean("allow-formatting-in-messages", false);
    }

    public String getMasterTemplate() {
        return getString("format.template", "{username} {separator} {message}");
    }

    public String getUsernameTemplate() {
        return getString("format.components.username.template", "<#AAAAAA>{player}</#AAAAAA>");
    }

    public String getSeparatorCharacter() {
        return getString("format.components.separator.character", "»");
    }

    public String getSeparatorTemplate() {
        return getString("format.components.separator.template", "<#FFFF55>{character}</#FFFF55>");
    }

    public String getMessageTemplate() {
        return getString("format.components.message.template", "<#FFFFFF>{message}</#FFFFFF>");
    }

    private String getString(String path, String fallback) {
        if (config == null) {
            return fallback;
        }
        String value = config.getString(path);
        return value == null ? fallback : value;
    }
}
