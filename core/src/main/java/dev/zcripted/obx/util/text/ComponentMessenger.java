package dev.zcripted.obx.util.text;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.zcripted.obx.util.ClassUtil;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ComponentMessenger {

    private static final String TEXT_COMPONENT_CLASS = "net.md_5.bungee.api.chat.TextComponent";
    private static final String BASE_COMPONENT_CLASS = "net.md_5.bungee.api.chat.BaseComponent";
    private static final String CHAT_MESSAGE_TYPE_CLASS = "net.md_5.bungee.api.ChatMessageType";
    private static final boolean BUNGEE_AVAILABLE = hasClass(TEXT_COMPONENT_CLASS);
    private static final String ADVENTURE_COMPONENT_CLASS = "net.kyori.adventure.text.Component";
    private static final String ADVENTURE_SERIALIZER_CLASS = "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer";
    private static final String ADVENTURE_HOVER_EVENT_CLASS = "net.kyori.adventure.text.event.HoverEvent";
    private static final String ADVENTURE_CLICK_EVENT_CLASS = "net.kyori.adventure.text.event.ClickEvent";
    private static final boolean ADVENTURE_AVAILABLE = hasClass(ADVENTURE_COMPONENT_CLASS);

    private ComponentMessenger() {
    }

    public static void sendHoverMessage(CommandSender sender, String message, List<String> hoverLines, String clickSuggestion) {
        sendHoverMessage(sender, message, hoverLines, clickSuggestion, false);
    }

    public static void sendHoverMessage(CommandSender sender, String message, List<String> hoverLines, String clickValue, boolean runCommand) {
        if (sender == null) {
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(message);
            return;
        }
        Player player = (Player) sender;
        if (sendBungeeMessage(player, message, hoverLines, clickValue, runCommand)) {
            return;
        }
        if (ADVENTURE_AVAILABLE && sendAdventureMessage(player, message, hoverLines, clickValue, runCommand)) {
            return;
        }
        player.sendMessage(message);
    }

    public static void sendJoinedHoverMessages(CommandSender sender, List<InteractiveMessagePart> parts) {
        if (sender == null || parts == null || parts.isEmpty()) {
            return;
        }
        List<InteractiveMessagePart> validParts = new ArrayList<>();
        for (InteractiveMessagePart part : parts) {
            if (part == null || part.message == null || part.message.isEmpty()) {
                continue;
            }
            validParts.add(part);
        }
        if (validParts.isEmpty()) {
            return;
        }
        String fallback = joinMessages(validParts);
        if (!(sender instanceof Player)) {
            sender.sendMessage(fallback);
            return;
        }
        Player player = (Player) sender;
        if (sendJoinedBungeeMessage(player, validParts)) {
            return;
        }
        player.sendMessage(fallback);
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null) {
            return;
        }
        // 1) Adventure's Audience#sendActionBar(Component) — the modern, reliable
        //    path on Paper 1.16+. The Spigot ChatMessageType.ACTION_BAR reflection
        //    (step 2) no longer resolves on recent Paper builds, which is why the
        //    launchpad countdown was leaking into chat via the old fallback.
        if (ADVENTURE_AVAILABLE && sendAdventureActionBar(player, message)) {
            return;
        }
        // 2) Spigot fallback: player.spigot().sendMessage(ChatMessageType.ACTION_BAR, ...).
        if (BUNGEE_AVAILABLE && sendSpigotActionBar(player, message)) {
            return;
        }
        // 3) No action-bar transport available. Intentionally NOT falling back to
        //    chat — this is called several times per second for the launchpad
        //    countdown and a chat fallback would flood the player's chat.
    }

    private static boolean sendAdventureActionBar(Player player, String message) {
        try {
            Object component = adventureDeserialize(message);
            if (component == null) {
                return false;
            }
            Class<?> componentClass = Class.forName(ADVENTURE_COMPONENT_CLASS);
            Method sendActionBar = null;
            for (Method method : player.getClass().getMethods()) {
                if ("sendActionBar".equals(method.getName()) && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(componentClass)) {
                    sendActionBar = method;
                    break;
                }
            }
            if (sendActionBar == null) {
                return false;
            }
            sendActionBar.invoke(player, component);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean sendSpigotActionBar(Player player, String message) {
        try {
            Object components = fromLegacyText(message);
            if (components == null) {
                return false;
            }
            Object spigot = player.spigot();
            Class<?> chatMessageType = Class.forName(CHAT_MESSAGE_TYPE_CLASS);
            Object actionBar = Enum.valueOf(castEnum(chatMessageType), "ACTION_BAR");
            Method sendMessage = spigot.getClass().getMethod("sendMessage", chatMessageType, baseComponentArrayClass());
            sendMessage.invoke(spigot, new Object[]{actionBar, components});
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object fromLegacyText(String text) throws Exception {
        Class<?> textComponent = Class.forName(TEXT_COMPONENT_CLASS);
        Method method = textComponent.getMethod("fromLegacyText", String.class);
        return method.invoke(null, text == null ? "" : text);
    }

    private static boolean sendBungeeMessage(Player player, String message, List<String> hoverLines, String clickValue, boolean runCommand) {
        try {
            BaseComponent[] components = TextComponent.fromLegacyText(message == null ? "" : message);
            if (components.length == 0) {
                return false;
            }
            HoverEvent hoverEvent = null;
            if (hoverLines != null && !hoverLines.isEmpty()) {
                String hoverText = String.join("\n", hoverLines);
                BaseComponent[] hoverComponents = TextComponent.fromLegacyText(hoverText);
                hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents);
                markLegacyHover(hoverEvent);
            }
            ClickEvent clickEvent = null;
            if (clickValue != null && !clickValue.isEmpty()) {
                clickEvent = new ClickEvent(runCommand ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, clickValue);
            }
            if (hoverEvent != null || clickEvent != null) {
                for (BaseComponent component : components) {
                    if (hoverEvent != null) {
                        component.setHoverEvent(hoverEvent);
                    }
                    if (clickEvent != null) {
                        component.setClickEvent(clickEvent);
                    }
                }
            }
            player.spigot().sendMessage(components);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean sendJoinedBungeeMessage(Player player, List<InteractiveMessagePart> parts) {
        try {
            List<BaseComponent> joined = new ArrayList<>();
            for (InteractiveMessagePart part : parts) {
                BaseComponent[] components = TextComponent.fromLegacyText(part.message == null ? "" : part.message);
                if (components.length == 0) {
                    continue;
                }
                HoverEvent hoverEvent = null;
                if (part.hoverLines != null && !part.hoverLines.isEmpty()) {
                    String hoverText = String.join("\n", part.hoverLines);
                    BaseComponent[] hoverComponents = TextComponent.fromLegacyText(hoverText);
                    hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents);
                    markLegacyHover(hoverEvent);
                }
                ClickEvent clickEvent = null;
                if (part.clickValue != null && !part.clickValue.isEmpty()) {
                    ClickEvent.Action action;
                    if (part.clickActionName != null) {
                        ClickEvent.Action resolved;
                        try {
                            resolved = ClickEvent.Action.valueOf(part.clickActionName);
                        } catch (IllegalArgumentException olderApi) {
                            resolved = ClickEvent.Action.SUGGEST_COMMAND; // e.g. COPY_TO_CLIPBOARD on <1.15
                        }
                        action = resolved;
                    } else {
                        action = part.runCommand ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;
                    }
                    clickEvent = new ClickEvent(action, part.clickValue);
                }
                if (hoverEvent != null || clickEvent != null) {
                    for (BaseComponent component : components) {
                        if (hoverEvent != null) {
                            component.setHoverEvent(hoverEvent);
                        }
                        if (clickEvent != null) {
                            component.setClickEvent(clickEvent);
                        }
                    }
                }
                Collections.addAll(joined, components);
            }
            if (joined.isEmpty()) {
                return false;
            }
            player.spigot().sendMessage(joined.toArray(new BaseComponent[0]));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String joinMessages(List<InteractiveMessagePart> parts) {
        StringBuilder builder = new StringBuilder();
        for (InteractiveMessagePart part : parts) {
            builder.append(part.message);
        }
        return builder.toString();
    }

    private static void markLegacyHover(HoverEvent hoverEvent) {
        try {
            Method setLegacy = hoverEvent.getClass().getMethod("setLegacy", boolean.class);
            setLegacy.invoke(hoverEvent, true);
        } catch (Exception ignored) {
            // Legacy not supported on older APIs.
        }
    }

    private static Class<?> baseComponentArrayClass() throws ClassNotFoundException {
        Class<?> baseComponent = Class.forName(BASE_COMPONENT_CLASS);
        return Array.newInstance(baseComponent, 0).getClass();
    }

    private static boolean sendAdventureMessage(Player player, String message, List<String> hoverLines, String clickValue, boolean runCommand) {
        try {
            Object component = adventureDeserialize(message);
            if (component == null) {
                return false;
            }
            if (hoverLines != null && !hoverLines.isEmpty()) {
                Object hoverComponent = adventureDeserialize(String.join("\n", hoverLines));
                Object hoverEvent = createAdventureHover(hoverComponent);
                component = applyAdventureEvent(component, "hoverEvent", ADVENTURE_HOVER_EVENT_CLASS, hoverEvent);
            }
            if (clickValue != null && !clickValue.isEmpty()) {
                Object clickEvent = createAdventureClick(clickValue, runCommand);
                component = applyAdventureEvent(component, "clickEvent", ADVENTURE_CLICK_EVENT_CLASS, clickEvent);
            }
            Method sendMessage = player.getClass().getMethod("sendMessage", Class.forName(ADVENTURE_COMPONENT_CLASS));
            sendMessage.invoke(player, component);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Object adventureDeserialize(String text) throws Exception {
        Class<?> serializerClass = Class.forName(ADVENTURE_SERIALIZER_CLASS);
        Method legacySection = serializerClass.getMethod("legacySection");
        Object serializer = legacySection.invoke(null);
        Method deserialize = serializerClass.getMethod("deserialize", String.class);
        return deserialize.invoke(serializer, text == null ? "" : text);
    }

    private static Object createAdventureHover(Object hoverComponent) throws Exception {
        if (hoverComponent == null) {
            return null;
        }
        Class<?> hoverEventClass = Class.forName(ADVENTURE_HOVER_EVENT_CLASS);
        Method showText = hoverEventClass.getMethod("showText", Class.forName(ADVENTURE_COMPONENT_CLASS));
        return showText.invoke(null, hoverComponent);
    }

    private static Object createAdventureClick(String value, boolean runCommand) throws Exception {
        Class<?> clickEventClass = Class.forName(ADVENTURE_CLICK_EVENT_CLASS);
        String methodName = runCommand ? "runCommand" : "suggestCommand";
        Method method = clickEventClass.getMethod(methodName, String.class);
        return method.invoke(null, value);
    }

    private static Object applyAdventureEvent(Object component, String methodName, String eventClassName, Object event) throws Exception {
        if (event == null) {
            return component;
        }
        Method method = component.getClass().getMethod(methodName, Class.forName(eventClassName));
        return method.invoke(component, event);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum> castEnum(Class<?> type) {
        return (Class<? extends Enum>) type;
    }

    private static boolean hasClass(String name) {
        return ClassUtil.hasClass(name);
    }

    public static final class InteractiveMessagePart {

        private final String message;
        private final List<String> hoverLines;
        private final String clickValue;
        private final boolean runCommand;
        /** Explicit bungee ClickEvent.Action name (e.g. COPY_TO_CLIPBOARD); null = use runCommand. */
        private final String clickActionName;

        private InteractiveMessagePart(String message, List<String> hoverLines, String clickValue, boolean runCommand, String clickActionName) {
            this.message = message;
            this.hoverLines = hoverLines;
            this.clickValue = clickValue;
            this.runCommand = runCommand;
            this.clickActionName = clickActionName;
        }

        public static InteractiveMessagePart plain(String message) {
            return new InteractiveMessagePart(message, null, null, false, null);
        }

        public static InteractiveMessagePart interactive(String message, List<String> hoverLines, String clickValue, boolean runCommand) {
            return new InteractiveMessagePart(message, hoverLines, clickValue, runCommand, null);
        }

        /** A click that copies {@code copyText} to the player's clipboard (1.15+; suggests it otherwise). */
        public static InteractiveMessagePart copy(String message, List<String> hoverLines, String copyText) {
            return new InteractiveMessagePart(message, hoverLines, copyText, false, "COPY_TO_CLIPBOARD");
        }

        /** A click that opens {@code url} in the player's browser. */
        public static InteractiveMessagePart openUrl(String message, List<String> hoverLines, String url) {
            return new InteractiveMessagePart(message, hoverLines, url, false, "OPEN_URL");
        }
    }
}