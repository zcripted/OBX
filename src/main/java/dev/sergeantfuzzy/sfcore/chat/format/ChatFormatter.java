package dev.sergeantfuzzy.sfcore.chat.format;

import dev.sergeantfuzzy.sfcore.chat.service.ChatService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Composes a single MiniMessage-style chat line from the templates defined in
 * {@link ChatService}. Each component template is resolved first (using only
 * its own placeholders) and the rendered output is then substituted into the
 * master template, so that a server owner can safely re-arrange components
 * without worrying about template recursion.
 */
public final class ChatFormatter {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    private ChatFormatter() {
    }

    /**
     * Builds the final MiniMessage string ready to be sent through
     * {@code AdventureMessageUtil}. The {@code message} placeholder value
     * supplied in {@code basePlaceholders} is treated as plain user text.
     */
    public static String compose(ChatService service, Map<String, String> basePlaceholders) {
        Map<String, String> base = basePlaceholders == null ? java.util.Collections.<String, String>emptyMap() : basePlaceholders;

        String username = applyPlaceholders(service.getUsernameTemplate(), base);

        // Mutate-in-place: the {character} key only matters for the separator template,
        // so we install it, render, then back it out instead of allocating a fresh
        // LinkedHashMap copy of the whole base.
        String previousCharacter = base.containsKey("character") ? base.get("character") : null;
        boolean hadCharacter = base.containsKey("character");
        if (basePlaceholders == null) {
            // base is the empty map sentinel; stand up a tiny scratch map for the separator render.
            Map<String, String> separatorOnly = new LinkedHashMap<>(2);
            separatorOnly.put("character", service.getSeparatorCharacter());
            String separator = applyPlaceholders(service.getSeparatorTemplate(), separatorOnly);
            String message = applyPlaceholders(service.getMessageTemplate(), base);
            Map<String, String> masterPlaceholders = new LinkedHashMap<>(4);
            masterPlaceholders.put("username", username);
            masterPlaceholders.put("separator", separator);
            masterPlaceholders.put("message", message);
            return applyPlaceholders(service.getMasterTemplate(), masterPlaceholders);
        }
        base.put("character", service.getSeparatorCharacter());
        String separator = applyPlaceholders(service.getSeparatorTemplate(), base);
        if (hadCharacter) {
            base.put("character", previousCharacter);
        } else {
            base.remove("character");
        }

        String message = applyPlaceholders(service.getMessageTemplate(), base);

        // Same trick for the master render: install username/separator/message keys,
        // render, then restore. This avoids a 3-key allocation per chat line.
        String prevUsername = base.containsKey("username") ? base.get("username") : null;
        boolean hadUsername = base.containsKey("username");
        String prevSeparator = base.containsKey("separator") ? base.get("separator") : null;
        boolean hadSeparator = base.containsKey("separator");
        String prevMessage = base.containsKey("message") ? base.get("message") : null;
        boolean hadMessage = base.containsKey("message");
        base.put("username", username);
        base.put("separator", separator);
        base.put("message", message);
        try {
            return applyPlaceholders(service.getMasterTemplate(), base);
        } finally {
            if (hadUsername) base.put("username", prevUsername); else base.remove("username");
            if (hadSeparator) base.put("separator", prevSeparator); else base.remove("separator");
            if (hadMessage) base.put("message", prevMessage); else base.remove("message");
        }
    }

    /**
     * Sanitises raw user-typed chat so that, when {@code allowFormatting} is
     * false, the literal characters {@code <} and {@code >} cannot inject
     * MiniMessage tags into the rendered output. The replacements are
     * visually-similar typographic angle quotes so the player still sees
     * something close to what they typed.
     */
    public static String sanitiseMessage(String raw, boolean allowFormatting) {
        if (raw == null) {
            return "";
        }
        if (allowFormatting) {
            return raw;
        }
        return raw.replace('<', '‹').replace('>', '›');
    }

    private static String applyPlaceholders(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        if (template.indexOf('{') < 0 || placeholders == null || placeholders.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer buffer = new StringBuffer(template.length());
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = placeholders.get(key);
            if (replacement == null) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
