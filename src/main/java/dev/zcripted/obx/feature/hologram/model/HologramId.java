package dev.zcripted.obx.feature.hologram.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Typed wrapper around a hologram's user-supplied name. Acts as the registry
 * key everywhere a hologram is referenced (commands, storage, packet layer),
 * so the rest of the module can never accidentally collide a hologram id with
 * an unrelated raw string.
 *
 * <p>Allowed characters: lowercase letters, digits, underscore, dash. Names
 * are case-folded to lowercase on construction so {@code Welcome} and
 * {@code welcome} resolve to the same hologram — matching the behaviour of
 * OBX's warp ids and home names.
 */
public final class HologramId {

    private static final Pattern VALID = Pattern.compile("[a-z0-9_-]{1,32}");

    private final String value;

    private HologramId(String value) {
        this.value = value;
    }

    /**
     * Parses raw user input. Returns {@code null} when the input is empty or
     * contains illegal characters — callers should surface the
     * {@code hologram.error.invalid_id} language key.
     */
    public static HologramId parse(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ENGLISH);
        if (!VALID.matcher(normalized).matches()) {
            return null;
        }
        return new HologramId(normalized);
    }

    /** Trusted constructor for ids that have already been validated (e.g. loaded from disk). */
    public static HologramId of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("HologramId cannot be null");
        }
        return new HologramId(value.toLowerCase(Locale.ENGLISH));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HologramId && ((HologramId) other).value.equals(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
