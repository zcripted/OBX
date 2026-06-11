package dev.zcripted.obx.util.text;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Neutralizes formatting injection in user-typed message bodies (/msg, /me, /broadcast,
 * /staffchat, mail). It strips legacy {@code &}/{@code §} color &amp; format codes (so a
 * player can't spoof colors, obfuscation {@code &k}, or system/staff styling) and replaces
 * MiniMessage angle brackets with look-alike typographic quotes (so a body can't inject
 * {@code <click>}/{@code <hover>} tags onto other players' screens on Adventure-rendered
 * surfaces).
 *
 * <p>Senders holding {@link #COLOR_PERMISSION} keep their {@code &}-color codes (MiniMessage
 * brackets are still neutralized for everyone).</p>
 */
public final class MessageSanitizer {

    /** Permission that lets a sender use {@code &}-color codes in message bodies. */
    public static final String COLOR_PERMISSION = "obx.message.color";

    private MessageSanitizer() {
    }

    /** Sanitizes {@code raw} from {@code sender}, honoring {@link #COLOR_PERMISSION}. */
    public static String sanitize(CommandSender sender, String raw) {
        return sanitize(raw, sender != null && sender.hasPermission(COLOR_PERMISSION));
    }

    public static String sanitize(String raw, boolean allowColor) {
        if (raw == null) {
            return "";
        }
        // Never let user text inject MiniMessage tags on Adventure-rendered surfaces.
        String out = raw.replace('<', '‹').replace('>', '›');
        if (!allowColor) {
            out = stripCodes(out);
        }
        return out;
    }

    /**
     * Sanitizes a player's CHAT message body. Like {@link #sanitize(CommandSender, String)} but
     * supports servers that opt into in-chat MiniMessage formatting:
     * <ul>
     *   <li>Legacy {@code &}/{@code §} codes are kept only for senders with {@link #COLOR_PERMISSION}
     *       (otherwise stripped) — so plain players can't colorize/obfuscate or spoof staff styling.</li>
     *   <li>When {@code allowFormatting} is off (or the sender lacks color permission), every
     *       MiniMessage tag is neutralized (angle brackets become look-alikes).</li>
     *   <li>When {@code allowFormatting} is on AND the sender has color permission, MiniMessage tags
     *       are allowed EXCEPT the interactive ones ({@code <click>}, {@code <hover>}, {@code <insert>}),
     *       which are always stripped so a player can colorize their own text but can never place
     *       clickable run-command bait or hovers on other players' screens.</li>
     * </ul>
     */
    public static String sanitizeChat(CommandSender sender, String raw, boolean allowFormatting) {
        if (raw == null) {
            return "";
        }
        boolean allowColor = sender != null && sender.hasPermission(COLOR_PERMISSION);
        String out = raw;
        if (!allowColor) {
            out = stripCodes(out);
        }
        if (allowFormatting && allowColor) {
            out = stripInteractiveTags(out);
        } else {
            out = out.replace('<', '‹').replace('>', '›');
        }
        return out;
    }

    /** Neutralizes {@code <} / {@code >} so a value can't inject MiniMessage tags when interpolated. */
    public static String neutralizeTags(String raw) {
        return raw == null ? "" : raw.replace('<', '‹').replace('>', '›');
    }

    /**
     * Wraps a MiniMessage tag argument in a quote delimiter that is NOT present in the payload, so a
     * literal quote (e.g. an apostrophe in a Geyser/Bedrock gamertag or a hover/MOTD line) can't
     * terminate the tag early and spill the rest of the line out of the {@code <hover>}/{@code <click>}.
     *
     * <p>Robust for both render paths — the real MiniMessage parser <em>and</em> the Adventure-core
     * fallback's regex tokenizer (which matches {@code '[^']*'} / {@code "[^\"]*"} and does NOT honour
     * {@code \'} escapes) — so we pick a safe delimiter instead of escaping. If the content somehow
     * holds both quote types, single quotes are neutralised to a typographic apostrophe (U+2019).
     */
    public static String quoteArg(String content) {
        if (content == null) {
            return "''";
        }
        if (content.indexOf('\'') < 0) {
            return "'" + content + "'";
        }
        if (content.indexOf('"') < 0) {
            return "\"" + content + "\"";
        }
        return "'" + content.replace('\'', '’') + "'";
    }

    /**
     * Neutralizes interactive MiniMessage tags (click/hover/insert + closers). Rather than try to
     * match the whole tag — whose quoted argument can itself contain {@code <}/{@code >} and defeat a
     * naive {@code [^<>]*} match, leaving the opener intact — we neutralize ONLY the leading {@code <}
     * of any such tag via a quote-immune lookahead. The bracket becomes a look-alike, so MiniMessage
     * can no longer parse it as an interactive tag regardless of its arguments. Non-interactive tags
     * (color/gradient/font) are untouched; none can run a command or attach a hover.
     */
    private static String stripInteractiveTags(String text) {
        // MiniMessage's real insertion tag is <insertion:'…'>, so cover both insert and insertion.
        return text.replaceAll("(?i)<(?=/?(?:click|hover|insert(?:ion)?)\\b)", "‹");
    }

    /** Removes legacy color/format codes (both {@code &} and {@code §}, incl. {@code &#RRGGBB}). */
    private static String stripCodes(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' || c == ChatColor.COLOR_CHAR) {
                // &#RRGGBB hex
                if (c == '&' && i + 7 < text.length() && text.charAt(i + 1) == '#' && isHex(text, i + 2, 6)) {
                    i += 7;
                    continue;
                }
                // &<code> / §<code>
                if (i + 1 < text.length() && isFormatCode(text.charAt(i + 1))) {
                    i += 1;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isFormatCode(char c) {
        return "0123456789abcdefklmnorxABCDEFKLMNORX".indexOf(c) >= 0;
    }

    private static boolean isHex(String s, int start, int len) {
        if (start + len > s.length()) {
            return false;
        }
        for (int i = start; i < start + len; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}