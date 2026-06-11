package dev.zcripted.obx.util.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Covers the {@link Placeholders} substitution-map builder used across every
 * localized message. The contract that matters: values are stringified, a null
 * value becomes "" (never the literal "null"), and {@code merge} is
 * non-destructive to the original map.
 */
class PlaceholdersTest {

    @Test
    @DisplayName("with() stringifies the value")
    void withStringifies() {
        Map<String, String> map = Placeholders.with("count", 5);
        assertEquals("5", map.get("count"));
    }

    @Test
    @DisplayName("a null value becomes an empty string, not \"null\"")
    void nullBecomesEmpty() {
        Map<String, String> map = Placeholders.with("player", null);
        assertEquals("", map.get("player"));
    }

    @Test
    @DisplayName("the two-pair overload carries both entries")
    void twoPairOverload() {
        Map<String, String> map = Placeholders.with("a", 1, "b", "two");
        assertEquals("1", map.get("a"));
        assertEquals("two", map.get("b"));
    }

    @Test
    @DisplayName("merge() adds a key without mutating the original map")
    void mergeIsNonDestructive() {
        Map<String, String> original = Placeholders.with("a", "1");
        Map<String, String> merged = Placeholders.merge(original, "b", "2");

        assertEquals("1", merged.get("a"));
        assertEquals("2", merged.get("b"));
        assertFalse(original.containsKey("b"), "merge() must not mutate the original map");
    }

    @Test
    @DisplayName("merge() tolerates a null original")
    void mergeToleratesNullOriginal() {
        Map<String, String> merged = Placeholders.merge(null, "k", "v");
        assertEquals("v", merged.get("k"));
        assertEquals(1, merged.size());
    }
}