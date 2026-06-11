package dev.zcripted.obx.feature.deathdrop.util;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Tiny reflective bridge to the Bukkit Persistent Data Container API (entity PDC, 1.16+).
 *
 * <p>The plugin compiles against a 1.12 baseline, so {@code PersistentDataContainer} /
 * {@code PersistentDataType} aren't on the compile classpath — every call here is resolved through
 * reflection and degrades to a no-op / {@code null} when the API is absent (1.12–1.15). This lets the
 * death-drop feature persist a carry-all's contents on the dropped item entity so they survive a
 * server restart on modern servers, while older servers transparently fall back to in-memory tracking.
 */
public final class EntityPdc {

    private static final boolean AVAILABLE;
    private static final Method GET_CONTAINER;     // Entity#getPersistentDataContainer()
    private static final Method CONTAINER_SET;     // PersistentDataContainer#set(NamespacedKey, PersistentDataType, Object)
    private static final Method CONTAINER_GET;     // PersistentDataContainer#get(NamespacedKey, PersistentDataType)
    private static final Method CONTAINER_REMOVE;  // PersistentDataContainer#remove(NamespacedKey)
    private static final Object STRING_TYPE;       // PersistentDataType.STRING
    private static final Constructor<?> NK_CTOR;   // new NamespacedKey(Plugin, String)

    static {
        boolean ok = false;
        Method getContainer = null, set = null, get = null, remove = null;
        Object stringType = null;
        Constructor<?> nkCtor = null;
        try {
            Class<?> containerClass = Class.forName("org.bukkit.persistence.PersistentDataContainer");
            Class<?> typeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Class<?> nkClass = Class.forName("org.bukkit.NamespacedKey");
            getContainer = Entity.class.getMethod("getPersistentDataContainer");
            set = containerClass.getMethod("set", nkClass, typeClass, Object.class);
            get = containerClass.getMethod("get", nkClass, typeClass);
            remove = containerClass.getMethod("remove", nkClass);
            stringType = typeClass.getField("STRING").get(null);
            nkCtor = nkClass.getConstructor(Plugin.class, String.class);
            ok = getContainer != null && set != null && get != null && stringType != null && nkCtor != null;
        } catch (Throwable ignored) {
            ok = false;
        }
        AVAILABLE = ok;
        GET_CONTAINER = getContainer;
        CONTAINER_SET = set;
        CONTAINER_GET = get;
        CONTAINER_REMOVE = remove;
        STRING_TYPE = stringType;
        NK_CTOR = nkCtor;
    }

    private EntityPdc() {
    }

    /** Whether the entity PDC API is usable on this server (1.16+). */
    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /** Stores {@code value} under {@code key} on the entity's PDC. No-op if unavailable. */
    public static void setString(Plugin plugin, Entity entity, String key, String value) {
        if (!AVAILABLE || entity == null || value == null) {
            return;
        }
        try {
            Object container = GET_CONTAINER.invoke(entity);
            Object nk = NK_CTOR.newInstance(plugin, key);
            CONTAINER_SET.invoke(container, nk, STRING_TYPE, value);
        } catch (Throwable ignored) {
            // best-effort persistence
        }
    }

    /** Reads a string previously stored under {@code key}, or {@code null} if absent/unavailable. */
    public static String getString(Plugin plugin, Entity entity, String key) {
        if (!AVAILABLE || entity == null) {
            return null;
        }
        try {
            Object container = GET_CONTAINER.invoke(entity);
            Object nk = NK_CTOR.newInstance(plugin, key);
            Object value = CONTAINER_GET.invoke(container, nk, STRING_TYPE);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Removes {@code key} from the entity's PDC. No-op if unavailable. */
    public static void remove(Plugin plugin, Entity entity, String key) {
        if (!AVAILABLE || entity == null || CONTAINER_REMOVE == null) {
            return;
        }
        try {
            Object container = GET_CONTAINER.invoke(entity);
            Object nk = NK_CTOR.newInstance(plugin, key);
            CONTAINER_REMOVE.invoke(container, nk);
        } catch (Throwable ignored) {
            // best-effort
        }
    }
}