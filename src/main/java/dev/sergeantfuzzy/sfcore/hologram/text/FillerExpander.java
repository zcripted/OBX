package dev.sergeantfuzzy.sfcore.hologram.text;

/**
 * Implements the {@code %filler%} token: expands to enough invisible / spacer
 * characters that the rendered line fits a target width, producing the
 * two-column "top list" effect used by leaderboards and welcome boards.
 *
 * <p>Width is approximate because pixel width depends on the font (resource
 * pack, client locale, MC version). The expander uses character count as a
 * coarse but predictable proxy, suitable for monospaced-looking display-entity
 * text and acceptable for armor-stand custom names. Operators tune the
 * target via the {@code line-width} setting on the hologram (passed through
 * to the resolver context, default 200).
 */
public final class FillerExpander {

    private FillerExpander() {
    }

    public static String expand(String line, int targetCharWidth) {
        if (line == null || !line.contains("%filler%")) {
            return line;
        }
        int desired = Math.max(20, targetCharWidth);
        // Visible-character count after stripping section codes — a coarse
        // proxy that's stable across legacy and MiniMessage-translated output.
        String stripped = stripCodes(line.replace("%filler%", ""));
        int currentChars = stripped.length();
        int gap = Math.max(1, desired / 6 - currentChars);
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < gap; i++) {
            pad.append(' ');
        }
        return line.replace("%filler%", pad.toString());
    }

    private static String stripCodes(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < input.length()) {
                i++;
                continue;
            }
            builder.append(c);
        }
        return builder.toString();
    }
}
