package dev.zcripted.obx.core.language;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers {@link LanguageRegistry#fromInput(String)} — the parser behind
 * {@code /language} and {@code /sprache}. It must accept codes, English and
 * German names, and assorted aliases case-insensitively, and reject anything
 * unknown rather than guessing.
 */
class LanguageRegistryTest {

    @Test
    @DisplayName("canonical codes resolve")
    void codesResolve() {
        assertSame(LanguageRegistry.EN, LanguageRegistry.fromInput("en"));
        assertSame(LanguageRegistry.DE, LanguageRegistry.fromInput("de"));
    }

    @Test
    @DisplayName("aliases and display names resolve, case-insensitively")
    void aliasesResolve() {
        assertSame(LanguageRegistry.EN, LanguageRegistry.fromInput("English"));
        assertSame(LanguageRegistry.EN, LanguageRegistry.fromInput("ENGLISCH"));
        assertSame(LanguageRegistry.EN, LanguageRegistry.fromInput("eng"));
        assertSame(LanguageRegistry.DE, LanguageRegistry.fromInput("Deutsch"));
        assertSame(LanguageRegistry.DE, LanguageRegistry.fromInput("german"));
        assertSame(LanguageRegistry.DE, LanguageRegistry.fromInput("GER"));
    }

    @Test
    @DisplayName("surrounding whitespace is tolerated")
    void whitespaceTolerated() {
        assertSame(LanguageRegistry.EN, LanguageRegistry.fromInput("  english  "));
    }

    @Test
    @DisplayName("unknown / null input returns null rather than a default")
    void unknownReturnsNull() {
        assertNull(LanguageRegistry.fromInput("klingon"));
        assertNull(LanguageRegistry.fromInput(""));
        assertNull(LanguageRegistry.fromInput(null));
    }

    @Test
    @DisplayName("each registry carries the expected generated file name")
    void fileNames() {
        assertEquals("language_en.yml", LanguageRegistry.EN.fileName());
        assertEquals("sprache_de.yml", LanguageRegistry.DE.fileName());
    }
}
