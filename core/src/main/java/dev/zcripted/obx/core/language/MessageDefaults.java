package dev.zcripted.obx.core.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the embedded default messages for every supported language and
 * exposes them to {@link LanguageManager}, which writes the generated
 * {@code lang/<code>.yml} files.
 *
 * <p>The actual message catalogues live in one self-contained class per language
 * so a translator can copy and edit a single file without cross-language mixup:
 * <ul>
 *   <li>{@link MessageDefaultsEN} &mdash; English (the base locale)</li>
 *   <li>{@link MessageDefaultsDE} &mdash; German</li>
 *   <li>{@link MessageDefaultsES} &mdash; Spanish</li>
 * </ul>
 * Each contributes the full key set (currently {@value #KEY_COUNT_DOC} keys);
 * {@code MessageDefaultsTest} enforces that the three catalogues stay in lock-step
 * (same keys, same value shapes). Section comments (the per-category YAML header
 * blocks written above each group) are language-specific for all three.
 *
 * <p>Values are {@link String}, {@link List} (hover / MOTD lines), or nested
 * {@link Map} (structured MOTD nodes), built with the {@link #list}, {@link #slist}
 * and {@link #map} helpers. Non-ASCII glyphs may be written as {@code \\uXXXX}
 * escapes so the source is encoding-independent regardless of how it is saved.
 */
public final class MessageDefaults {

    /** Informational only (see class javadoc); the real count is {@code EN.size()}. */
    private static final String KEY_COUNT_DOC = "1130";

    private static final LinkedHashMap<String, Object> EN = new LinkedHashMap<String, Object>();
    private static final LinkedHashMap<String, Object> DE = new LinkedHashMap<String, Object>();
    private static final LinkedHashMap<String, Object> ES = new LinkedHashMap<String, Object>();
    private static final LinkedHashMap<String, List<String>> SEC_EN = new LinkedHashMap<String, List<String>>();
    private static final LinkedHashMap<String, List<String>> SEC_DE = new LinkedHashMap<String, List<String>>();
    private static final LinkedHashMap<String, List<String>> SEC_ES = new LinkedHashMap<String, List<String>>();

    static {
        MessageDefaultsEN.contribute(EN);
        MessageDefaultsDE.contribute(DE);
        MessageDefaultsES.contribute(ES);
        MessageDefaultsEN.sections(SEC_EN);
        MessageDefaultsDE.sections(SEC_DE);
        MessageDefaultsES.sections(SEC_ES);
    }

    private MessageDefaults() {
    }

    /** Full default map for a language (a fresh copy callers may mutate). */
    public static Map<String, Object> defaults(LanguageRegistry registry) {
        if (registry == LanguageRegistry.DE) {
            return new LinkedHashMap<String, Object>(DE);
        }
        if (registry == LanguageRegistry.ES) {
            return new LinkedHashMap<String, Object>(ES);
        }
        return new LinkedHashMap<String, Object>(EN);
    }

    /** Per-category YAML header comments (each language has its own). */
    public static Map<String, List<String>> sectionComments(LanguageRegistry registry) {
        LinkedHashMap<String, List<String>> source = SEC_EN;
        if (registry == LanguageRegistry.DE) {
            source = SEC_DE;
        } else if (registry == LanguageRegistry.ES) {
            source = SEC_ES;
        }
        LinkedHashMap<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> e : source.entrySet()) {
            copy.put(e.getKey(), new ArrayList<String>(e.getValue()));
        }
        return copy;
    }

    public static int requiredKeys() {
        return EN.size();
    }

    // -- value constructors, package-private so the per-language catalogue classes
    //    can static-import them (list / slist / map). --

    static List<Object> list(Object... xs) {
        return new ArrayList<Object>(Arrays.asList(xs));
    }

    static List<String> slist(String... xs) {
        return new ArrayList<String>(Arrays.asList(xs));
    }

    static Map<String, Object> map(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}