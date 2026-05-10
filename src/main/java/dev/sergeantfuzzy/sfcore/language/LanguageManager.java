package dev.sergeantfuzzy.sfcore.language;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LanguageManager {

    private final Main plugin;
    private final Map<LanguageRegistry, LanguageFile> languageFiles = new HashMap<>();
    private final Map<UUID, LanguageRegistry> playerLanguages = new HashMap<>();
    private File playerLanguageFile;

    public LanguageManager(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        ensureLanguageFolder();
        loadPlayerLanguages();
        for (LanguageRegistry registry : LanguageRegistry.values()) {
            LanguageFile file = new LanguageFile(plugin, registry);
            languageFiles.put(registry, file);
            Map<String, Object> defaults = MessageDefaults.defaults(registry);
            Map<String, List<String>> comments = MessageDefaults.sectionComments(registry);
            file.ensureExists(defaults, comments);
            int added = file.syncDefaults(defaults, comments);
            if (added > 0) {
                plugin.getLogger().info("[SF-Core] Added " + added + " missing keys to " + registry.fileName());
            }
        }
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Collections.<String, String>emptyMap());
    }

    public void send(CommandSender sender, String key, Map<String, String> replacements) {
        if (sender == null) {
            return;
        }
        LanguageRegistry registry = getLanguage(sender);
        boolean console = sender instanceof ConsoleCommandSender;
        for (String line : resolveMessages(registry, key, replacements, console)) {
            sender.sendMessage(line);
        }
    }

    public String get(CommandSender sender, String key) {
        return get(sender, key, Collections.<String, String>emptyMap());
    }

    public String get(CommandSender sender, String key, Map<String, String> replacements) {
        if (sender == null) {
            return "";
        }
        LanguageRegistry registry = getLanguage(sender);
        boolean console = sender instanceof ConsoleCommandSender;
        List<String> lines = resolveMessages(registry, key, replacements, console);
        return lines.isEmpty() ? "" : lines.get(0);
    }

    public List<String> list(CommandSender sender, String key, Map<String, String> replacements) {
        if (sender == null) {
            return Collections.emptyList();
        }
        LanguageRegistry registry = getLanguage(sender);
        boolean console = sender instanceof ConsoleCommandSender;
        return resolveMessages(registry, key, replacements, console);
    }

    public void broadcast(String key, Map<String, String> replacements) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            send(player, key, replacements);
        }
        send(plugin.getServer().getConsoleSender(), key, replacements);
    }

    public String formatConsole(String key, Map<String, String> replacements) {
        List<String> lines = resolveMessages(LanguageRegistry.EN, key, replacements, true);
        return lines.isEmpty() ? "" : lines.get(0);
    }

    public LanguageRegistry getLanguage(CommandSender sender) {
        if (sender instanceof Player) {
            return getLanguage(((Player) sender).getUniqueId());
        }
        return LanguageRegistry.EN;
    }

    public LanguageRegistry getLanguage(UUID uuid) {
        if (uuid == null) {
            return LanguageRegistry.EN;
        }
        return playerLanguages.getOrDefault(uuid, LanguageRegistry.EN);
    }

    public void setLanguage(UUID uuid, LanguageRegistry registry) {
        if (uuid == null || registry == null) {
            return;
        }
        playerLanguages.put(uuid, registry);
        savePlayerLanguages();
    }

    public String getPrefix(LanguageRegistry registry) {
        Object value = fetchValue(registry, "core.prefix", false);
        if (value instanceof String) {
            return colorize((String) value);
        }
        return "";
    }

    private List<String> resolveMessages(LanguageRegistry registry, String key, Map<String, String> replacements, boolean console) {
        String resolvedKey = console ? selectConsoleKey(registry, key) : key;
        Object raw = fetchValue(registry, resolvedKey, console);
        if (raw == null) {
            return Collections.singletonList(colorize("{prefix}" + key));
        }
        Map<String, String> withPrefix = new LinkedHashMap<>(replacements == null ? Collections.emptyMap() : replacements);
        withPrefix.put("prefix", getPrefix(registry));

        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            java.util.List<String> messages = new java.util.ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    messages.add(applyPlaceholders(String.valueOf(entry), withPrefix));
                }
            }
            return messages;
        }
        String formatted = applyPlaceholders(String.valueOf(raw), withPrefix);
        return Collections.singletonList(formatted);
    }

    private String selectConsoleKey(LanguageRegistry registry, String key) {
        String consoleKey = key + "-console";
        LanguageFile file = languageFiles.get(registry);
        if (file != null && file.getConfig().contains(consoleKey)) {
            return consoleKey;
        }
        Map<String, Object> defaults = MessageDefaults.defaults(registry);
        return defaults.containsKey(consoleKey) ? consoleKey : key;
    }

    private Object fetchValue(LanguageRegistry registry, String key, boolean console) {
        LanguageFile file = languageFiles.get(registry);
        Object value = file != null ? file.readValue(key) : null;
        if (value != null) {
            return value;
        }
        Map<String, Object> defaults = MessageDefaults.defaults(registry);
        if (defaults.containsKey(key)) {
            return defaults.get(key);
        }
        if (console && registry != LanguageRegistry.EN) {
            return fetchValue(LanguageRegistry.EN, key, true);
        }
        return null;
    }

    private String applyPlaceholders(String input, Map<String, String> replacements) {
        if (input == null || input.isEmpty()) {
            return colorize(input);
        }
        String result = input;
        // Skip the placeholder substitution loop entirely when the template has no
        // {...} occurrences. Most short messages (one-liners with just &-codes) hit
        // this path and avoid N pointless full-string replace() walks.
        if (result.indexOf('{') >= 0 && replacements != null && !replacements.isEmpty()) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        if (result.indexOf('\\') >= 0) {
            result = result.replace("\\n", "\n");
        }
        return colorize(result);
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    private void ensureLanguageFolder() {
        File folder = new File(plugin.getDataFolder(), "languages");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.playerLanguageFile = new File(plugin.getDataFolder(), "player-languages.yml");
    }

    private void loadPlayerLanguages() {
        playerLanguages.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerLanguageFile);
        for (String key : yaml.getKeys(false)) {
            LanguageRegistry registry = LanguageRegistry.fromInput(yaml.getString(key, "en"));
            if (registry != null) {
                try {
                    UUID uuid = UUID.fromString(key);
                    playerLanguages.put(uuid, registry);
                } catch (IllegalArgumentException ignored) {
                    // skip invalid uuid entries
                }
            }
        }
    }

    private void savePlayerLanguages() {
        // Snapshot on the main thread so the off-thread writer doesn't race with
        // /language toggles, then push the actual file write to the async pool.
        final YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, LanguageRegistry> entry : playerLanguages.entrySet()) {
            yaml.set(entry.getKey().toString(), entry.getValue().code());
        }
        Runnable writer = () -> {
            try {
                yaml.save(playerLanguageFile);
            } catch (IOException exception) {
                plugin.getLogger().warning("Failed to save player language preferences: " + exception.getMessage());
            }
        };
        if (plugin.getSchedulerAdapter() != null) {
            plugin.getSchedulerAdapter().runAsync(writer);
        } else {
            writer.run();
        }
    }
}