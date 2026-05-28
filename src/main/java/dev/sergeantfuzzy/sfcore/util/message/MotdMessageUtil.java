package dev.sergeantfuzzy.sfcore.util.message;

import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

public final class MotdMessageUtil {

    public static final int DEFAULT_MOTD_CENTER_PX = 154;

    private static final Pattern CENTER_OPEN = Pattern.compile("(?i)<center>");
    private static final Pattern CENTER_CLOSE = Pattern.compile("(?i)</center>");
    private static final Map<Character, Integer> CHARACTER_WIDTHS = createCharacterWidths();

    private MotdMessageUtil() {
    }

    public static String formatMotdLine(String input, Map<String, String> placeholders, int centerPx) {
        if (input == null) {
            return "";
        }
        boolean centered = containsCenterTag(input);
        String resolved = colorize(applyPlaceholders(stripCenterTags(input), placeholders));
        if (!centered) {
            return resolved;
        }
        return centerLegacyText(resolved, centerPx <= 0 ? DEFAULT_MOTD_CENTER_PX : centerPx);
    }

    public static String formatText(String input, Map<String, String> placeholders) {
        if (input == null) {
            return "";
        }
        return colorize(applyPlaceholders(stripCenterTags(input), placeholders));
    }

    private static boolean containsCenterTag(String input) {
        if (input == null) {
            return false;
        }
        return CENTER_OPEN.matcher(input).find() && CENTER_CLOSE.matcher(input).find();
    }

    private static String stripCenterTags(String input) {
        if (input == null) {
            return "";
        }
        return CENTER_CLOSE.matcher(CENTER_OPEN.matcher(input).replaceAll("")).replaceAll("");
    }

    private static String applyPlaceholders(String input, Map<String, String> placeholders) {
        String resolved = input == null ? "" : input;
        Map<String, String> values = placeholders == null ? Collections.<String, String>emptyMap() : placeholders;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            resolved = resolved.replace("{" + entry.getKey() + "}", value);
        }
        return resolved.replace("\\n", "\n");
    }

    private static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        // Gradients, hex colors, and MiniMessage tags can't be expressed by the
        // legacy &-code translator — those go through the shared gradient engine,
        // which expands <gradient:a:b>…</gradient> and &#RRGGBB / <#RRGGBB> into a
        // per-glyph §x hex run (a true smooth gradient on 1.16+). Pure &-code
        // lines (including the &x§R… legacy-hex form) keep using the legacy
        // translator so they render identically on every client, including
        // pre-1.16, with zero behavior change.
        if (needsGradientEngine(input)) {
            return AdventureMessageUtil.renderLegacy(input);
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /**
     * True when {@code input} carries markup the legacy &-code translator can't
     * render: a MiniMessage tag ({@code <gradient>}, {@code <#rrggbb>},
     * {@code <gold>}, {@code <bold>}, …) or the {@code &#RRGGBB} hex shorthand.
     */
    private static boolean needsGradientEngine(String input) {
        return input.indexOf('<') >= 0 || input.contains("&#");
    }

    private static String centerLegacyText(String text, int centerPx) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String content = trimLeadingSpaces(text);
        String formatPrefix = extractLeadingFormatCodes(content);
        String visibleContent = content.substring(formatPrefix.length());

        int messagePxSize = 0;
        boolean previousCode = false;
        boolean bold = false;

        for (int i = 0; i < visibleContent.length(); i++) {
            char current = visibleContent.charAt(i);
            if (previousCode) {
                previousCode = false;
                char code = Character.toLowerCase(current);
                if (code == 'l') {
                    bold = true;
                } else if (isFormattingReset(code)) {
                    bold = false;
                }
                continue;
            }
            if (current == ChatColor.COLOR_CHAR) {
                previousCode = true;
                continue;
            }

            int width = getCharacterWidth(current);
            messagePxSize += bold && current != ' ' ? width + 1 : width;
            messagePxSize++;
        }

        int toCompensate = centerPx - (messagePxSize / 2);
        if (toCompensate <= 0) {
            return content;
        }

        int spaceLength = getCharacterWidth(' ') + 1;
        StringBuilder builder = new StringBuilder();
        int compensated = 0;
        while (compensated < toCompensate) {
            builder.append(' ');
            compensated += spaceLength;
        }
        if (formatPrefix.isEmpty()) {
            formatPrefix = ChatColor.RESET.toString();
        }
        return formatPrefix + builder + visibleContent;
    }

    private static String trimLeadingSpaces(String input) {
        int index = 0;
        while (index < input.length() && input.charAt(index) == ' ') {
            index++;
        }
        return index == 0 ? input : input.substring(index);
    }

    private static String extractLeadingFormatCodes(String input) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        while (index + 1 < input.length() && input.charAt(index) == ChatColor.COLOR_CHAR) {
            builder.append(input.charAt(index)).append(input.charAt(index + 1));
            index += 2;
        }
        return builder.toString();
    }

    private static boolean isFormattingReset(char code) {
        return (code >= '0' && code <= '9')
                || (code >= 'a' && code <= 'f')
                || code == 'r'
                || code == 'x';
    }

    private static int getCharacterWidth(char character) {
        Integer width = CHARACTER_WIDTHS.get(character);
        return width == null ? 5 : width.intValue();
    }

    private static Map<Character, Integer> createCharacterWidths() {
        Map<Character, Integer> widths = new HashMap<>();

        put(widths, 3, ' ', '"', '[', ']', 'I', 't');
        put(widths, 1, '!', '\'', ',', '.', ':', ';', 'i', '|', 'l');
        put(widths, 2, '`');
        put(widths, 4, '(', ')', '<', '>', 'f', 'k', '{', '}');
        put(widths, 5,
                '#', '$', '%', '&', '*', '+', '-', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                '=', '?', '@', '\\', '^', '_',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
                'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                'a', 'b', 'c', 'd', 'e', 'g', 'h', 'j', 'm', 'n', 'o', 'p', 'q', 'r', 's', 'u', 'v', 'w', 'x', 'y', 'z'
        );
        put(widths, 6, '@', '~');

        return widths;
    }

    private static void put(Map<Character, Integer> widths, int value, char... characters) {
        for (char character : characters) {
            widths.put(character, Integer.valueOf(value));
        }
    }
}
