package dev.sergeantfuzzy.sfcore.hologram;

/**
 * Shared prefix + divider style for every hologram-system chat message.
 *
 * <p>Matches the SF-Core ({@code ▍ 𝗦𝗙-𝗖𝗢𝗥𝗘 ›}) and Arcanum
 * ({@code ✦ 𝗔𝗥𝗖𝗔𝗡𝗨𝗠 ➠}) wordmark convention:
 * {@code <accent-icon> <bold-name> <separator> <content-color>}. Uses an aqua
 * palette ({@code §b} / {@code §3}) and the {@code ⌬} (U+232C, benzene-ring)
 * glyph as the icon — a wireframe-hex shape that reads as "holographic
 * projection". The bold word is the math sans-serif bold variant
 * ({@code 𝗛𝗢𝗟𝗢𝗚𝗥𝗔𝗠}) so it survives unicode-safe transports without
 * relying on chat color bold (which doesn't render in every client).
 */
public final class HoloMessages {

    /** Inline prefix used in command feedback (Arcanum-style: icon + name + ➠). */
    public static final String PREFIX_INLINE = "§3⌬ §b𝗛𝗢𝗟𝗢𝗚𝗥𝗔𝗠 §8➠ §7";

    /** Boxed prefix used in headers and report rows (SF-Core-style: ▍ + name + ›). */
    public static final String PREFIX_BOX = "§3▍ §b𝗛𝗢𝗟𝗢𝗚𝗥𝗔𝗠  §8›  §f";

    /** Divider used between header and body in boxed reports. */
    public static final String DIVIDER = "§8──────────────────────────────";

    private HoloMessages() {
    }

    /** Returns the inline-prefixed message body. */
    public static String inline(String body) {
        return PREFIX_INLINE + (body == null ? "" : body);
    }

    /** Returns the boxed-prefixed header line. */
    public static String header(String body) {
        return PREFIX_BOX + (body == null ? "" : body);
    }
}
