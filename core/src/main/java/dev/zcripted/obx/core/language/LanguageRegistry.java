package dev.zcripted.obx.core.language;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public enum LanguageRegistry {
    EN("en", "en.yml", "English", Arrays.asList("en", "eng", "english", "englisch")),
    DE("de", "de.yml", "Deutsch", Arrays.asList("de", "ger", "german", "deutsch"));

    private final String code;
    private final String fileName;
    private final String displayName;
    private final List<String> aliases;

    LanguageRegistry(String code, String fileName, String displayName, List<String> aliases) {
        this.code = code;
        this.fileName = fileName;
        this.displayName = displayName;
        this.aliases = aliases == null ? Collections.<String>emptyList() : aliases;
    }

    public String code() {
        return code;
    }

    public String fileName() {
        return fileName;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> aliases() {
        return aliases;
    }

    public static LanguageRegistry fromInput(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim().toLowerCase(Locale.ENGLISH);
        for (LanguageRegistry registry : values()) {
            if (registry.code.equalsIgnoreCase(normalized)) {
                return registry;
            }
            for (String alias : registry.aliases) {
                if (alias.equalsIgnoreCase(normalized)) {
                    return registry;
                }
            }
        }
        return null;
    }
}