package dev.zcripted.obx.core.language;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the MOTD hover/click quoting hardening ({@link LanguageManager#quoteArg(String)}): a
 * literal quote in a hover/click payload (e.g. the apostrophe in "player's") must not terminate the
 * {@code <hover:show_text:'…'>} tag early and spill the rest of the line out of the tooltip.
 */
class MotdHoverQuotingTest {

    /** Mirror of {@code AdventureMessageUtil.TAG} — the Adventure-core fallback's tag tokenizer. */
    private static final Pattern TAG = Pattern.compile(
            "<(/?)([a-zA-Z0-9_#:!\\.\\-]+)((?::(?:[^>'\"]|'[^']*'|\"[^\"]*\")*))?>");

    @Test
    void picksDelimiterNotPresentInContent() {
        assertEquals("'plain text'", LanguageManager.quoteArg("plain text"));
        // Contains an apostrophe, no double quote -> wrap in double quotes.
        assertEquals("\"it's here\"", LanguageManager.quoteArg("it's here"));
        // Contains a double quote, no apostrophe -> wrap in single quotes.
        assertEquals("'say \"hi\"'", LanguageManager.quoteArg("say \"hi\""));
        // Null is safe.
        assertEquals("''", LanguageManager.quoteArg(null));
    }

    @Test
    void apostropheContentNeutralisedWhenBothQuoteTypesPresent() {
        String wrapped = LanguageManager.quoteArg("both ' and \"");
        // Single quotes neutralised to U+2019 so the single-quoted form stays intact.
        assertEquals("'both ’ and \"'", wrapped);
        assertTrue(TAG.matcher("<hover:show_text:" + wrapped + ">").matches());
    }

    @Test
    void hardenedHoverTagIsASingleCompleteTag() {
        String content = "Shown in each player's /language";
        String tag = "<hover:show_text:" + LanguageManager.quoteArg(content) + ">";
        Matcher matcher = TAG.matcher(tag);
        assertTrue(matcher.matches(),
                "hardened hover tag must tokenize as one complete tag, got: " + tag);
    }

    @Test
    void naiveSingleQuotedApostropheWouldBreakTheTag() {
        // Demonstrates the original bug: a raw apostrophe inside single quotes is NOT one tag.
        String content = "Shown in each player's /language";
        String brokenTag = "<hover:show_text:'" + content + "'>";
        assertFalse(TAG.matcher(brokenTag).matches(),
                "raw apostrophe in a single-quoted argument must spill (this is the bug we fixed)");
    }
}
