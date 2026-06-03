package dev.zcripted.obx.hologram.text;

import java.lang.reflect.Method;

/**
 * Reflective adapter to Adventure's MiniMessage. The plugin does not depend
 * on Adventure at compile time — every type is loaded via {@link Class#forName}
 * so the same JAR runs on Spigot 1.12 (no Adventure) and on Paper 1.21
 * (Adventure built-in) without code-path changes.
 *
 * <p>On platforms where the probe fails, {@link #apply(String)} returns its
 * input unchanged so the legacy {@code &} renderer (run earlier in the
 * pipeline) still produces something readable.
 *
 * <p>Output is converted back to legacy section-codes via
 * {@code LegacyComponentSerializer.legacySection()} so backend-side
 * {@code setText(String)} setters can render gradients / hex / decorations
 * without requiring a Paper-only {@code setText(Component)} overload.
 */
public final class MiniMessageBridge {

    private static final ReflectionState STATE = new ReflectionState();

    private MiniMessageBridge() {
    }

    public static boolean available() {
        return STATE.ok;
    }

    public static String apply(String template) {
        if (template == null) {
            return "";
        }
        if (!STATE.ok) {
            return template;
        }
        try {
            Object component = STATE.deserialize.invoke(STATE.miniMessageInstance, template);
            Object serialized = STATE.legacySerialize.invoke(STATE.legacySerializerInstance, component);
            return serialized == null ? template : serialized.toString();
        } catch (Throwable ignored) {
            return template;
        }
    }

    private static final class ReflectionState {
        final boolean ok;
        final Object miniMessageInstance;
        final Object legacySerializerInstance;
        final Method deserialize;
        final Method legacySerialize;

        ReflectionState() {
            Object mmInstance = null;
            Object legacyInstance = null;
            Method deserializeMethod = null;
            Method legacyMethod = null;
            boolean okLocal = false;
            try {
                Class<?> mm = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
                mmInstance = mm.getMethod("miniMessage").invoke(null);
                Class<?> component = Class.forName("net.kyori.adventure.text.Component");
                deserializeMethod = mm.getMethod("deserialize", String.class);
                Class<?> legacy = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
                legacyInstance = legacy.getMethod("legacySection").invoke(null);
                legacyMethod = legacy.getMethod("serialize", component);
                okLocal = true;
            } catch (Throwable ignored) {
                okLocal = false;
            }
            ok = okLocal;
            miniMessageInstance = mmInstance;
            legacySerializerInstance = legacyInstance;
            deserialize = deserializeMethod;
            legacySerialize = legacyMethod;
        }
    }
}
