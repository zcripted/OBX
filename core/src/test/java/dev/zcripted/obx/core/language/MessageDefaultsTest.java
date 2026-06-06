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
 * Guards the project's multilingual contract. CLAUDE.md requires every message to
 * exist in English, German, and Spanish; those {@code lang/*.yml} files are
 * generated from the per-language catalogues ({@link MessageDefaultsEN},
 * {@link MessageDefaultsDE}, {@link MessageDefaultsES}). Verifying that the three
 * stay in lock-step here catches a missing or mis-shaped translation at build time
 * instead of at runtime on a non-English server.
 */
class MessageDefaultsTest {

    private final Map<String, Object> en = MessageDefaults.defaults(LanguageRegistry.EN);
    private final Map<String, Object> de = MessageDefaults.defaults(LanguageRegistry.DE);
    private final Map<String, Object> es = MessageDefaults.defaults(LanguageRegistry.ES);

    @Test
    @DisplayName("EN, DE and ES expose the exact same set of keys")
    void allLanguagesShareKeys() {
        assertSameKeys("EN", en, "DE", de);
        assertSameKeys("EN", en, "ES", es);
        assertEquals(en.size(), de.size(), "EN and DE default maps differ in size");
        assertEquals(en.size(), es.size(), "EN and ES default maps differ in size");
    }

    private static void assertSameKeys(String aName, Map<String, Object> a, String bName, Map<String, Object> b) {
        List<String> missingInB = new ArrayList<>();
        for (String key : a.keySet()) {
            if (!b.containsKey(key)) missingInB.add(key);
        }
        List<String> missingInA = new ArrayList<>();
        for (String key : b.keySet()) {
            if (!a.containsKey(key)) missingInA.add(key);
        }
        assertTrue(missingInB.isEmpty(), "Keys present in " + aName + " but missing in " + bName + ": " + missingInB);
        assertTrue(missingInA.isEmpty(), "Keys present in " + bName + " but missing in " + aName + ": " + missingInA);
    }

    @Test
    @DisplayName("requiredKeys() matches every generated default map size")
    void requiredKeysMatchesMapSize() {
        assertEquals(MessageDefaults.requiredKeys(), en.size());
        assertEquals(MessageDefaults.requiredKeys(), de.size());
        assertEquals(MessageDefaults.requiredKeys(), es.size());
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
    @DisplayName("value shapes match across languages (String vs List vs Map, and list line counts)")
    void valueShapesAreConsistent() {
        List<String> mismatches = new ArrayList<>();
        for (String key : en.keySet()) {
            Object e = en.get(key);
            checkShape(key, "DE", e, de.get(key), mismatches);
            checkShape(key, "ES", e, es.get(key), mismatches);
        }
        assertTrue(mismatches.isEmpty(), "Value-shape mismatches vs English: " + mismatches);
    }

    private static void checkShape(String key, String lang, Object en, Object other, List<String> out) {
        if ((en instanceof String) != (other instanceof String)
                || (en instanceof List) != (other instanceof List)
                || (en instanceof Map) != (other instanceof Map)) {
            out.add(lang + ":" + key + " (type)");
            return;
        }
        if (en instanceof List && ((List<?>) en).size() != ((List<?>) other).size()) {
            out.add(lang + ":" + key + " (list size " + ((List<?>) other).size()
                    + " != " + ((List<?>) en).size() + ")");
        }
    }

    @Test
    @DisplayName("section headers exist in every language with the same keys")
    void sectionHeadersShareKeys() {
        Map<String, List<String>> secEn = MessageDefaults.sectionComments(LanguageRegistry.EN);
        Map<String, List<String>> secDe = MessageDefaults.sectionComments(LanguageRegistry.DE);
        Map<String, List<String>> secEs = MessageDefaults.sectionComments(LanguageRegistry.ES);
        assertTrue(secEn.size() > 0, "Expected section header comments");
        assertEquals(secEn.keySet(), secDe.keySet(), "DE section header keys differ from EN");
        assertEquals(secEn.keySet(), secEs.keySet(), "ES section header keys differ from EN");
    }

    @Test
    @DisplayName("the catalogue is non-trivial (sanity: hundreds of keys)")
    void mapIsPopulated() {
        assertTrue(en.size() > 100, "Expected a substantial message catalogue, got " + en.size());
    }
}
