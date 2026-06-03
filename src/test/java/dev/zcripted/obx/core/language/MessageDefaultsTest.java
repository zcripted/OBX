package dev.zcripted.obx.core.language;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the project's bilingual contract. CLAUDE.md requires that every message
 * exists in both {@code language_en.yml} and {@code sprache_de.yml}; those files
 * are generated from {@link MessageDefaults}, so verifying parity here catches a
 * missing translation at build time instead of at runtime on a German server.
 */
class MessageDefaultsTest {

    private final Map<String, Object> en = MessageDefaults.defaults(LanguageRegistry.EN);
    private final Map<String, Object> de = MessageDefaults.defaults(LanguageRegistry.DE);

    @Test
    @DisplayName("EN and DE expose the exact same set of keys")
    void enAndDeKeysMatch() {
        List<String> missingInDe = new ArrayList<>();
        for (String key : en.keySet()) {
            if (!de.containsKey(key)) missingInDe.add(key);
        }
        List<String> missingInEn = new ArrayList<>();
        for (String key : de.keySet()) {
            if (!en.containsKey(key)) missingInEn.add(key);
        }
        assertTrue(missingInDe.isEmpty(), "Keys present in EN but missing in DE: " + missingInDe);
        assertTrue(missingInEn.isEmpty(), "Keys present in DE but missing in EN: " + missingInEn);
        assertEquals(en.size(), de.size(), "EN and DE default maps differ in size");
    }

    @Test
    @DisplayName("requiredKeys() matches the generated default map size")
    void requiredKeysMatchesMapSize() {
        assertEquals(MessageDefaults.requiredKeys(), en.size());
        assertEquals(MessageDefaults.requiredKeys(), de.size());
    }

    @Test
    @DisplayName("no default value is null or an empty string")
    void noNullOrEmptyValues() {
        for (LanguageRegistry registry : LanguageRegistry.values()) {
            Map<String, Object> values = MessageDefaults.defaults(registry);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                Object value = entry.getValue();
                assertNotNull(value, "Null default for " + registry.code() + " key: " + entry.getKey());
                if (value instanceof String) {
                    assertFalse(((String) value).isEmpty(),
                            "Empty string default for " + registry.code() + " key: " + entry.getKey());
                }
            }
        }
    }

    @Test
    @DisplayName("the default map is non-trivial (sanity: hundreds of keys)")
    void mapIsPopulated() {
        assertTrue(en.size() > 100, "Expected a substantial message catalogue, got " + en.size());
    }
}
