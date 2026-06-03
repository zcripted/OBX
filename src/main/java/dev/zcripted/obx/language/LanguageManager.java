package dev.zcripted.obx.language;

import dev.zcripted.obx.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LanguageManager {

    private final Main plugin;
    private final Map<LanguageRegistry, LanguageFile> languageFiles = new HashMap<>();
    private final Map<UUID, LanguageRegistry> playerLanguages = new HashMap<>();
    private File playerLanguageFile;

    public LanguageManager(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        ensureLanguageFolder();
        loadPlayerLanguages();
        List<String> createdFiles = new ArrayList<>();
        for (LanguageRegistry registry : LanguageRegistry.values()) {
            LanguageFile file = new LanguageFile(plugin, registry);
            languageFiles.put(registry, file);
            Map<String, Object> defaults = MessageDefaults.defaults(registry);
            Map<String, List<String>> comments = MessageDefaults.sectionComments(registry);
            if (file.ensureExists(defaults, comments)) {
                // Freshly written from defaults — it already has every key, so
                // skip the sync pass (and its "Added N missing keys" line, which
                // would otherwise miscount a fresh file). Sync only matters for a
                // pre-existing file that may be missing keys.
                createdFiles.add(file.getFileName());
            } else {
                int added = file.syncDefaults(defaults, comments);
                if (added > 0) {
                    dev.zcripted.obx.util.message.ConsoleLog.info(plugin,
                            "Added " + added + " missing keys to " + registry.fileName());
                }
            }
        }
        // Fold every freshly-generated default into one tidy console line
        // instead of one "Created default language file" line per language.
        dev.zcripted.obx.util.message.ConsoleLog.list(plugin,
                "Generated default language files:", createdFiles);
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Collections.<String, String>emptyMap());
    }

    public void send(CommandSender sender, String key, Map<String, String> replacements) {
        if (sender == null) {
            return;
        }
        // Usage messages get the clickable command + hover treatment so every
        // "/cmd <args>" can be suggested into chat with one click.
        if (isUsageMessage(key)) {
            sendUsage(sender, key, replacements);
            return;
        }
        LanguageRegistry registry = getLanguage(sender);
        boolean console = sender instanceof ConsoleCommandSender;
        for (String line : resolveMessages(registry, key, replacements, console)) {
            sender.sendMessage(line);
        }
    }

    /**
     * A usage message is any {@code *.usage} / {@code *.usage.*} / {@code *.usage-*}
     * key sent standalone to a player. The {@code commands.obx.entry.*} help-entry
     * syntaxes are excluded — they are embedded inside the /obx help GUI (fetched
     * via {@link #get}/{@link #list}), not sent on their own.
     */
    private static boolean isUsageMessage(String key) {
        if (key == null || key.startsWith("commands.obx.entry.")) {
            return false;
        }
        return key.endsWith(".usage") || key.endsWith("-usage")
                || key.contains(".usage.") || key.contains(".usage-");
    }

    /**
     * Renders a usage message with the {@code /command} token turned into a
     * click-to-suggest component carrying the shared {@code core.usage-hint.hover}
     * tooltip. Handles both the boxed {@code usageBox} layout (each line processed
     * independently) and the single-line {@code {prefix}&eUsage: &5/cmd …} form, and
     * degrades to a plain colorized line for console senders (no component
     * transport — {@link dev.zcripted.obx.util.text.ComponentMessenger} falls back).
     */
    private void sendUsage(CommandSender sender, String key, Map<String, String> replacements) {
        LanguageRegistry registry = getLanguage(sender);
        boolean console = sender instanceof ConsoleCommandSender;
        String resolvedKey = console ? selectConsoleKey(registry, key) : key;
        Object raw = fetchValue(registry, resolvedKey, console);
        if (raw == null) {
            sender.sendMessage(colorize("{prefix}" + key));
            return;
        }
        Map<String, String> withPrefix = new LinkedHashMap<>(replacements == null ? Collections.emptyMap() : replacements);
        withPrefix.put("prefix", prefixFor(registry, prefixKeyFor(key)));

        List<String> templates = new ArrayList<>();
        if (raw instanceof List) {
            for (Object entry : (List<?>) raw) {
                templates.add(entry == null ? "" : String.valueOf(entry));
            }
        } else {
            templates.add(String.valueOf(raw));
        }
        for (String template : templates) {
            // Placeholders (incl. {prefix}) applied, but NOT colorized yet — the
            // line is split into segments first, then each segment is colorized.
            sendUsageLine(sender, registry, key, substitutePlaceholders(template, withPrefix));
        }
    }

    /** Matches a leading {@code &}/{@code §} legacy or {@code &#RRGGBB} hex color token. */
    private static final java.util.regex.Pattern STRIP_COLOR =
            java.util.regex.Pattern.compile("(?i)[&§](#[0-9a-f]{6}|[0-9a-fk-or])");

    /**
     * Sends one usage line. If it carries a {@code /command} token, the command
     * (up to the boxed {@code &8›} divider, else end of line) becomes a
     * click-to-suggest segment; the rest stays plain. Lines with no command (the
     * box title bar, rule, blanks) are sent plainly.
     */
    private void sendUsageLine(CommandSender sender, LanguageRegistry registry, String key, String line) {
        int slash = commandSlash(line);
        if (slash < 0) {
            // No command token on this line (title bar, rule, blanks, or a label like
            // "Join/Leave" whose slash isn't a command) — send it plainly.
            sender.sendMessage(colorize(line));
            return;
        }
        int divider = line.indexOf("&8›", slash); // &8›  — boxed "syntax › description" split
        int end = divider >= 0 ? divider : line.length();
        String prefixPart = line.substring(0, slash);
        String commandPart = line.substring(slash, end);
        String suffixPart = line.substring(end);

        String suggest = suggestValue(commandPart);
        List<String> hover = usageHoverLines(registry, suggest.trim(), key);

        List<dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<>();
        if (!prefixPart.isEmpty()) {
            parts.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart.plain(colorize(prefixPart)));
        }
        parts.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart
                .interactive(colorize("&5" + commandPart), hover, suggest, false));
        if (!suffixPart.isEmpty()) {
            parts.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart.plain(colorize(suffixPart)));
        }
        dev.zcripted.obx.util.text.ComponentMessenger.sendJoinedHoverMessages(sender, parts);
    }

    /**
     * Index of the first {@code /} that begins a real command token, or {@code -1}.
     * A command slash is at the start of the line, preceded by a space, or preceded
     * by a {@code &x}/{@code §x} color code — this avoids mistaking the {@code /} in a
     * label like {@code "Join/Leave"} (preceded by a letter) for a command.
     */
    private static int commandSlash(String line) {
        int from = 0;
        while (true) {
            int slash = line.indexOf('/', from);
            if (slash < 0) {
                return -1;
            }
            if (slash == 0) {
                return slash;
            }
            char prev = line.charAt(slash - 1);
            if (prev == ' '
                    || (slash >= 2 && (line.charAt(slash - 2) == '&' || line.charAt(slash - 2) == '§'))) {
                return slash;
            }
            from = slash + 1;
        }
    }

    /**
     * The bare command a usage click suggests: the literal command text up to the
     * first {@code <}/{@code [} argument placeholder, with a trailing space so the
     * player can type arguments immediately (e.g. {@code /give <player> …} →
     * {@code "/give "}).
     */
    private static String suggestValue(String commandPart) {
        String stripped = stripColors(commandPart);
        int cut = stripped.length();
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (c == '<' || c == '[') {
                cut = i;
                break;
            }
        }
        String literal = stripped.substring(0, cut).trim();
        if (literal.isEmpty()) {
            literal = stripped.trim();
        }
        return literal.isEmpty() ? literal : literal + " ";
    }

    /**
     * Resolves + colorizes the shared usage hover (substituting {@code {command}}),
     * then appends any command-specific note registered under {@code <key>.hint}
     * (e.g. {@code item.give.usage.hint} explaining that any amount is accepted).
     */
    private List<String> usageHoverLines(LanguageRegistry registry, String command, String key) {
        Map<String, String> replacements = Collections.singletonMap("command", command);
        List<String> out = new ArrayList<>();
        appendHoverLines(out, fetchValue(registry, "core.usage-hint.hover", false), replacements);
        Object hint = key == null ? null : fetchValue(registry, key + ".hint", false);
        if (hint != null) {
            out.add(applyPlaceholders("&8──────────────────────────", replacements));
            appendHoverLines(out, hint, replacements);
        }
        return out;
    }

    private void appendHoverLines(List<String> out, Object raw, Map<String, String> replacements) {
        if (raw instanceof List) {
            for (Object entry : (List<?>) raw) {
                out.add(applyPlaceholders(entry == null ? "" : String.valueOf(entry), replacements));
            }
        } else if (raw instanceof String) {
            out.add(applyPlaceholders((String) raw, replacements));
        }
    }

    private static String stripColors(String input) {
        return input == null ? "" : STRIP_COLOR.matcher(input).replaceAll("");
    }

    public String get(CommandSender sender, String key) {
        return get(sender, key, Collections.<String, String>emptyMap());
    }

    public String get(CommandSender sender, String key, Map<String, String> replacements) {
        if (sender == null) {
            return "";
        }
        LanguageRegistry registry = getLanguage(sender);
        boolean console = sender instanceof ConsoleCommandSender;
        List<String> lines = resolveMessages(registry, key, replacements, console);
        return lines.isEmpty() ? "" : lines.get(0);
    }

    public List<String> list(CommandSender sender, String key, Map<String, String> replacements) {
        if (sender == null) {
            return Collections.emptyList();
        }
        LanguageRegistry registry = getLanguage(sender);
        boolean console = sender instanceof ConsoleCommandSender;
        return resolveMessages(registry, key, replacements, console);
    }

    public void broadcast(String key, Map<String, String> replacements) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            send(player, key, replacements);
        }
        send(plugin.getServer().getConsoleSender(), key, replacements);
    }

    public String formatConsole(String key, Map<String, String> replacements) {
        List<String> lines = resolveMessages(LanguageRegistry.EN, key, replacements, true);
        return lines.isEmpty() ? "" : lines.get(0);
    }

    public LanguageRegistry getLanguage(CommandSender sender) {
        if (sender instanceof Player) {
            return getLanguage(((Player) sender).getUniqueId());
        }
        return LanguageRegistry.EN;
    }

    public LanguageRegistry getLanguage(UUID uuid) {
        if (uuid == null) {
            return LanguageRegistry.EN;
        }
        return playerLanguages.getOrDefault(uuid, LanguageRegistry.EN);
    }

    public void setLanguage(UUID uuid, LanguageRegistry registry) {
        if (uuid == null || registry == null) {
            return;
        }
        playerLanguages.put(uuid, registry);
        savePlayerLanguages();
    }

    /**
     * Sends a <strong>structured interactive message</strong> registered under
     * {@code base} (see {@code MessageDefaults.addInteractive}): a colorized
     * {@code base.text} line carrying the {@code base.hover} tooltip and the
     * {@code base.click.*} action. Falls back to a plain colorized line when the
     * recipient/platform can't render components. No-op if {@code base.text} is
     * absent.
     */
    public void sendInteractive(CommandSender sender, String base, Map<String, String> replacements) {
        if (sender == null) {
            return;
        }
        dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart part =
                getInteractivePart(sender, base, replacements);
        if (part == null) {
            return;
        }
        dev.zcripted.obx.util.text.ComponentMessenger.sendJoinedHoverMessages(
                sender, java.util.Collections.singletonList(part));
    }

    public void sendInteractive(CommandSender sender, String base) {
        sendInteractive(sender, base, Collections.<String, String>emptyMap());
    }

    /**
     * Builds (but does not send) the interactive part for {@code base}, with
     * placeholders + the resolved {@code {prefix}} applied to the text and hover.
     * Returns {@code null} when {@code base.text} is missing. Used both by
     * {@link #sendInteractive} and by callers that join several interactive
     * segments onto one line (e.g. the /info action row).
     */
    public dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart getInteractivePart(
            CommandSender sender, String base, Map<String, String> replacements) {
        if (sender == null) {
            return null;
        }
        LanguageRegistry registry = getLanguage(sender);
        Object textRaw = fetchValue(registry, base + ".text", false);
        if (!(textRaw instanceof String)) {
            return null;
        }
        Map<String, String> withPrefix = new LinkedHashMap<>(replacements == null ? Collections.emptyMap() : replacements);
        withPrefix.put("prefix", prefixFor(registry, prefixKeyFor(base)));

        String text = applyPlaceholders((String) textRaw, withPrefix);

        List<String> hover = null;
        Object hoverRaw = fetchValue(registry, base + ".hover", false);
        if (hoverRaw instanceof List) {
            hover = new ArrayList<>();
            for (Object entry : (List<?>) hoverRaw) {
                hover.add(applyPlaceholders(entry == null ? "" : String.valueOf(entry), withPrefix));
            }
        } else if (hoverRaw instanceof String) {
            hover = Collections.singletonList(applyPlaceholders((String) hoverRaw, withPrefix));
        }

        Object actionRaw = fetchValue(registry, base + ".click.action", false);
        Object valueRaw = fetchValue(registry, base + ".click.value", false);
        String action = actionRaw instanceof String ? ((String) actionRaw).trim() : null;
        // Click value carries placeholders ({player}, etc.) but no &-colors, so
        // substitute placeholders without colorizing the command/URL payload.
        String value = valueRaw instanceof String ? substitutePlaceholders((String) valueRaw, withPrefix) : null;

        if (action == null || action.isEmpty() || value == null || value.isEmpty()) {
            // Hover-only (or plain) — no click.
            return dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart
                    .interactive(text, hover, null, false);
        }
        switch (action.toLowerCase(java.util.Locale.ENGLISH)) {
            case "suggest_command":
                return dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart
                        .interactive(text, hover, value, false);
            case "copy_to_clipboard":
                return dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart
                        .copy(text, hover, value);
            case "open_url":
                return dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart
                        .openUrl(text, hover, value);
            case "run_command":
            default:
                return dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart
                        .interactive(text, hover, value, true);
        }
    }

    /**
     * Substitutes {@code {placeholder}} tokens without applying &amp;-color
     * translation — for click payloads (commands / URLs) that must stay literal.
     */
    private String substitutePlaceholders(String input, Map<String, String> replacements) {
        if (input == null || input.isEmpty() || input.indexOf('{') < 0
                || replacements == null || replacements.isEmpty()) {
            return input == null ? "" : input;
        }
        String result = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /** Resolves which wordmark prefix a key uses (mirrors resolveMessages). */
    private String prefixKeyFor(String key) {
        if (key == null) {
            return "core.prefix";
        }
        if (key.startsWith("enchant.")) {
            return "enchant.prefix";
        }
        if (key.startsWith("teleport.spawn.")) {
            return "teleport.spawn.prefix";
        }
        if (key.startsWith("commands.obx.reload.") || key.startsWith("commands.obx.debug.")) {
            return "system.prefix";
        }
        if (key.startsWith("inbox.") || key.equals("message.stored")) {
            return "inbox.prefix";
        }
        return "core.prefix";
    }

    public String getPrefix(LanguageRegistry registry) {
        return prefixFor(registry, "core.prefix");
    }

    /**
     * Returns the <strong>raw</strong>, uncolorized template string for {@code key}
     * in {@code registry} (falling back to English, then to {@code ""}).
     *
     * <p>Unlike {@link #get}/{@link #send}, this performs no {@code &}-code or hex
     * translation and injects no {@code {prefix}} — it hands back the template
     * exactly as authored. It exists for messages that carry full Adventure /
     * MiniMessage markup ({@code <gradient>}, {@code <hover>}, {@code <click>}) and
     * are rendered by {@link dev.zcripted.obx.util.message.AdventureMessageUtil}
     * rather than the legacy colorizer (which would mangle those tags). The
     * welcome join/leave broadcasts use this.
     */
    public String rawTemplate(LanguageRegistry registry, String key) {
        Object value = fetchValue(registry, key, false);
        return value instanceof String ? (String) value : "";
    }

    /**
     * Like {@link #rawTemplate} but for list-valued message keys (the multi-line
     * welcome MOTD). Returns the raw lines verbatim, or an empty list when absent.
     * A scalar value is wrapped as a single-element list so authors can store
     * either form.
     */
    public List<String> rawList(LanguageRegistry registry, String key) {
        Object value = fetchValue(registry, key, false);
        if (value instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object entry : (List<?>) value) {
                out.add(entry == null ? "" : String.valueOf(entry));
            }
            return out;
        }
        if (value instanceof String) {
            return Collections.singletonList((String) value);
        }
        return Collections.emptyList();
    }

    /**
     * Resolves a <strong>structured MOTD line list</strong> ({@code welcome.motd-*})
     * into rendered MiniMessage line strings for
     * {@link dev.zcripted.obx.util.message.AdventureMessageUtil#sendLines}.
     *
     * <p>Each list element is either:
     * <ul>
     *   <li>a plain <em>String</em> — passed through unchanged (may itself contain
     *       inline {@code <hover>}/{@code <click>} tags, e.g. the multi-link
     *       "Made by" line); or</li>
     *   <li>a structured <em>map</em> {@code {text, hover, click{action,value}}} —
     *       reassembled here into an inline
     *       {@code <hover:show_text:'…'><click:action:'…'>text</click></hover>}
     *       line, so the stored YAML stays clean/branched while the rendered output
     *       is identical to the hand-written inline form.</li>
     * </ul>
     *
     * <p>Reads via {@link LanguageFile#readRawList} (not the String-coercing
     * {@code readValue}) so the maps survive, falling back to the EN defaults.
     */
    public List<String> resolveMotdLines(LanguageRegistry registry, String key) {
        List<?> raw = rawStructuredList(registry, key);
        if (raw == null) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>(raw.size());
        for (Object element : raw) {
            if (element instanceof Map) {
                lines.add(renderMotdNode((Map<?, ?>) element));
            } else {
                lines.add(element == null ? "" : String.valueOf(element));
            }
        }
        return lines;
    }

    /** Reads the structured list from the lang file (maps intact), else EN defaults. */
    private List<?> rawStructuredList(LanguageRegistry registry, String key) {
        LanguageFile file = languageFiles.get(registry);
        if (file != null) {
            List<?> fromFile = file.readRawList(key);
            if (fromFile != null) {
                return fromFile;
            }
        }
        Map<String, Object> defaults = MessageDefaults.defaults(registry);
        Object def = defaults.get(key);
        if (def instanceof List) {
            return (List<?>) def;
        }
        if (registry != LanguageRegistry.EN) {
            return rawStructuredList(LanguageRegistry.EN, key);
        }
        return null;
    }

    /**
     * Reassembles one {@code {text,hover,click}} MOTD node into a single inline
     * MiniMessage line. The {@code text} is the visible content; {@code hover}
     * lines are joined with {@code \n} into a {@code show_text} tooltip; an optional
     * {@code click{action,value}} wraps it in a {@code <click>}. Quotes in hover/
     * value are not expected (authoring is controlled), but single quotes in the
     * payload are tolerated by AdventureMessageUtil's quote-aware tag parser.
     */
    private String renderMotdNode(Map<?, ?> node) {
        Object textObj = node.get("text");
        String text = textObj == null ? "" : String.valueOf(textObj);

        String hoverJoined = null;
        Object hoverObj = node.get("hover");
        if (hoverObj instanceof List) {
            StringBuilder sb = new StringBuilder();
            List<?> hoverList = (List<?>) hoverObj;
            for (int i = 0; i < hoverList.size(); i++) {
                if (i > 0) {
                    sb.append("\\n");
                }
                Object h = hoverList.get(i);
                sb.append(h == null ? "" : String.valueOf(h));
            }
            hoverJoined = sb.toString();
        } else if (hoverObj instanceof String) {
            hoverJoined = (String) hoverObj;
        }

        String clickAction = null;
        String clickValue = null;
        Object clickObj = node.get("click");
        if (clickObj instanceof Map) {
            Map<?, ?> click = (Map<?, ?>) clickObj;
            Object a = click.get("action");
            Object v = click.get("value");
            clickAction = a == null ? null : String.valueOf(a).trim();
            clickValue = v == null ? null : String.valueOf(v);
        }

        String inner = text;
        boolean hasClick = clickAction != null && !clickAction.isEmpty() && clickValue != null && !clickValue.isEmpty();
        if (hasClick) {
            inner = "<click:" + clickAction.toLowerCase(java.util.Locale.ENGLISH) + ":'" + clickValue + "'>" + inner + "</click>";
        }
        if (hoverJoined != null && !hoverJoined.isEmpty()) {
            inner = "<hover:show_text:'" + hoverJoined + "'>" + inner + "</hover>";
        }
        return inner;
    }

    /**
     * Resolves and colorizes a prefix message key. The Arcanum enchantment module
     * uses its own {@code enchant.prefix} wordmark instead of {@code core.prefix}.
     */
    private String prefixFor(LanguageRegistry registry, String prefixKey) {
        Object value = fetchValue(registry, prefixKey, false);
        if (value instanceof String) {
            return colorize((String) value);
        }
        return "";
    }

    private List<String> resolveMessages(LanguageRegistry registry, String key, Map<String, String> replacements, boolean console) {
        String resolvedKey = console ? selectConsoleKey(registry, key) : key;
        Object raw = fetchValue(registry, resolvedKey, console);
        if (raw == null) {
            return Collections.singletonList(colorize("{prefix}" + key));
        }
        Map<String, String> withPrefix = new LinkedHashMap<>(replacements == null ? Collections.emptyMap() : replacements);
        // Arcanum enchantment messages carry their own ✦ ARCANUM wordmark; spawn
        // messages carry the light-yellow 𝗦𝗣𝗔𝗪𝗡 wordmark.
        String prefixKey = "core.prefix";
        if (key != null && key.startsWith("enchant.")) {
            prefixKey = "enchant.prefix";
        } else if (key != null && key.startsWith("teleport.spawn.")) {
            prefixKey = "teleport.spawn.prefix";
        } else if (key != null && (key.startsWith("commands.obx.reload.") || key.startsWith("commands.obx.debug."))) {
            // /obx reload (+ config / file) and /obx debug carry the SYSTEM wordmark.
            prefixKey = "system.prefix";
        } else if (key != null && (key.startsWith("inbox.") || key.equals("message.stored"))) {
            // Inbox feedback ("saved to inbox", "N messages in your inbox", read/delete/
            // bookmark/clear) carries the ✉ INBOX wordmark.
            prefixKey = "inbox.prefix";
        }
        withPrefix.put("prefix", prefixFor(registry, prefixKey));

        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            java.util.List<String> messages = new java.util.ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    messages.add(applyPlaceholders(String.valueOf(entry), withPrefix));
                }
            }
            return messages;
        }
        String formatted = applyPlaceholders(String.valueOf(raw), withPrefix);
        return Collections.singletonList(formatted);
    }

    private String selectConsoleKey(LanguageRegistry registry, String key) {
        String consoleKey = key + "-console";
        LanguageFile file = languageFiles.get(registry);
        if (file != null && file.getConfig().contains(consoleKey)) {
            return consoleKey;
        }
        Map<String, Object> defaults = MessageDefaults.defaults(registry);
        return defaults.containsKey(consoleKey) ? consoleKey : key;
    }

    private Object fetchValue(LanguageRegistry registry, String key, boolean console) {
        LanguageFile file = languageFiles.get(registry);
        Object value = file != null ? file.readValue(key) : null;
        if (value != null) {
            return value;
        }
        Map<String, Object> defaults = MessageDefaults.defaults(registry);
        if (defaults.containsKey(key)) {
            return defaults.get(key);
        }
        if (console && registry != LanguageRegistry.EN) {
            return fetchValue(LanguageRegistry.EN, key, true);
        }
        return null;
    }

    private String applyPlaceholders(String input, Map<String, String> replacements) {
        if (input == null || input.isEmpty()) {
            return colorize(input);
        }
        String result = input;
        // Skip the placeholder substitution loop entirely when the template has no
        // {...} occurrences. Most short messages (one-liners with just &-codes) hit
        // this path and avoid N pointless full-string replace() walks.
        if (result.indexOf('{') >= 0 && replacements != null && !replacements.isEmpty()) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        if (result.indexOf('\\') >= 0) {
            result = result.replace("\\n", "\n");
        }
        return colorize(result);
    }

    /** Matches {@code &#RRGGBB} hex color tokens for 1.16+ §x translation. */
    private static final java.util.regex.Pattern HEX_COLOR = java.util.regex.Pattern.compile("&#([0-9A-Fa-f]{6})");

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', translateHex(input == null ? "" : input));
    }

    /** Converts {@code &#RRGGBB} into the §x§R§R§G§G§B§B legacy hex sequence (renders on 1.16+). */
    private static String translateHex(String input) {
        if (input.indexOf("&#") < 0) {
            return input;
        }
        java.util.regex.Matcher matcher = HEX_COLOR.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder seq = new StringBuilder("§x");
            for (int i = 0; i < hex.length(); i++) {
                seq.append('§').append(hex.charAt(i));
            }
            matcher.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(seq.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private void ensureLanguageFolder() {
        File folder = new File(plugin.getDataFolder(), "languages");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.playerLanguageFile = new File(plugin.getDataFolder(), "player-languages.yml");
    }

    private void loadPlayerLanguages() {
        playerLanguages.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerLanguageFile);
        for (String key : yaml.getKeys(false)) {
            LanguageRegistry registry = LanguageRegistry.fromInput(yaml.getString(key, "en"));
            if (registry != null) {
                try {
                    UUID uuid = UUID.fromString(key);
                    playerLanguages.put(uuid, registry);
                } catch (IllegalArgumentException ignored) {
                    // skip invalid uuid entries
                }
            }
        }
    }

    private void savePlayerLanguages() {
        // Snapshot on the main thread so the off-thread writer doesn't race with
        // /language toggles, then push the actual file write to the async pool.
        final YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, LanguageRegistry> entry : playerLanguages.entrySet()) {
            yaml.set(entry.getKey().toString(), entry.getValue().code());
        }
        Runnable writer = () -> {
            try {
                yaml.save(playerLanguageFile);
            } catch (IOException exception) {
                plugin.getLogger().warning("Failed to save player language preferences: " + exception.getMessage());
            }
        };
        if (plugin.getSchedulerAdapter() != null) {
            plugin.getSchedulerAdapter().runAsync(writer);
        } else {
            writer.run();
        }
    }
}