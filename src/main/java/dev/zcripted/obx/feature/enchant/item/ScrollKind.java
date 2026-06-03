package dev.zcripted.obx.feature.enchant.item;

import java.util.Locale;

/**
 * The kinds of Arcanum item produced by the module. Each kind is identified at
 * runtime by an italic dark-gray marker footer line in the item's lore (the
 * PDC-less identification scheme that mirrors {@code EnchantStorage}).
 */
public enum ScrollKind {

    /** Carries one enchantment at a level; consumed on apply. */
    ENCHANT_SCROLL("Scroll"),
    /** Traditional enchanted-book version of an enchantment (non-scroll). */
    BOOK("Book"),
    /** Saves an item from destruction on a failed application. */
    PROTECTION("Protection Scroll"),
    /** Boosts the next application's success rate. */
    SUCCESS("Success Scroll"),
    /** Removes an enchantment from an item, returning it as a scroll. */
    EXTRACTION("Extraction Scroll"),
    /** Re-rolls an enchantment of the same category on an item. */
    TRANSMUTATION("Transmutation Scroll");

    private final String label;

    ScrollKind(String label) {
        this.label = label;
    }

    /** Human-readable label embedded in the marker footer (e.g. {@code "Protection Scroll"}). */
    public String getLabel() {
        return label;
    }

    public static ScrollKind fromLabel(String stripped) {
        if (stripped == null) {
            return null;
        }
        String normalized = stripped.trim();
        for (ScrollKind kind : values()) {
            if (kind.label.equalsIgnoreCase(normalized)) {
                return kind;
            }
        }
        return null;
    }

    public static ScrollKind fromId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.trim().toUpperCase(Locale.ENGLISH);
        for (ScrollKind kind : values()) {
            if (kind.name().equals(normalized)) {
                return kind;
            }
        }
        return null;
    }
}
