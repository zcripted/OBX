package dev.sergeantfuzzy.sfcore.language;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LanguageFile {

    private final Main plugin;
    private final LanguageRegistry language;
    private final File file;
    private YamlConfiguration config;

    public LanguageFile(Main plugin, LanguageRegistry language) {
        this.plugin = plugin;
        this.language = language;
        this.file = new File(new File(plugin.getDataFolder(), "languages"), language.fileName());
    }

    /**
     * Generates the file from defaults if missing, or self-heals an existing one.
     * Returns {@code true} when a fresh default file was created, so the caller
     * can fold all newly-created files into a single console summary rather than
     * logging one line per file.
     */
    public boolean ensureExists(Map<String, Object> defaults, Map<String, List<String>> sectionComments) {
        if (file.exists()) {
            repairMojibake();
            load();
            return false;
        }
        writeMerged(defaults, sectionComments);
        load();
        return true;
    }

    public String getFileName() {
        return file.getName();
    }

    public void load() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Self-heals an already-generated language file whose German (or other Latin-1
     * supplement) characters were double-encoded — i.e. UTF-8 bytes written, then
     * re-read as CP1252 and re-saved as UTF-8 (so {@code ü} became {@code Ã¼},
     * {@code ä} became {@code Ã¤}, etc.). Earlier builds shipped that mojibake, and
     * because {@code syncDefaults} never overwrites existing keys the broken values
     * would otherwise persist. We rewrite the file (UTF-8, no BOM) only when a known
     * mojibake sequence is actually present, so a clean file is left untouched and a
     * server owner's own edits are preserved (these sequences are never valid text).
     */
    private void repairMojibake() {
        if (!file.exists()) {
            return;
        }
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            String fixed = content
                    .replace("Ã¤", "ä")   // Ã¤ -> ä
                    .replace("Ã¶", "ö")   // Ã¶ -> ö
                    .replace("Ã¼", "ü")   // Ã¼ -> ü
                    .replace("ÃŸ", "ß")   // ÃŸ -> ß
                    .replace("Ã„", "Ä")   // Ã„ -> Ä
                    .replace("Ã–", "Ö")   // Ã– -> Ö
                    .replace("Ãœ", "Ü");  // Ãœ -> Ü
            if (!fixed.equals(content)) {
                Files.write(file.toPath(), fixed.getBytes(StandardCharsets.UTF_8));
                dev.sergeantfuzzy.sfcore.util.message.ConsoleLog.info(plugin,
                        "Repaired mis-encoded characters in " + file.getName() + ".");
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not check/repair encoding of " + file.getName()
                    + ": " + exception.getMessage());
        }
    }

    public int syncDefaults(Map<String, Object> defaults, Map<String, List<String>> sectionComments) {
        if (config == null) {
            load();
        }
        Map<String, Object> merged = new LinkedHashMap<>();
        Set<String> sectionKeys = new LinkedHashSet<>();
        for (String key : defaults.keySet()) {
            int index = key.indexOf('.');
            while (index > 0) {
                sectionKeys.add(key.substring(0, index));
                index = key.indexOf('.', index + 1);
            }
        }
        Set<String> existingLeafKeys = collectLeafKeys();
        int added = 0;
        List<String> sortedDefaults = new ArrayList<>(defaults.keySet());
        Collections.sort(sortedDefaults);
        for (String key : sortedDefaults) {
            Object current = readValue(key);
            if (current == null) {
                current = defaults.get(key);
                added++;
            }
            merged.put(key, current);
        }
        for (String key : existingLeafKeys) {
            if (sectionKeys.contains(key)) {
                continue;
            }
            if (!merged.containsKey(key)) {
                Object value = readValue(key);
                if (value != null) {
                    merged.put(key, value);
                }
            }
        }
        if (added > 0 || merged.size() != existingLeafKeys.size()) {
            writeMerged(merged, sectionComments);
            load();
        }
        return added;
    }

    public Object readValue(String key) {
        if (config == null || !config.contains(key)) {
            return null;
        }
        if (config.isConfigurationSection(key)) {
            return null;
        }
        if (config.isList(key)) {
            return config.getStringList(key);
        }
        return config.getString(key);
    }

    public YamlConfiguration getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    private void writeMerged(Map<String, Object> values, Map<String, List<String>> sectionComments) {
        YamlConfiguration out = new YamlConfiguration();
        List<String> sortedKeys = new ArrayList<>(values.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            out.set(key, values.get(key));
        }
        String raw = out.saveToString();
        String withComments = injectComments(raw, sectionComments);
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            Files.write(file.toPath(), withComments.getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to write language file " + file.getName() + ": " + exception.getMessage());
        }
    }

    private String injectComments(String raw, Map<String, List<String>> sectionComments) {
        String[] lines = raw.split("\\r?\\n");
        StringBuilder builder = new StringBuilder();
        Set<String> printed = new LinkedHashSet<>();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                builder.append(line).append(System.lineSeparator());
                continue;
            }
            if (!line.startsWith(" ")) {
                String key = line.split(":", 2)[0].trim();
                List<String> comments = sectionComments.getOrDefault(key, Collections.<String>emptyList());
                if (!comments.isEmpty() && printed.add(key)) {
                    for (String comment : comments) {
                        builder.append(comment).append(System.lineSeparator());
                    }
                }
            }
            builder.append(line).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private Set<String> collectLeafKeys() {
        if (config == null) {
            return Collections.emptySet();
        }
        Set<String> keys = new LinkedHashSet<>();
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                keys.add(key);
            }
        }
        return keys;
    }
}
