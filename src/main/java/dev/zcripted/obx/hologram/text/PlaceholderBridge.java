package dev.zcripted.obx.hologram.text;

import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;

/**
 * Reflective adapter to PlaceholderAPI. OBX has no compile-time dependency
 * on the PAPI plugin — the bridge probes for {@code me.clip.placeholderapi.PlaceholderAPI}
 * at static init. When absent the bridge degrades to the identity function so
 * any template that uses placeholders just shows the raw {@code %placeholder%}
 * text. No exceptions are propagated.
 *
 * <p>Probed signature: {@code public static String setPlaceholders(OfflinePlayer, String)}.
 * Cached as a {@link Method} handle on success.
 */
public final class PlaceholderBridge {

    private static final Method SET_PLACEHOLDERS = resolveMethod();
    public static final boolean AVAILABLE = SET_PLACEHOLDERS != null;

    private PlaceholderBridge() {
    }

    public static String apply(OfflinePlayer player, String template) {
        if (template == null) {
            return "";
        }
        if (!AVAILABLE || player == null) {
            return template;
        }
        try {
            Object result = SET_PLACEHOLDERS.invoke(null, player, template);
            return result == null ? template : result.toString();
        } catch (Throwable ignored) {
            return template;
        }
    }

    private static Method resolveMethod() {
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return papi.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
