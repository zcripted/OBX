package dev.zcripted.obx.util.message;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import dev.zcripted.obx.core.platform.PlatformInfo;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight MiniMessage-style formatter that supports legacy &amp; codes, hex colors,
 * gradients, hover tooltips, click events, decorations, and shadow colors.
 *
 * <p>When the Adventure API (Paper 1.16+) is available it is used directly so the shadow color
 * decoration renders on supporting clients (1.21.4+). Otherwise, output is rendered through the
 * BungeeCord chat components bundled with Spigot, in which case shadow colors are silently
 * ignored on legacy clients.
 *
 * <p>Reference: <a href="https://docs.advntr.dev/minimessage/format.html">Adventure MiniMessage format</a>
 */
public final class AdventureMessageUtil {

    /** Public reference link surfaced in config files and docs. */
    public static final String FORMATTING_REFERENCE_URL = "https://docs.advntr.dev/minimessage/format.html";

    private static final Pattern LEGACY_HEX = Pattern.compile("(?i)&#([0-9A-F]{6})");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");
    private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z0-9_#:!\\.\\-]+)((?::(?:[^>'\"]|'[^']*'|\"[^\"]*\")*))?>");

    private static final boolean ADVENTURE_AVAILABLE = hasClass("net.kyori.adventure.text.minimessage.MiniMessage")
            && hasClass("net.kyori.adventure.text.Component")
            && hasClass("net.kyori.adventure.audience.Audience");

    /**
     * One-time-cached Adventure handles. All field reads happen on hot paths (every chat
     * message, every tablist refresh) so resolving them once keeps reflection out of the
     * tick loop. Each field is {@code null} on non-Adventure servers; {@link #ADVENTURE_READY}
     * is the single boolean that callers should branch on.
     */
    private static final Class<?> ADVENTURE_COMPONENT_CLASS;
    private static final Object ADVENTURE_MINIMESSAGE_INSTANCE;
    private static final Method ADVENTURE_DESERIALIZE;
    private static final boolean ADVENTURE_READY;

    /**
     * Reflection handles for building Adventure {@link net.kyori.adventure.text.Component}
     * instances directly, bypassing MiniMessage's tag parser. MiniMessage works on most
     * Paper versions, but a handful of builds (and any plugin/proxy that wraps Adventure
     * output) collapse a gradient's per-glyph color tags back into a small number of
     * solid bands by the time the chat packet leaves the server. Building the Component
     * tree ourselves with explicit per-glyph TextColor children removes every parser
     * between us and the wire and gives a true gradient on every Paper version we've
     * been able to test.
     *
     * <p>All handles are {@code null} when the direct path can't be wired up; callers
     * branch on {@link #ADVENTURE_DIRECT_READY}.
     */
    private static final boolean ADVENTURE_DIRECT_READY;
    /**
     * The Adventure {@code Component} class as resolved for the direct-build path. Unlike
     * {@link #ADVENTURE_COMPONENT_CLASS} (which is only set when MiniMessage is present), this is
     * available whenever Adventure <em>core</em> is on the classpath — so component-building callers
     * ({@link #toComponent}) work on Paper builds that ship Adventure core but not MiniMessage.
     */
    private static final Class<?> ADVENTURE_DIRECT_COMPONENT_CLASS;
    private static final Method ADVENTURE_TEXT_OF_STRING;
    private static final Method ADVENTURE_COMPONENT_APPEND;
    private static final Method ADVENTURE_COMPONENT_COLOR;
    private static final Method ADVENTURE_COMPONENT_DECORATE;
    private static final Method ADVENTURE_COMPONENT_DECORATION_BOOL;
    private static final Method ADVENTURE_COMPONENT_HOVER_EVENT;
    private static final Method ADVENTURE_COMPONENT_CLICK_EVENT;
    private static final Method ADVENTURE_TEXTCOLOR_COLOR_INT;
    private static final Class<?> ADVENTURE_TEXTCOLOR_CLASS;
    private static final Class<?> ADVENTURE_TEXTDECORATION_CLASS;
    private static final Object ADVENTURE_DEC_BOLD;
    private static final Object ADVENTURE_DEC_ITALIC;
    private static final Object ADVENTURE_DEC_UNDERLINED;
    private static final Object ADVENTURE_DEC_STRIKETHROUGH;
    private static final Object ADVENTURE_DEC_OBFUSCATED;
    private static final Class<?> ADVENTURE_HOVEREVENT_CLASS;
    private static final Method ADVENTURE_HOVER_SHOW_TEXT;
    private static final Class<?> ADVENTURE_CLICKEVENT_CLASS;
    private static final Method ADVENTURE_CLICK_RUN_COMMAND;
    private static final Method ADVENTURE_CLICK_SUGGEST_COMMAND;
    private static final Method ADVENTURE_CLICK_OPEN_URL;
    private static final Method ADVENTURE_CLICK_COPY;
    private static final Method ADVENTURE_CLICK_CHANGE_PAGE;

    /**
     * Cached send-message methods per concrete Player class. Resolved on first message
     * to a given player class and reused for the lifetime of the JVM.
     */
    private static final ConcurrentHashMap<Class<?>, Method> ADVENTURE_SEND_MESSAGE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> ADVENTURE_TABLIST_SEND = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> SPIGOT_TABLIST_SEND = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> LEGACY_HEADER_SETTERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> LEGACY_FOOTER_SETTERS = new ConcurrentHashMap<>();

    private static final Method MISSING_METHOD = sentinelMethod();

    /** Cached {@code ChatColor.of(String)} reflection used on legacy clients that lack hex support. */
    private static volatile Method CHAT_COLOR_OF;
    private static volatile Method TEXT_COMPONENT_SET_COLOR;
    private static volatile boolean HEX_REFLECTION_RESOLVED;

    static {
        Class<?> componentClass = null;
        Object miniMessageInstance = null;
        Method deserialize = null;
        boolean ready = false;
        if (ADVENTURE_AVAILABLE) {
            try {
                Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
                componentClass = Class.forName("net.kyori.adventure.text.Component");
                miniMessageInstance = miniMessageClass.getMethod("miniMessage").invoke(null);
                deserialize = miniMessageClass.getMethod("deserialize", String.class);
                ready = miniMessageInstance != null && deserialize != null;
            } catch (Throwable ignored) {
                ready = false;
            }
        }
        ADVENTURE_COMPONENT_CLASS = ready ? componentClass : null;
        ADVENTURE_MINIMESSAGE_INSTANCE = ready ? miniMessageInstance : null;
        ADVENTURE_DESERIALIZE = ready ? deserialize : null;
        ADVENTURE_READY = ready;

        // Resolve the direct-build reflection surface. We look up methods by name and
        // assignable-parameter compatibility rather than exact signature: Adventure's
        // hoverEvent for example takes HoverEventSource<?>, not HoverEvent — an exact
        // getMethod(... HoverEvent) lookup would throw and silently disable the entire
        // direct path. Each handle is resolved independently so one missing method
        // doesn't cascade-fail the others.
        Method textOf = null, append = null, color = null, decorate = null, decorationBool = null;
        Method hoverEvt = null, clickEvt = null, tcColor = null;
        Class<?> tcClass = null, tdClass = null, hoverEventClass = null, clickEventClass = null;
        Object decBold = null, decItalic = null, decUnderlined = null, decStrikethrough = null, decObfuscated = null;
        Method hoverShowText = null;
        Method clickRunCommand = null, clickSuggestCommand = null, clickOpenUrl = null, clickCopy = null, clickChangePage = null;
        Class<?> compClass = null;
        try {
            compClass = componentClass != null ? componentClass : Class.forName("net.kyori.adventure.text.Component");
        } catch (Throwable ignored) {
            compClass = null;
        }
        try { tcClass = Class.forName("net.kyori.adventure.text.format.TextColor"); } catch (Throwable ignored) {}
        try { tdClass = Class.forName("net.kyori.adventure.text.format.TextDecoration"); } catch (Throwable ignored) {}
        try { hoverEventClass = Class.forName("net.kyori.adventure.text.event.HoverEvent"); } catch (Throwable ignored) {}
        try { clickEventClass = Class.forName("net.kyori.adventure.text.event.ClickEvent"); } catch (Throwable ignored) {}

        if (compClass != null) {
            textOf = findSingleArgMethod(compClass, "text", String.class);
            append = findSingleArgMethod(compClass, "append", compClass);
            if (tcClass != null) color = findSingleArgMethod(compClass, "color", tcClass);
            if (tdClass != null) decorate = findSingleArgMethod(compClass, "decorate", tdClass);
            if (tdClass != null) {
                // Component.decoration(TextDecoration, boolean) — sets the
                // decoration to TRUE or FALSE explicitly, never NOT_SET.
                // Required for hover content so each span carries an explicit
                // bold/italic/etc. flag and cannot inherit the bold from the
                // visible component the hover is attached to.
                try { decorationBool = compClass.getMethod("decoration", tdClass, boolean.class); } catch (Throwable ignored) {}
            }
            if (hoverEventClass != null) hoverEvt = findSingleArgMethod(compClass, "hoverEvent", hoverEventClass);
            if (clickEventClass != null) clickEvt = findSingleArgMethod(compClass, "clickEvent", clickEventClass);
        }
        if (tcClass != null) {
            try { tcColor = tcClass.getMethod("color", int.class); } catch (Throwable ignored) {}
        }
        if (tdClass != null) {
            try { decBold = tdClass.getField("BOLD").get(null); } catch (Throwable ignored) {}
            try { decItalic = tdClass.getField("ITALIC").get(null); } catch (Throwable ignored) {}
            try { decUnderlined = tdClass.getField("UNDERLINED").get(null); } catch (Throwable ignored) {}
            try { decStrikethrough = tdClass.getField("STRIKETHROUGH").get(null); } catch (Throwable ignored) {}
            try { decObfuscated = tdClass.getField("OBFUSCATED").get(null); } catch (Throwable ignored) {}
        }
        if (hoverEventClass != null && compClass != null) {
            hoverShowText = findSingleArgStaticMethod(hoverEventClass, "showText", compClass);
        }
        if (clickEventClass != null) {
            try { clickRunCommand = clickEventClass.getMethod("runCommand", String.class); } catch (Throwable ignored) {}
            try { clickSuggestCommand = clickEventClass.getMethod("suggestCommand", String.class); } catch (Throwable ignored) {}
            try { clickOpenUrl = clickEventClass.getMethod("openUrl", String.class); } catch (Throwable ignored) {}
            try { clickCopy = clickEventClass.getMethod("copyToClipboard", String.class); } catch (Throwable ignored) {}
            try { clickChangePage = clickEventClass.getMethod("changePage", int.class); } catch (Throwable ignored) {}
            if (clickChangePage == null) {
                try { clickChangePage = clickEventClass.getMethod("changePage", String.class); } catch (Throwable ignored) {}
            }
        }

        // The direct path needs every required handle. Hover/click are optional
        // (a message without them still renders fine) but bold/color/text/append
        // and the decoration field BOLD are non-negotiable for the gradient case.
        boolean directReady = textOf != null && append != null && color != null && decorate != null
                && tcColor != null && decBold != null;
        ADVENTURE_TEXT_OF_STRING = directReady ? textOf : null;
        ADVENTURE_COMPONENT_APPEND = directReady ? append : null;
        ADVENTURE_COMPONENT_COLOR = directReady ? color : null;
        ADVENTURE_COMPONENT_DECORATE = directReady ? decorate : null;
        ADVENTURE_COMPONENT_DECORATION_BOOL = directReady ? decorationBool : null;
        ADVENTURE_COMPONENT_HOVER_EVENT = directReady ? hoverEvt : null;
        ADVENTURE_COMPONENT_CLICK_EVENT = directReady ? clickEvt : null;
        ADVENTURE_TEXTCOLOR_COLOR_INT = directReady ? tcColor : null;
        ADVENTURE_TEXTCOLOR_CLASS = directReady ? tcClass : null;
        ADVENTURE_TEXTDECORATION_CLASS = directReady ? tdClass : null;
        ADVENTURE_DEC_BOLD = directReady ? decBold : null;
        ADVENTURE_DEC_ITALIC = directReady ? decItalic : null;
        ADVENTURE_DEC_UNDERLINED = directReady ? decUnderlined : null;
        ADVENTURE_DEC_STRIKETHROUGH = directReady ? decStrikethrough : null;
        ADVENTURE_DEC_OBFUSCATED = directReady ? decObfuscated : null;
        ADVENTURE_HOVEREVENT_CLASS = directReady ? hoverEventClass : null;
        ADVENTURE_HOVER_SHOW_TEXT = directReady ? hoverShowText : null;
        ADVENTURE_CLICKEVENT_CLASS = directReady ? clickEventClass : null;
        ADVENTURE_CLICK_RUN_COMMAND = directReady ? clickRunCommand : null;
        ADVENTURE_CLICK_SUGGEST_COMMAND = directReady ? clickSuggestCommand : null;
        ADVENTURE_CLICK_OPEN_URL = directReady ? clickOpenUrl : null;
        ADVENTURE_CLICK_COPY = directReady ? clickCopy : null;
        ADVENTURE_CLICK_CHANGE_PAGE = directReady ? clickChangePage : null;
        ADVENTURE_DIRECT_COMPONENT_CLASS = directReady ? compClass : null;
        ADVENTURE_DIRECT_READY = directReady;

    }

    /**
     * Looks up a public method on {@code clazz} (including inherited) by name and a
     * single parameter type that can be assigned from {@code argType}. This handles
     * Adventure cases where the actual signature uses a supertype (e.g.
     * {@code hoverEvent(HoverEventSource<?>)} when we want to pass a concrete
     * {@code HoverEvent}).
     */
    private static Method findSingleArgMethod(Class<?> clazz, String name, Class<?> argType) {
        Method bestMatch = null;
        int bestSpecificity = Integer.MAX_VALUE;
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (!param.isAssignableFrom(argType)) {
                continue;
            }
            // Prefer the most specific match (parameter type closest to argType) so
            // that if both a Component- and a ComponentLike-typed overload exist we
            // pick the Component one. Specificity is approximated as the parent-chain
            // distance from argType up to the parameter type.
            int distance = 0;
            Class<?> walk = argType;
            while (walk != null && walk != param) {
                walk = walk.getSuperclass();
                distance++;
            }
            if (walk != param) {
                // No direct subclass walk hit the parameter type — it's an interface
                // match; still valid, but rank it after concrete-class matches.
                distance += 1000;
            }
            if (distance < bestSpecificity) {
                bestSpecificity = distance;
                bestMatch = method;
            }
        }
        return bestMatch;
    }

    /**
     * Same as {@link #findSingleArgMethod} but only matches static methods. Used for
     * the {@code HoverEvent.showText(...)} factory.
     */
    private static Method findSingleArgStaticMethod(Class<?> clazz, String name, Class<?> argType) {
        Method match = findSingleArgMethod(clazz, name, argType);
        if (match != null && java.lang.reflect.Modifier.isStatic(match.getModifiers())) {
            return match;
        }
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) continue;
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
            Class<?> param = method.getParameterTypes()[0];
            if (param.isAssignableFrom(argType)) {
                return method;
            }
        }
        return null;
    }

    private AdventureMessageUtil() {
    }

    public static void send(Player player, String raw, Map<String, String> placeholders) {
        if (player == null || raw == null) {
            return;
        }
        String resolved = applyPlaceholders(raw, placeholders);
        // The direct-build path only needs Adventure *core* (Component/TextColor/
        // TextDecoration), which Paper ships from 1.16 — it does NOT need MiniMessage.
        // Gate on ADVENTURE_DIRECT_READY too so a Paper build without MiniMessage
        // (1.16–1.17) still gets the true per-glyph gradient instead of dropping to
        // the legacy fallback.
        if ((ADVENTURE_DIRECT_READY || ADVENTURE_AVAILABLE) && trySendAdventure(player, resolved)) {
            return;
        }
        boolean containsGradient = resolved.indexOf('<') >= 0 && indexOfIgnoreCase(resolved, "<gradient", 0) >= 0;
        // BungeeCord BaseComponent[] preserves hover/click events. Legacy
        // section-coded text with embedded §x sequences would render the hex
        // colors fine but it CANNOT carry hoverEvent / clickEvent — they're
        // packet-level Component fields the legacy serializer has no syntax
        // for. So when the input carries hover or click markup, we skip the
        // legacy-hex shortcut and go straight to BungeeCord, which builds a
        // BaseComponent per span with hex color + hover + click intact.
        boolean containsInteractive = hasInteractiveMarkup(resolved);
        if (!containsInteractive && trySendLegacyHexText(player, resolved, containsGradient)) {
            return;
        }
        BaseComponent[] components = renderBungee(resolved);
        if (components.length == 0) {
            return;
        }
        try {
            if (containsGradient || containsInteractive) {
                logFirstGradientPath("bungee/BaseComponent[] (length=" + components.length
                        + (containsInteractive ? ", interactive=true" : "") + ")");
            }
            player.spigot().sendMessage(components);
        } catch (Throwable ignored) {
            player.sendMessage(stripFormatting(resolved));
        }
    }

    /**
     * Returns {@code true} if {@code resolved} contains a {@code <hover:...>} or
     * {@code <click:...>} tag. The legacy-hex-text fallback strips these — when
     * a message has them, we must use a Component-based transport (Adventure or
     * BungeeCord) so the hover/click fields make it onto the wire.
     */
    private static boolean hasInteractiveMarkup(String resolved) {
        if (resolved == null || resolved.indexOf('<') < 0) {
            return false;
        }
        return indexOfIgnoreCase(resolved, "<hover", 0) >= 0
                || indexOfIgnoreCase(resolved, "<click", 0) >= 0;
    }

    /**
     * Renders {@code resolved} into a legacy section-coded string (with embedded
     * {@code §x§A§B§C§D§E§F} hex sequences for every per-glyph color) and ships it
     * via {@link Player#sendMessage(String)}. Paper / Spigot deserialize the string
     * into a Component using the legacy serializer, so the wire format ends up
     * carrying explicit per-glyph hex colors — same as the direct-build path,
     * different transport.
     *
     * @param player           recipient
     * @param resolved         placeholder-resolved input
     * @param containsGradient true iff the input had a {@code <gradient>} block;
     *                         used only to decide whether to emit the diagnostic log
     */
    private static boolean trySendLegacyHexText(Player player, String resolved, boolean containsGradient) {
        try {
            String legacy = renderToLegacyHexText(resolved);
            if (legacy.isEmpty()) {
                return false;
            }
            player.sendMessage(legacy);
            if (containsGradient) {
                logFirstGradientPath("legacy-hex/sent (length=" + legacy.length() + ")");
            }
            return true;
        } catch (Throwable t) {
            if (containsGradient) {
                logFirstGradientPath("legacy-hex/threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
            return false;
        }
    }

    /**
     * Walks the parsed spans and emits a legacy section-coded string. Each span's
     * color (if hex) is encoded as {@code §x§A§B§C§D§E§F}; decorations as their
     * legacy {@code §l/§o/§n/§m/§k} codes. Hover/click events are dropped — they
     * cannot be represented in a legacy text string. Suitable for the chat MOTD
     * but NOT for messages that depend on hover/click interactivity.
     */
    private static String renderToLegacyHexText(String resolved) {
        List<Span> spans = parseToSpans(expandGradients(resolved));
        StringBuilder sb = new StringBuilder();
        for (Span span : spans) {
            if (span == null || span.text == null || span.text.isEmpty()) {
                continue;
            }
            if (span.color != null && span.color.length() == 6) {
                sb.append(ChatColor.COLOR_CHAR).append('x');
                for (int i = 0; i < 6; i++) {
                    sb.append(ChatColor.COLOR_CHAR).append(Character.toLowerCase(span.color.charAt(i)));
                }
            } else {
                // Reset to default — keeps the previous span's color from leaking
                // into spans that intentionally have no color set.
                sb.append(ChatColor.COLOR_CHAR).append('r');
            }
            // Decorations follow color (color codes reset decorations on every client).
            if (span.bold) sb.append(ChatColor.COLOR_CHAR).append('l');
            if (span.italic) sb.append(ChatColor.COLOR_CHAR).append('o');
            if (span.underlined) sb.append(ChatColor.COLOR_CHAR).append('n');
            if (span.strikethrough) sb.append(ChatColor.COLOR_CHAR).append('m');
            if (span.obfuscated) sb.append(ChatColor.COLOR_CHAR).append('k');
            sb.append(span.text);
        }
        return sb.toString();
    }

    /**
     * Renders MiniMessage / legacy / gradient markup into a legacy section-coded
     * string for APIs that only accept a {@code String} (the server-list MOTD,
     * legacy tablist headers, player-sample names). This is the bridge that lets
     * those String-only surfaces show the same gradients and hex colors as the
     * Component-based chat path.
     *
     * <p>Output rules:
     * <ul>
     *   <li>The 16 standard Minecraft colors are emitted as their plain
     *       {@code §0}–{@code §f} code so a non-gradient line renders identically
     *       on every client (including pre-1.16).</li>
     *   <li>Gradient glyphs and custom hex colors are emitted as
     *       {@code §x§R§R§G§G§B§B} — a true per-glyph RGB run that renders as a
     *       smooth gradient on 1.16+ clients.</li>
     *   <li>Decorations follow the color as {@code §l/§o/§n/§m/§k}.</li>
     *   <li>Hover/click are dropped — a plain String can't carry them.</li>
     * </ul>
     *
     * <p>Input may use {@code &}-codes, {@code &#RRGGBB} / {@code <#RRGGBB>} hex,
     * named tags ({@code <dark_purple>}, {@code <bold>}), and {@code <gradient:a:b>…</gradient>}
     * blocks; gradients are expanded to explicit per-glyph colors first so the
     * result never depends on a downstream MiniMessage renderer.
     */
    /**
     * Renders {@code raw} (legacy {@code &}/hex, named tags, {@code <gradient>}, {@code <bold>}) into
     * an Adventure {@code Component} object — used where the legacy section-coded string would hit a
     * length cap (e.g. a scoreboard objective display name). Returns null if Adventure is unavailable.
     */
    public static Object toComponent(String raw) {
        // The direct-build path (not MiniMessage) is what actually assembles the Component, so gate
        // on its readiness — this lets scoreboard titles render a true gradient on Paper builds that
        // have Adventure core but not MiniMessage, where ADVENTURE_COMPONENT_CLASS would be null.
        if (!ADVENTURE_DIRECT_READY || raw == null) {
            return null;
        }
        try {
            List<Span> spans = parseToSpans(expandGradients(raw));
            if (spans.isEmpty()) {
                spans.add(emptyTextSpan());
            }
            return buildComponentFromSpans(spans);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** The Adventure {@code Component} class (or null if Adventure isn't on the classpath). */
    public static Class<?> adventureComponentClass() {
        return ADVENTURE_DIRECT_COMPONENT_CLASS != null ? ADVENTURE_DIRECT_COMPONENT_CLASS : ADVENTURE_COMPONENT_CLASS;
    }

    public static String renderLegacy(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw == null ? "" : raw;
        }
        List<Span> spans = parseToSpans(expandGradients(raw));
        if (spans.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        boolean colorEmitted = false;
        for (Span span : spans) {
            if (span == null || span.text == null || span.text.isEmpty()) {
                continue;
            }
            if (span.color != null && span.color.length() == 6) {
                String code = hexToStandardCode(span.color);
                if (code != null) {
                    sb.append(ChatColor.COLOR_CHAR).append(code);
                } else {
                    sb.append(ChatColor.COLOR_CHAR).append('x');
                    for (int i = 0; i < 6; i++) {
                        sb.append(ChatColor.COLOR_CHAR).append(Character.toLowerCase(span.color.charAt(i)));
                    }
                }
                colorEmitted = true;
            } else if (colorEmitted) {
                // No color on this span but an earlier one set one — reset so the
                // previous color doesn't bleed into this (uncolored) text.
                sb.append(ChatColor.COLOR_CHAR).append('r');
            }
            if (span.bold) sb.append(ChatColor.COLOR_CHAR).append('l');
            if (span.italic) sb.append(ChatColor.COLOR_CHAR).append('o');
            if (span.underlined) sb.append(ChatColor.COLOR_CHAR).append('n');
            if (span.strikethrough) sb.append(ChatColor.COLOR_CHAR).append('m');
            if (span.obfuscated) sb.append(ChatColor.COLOR_CHAR).append('k');
            sb.append(span.text);
        }
        return sb.toString();
    }

    /**
     * Like {@link #renderLegacy} but never emits {@code §x} hex runs — every color is mapped to the
     * nearest of the 16 standard Minecraft colors (a single {@code §}-code). Use this on pre-1.16
     * clients (which can't render {@code §x} hex) so a gradient still shows as a coarse, multi-band
     * approximation in standard colors instead of garbled text. The result is also far shorter, so it
     * fits the 1.8–1.12 32-char scoreboard-title cap.
     */
    public static String renderLegacyDownsampled(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw == null ? "" : raw;
        }
        List<Span> spans = parseToSpans(expandGradients(raw));
        if (spans.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        boolean colorEmitted = false;
        for (Span span : spans) {
            if (span == null || span.text == null || span.text.isEmpty()) {
                continue;
            }
            if (span.color != null && span.color.length() == 6) {
                String code = hexToStandardCode(span.color);
                char ch = code != null ? code.charAt(0) : closestLegacyColor(span.color).getChar();
                sb.append(ChatColor.COLOR_CHAR).append(ch);
                colorEmitted = true;
            } else if (colorEmitted) {
                sb.append(ChatColor.COLOR_CHAR).append('r');
            }
            if (span.bold) sb.append(ChatColor.COLOR_CHAR).append('l');
            if (span.italic) sb.append(ChatColor.COLOR_CHAR).append('o');
            if (span.underlined) sb.append(ChatColor.COLOR_CHAR).append('n');
            if (span.strikethrough) sb.append(ChatColor.COLOR_CHAR).append('m');
            if (span.obfuscated) sb.append(ChatColor.COLOR_CHAR).append('k');
            sb.append(span.text);
        }
        return sb.toString();
    }

    /**
     * Renders {@code raw} to a legacy section-coded string suited to the recipient's client: a
     * smooth {@code §x} per-glyph hex gradient on 1.16+ (where hex renders), or a coarse
     * nearest-standard-color approximation on older clients. Used by String-only surfaces (the
     * legacy tablist header/footer, scoreboard objective titles) so gradients show there too.
     */
    public static String renderLegacyForClient(String raw) {
        return hexCapable() ? renderLegacy(raw) : renderLegacyDownsampled(raw);
    }

    /** Whether the running server (a proxy for its clients) supports {@code §x} hex colors (1.16+). */
    public static boolean hexCapable() {
        Boolean cached = HEX_CAPABLE;
        if (cached != null) {
            return cached;
        }
        boolean capable;
        try {
            PlatformInfo info = PlatformInfo.get();
            capable = info == null || info.isAtLeast(1, 16);
        } catch (Throwable ignored) {
            capable = true; // assume a modern server when the probe isn't ready
        }
        HEX_CAPABLE = capable;
        return capable;
    }

    private static volatile Boolean HEX_CAPABLE;

    /**
     * Maps a 6-digit hex string to the matching legacy {@code §}-code character
     * when it is exactly one of the 16 standard Minecraft colors, else returns
     * {@code null} (caller should emit a {@code §x} hex run instead).
     */
    private static String hexToStandardCode(String hex) {
        if (hex == null || hex.length() != 6) {
            return null;
        }
        switch (hex.toUpperCase(Locale.ROOT)) {
            case "000000": return "0";
            case "0000AA": return "1";
            case "00AA00": return "2";
            case "00AAAA": return "3";
            case "AA0000": return "4";
            case "AA00AA": return "5";
            case "FFAA00": return "6";
            case "AAAAAA": return "7";
            case "555555": return "8";
            case "5555FF": return "9";
            case "55FF55": return "a";
            case "55FFFF": return "b";
            case "FF5555": return "c";
            case "FF55FF": return "d";
            case "FFFF55": return "e";
            case "FFFFFF": return "f";
            default: return null;
        }
    }

    public static void sendLines(Player player, List<String> lines, Map<String, String> placeholders) {
        if (player == null || lines == null) {
            return;
        }
        for (String line : lines) {
            send(player, line == null ? "" : line, placeholders);
        }
    }

    public static void broadcast(Server server, String raw, Map<String, String> placeholders, boolean toConsole) {
        if (server == null || raw == null) {
            return;
        }
        String resolved = applyPlaceholders(raw, placeholders);
        for (Player online : server.getOnlinePlayers()) {
            if ((ADVENTURE_DIRECT_READY || ADVENTURE_AVAILABLE) && trySendAdventure(online, resolved)) {
                continue;
            }
            BaseComponent[] components = renderBungee(resolved);
            try {
                online.spigot().sendMessage(components);
            } catch (Throwable ignored) {
                online.sendMessage(stripFormatting(resolved));
            }
        }
        if (toConsole) {
            ConsoleCommandSender console = server.getConsoleSender();
            if (console != null) {
                console.sendMessage(stripFormatting(resolved));
            }
        }
    }

    public static String preview(String raw, Map<String, String> placeholders) {
        if (raw == null) {
            return "";
        }
        return stripFormatting(applyPlaceholders(raw, placeholders));
    }

    /**
     * Applies a MiniMessage-styled header and footer to the supplied player's tablist.
     * Adventure's {@code sendPlayerListHeaderAndFooter} is preferred when available
     * (Paper 1.16+); otherwise falls back to the Spigot
     * {@code setPlayerListHeaderFooter(BaseComponent[], BaseComponent[])} method, and
     * finally to the legacy {@code setPlayerListHeader / setPlayerListFooter(String)}
     * pair on 1.13+.
     */
    public static void applyTablist(Player player, String headerRaw, String footerRaw, Map<String, String> placeholders) {
        if (player == null) {
            return;
        }
        String header = applyPlaceholders(headerRaw == null ? "" : headerRaw, placeholders);
        String footer = applyPlaceholders(footerRaw == null ? "" : footerRaw, placeholders);
        if (ADVENTURE_AVAILABLE && tryAdventureTablist(player, header, footer)) {
            return;
        }
        if (trySpigotTablist(player, header, footer)) {
            return;
        }
        try {
            Class<?> playerClass = player.getClass();
            Method setHeader = LEGACY_HEADER_SETTERS.computeIfAbsent(playerClass, AdventureMessageUtil::lookupLegacyHeaderSetter);
            Method setFooter = LEGACY_FOOTER_SETTERS.computeIfAbsent(playerClass, AdventureMessageUtil::lookupLegacyFooterSetter);
            if (setHeader != MISSING_METHOD && setFooter != MISSING_METHOD) {
                // Render gradients/hex to a legacy section-coded string (§x per glyph on 1.16+,
                // nearest-standard on older clients) rather than STRIPPING the <gradient>/<bold>
                // tags — so the bold purple "OBX" gradient still shows on this String-only fallback.
                setHeader.invoke(player, renderLegacyForClient(header));
                setFooter.invoke(player, renderLegacyForClient(footer));
            }
        } catch (Throwable ignored) {
            // No tablist API available - silently no-op.
        }
    }

    /**
     * Sets the tablist entry name shown for the supplied player. Empty input is a no-op
     * so callers can route the {@code player-format} config straight through without
     * pre-checking.
     */
    public static void applyTablistName(Player player, String raw, Map<String, String> placeholders) {
        if (player == null || raw == null || raw.isEmpty()) {
            return;
        }
        String resolved = applyPlaceholders(raw, placeholders);
        BaseComponent[] components = renderBungee(resolved);
        StringBuilder legacy = new StringBuilder();
        for (BaseComponent component : components) {
            legacy.append(component.toLegacyText());
        }
        try {
            player.setPlayerListName(legacy.toString());
        } catch (Throwable ignored) {
            // Some platforms reject names longer than 16 chars; trim and retry.
            try {
                player.setPlayerListName(legacy.length() > 40 ? legacy.substring(0, 40) : legacy.toString());
            } catch (Throwable secondary) {
                // No-op if even the truncated path fails.
            }
        }
    }

    private static boolean tryAdventureTablist(Player player, String header, String footer) {
        if (!ADVENTURE_READY) {
            return false;
        }
        try {
            Method send = ADVENTURE_TABLIST_SEND.computeIfAbsent(player.getClass(),
                    AdventureMessageUtil::lookupAdventureTablistSend);
            if (send == MISSING_METHOD) {
                return false;
            }
            Object headerComponent = ADVENTURE_DESERIALIZE.invoke(ADVENTURE_MINIMESSAGE_INSTANCE, legacyToMiniMessage(header));
            Object footerComponent = ADVENTURE_DESERIALIZE.invoke(ADVENTURE_MINIMESSAGE_INSTANCE, legacyToMiniMessage(footer));
            send.invoke(player, headerComponent, footerComponent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method lookupAdventureTablistSend(Class<?> playerClass) {
        if (ADVENTURE_COMPONENT_CLASS == null) {
            return MISSING_METHOD;
        }
        try {
            return playerClass.getMethod("sendPlayerListHeaderAndFooter", ADVENTURE_COMPONENT_CLASS, ADVENTURE_COMPONENT_CLASS);
        } catch (NoSuchMethodException ignored) {
            return MISSING_METHOD;
        }
    }

    private static boolean trySpigotTablist(Player player, String header, String footer) {
        try {
            Object spigot = player.spigot();
            Method method = SPIGOT_TABLIST_SEND.computeIfAbsent(spigot.getClass(),
                    AdventureMessageUtil::lookupSpigotTablistSend);
            if (method == MISSING_METHOD) {
                return false;
            }
            BaseComponent[] headerComponents = renderBungee(header);
            BaseComponent[] footerComponents = renderBungee(footer);
            method.invoke(spigot, headerComponents, footerComponents);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method lookupSpigotTablistSend(Class<?> spigotClass) {
        try {
            return spigotClass.getMethod("setPlayerListHeaderFooter", BaseComponent[].class, BaseComponent[].class);
        } catch (NoSuchMethodException ignored) {
            return MISSING_METHOD;
        }
    }

    private static Method lookupLegacyHeaderSetter(Class<?> playerClass) {
        try {
            return playerClass.getMethod("setPlayerListHeader", String.class);
        } catch (NoSuchMethodException ignored) {
            return MISSING_METHOD;
        }
    }

    private static Method lookupLegacyFooterSetter(Class<?> playerClass) {
        try {
            return playerClass.getMethod("setPlayerListFooter", String.class);
        } catch (NoSuchMethodException ignored) {
            return MISSING_METHOD;
        }
    }

    private static Method sentinelMethod() {
        try {
            return Object.class.getMethod("toString");
        } catch (NoSuchMethodException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static String stripUnsupportedTags(String input) {
        if (input == null) {
            return "";
        }
        return TAG.matcher(input).replaceAll("");
    }

    /**
     * Converts a MiniMessage-style input into an ANSI-colored string suitable for direct
     * writing to a terminal that supports truecolor escapes (the same path used by the
     * OBX boot banner). Hover and click annotations are dropped since they cannot be
     * represented in console output. The returned string always ends with a reset escape.
     */
    public static String renderAnsi(String raw, Map<String, String> placeholders) {
        if (raw == null) {
            return "";
        }
        String resolved = applyPlaceholders(raw, placeholders);
        return renderAnsiFromResolved(resolved);
    }

    private static String renderAnsiFromResolved(String resolved) {
        List<Span> spans = parseToSpans(expandGradients(resolved));
        if (spans.isEmpty()) {
            return ANSI_RESET;
        }
        StringBuilder builder = new StringBuilder(resolved.length() + 32);
        for (Span span : spans) {
            if (span.text == null || span.text.isEmpty()) {
                continue;
            }
            builder.append(ANSI_RESET);
            if (span.color != null) {
                int[] rgb = parseHex(span.color);
                if (rgb != null) {
                    builder.append("[38;2;").append(rgb[0]).append(';').append(rgb[1]).append(';').append(rgb[2]).append('m');
                }
            }
            if (span.bold) builder.append("[1m");
            if (span.italic) builder.append("[3m");
            if (span.underlined) builder.append("[4m");
            if (span.strikethrough) builder.append("[9m");
            if (span.obfuscated) builder.append("[5m");
            builder.append(span.text);
        }
        builder.append(ANSI_RESET);
        return builder.toString();
    }

    private static final String ANSI_RESET = "[0m";

    private static String applyPlaceholders(String input, Map<String, String> placeholders) {
        if (input == null) {
            return "";
        }
        boolean hasBraces = input.indexOf('{') >= 0;
        boolean hasEscape = input.indexOf('\\') >= 0;
        if (!hasBraces) {
            return hasEscape ? input.replace("\\n", "\n") : input;
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return hasEscape ? input.replace("\\n", "\n") : input;
        }
        Matcher matcher = PLACEHOLDER.matcher(input);
        StringBuffer buffer = new StringBuffer(input.length());
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = placeholders.get(key);
            if (replacement == null) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        String result = buffer.toString();
        return result.indexOf('\\') < 0 ? result : result.replace("\\n", "\n");
    }

    // -------------------------------------------------------------------------
    // Adventure path (used when MiniMessage is on the runtime classpath)
    // -------------------------------------------------------------------------

    private static boolean trySendAdventure(Player player, String raw) {
        // Direct-build path first: parse to spans (which expands gradients into
        // explicit per-glyph colors) and assemble an Adventure Component tree
        // ourselves. This skips MiniMessage's parser entirely and matches the
        // shape the wire format expects, glyph-for-glyph. Some MiniMessage builds
        // and downstream chat decorators were collapsing the per-glyph color
        // tags back into solid bands by the time the chat packet was sent —
        // this path eliminates every intermediate parser.
        if (ADVENTURE_DIRECT_READY && trySendAdventureDirect(player, raw)) {
            return true;
        }
        if (!ADVENTURE_READY) {
            return false;
        }
        try {
            String preprocessed = legacyToMiniMessage(raw);
            Method sendMessage = ADVENTURE_SEND_MESSAGE.computeIfAbsent(player.getClass(),
                    AdventureMessageUtil::lookupAdventureSendMessage);
            if (sendMessage == MISSING_METHOD) {
                return false;
            }
            Object component = ADVENTURE_DESERIALIZE.invoke(ADVENTURE_MINIMESSAGE_INSTANCE, preprocessed);
            sendMessage.invoke(player, component);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Builds an Adventure {@code Component} directly from parsed spans (no MiniMessage
     * parser involved) and dispatches it. Returns {@code false} if the reflection
     * surface couldn't be wired or the build failed; the caller then tries the
     * MiniMessage path.
     */
    private static boolean trySendAdventureDirect(Player player, String raw) {
        boolean containsGradient = raw != null && raw.indexOf('<') >= 0
                && indexOfIgnoreCase(raw, "<gradient", 0) >= 0;
        boolean containsInteractive = hasInteractiveMarkup(raw);
        boolean shouldLog = containsGradient || containsInteractive;
        try {
            List<Span> spans = parseToSpans(expandGradients(raw));
            if (spans.isEmpty()) {
                spans.add(emptyTextSpan());
            }
            int hoverSpans = 0;
            int clickSpans = 0;
            for (Span s : spans) {
                if (s == null) continue;
                if (s.hoverLines != null && !s.hoverLines.isEmpty()) hoverSpans++;
                if (s.clickAction != null && s.clickValue != null) clickSpans++;
            }
            Object component = buildComponentFromSpans(spans);
            if (component == null) {
                if (shouldLog) {
                    logFirstGradientPath("direct/build-failed (component=null, spans=" + spans.size()
                            + ", hover-spans=" + hoverSpans + ", click-spans=" + clickSpans + ")");
                }
                return false;
            }
            Method sendMessage = ADVENTURE_SEND_MESSAGE.computeIfAbsent(player.getClass(),
                    AdventureMessageUtil::lookupAdventureSendMessage);
            if (sendMessage == MISSING_METHOD) {
                if (shouldLog) {
                    logFirstGradientPath("direct/no-sendMessage-method on " + player.getClass().getName()
                            + " (hover-spans=" + hoverSpans + ", click-spans=" + clickSpans + ")");
                }
                return false;
            }
            sendMessage.invoke(player, component);
            if (shouldLog) {
                logFirstGradientPath("direct/sent (spans=" + spans.size()
                        + ", hover-spans=" + hoverSpans + ", click-spans=" + clickSpans
                        + ", hover-reflection=" + (ADVENTURE_HOVER_SHOW_TEXT != null && ADVENTURE_COMPONENT_HOVER_EVENT != null)
                        + ", click-reflection=" + (ADVENTURE_COMPONENT_CLICK_EVENT != null)
                        + ", first-3-colors=" + describeFirstColors(spans, 3) + ")");
            }
            return true;
        } catch (Throwable t) {
            if (shouldLog) {
                logFirstGradientPath("direct/threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
            return false;
        }
    }

    /**
     * No-op retained so the (now silent) diagnostic call sites don't need to be
     * touched. The gradient rendering works via whichever path succeeds; the
     * runtime-path chatter is no longer logged to console.
     */
    private static void logFirstGradientPath(String detail) {
        // Intentionally silent.
    }

    private static String describeFirstColors(List<Span> spans, int limit) {
        StringBuilder sb = new StringBuilder("[");
        int shown = 0;
        for (Span span : spans) {
            if (span == null || span.text == null || span.text.isEmpty() || span.color == null) continue;
            if (shown > 0) sb.append(", ");
            sb.append('"').append(span.text.replace("\"", "\\\"")).append("\"=#").append(span.color);
            shown++;
            if (shown >= limit) break;
        }
        return sb.append(']').toString();
    }

    private static Span emptyTextSpan() {
        Span span = new Span();
        span.text = "";
        return span;
    }

    /**
     * Converts parsed spans into a single Adventure Component. Consecutive spans that
     * share an identical hover/click event are nested under a single wrapper that
     * carries the event once; the wrapper's per-glyph children carry only their
     * color and decorations. This is critical for gradient-styled banners wrapped
     * in a hover tooltip — without grouping, every visible glyph would emit its
     * own copy of the tooltip body in the chat-packet JSON. With a 16-line
     * tooltip and ~20 visible glyphs that pushes the packet past Mojang's
     * pre-1.18 ~32 KB chat-packet limit and the message either drops on the
     * wire or arrives with the per-glyph color tags collapsed by an upstream
     * truncation. Grouping keeps the hover body singular while preserving every
     * span's individual color, so the gradient renders correctly across every
     * client version.
     */
    private static Object buildComponentFromSpans(List<Span> spans) {
        if (!ADVENTURE_DIRECT_READY || spans == null) {
            return null;
        }
        Object parent;
        try {
            parent = ADVENTURE_TEXT_OF_STRING.invoke(null, "");
        } catch (Throwable t) {
            return null;
        }
        int n = spans.size();
        int i = 0;
        while (i < n) {
            Span first = spans.get(i);
            if (first == null || first.text == null || first.text.isEmpty()) {
                i++;
                continue;
            }
            boolean hasHover = first.hoverLines != null && !first.hoverLines.isEmpty()
                    && ADVENTURE_HOVER_SHOW_TEXT != null && ADVENTURE_COMPONENT_HOVER_EVENT != null;
            boolean hasClick = first.clickAction != null && first.clickValue != null
                    && ADVENTURE_COMPONENT_CLICK_EVENT != null;
            // Find the longest run of consecutive spans that share the same
            // hover and click. The run defines the scope of one wrapper.
            int runEnd = i + 1;
            if (hasHover || hasClick) {
                while (runEnd < n) {
                    Span next = spans.get(runEnd);
                    if (next == null || next.text == null || next.text.isEmpty()) {
                        runEnd++;
                        continue;
                    }
                    if (!sameInteractive(first, next)) {
                        break;
                    }
                    runEnd++;
                }
            }
            boolean groupable = (hasHover || hasClick) && runEnd > i + 1;
            try {
                if (groupable) {
                    Object wrapper = ADVENTURE_TEXT_OF_STRING.invoke(null, "");
                    for (int k = i; k < runEnd; k++) {
                        Span span = spans.get(k);
                        if (span == null || span.text == null || span.text.isEmpty()) {
                            continue;
                        }
                        Object child = buildSpanChild(span, false);
                        if (child != null) {
                            wrapper = ADVENTURE_COMPONENT_APPEND.invoke(wrapper, child);
                        }
                    }
                    if (hasHover) {
                        Object hoverComponent = buildHoverComponent(first.hoverLines);
                        if (hoverComponent != null) {
                            Object hoverEvent = ADVENTURE_HOVER_SHOW_TEXT.invoke(null, hoverComponent);
                            wrapper = ADVENTURE_COMPONENT_HOVER_EVENT.invoke(wrapper, hoverEvent);
                        }
                    }
                    if (hasClick) {
                        Object clickEvent = buildClickEvent(first.clickAction, first.clickValue);
                        if (clickEvent != null) {
                            wrapper = ADVENTURE_COMPONENT_CLICK_EVENT.invoke(wrapper, clickEvent);
                        }
                    }
                    parent = ADVENTURE_COMPONENT_APPEND.invoke(parent, wrapper);
                    i = runEnd;
                } else {
                    Object child = buildSpanChild(first, true);
                    if (child != null) {
                        parent = ADVENTURE_COMPONENT_APPEND.invoke(parent, child);
                    }
                    i++;
                }
            } catch (Throwable t) {
                // Skip this span/group rather than aborting the entire build —
                // losing one glyph is far better than collapsing the whole
                // message into the BungeeCord/legacy fallback path.
                i = Math.max(i + 1, runEnd);
            }
        }
        return parent;
    }

    /**
     * Builds one Adventure Component for a single span. Always emits the span's
     * color and the explicit TRUE/FALSE for every decoration (so the child does
     * not inherit decorations from a parent wrapper). Hover/click are emitted
     * only when {@code includeInteractive} is true — when the caller has hoisted
     * the hover/click onto a parent wrapper, the children must omit it to avoid
     * the per-glyph duplication that bloats the chat packet.
     */
    private static Object buildSpanChild(Span span, boolean includeInteractive) throws Exception {
        Object child = ADVENTURE_TEXT_OF_STRING.invoke(null, span.text);
        if (span.color != null) {
            try {
                int rgb = Integer.parseInt(span.color, 16);
                Object textColor = ADVENTURE_TEXTCOLOR_COLOR_INT.invoke(null, rgb);
                child = ADVENTURE_COMPONENT_COLOR.invoke(child, textColor);
            } catch (NumberFormatException ignored) {
                // unparseable hex — keep child uncolored
            }
        }
        if (ADVENTURE_COMPONENT_DECORATION_BOOL != null) {
            if (ADVENTURE_DEC_BOLD != null) {
                child = ADVENTURE_COMPONENT_DECORATION_BOOL.invoke(child, ADVENTURE_DEC_BOLD, span.bold);
            }
            if (ADVENTURE_DEC_ITALIC != null) {
                child = ADVENTURE_COMPONENT_DECORATION_BOOL.invoke(child, ADVENTURE_DEC_ITALIC, span.italic);
            }
            if (ADVENTURE_DEC_UNDERLINED != null) {
                child = ADVENTURE_COMPONENT_DECORATION_BOOL.invoke(child, ADVENTURE_DEC_UNDERLINED, span.underlined);
            }
            if (ADVENTURE_DEC_STRIKETHROUGH != null) {
                child = ADVENTURE_COMPONENT_DECORATION_BOOL.invoke(child, ADVENTURE_DEC_STRIKETHROUGH, span.strikethrough);
            }
            if (ADVENTURE_DEC_OBFUSCATED != null) {
                child = ADVENTURE_COMPONENT_DECORATION_BOOL.invoke(child, ADVENTURE_DEC_OBFUSCATED, span.obfuscated);
            }
        } else {
            if (span.bold && ADVENTURE_DEC_BOLD != null) {
                child = ADVENTURE_COMPONENT_DECORATE.invoke(child, ADVENTURE_DEC_BOLD);
            }
            if (span.italic && ADVENTURE_DEC_ITALIC != null) {
                child = ADVENTURE_COMPONENT_DECORATE.invoke(child, ADVENTURE_DEC_ITALIC);
            }
            if (span.underlined && ADVENTURE_DEC_UNDERLINED != null) {
                child = ADVENTURE_COMPONENT_DECORATE.invoke(child, ADVENTURE_DEC_UNDERLINED);
            }
            if (span.strikethrough && ADVENTURE_DEC_STRIKETHROUGH != null) {
                child = ADVENTURE_COMPONENT_DECORATE.invoke(child, ADVENTURE_DEC_STRIKETHROUGH);
            }
            if (span.obfuscated && ADVENTURE_DEC_OBFUSCATED != null) {
                child = ADVENTURE_COMPONENT_DECORATE.invoke(child, ADVENTURE_DEC_OBFUSCATED);
            }
        }
        if (includeInteractive) {
            if (span.hoverLines != null && !span.hoverLines.isEmpty()
                    && ADVENTURE_HOVER_SHOW_TEXT != null && ADVENTURE_COMPONENT_HOVER_EVENT != null) {
                Object hoverComponent = buildHoverComponent(span.hoverLines);
                if (hoverComponent != null) {
                    Object hoverEvent = ADVENTURE_HOVER_SHOW_TEXT.invoke(null, hoverComponent);
                    child = ADVENTURE_COMPONENT_HOVER_EVENT.invoke(child, hoverEvent);
                }
            }
            if (span.clickAction != null && span.clickValue != null
                    && ADVENTURE_COMPONENT_CLICK_EVENT != null) {
                Object clickEvent = buildClickEvent(span.clickAction, span.clickValue);
                if (clickEvent != null) {
                    child = ADVENTURE_COMPONENT_CLICK_EVENT.invoke(child, clickEvent);
                }
            }
        }
        return child;
    }

    /**
     * Returns {@code true} when both spans carry the same hover lines and the
     * same click action+value. Used to decide whether two consecutive spans can
     * share a single interactive wrapper.
     */
    private static boolean sameInteractive(Span a, Span b) {
        if (a == null || b == null) return false;
        // Hover lines: identity by sequence equality
        List<String> ah = a.hoverLines;
        List<String> bh = b.hoverLines;
        if ((ah == null) != (bh == null)) return false;
        if (ah != null && !ah.equals(bh)) return false;
        // Click action + value
        if (!java.util.Objects.equals(a.clickAction, b.clickAction)) return false;
        if (!java.util.Objects.equals(a.clickValue, b.clickValue)) return false;
        return true;
    }

    private static Object buildHoverComponent(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                joined.append('\n');
            }
            joined.append(lines.get(i) == null ? "" : lines.get(i));
        }
        // Hover content may carry legacy &x codes, so route through the same legacy
        // → tag → expansion preprocessor as the main message before parsing to spans.
        String preprocessed = legacyToMiniMessage(joined.toString());
        List<Span> hoverSpans = parseToSpans(preprocessed);
        return buildComponentFromSpans(hoverSpans);
    }

    private static Object buildClickEvent(String action, String value) {
        if (action == null || value == null) {
            return null;
        }
        try {
            switch (action.toUpperCase(Locale.ROOT)) {
                case "RUN_COMMAND":
                    return ADVENTURE_CLICK_RUN_COMMAND == null ? null : ADVENTURE_CLICK_RUN_COMMAND.invoke(null, value);
                case "SUGGEST_COMMAND":
                    return ADVENTURE_CLICK_SUGGEST_COMMAND == null ? null : ADVENTURE_CLICK_SUGGEST_COMMAND.invoke(null, value);
                case "OPEN_URL":
                    return ADVENTURE_CLICK_OPEN_URL == null ? null : ADVENTURE_CLICK_OPEN_URL.invoke(null, value);
                case "COPY_TO_CLIPBOARD":
                    return ADVENTURE_CLICK_COPY == null ? null : ADVENTURE_CLICK_COPY.invoke(null, value);
                case "CHANGE_PAGE":
                    if (ADVENTURE_CLICK_CHANGE_PAGE == null) {
                        return null;
                    }
                    Class<?>[] params = ADVENTURE_CLICK_CHANGE_PAGE.getParameterTypes();
                    if (params.length == 1 && params[0] == int.class) {
                        try {
                            return ADVENTURE_CLICK_CHANGE_PAGE.invoke(null, Integer.parseInt(value));
                        } catch (NumberFormatException ignored) {
                            return null;
                        }
                    }
                    return ADVENTURE_CLICK_CHANGE_PAGE.invoke(null, value);
                default:
                    return null;
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method lookupAdventureSendMessage(Class<?> playerClass) {
        if (ADVENTURE_COMPONENT_CLASS == null) {
            return MISSING_METHOD;
        }
        // Use the assignable-parameter matcher rather than exact-signature
        // {@code getMethod}. Paper's CraftPlayer inherits {@code sendMessage}
        // from Adventure's {@code Audience}, but that method's declared
        // parameter is {@code Component} on some forks and {@code ComponentLike}
        // on others. {@code Component} is a {@code ComponentLike}, so the
        // flexible matcher finds either overload.
        Method match = findSingleArgMethod(playerClass, "sendMessage", ADVENTURE_COMPONENT_CLASS);
        return match != null ? match : MISSING_METHOD;
    }

    private static String legacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        // Translate &#RRGGBB to <#RRGGBB>
        Matcher hexMatcher = LEGACY_HEX.matcher(input);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexBuffer, "<#" + hexMatcher.group(1).toUpperCase(Locale.ROOT) + ">");
        }
        hexMatcher.appendTail(hexBuffer);
        String working = hexBuffer.toString();
        StringBuilder builder = new StringBuilder(working.length() + 16);
        for (int i = 0; i < working.length(); i++) {
            char c = working.charAt(i);
            if ((c == '&' || c == ChatColor.COLOR_CHAR) && i + 1 < working.length()) {
                char code = Character.toLowerCase(working.charAt(i + 1));
                String tag = legacyCodeToTag(code);
                if (tag != null) {
                    builder.append(tag);
                    i++;
                    continue;
                }
            }
            builder.append(c);
        }
        // Pre-expand <gradient:c1:c2>...</gradient> into explicit per-character
        // <#RRGGBB> tags so MiniMessage never invokes its built-in gradient
        // renderer. Some MiniMessage builds collapse the gradient into a small
        // number of solid bands when nested inside decorations like <bold>; doing
        // the per-glyph interpolation ourselves and emitting plain color tags
        // gives a true gradient on every Paper/Folia version.
        return expandGradients(builder.toString());
    }

    /**
     * Replaces every {@code <gradient:c1:c2>INNER</gradient>} block with a sequence
     * of explicit {@code <#RRGGBB>} color tags interleaved with the inner glyphs,
     * preserving any nested tags. Whitespace consumes no slot in the color ramp,
     * so the visible characters always span the full {@code c1 → c2} range.
     *
     * <p>Recurses to expand nested gradients. Falls through unchanged for
     * gradient tags that lack two parseable hex colors or a matching close tag,
     * so MiniMessage can still render its own error path on bad input.
     */
    private static String expandGradients(String input) {
        if (input == null || input.isEmpty()) {
            return input == null ? "" : input;
        }
        if (indexOfIgnoreCase(input, "<gradient", 0) < 0) {
            return input;
        }
        int len = input.length();
        StringBuilder out = new StringBuilder(len + 64);
        int idx = 0;
        while (idx < len) {
            int start = indexOfIgnoreCaseQuoteAware(input, "<gradient", idx);
            if (start < 0) {
                out.append(input, idx, len);
                break;
            }
            // Confirm the match is the start of a gradient tag (next char must be ':' or '>').
            int afterName = start + "<gradient".length();
            if (afterName >= len || (input.charAt(afterName) != ':' && input.charAt(afterName) != '>')) {
                out.append(input, idx, afterName);
                idx = afterName;
                continue;
            }
            out.append(input, idx, start);

            int openEnd = findTagEnd(input, afterName);
            if (openEnd < 0) {
                out.append(input, start, len);
                break;
            }
            String tagBody = input.substring(start + 1, openEnd);
            List<String> args = splitTagArgs(tagBody);
            int[] startColor = null;
            int[] endColor = null;
            if (args.size() >= 3) {
                startColor = parseHex(stripHash(args.get(1)));
                endColor = parseHex(stripHash(args.get(2)));
            }
            int closeStart = findCloseTag(input, openEnd + 1, "gradient");
            if (closeStart < 0 || startColor == null || endColor == null) {
                // Bad gradient — leave it for MiniMessage to handle / report.
                out.append(input, start, openEnd + 1);
                idx = openEnd + 1;
                continue;
            }
            String inner = input.substring(openEnd + 1, closeStart);
            String expandedInner = expandGradients(inner);
            int totalVisible = countVisibleGlyphs(expandedInner);
            if (totalVisible == 0) {
                out.append(expandedInner);
            } else {
                int visIdx = 0;
                int p = 0;
                int innerLen = expandedInner.length();
                while (p < innerLen) {
                    char c = expandedInner.charAt(p);
                    if (c == '<') {
                        int tEnd = findTagEnd(expandedInner, p + 1);
                        if (tEnd < 0) {
                            out.append(expandedInner, p, innerLen);
                            break;
                        }
                        out.append(expandedInner, p, tEnd + 1);
                        p = tEnd + 1;
                        continue;
                    }
                    // Pass legacy color/format codes through untouched — they apply
                    // to the next glyph and must not be split across <#color> wrappers.
                    if ((c == '&' || c == ChatColor.COLOR_CHAR) && p + 1 < innerLen
                            && isLegacyCode(Character.toLowerCase(expandedInner.charAt(p + 1)))) {
                        out.append(c).append(expandedInner.charAt(p + 1));
                        p += 2;
                        continue;
                    }
                    if (Character.isWhitespace(c)) {
                        out.append(c);
                        p++;
                        continue;
                    }
                    double ratio = totalVisible <= 1 ? 0.0 : (double) visIdx / (totalVisible - 1);
                    int r = (int) Math.round(startColor[0] + (endColor[0] - startColor[0]) * ratio);
                    int g = (int) Math.round(startColor[1] + (endColor[1] - startColor[1]) * ratio);
                    int b = (int) Math.round(startColor[2] + (endColor[2] - startColor[2]) * ratio);
                    // Emit a fully-closed <color:#RRGGBB>X</color> per glyph rather
                    // than the unclosed <#RRGGBB> shorthand. Some MiniMessage builds
                    // and downstream chat formatters treat a long run of unclosed
                    // hex shorthands as a single deep-nested scope and merge adjacent
                    // glyphs back into one color band — fully closed scopes prevent
                    // that collapse on every Adventure version.
                    out.append("<color:#").append(String.format("%02X%02X%02X", r, g, b)).append('>');
                    out.append(c);
                    out.append("</color>");
                    visIdx++;
                    p++;
                }
            }
            idx = closeStart + "</gradient>".length();
        }
        return out.toString();
    }

    private static int countVisibleGlyphs(String text) {
        int count = 0;
        int len = text.length();
        int p = 0;
        while (p < len) {
            char c = text.charAt(p);
            if (c == '<') {
                int tEnd = findTagEnd(text, p + 1);
                if (tEnd < 0) {
                    return count;
                }
                p = tEnd + 1;
                continue;
            }
            // Skip legacy codes — they aren't rendered glyphs.
            if ((c == '&' || c == ChatColor.COLOR_CHAR) && p + 1 < len
                    && isLegacyCode(Character.toLowerCase(text.charAt(p + 1)))) {
                p += 2;
                continue;
            }
            if (!Character.isWhitespace(c)) {
                count++;
            }
            p++;
        }
        return count;
    }

    private static String legacyCodeToTag(char code) {
        switch (code) {
            case '0': return "<black>";
            case '1': return "<dark_blue>";
            case '2': return "<dark_green>";
            case '3': return "<dark_purple>";
            case '4': return "<dark_red>";
            case '5': return "<dark_purple>";
            case '6': return "<dark_purple>";
            case '7': return "<gray>";
            case '8': return "<dark_gray>";
            case '9': return "<blue>";
            case 'a': return "<green>";
            case 'b': return "<light_purple>";
            case 'c': return "<red>";
            case 'd': return "<light_purple>";
            case 'e': return "<yellow>";
            case 'f': return "<white>";
            case 'l': return "<bold>";
            case 'm': return "<strikethrough>";
            case 'n': return "<underlined>";
            case 'o': return "<italic>";
            case 'k': return "<obfuscated>";
            case 'r': return "<reset>";
            default: return null;
        }
    }

    // -------------------------------------------------------------------------
    // BungeeCord fallback parser
    // -------------------------------------------------------------------------

    private static BaseComponent[] renderBungee(String input) {
        List<Span> spans = parseToSpans(expandGradients(input));
        if (spans.isEmpty()) {
            return new BaseComponent[]{new TextComponent("")};
        }
        List<BaseComponent> components = new ArrayList<>();
        int n = spans.size();
        int i = 0;
        while (i < n) {
            Span first = spans.get(i);
            if (first == null || first.text == null || first.text.isEmpty()) {
                i++;
                continue;
            }
            boolean hasHover = first.hoverLines != null && !first.hoverLines.isEmpty();
            boolean hasClick = first.clickAction != null && first.clickValue != null;
            // Group consecutive spans that share the same hover / click into one
            // BaseComponent wrapper. The children inherit the parent's hover
            // and click via the chat protocol's NOT_SET inheritance, so the
            // serialized wire packet only carries one copy of the hover body
            // even when the gradient produced 20+ child glyphs.
            int runEnd = i + 1;
            if (hasHover || hasClick) {
                while (runEnd < n) {
                    Span next = spans.get(runEnd);
                    if (next == null || next.text == null || next.text.isEmpty()) {
                        runEnd++;
                        continue;
                    }
                    if (!sameInteractive(first, next)) {
                        break;
                    }
                    runEnd++;
                }
            }
            if ((hasHover || hasClick) && runEnd > i + 1) {
                TextComponent wrapper = new TextComponent("");
                List<BaseComponent> children = new ArrayList<>();
                for (int k = i; k < runEnd; k++) {
                    Span span = spans.get(k);
                    if (span == null || span.text == null || span.text.isEmpty()) {
                        continue;
                    }
                    TextComponent child = new TextComponent(TextComponent.fromLegacyText(""));
                    child.setText(span.text);
                    applyStyle(child, span, false);
                    children.add(child);
                }
                wrapper.setExtra(children);
                applyInteractiveOnly(wrapper, first);
                components.add(wrapper);
                i = runEnd;
            } else {
                TextComponent component = new TextComponent(TextComponent.fromLegacyText(""));
                component.setText(first.text);
                applyStyle(component, first, true);
                components.add(component);
                i++;
            }
        }
        if (components.isEmpty()) {
            return new BaseComponent[]{new TextComponent("")};
        }
        return components.toArray(new BaseComponent[0]);
    }

    private static void applyStyle(TextComponent component, Span span) {
        applyStyle(component, span, true);
    }

    /**
     * Applies the span's color and decorations to {@code component}. When
     * {@code includeInteractive} is {@code true}, the span's hover and click
     * are also attached to the component itself; when {@code false}, the
     * caller is responsible for attaching them on a parent wrapper instead
     * (so identical hover/click events that span multiple gradient glyphs
     * are sent on the wire only once instead of duplicated per-glyph).
     */
    private static void applyStyle(TextComponent component, Span span, boolean includeInteractive) {
        if (span.color != null) {
            applyHexColor(component, span.color);
        }
        // Set every decoration to an EXPLICIT Boolean (true OR false) so the
        // child component never inherits from its parent. The previous code
        // only called setBold(true) when the span was bold and left it null
        // otherwise — a null flag means "inherit from parent". When this
        // BungeeCord path renders a hover tooltip whose visible carrier is
        // bold (e.g. the gradient "Welcome to the Server" header), the
        // tooltip's body components inherited that bold and the entire
        // tooltip rendered in bold. Setting the flag explicitly anchors
        // every span and blocks the inheritance.
        component.setBold(span.bold);
        component.setItalic(span.italic);
        component.setUnderlined(span.underlined);
        component.setStrikethrough(span.strikethrough);
        component.setObfuscated(span.obfuscated);
        if (includeInteractive) {
            applyInteractiveOnly(component, span);
        }
    }

    /**
     * Attaches only the span's hover and click events to {@code component}.
     * Used to set hover/click on a wrapper that groups several gradient-glyph
     * children, so each child can omit its own hover/click without losing the
     * interactive behavior (children inherit hover/click from the wrapper at
     * the chat-packet level).
     */
    private static void applyInteractiveOnly(TextComponent component, Span span) {
        if (span.hoverLines != null && !span.hoverLines.isEmpty()) {
            // Hover content carries the same MiniMessage syntax as the main
            // message body — tags like <dark_purple>, <bold>, <gradient:...>, plus
            // legacy &codes. Routing it through renderBungee runs the same
            // parser as the main line so those tags become real component
            // styling. The previous TextComponent.fromLegacyText path only
            // understood &codes, so any <tag> survived untouched and the
            // client rendered the tooltip as raw MiniMessage text.
            BaseComponent[] hoverComponents = renderBungee(String.join("\n", span.hoverLines));
            HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents);
            try {
                Method setLegacy = HoverEvent.class.getMethod("setLegacy", boolean.class);
                setLegacy.invoke(hover, true);
            } catch (Exception ignored) {
                // legacy flag missing on older builds
            }
            component.setHoverEvent(hover);
        }
        if (span.clickAction != null && span.clickValue != null) {
            try {
                ClickEvent.Action action = ClickEvent.Action.valueOf(span.clickAction);
                component.setClickEvent(new ClickEvent(action, span.clickValue));
            } catch (IllegalArgumentException ignored) {
                // unknown action - ignore silently
            }
        }
    }

    private static void applyHexColor(TextComponent component, String hex) {
        if (!HEX_REFLECTION_RESOLVED) {
            resolveHexReflection();
        }
        Method ofMethod = CHAT_COLOR_OF;
        Method setColor = TEXT_COMPONENT_SET_COLOR;
        if (ofMethod != null && setColor != null) {
            try {
                Object color = ofMethod.invoke(null, "#" + hex);
                setColor.invoke(component, color);
                return;
            } catch (Throwable ignored) {
                // legacy server - fall through
            }
        }
        component.setColor(closestLegacyColor(hex).asBungee());
    }

    private static synchronized void resolveHexReflection() {
        if (HEX_REFLECTION_RESOLVED) {
            return;
        }
        try {
            // Hex colors live on net.md_5.bungee.api.ChatColor (added 1.16), NOT
            // org.bukkit.ChatColor — that one is the legacy 16-color enum with no
            // of(String). The previous lookup used the bukkit class, so it always
            // failed and applyHexColor fell back to closestLegacyColor(), which
            // snaps every gradient glyph to the nearest of 16 colors → the gradient
            // collapsed into 2 solid bands. TextComponent.setColor also takes the
            // bungee ChatColor, so both handles must come from that class.
            Class<?> bungeeColor = net.md_5.bungee.api.ChatColor.class;
            Method ofMethod = bungeeColor.getMethod("of", String.class);
            Method setColor = TextComponent.class.getMethod("setColor", bungeeColor);
            CHAT_COLOR_OF = ofMethod;
            TEXT_COMPONENT_SET_COLOR = setColor;
        } catch (Throwable ignored) {
            CHAT_COLOR_OF = null;
            TEXT_COMPONENT_SET_COLOR = null;
        }
        HEX_REFLECTION_RESOLVED = true;
    }

    private static ChatColor closestLegacyColor(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            ChatColor[] palette = {
                    ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_PURPLE,
                    ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.DARK_PURPLE, ChatColor.GRAY,
                    ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.LIGHT_PURPLE,
                    ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
            };
            int[][] rgb = {
                    {0, 0, 0}, {0, 0, 170}, {0, 170, 0}, {0, 170, 170},
                    {170, 0, 0}, {170, 0, 170}, {255, 170, 0}, {170, 170, 170},
                    {85, 85, 85}, {85, 85, 255}, {85, 255, 85}, {85, 255, 255},
                    {255, 85, 85}, {255, 85, 255}, {255, 255, 85}, {255, 255, 255}
            };
            int bestIndex = 15;
            int bestDist = Integer.MAX_VALUE;
            for (int i = 0; i < palette.length; i++) {
                int dr = rgb[i][0] - r;
                int dg = rgb[i][1] - g;
                int db = rgb[i][2] - b;
                int dist = dr * dr + dg * dg + db * db;
                if (dist < bestDist) {
                    bestDist = dist;
                    bestIndex = i;
                }
            }
            return palette[bestIndex];
        } catch (Throwable ignored) {
            return ChatColor.WHITE;
        }
    }

    private static String translateLegacy(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    private static String stripFormatting(String input) {
        if (input == null) {
            return "";
        }
        String working = TAG.matcher(input).replaceAll("");
        working = LEGACY_HEX.matcher(working).replaceAll("");
        return ChatColor.stripColor(translateLegacy(working));
    }

    // -------------------------------------------------------------------------
    // Span parser (manual MiniMessage subset)
    // -------------------------------------------------------------------------

    static List<Span> parseToSpans(String input) {
        List<Span> spans = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return spans;
        }
        String preprocessed = LEGACY_HEX.matcher(input).replaceAll("<#$1>");

        Deque<StyleFrame> stack = new ArrayDeque<>();
        stack.push(new StyleFrame());

        int len = preprocessed.length();
        int i = 0;
        StringBuilder buffer = new StringBuilder();
        while (i < len) {
            char c = preprocessed.charAt(i);
            if (c == '<') {
                // Quote-aware close-bracket search. {@code <hover:show_text:'<dark_purple>...'>}
                // contains a {@code >} INSIDE its single-quoted args; a plain
                // {@code indexOf('>')} would terminate the tag at that early
                // {@code >} and leak the rest as literal chat text.
                int end = findTagEnd(preprocessed, i + 1);
                if (end > i) {
                    String inside = preprocessed.substring(i + 1, end);
                    TagInfo tag = parseTag(inside);
                    if (tag != null) {
                        flushBuffer(buffer, stack.peek(), spans);
                        if (tag.closing) {
                            applyClose(stack, tag);
                        } else {
                            if ("gradient".equalsIgnoreCase(tag.name)) {
                                int closeIdx = findCloseTag(preprocessed, end + 1, "gradient");
                                String inner = closeIdx >= 0 ? preprocessed.substring(end + 1, closeIdx) : preprocessed.substring(end + 1);
                                emitGradient(spans, inner, tag.args, stack.peek());
                                i = closeIdx >= 0 ? closeIdx + "</gradient>".length() : len;
                                continue;
                            }
                            applyOpen(stack, tag);
                        }
                        i = end + 1;
                        continue;
                    }
                }
            } else if ((c == '&' || c == ChatColor.COLOR_CHAR) && i + 1 < len) {
                char code = Character.toLowerCase(preprocessed.charAt(i + 1));
                if (isLegacyCode(code)) {
                    flushBuffer(buffer, stack.peek(), spans);
                    applyLegacyCode(stack, code);
                    i += 2;
                    continue;
                }
            }
            buffer.append(c);
            i++;
        }
        flushBuffer(buffer, stack.peek(), spans);
        return spans;
    }

    private static int findCloseTag(String text, int from, String name) {
        String needle = "</" + name.toLowerCase(Locale.ROOT);
        int depth = 1;
        int idx = from;
        while (idx < text.length()) {
            int nextOpen = indexOfIgnoreCase(text, "<" + name.toLowerCase(Locale.ROOT), idx);
            int nextClose = indexOfIgnoreCase(text, needle, idx);
            if (nextClose < 0) {
                return -1;
            }
            if (nextOpen >= 0 && nextOpen < nextClose) {
                depth++;
                idx = nextOpen + 1;
                continue;
            }
            depth--;
            if (depth == 0) {
                return nextClose;
            }
            idx = nextClose + 1;
        }
        return -1;
    }

    /**
     * Returns the index of the {@code >} that closes the tag opened at the byte
     * BEFORE {@code from}. Skips over any {@code >} that lies inside a single-
     * or double-quoted argument string (MiniMessage hover/click args commonly
     * contain markup like {@code 'show_text:<dark_purple>X</dark_purple>'} where a naive
     * {@code indexOf('>')} would terminate the tag at the {@code >} of
     * {@code <dark_purple>}). Returns {@code -1} if the tag never closes.
     */
    private static int findTagEnd(String text, int from) {
        int len = text.length();
        char quote = 0;
        for (int i = from; i < len; i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                continue;
            }
            if (c == '>') {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfIgnoreCase(String haystack, String needle, int from) {
        int max = haystack.length() - needle.length();
        for (int i = from; i <= max; i++) {
            if (haystack.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Like {@link #indexOfIgnoreCase} but skips matches that occur inside a
     * single- or double-quoted argument run. Used to keep
     * {@link #expandGradients} from rewriting a {@code <gradient>} that lives
     * inside a {@code <hover:show_text:'...'>} or {@code <click:...:'...'>}
     * argument — those quoted strings are tooltip body content and must reach
     * the parser intact, since they're parsed in their own scope.
     */
    private static int indexOfIgnoreCaseQuoteAware(String haystack, String needle, int from) {
        int max = haystack.length() - needle.length();
        char quote = 0;
        for (int i = from; i <= max; i++) {
            char c = haystack.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                continue;
            }
            if (haystack.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }
        return -1;
    }

    private static void emitGradient(List<Span> spans, String inner, List<String> args, StyleFrame currentFrame) {
        if (inner == null || inner.isEmpty() || args == null || args.size() < 2) {
            flushPlain(spans, inner, currentFrame);
            return;
        }
        int[] start = parseHex(stripHash(args.get(0)));
        int[] end = parseHex(stripHash(args.get(1)));
        if (start == null || end == null) {
            flushPlain(spans, inner, currentFrame);
            return;
        }
        List<Span> innerSpans = parseToSpans(inner);
        // Count only visible (non-whitespace) glyphs when distributing the gradient.
        // Whitespace has no glyph to color, so letting it consume slots in the
        // ratio compresses the visible color range — making "  Hello  " inside a
        // gradient render as a near-solid mid-tone instead of a real spread.
        int visibleChars = 0;
        for (Span span : innerSpans) {
            for (int i = 0; i < span.text.length(); i++) {
                if (!Character.isWhitespace(span.text.charAt(i))) {
                    visibleChars++;
                }
            }
        }
        if (visibleChars == 0) {
            // Pure whitespace inside a gradient — nothing to color, but keep the
            // text so spacing/centering is preserved.
            for (Span span : innerSpans) {
                spans.add(span);
            }
            return;
        }
        int visibleIndex = 0;
        for (Span span : innerSpans) {
            for (int i = 0; i < span.text.length(); i++) {
                char ch = span.text.charAt(i);
                Span charSpan = span.copyWithText(String.valueOf(ch));
                if (!Character.isWhitespace(ch)) {
                    double ratio = visibleChars <= 1 ? 0 : (double) visibleIndex / (visibleChars - 1);
                    int r = (int) Math.round(start[0] + (end[0] - start[0]) * ratio);
                    int g = (int) Math.round(start[1] + (end[1] - start[1]) * ratio);
                    int b = (int) Math.round(start[2] + (end[2] - start[2]) * ratio);
                    charSpan.color = String.format("%02X%02X%02X", r, g, b);
                    visibleIndex++;
                }
                spans.add(charSpan);
            }
        }
    }

    private static void flushPlain(List<Span> spans, String text, StyleFrame frame) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Span span = frame.toSpan();
        span.text = text;
        spans.add(span);
    }

    private static int[] parseHex(String hex) {
        if (hex == null || hex.length() != 6) {
            return null;
        }
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String stripHash(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("#")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private static void flushBuffer(StringBuilder buffer, StyleFrame frame, List<Span> spans) {
        if (buffer.length() == 0) {
            return;
        }
        Span span = frame.toSpan();
        span.text = buffer.toString();
        spans.add(span);
        buffer.setLength(0);
    }

    private static boolean isLegacyCode(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'l' || c == 'm'
                || c == 'n' || c == 'o' || c == 'k' || c == 'r';
    }

    private static void applyLegacyCode(Deque<StyleFrame> stack, char code) {
        StyleFrame frame = stack.peek();
        if (frame == null) {
            return;
        }
        if (code == 'r') {
            frame.color = null;
            frame.bold = false;
            frame.italic = false;
            frame.underlined = false;
            frame.strikethrough = false;
            frame.obfuscated = false;
            return;
        }
        switch (code) {
            case 'l': frame.bold = true; return;
            case 'm': frame.strikethrough = true; return;
            case 'n': frame.underlined = true; return;
            case 'o': frame.italic = true; return;
            case 'k': frame.obfuscated = true; return;
            default:
                String hex = legacyCodeToHex(code);
                if (hex != null) {
                    frame.color = hex;
                    frame.bold = false;
                    frame.italic = false;
                    frame.underlined = false;
                    frame.strikethrough = false;
                    frame.obfuscated = false;
                }
        }
    }

    private static String legacyCodeToHex(char code) {
        switch (code) {
            case '0': return "000000";
            case '1': return "0000AA";
            case '2': return "00AA00";
            case '3': return "00AAAA";
            case '4': return "AA0000";
            case '5': return "AA00AA";
            case '6': return "FFAA00";
            case '7': return "AAAAAA";
            case '8': return "555555";
            case '9': return "5555FF";
            case 'a': return "55FF55";
            case 'b': return "55FFFF";
            case 'c': return "FF5555";
            case 'd': return "FF55FF";
            case 'e': return "FFFF55";
            case 'f': return "FFFFFF";
            default: return null;
        }
    }

    private static void applyOpen(Deque<StyleFrame> stack, TagInfo tag) {
        StyleFrame current = stack.peek();
        StyleFrame next = current == null ? new StyleFrame() : current.copy();
        next.openTag = tag.name.toLowerCase(Locale.ROOT);
        String name = next.openTag;
        if (name.startsWith("#")) {
            next.color = stripHash(name);
        } else if ("color".equals(name) || "colour".equals(name) || "c".equals(name)) {
            if (!tag.args.isEmpty()) {
                String arg = tag.args.get(0);
                String hex = namedColorToHex(arg);
                next.color = hex != null ? hex : stripHash(arg);
            }
        } else if ("bold".equals(name) || "b".equals(name)) {
            next.bold = true;
        } else if ("italic".equals(name) || "i".equals(name) || "em".equals(name)) {
            next.italic = true;
        } else if ("underlined".equals(name) || "underline".equals(name) || "u".equals(name)) {
            next.underlined = true;
        } else if ("strikethrough".equals(name) || "st".equals(name)) {
            next.strikethrough = true;
        } else if ("obfuscated".equals(name) || "obf".equals(name)) {
            next.obfuscated = true;
        } else if ("reset".equals(name)) {
            next.reset();
        } else if ("hover".equals(name)) {
            if (tag.args.size() >= 2 && "show_text".equalsIgnoreCase(tag.args.get(0))) {
                String hover = tag.args.get(1);
                next.hoverLines = splitHoverLines(hover);
            }
        } else if ("click".equals(name)) {
            if (tag.args.size() >= 2) {
                next.clickAction = mapClickAction(tag.args.get(0));
                next.clickValue = tag.args.get(1);
            }
        } else if ("shadow".equals(name)) {
            if (!tag.args.isEmpty()) {
                next.shadow = stripHash(tag.args.get(0));
            }
        } else {
            String hex = namedColorToHex(name);
            if (hex != null) {
                next.color = hex;
            }
        }
        stack.push(next);
    }

    private static List<String> splitHoverLines(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        String unquoted = raw;
        if ((unquoted.startsWith("'") && unquoted.endsWith("'") && unquoted.length() >= 2)
                || (unquoted.startsWith("\"") && unquoted.endsWith("\"") && unquoted.length() >= 2)) {
            unquoted = unquoted.substring(1, unquoted.length() - 1);
        }
        unquoted = unquoted.replace("\\n", "\n");
        String[] segments = unquoted.split("\n", -1);
        return new ArrayList<>(java.util.Arrays.asList(segments));
    }

    private static String mapClickAction(String input) {
        if (input == null) {
            return null;
        }
        switch (input.toLowerCase(Locale.ROOT)) {
            case "run_command": return "RUN_COMMAND";
            case "suggest_command": return "SUGGEST_COMMAND";
            case "open_url": return "OPEN_URL";
            case "copy_to_clipboard": return "COPY_TO_CLIPBOARD";
            case "change_page": return "CHANGE_PAGE";
            default: return null;
        }
    }

    private static void applyClose(Deque<StyleFrame> stack, TagInfo tag) {
        if (stack.size() <= 1) {
            return;
        }
        String target = tag.name.toLowerCase(Locale.ROOT);
        Deque<StyleFrame> aside = new ArrayDeque<>();
        while (stack.size() > 1) {
            StyleFrame popped = stack.pop();
            if (popped.openTag != null && popped.openTag.equalsIgnoreCase(target)) {
                return;
            }
            aside.push(popped);
        }
        // Tag did not match - restore stack
        while (!aside.isEmpty()) {
            stack.push(aside.pop());
        }
    }

    private static TagInfo parseTag(String inside) {
        if (inside == null || inside.isEmpty()) {
            return null;
        }
        boolean closing = inside.charAt(0) == '/';
        String body = closing ? inside.substring(1) : inside;
        if (body.isEmpty()) {
            return null;
        }
        List<String> parts = splitTagArgs(body);
        if (parts.isEmpty()) {
            return null;
        }
        String name = parts.get(0);
        List<String> args = parts.size() > 1 ? new ArrayList<>(parts.subList(1, parts.size())) : Collections.<String>emptyList();
        return new TagInfo(name, args, closing);
    }

    private static List<String> splitTagArgs(String body) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else {
                    cur.append(c);
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                continue;
            }
            if (c == ':') {
                parts.add(cur.toString());
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        if (cur.length() > 0 || !parts.isEmpty()) {
            parts.add(cur.toString());
        }
        return parts;
    }

    private static String namedColorToHex(String name) {
        if (name == null) {
            return null;
        }
        switch (name.toLowerCase(Locale.ROOT)) {
            case "black": return "000000";
            case "dark_blue": return "0000AA";
            case "dark_green": return "00AA00";
            case "dark_aqua":
            case "dark_cyan":
                return "00AAAA";
            case "dark_red": return "AA0000";
            case "dark_purple":
            case "dark_magenta":
                return "AA00AA";
            case "gold":
            case "orange":
                return "FFAA00";
            case "gray":
            case "grey":
                return "AAAAAA";
            case "dark_gray":
            case "dark_grey":
                return "555555";
            case "blue": return "5555FF";
            case "green": return "55FF55";
            case "aqua":
            case "cyan":
                return "55FFFF";
            case "red": return "FF5555";
            case "light_purple":
            case "pink":
            case "magenta":
                return "FF55FF";
            case "yellow": return "FFFF55";
            case "white": return "FFFFFF";
            default: return null;
        }
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Internal value types
    // -------------------------------------------------------------------------

    private static final class TagInfo {
        final String name;
        final List<String> args;
        final boolean closing;

        TagInfo(String name, List<String> args, boolean closing) {
            this.name = name;
            this.args = args;
            this.closing = closing;
        }
    }

    private static final class StyleFrame {
        String color;
        String shadow;
        boolean bold;
        boolean italic;
        boolean underlined;
        boolean strikethrough;
        boolean obfuscated;
        List<String> hoverLines;
        String clickAction;
        String clickValue;
        String openTag;

        void reset() {
            color = null;
            shadow = null;
            bold = false;
            italic = false;
            underlined = false;
            strikethrough = false;
            obfuscated = false;
            hoverLines = null;
            clickAction = null;
            clickValue = null;
        }

        StyleFrame copy() {
            StyleFrame frame = new StyleFrame();
            frame.color = this.color;
            frame.shadow = this.shadow;
            frame.bold = this.bold;
            frame.italic = this.italic;
            frame.underlined = this.underlined;
            frame.strikethrough = this.strikethrough;
            frame.obfuscated = this.obfuscated;
            frame.hoverLines = this.hoverLines == null ? null : new ArrayList<>(this.hoverLines);
            frame.clickAction = this.clickAction;
            frame.clickValue = this.clickValue;
            return frame;
        }

        Span toSpan() {
            Span span = new Span();
            span.color = this.color;
            span.shadow = this.shadow;
            span.bold = this.bold;
            span.italic = this.italic;
            span.underlined = this.underlined;
            span.strikethrough = this.strikethrough;
            span.obfuscated = this.obfuscated;
            span.hoverLines = this.hoverLines == null ? null : new ArrayList<>(this.hoverLines);
            span.clickAction = this.clickAction;
            span.clickValue = this.clickValue;
            return span;
        }
    }

    static final class Span {
        String text;
        String color;
        String shadow;
        boolean bold;
        boolean italic;
        boolean underlined;
        boolean strikethrough;
        boolean obfuscated;
        List<String> hoverLines;
        String clickAction;
        String clickValue;

        Span copyWithText(String newText) {
            Span span = new Span();
            span.text = newText;
            span.color = this.color;
            span.shadow = this.shadow;
            span.bold = this.bold;
            span.italic = this.italic;
            span.underlined = this.underlined;
            span.strikethrough = this.strikethrough;
            span.obfuscated = this.obfuscated;
            span.hoverLines = this.hoverLines == null ? null : new ArrayList<>(this.hoverLines);
            span.clickAction = this.clickAction;
            span.clickValue = this.clickValue;
            return span;
        }
    }
}
