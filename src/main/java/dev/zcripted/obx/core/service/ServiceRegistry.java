package dev.zcripted.obx.core.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Type-keyed container for the plugin's shared services.
 *
 * <p>Feature modules register their services here during {@code enable()}; the
 * thin {@link dev.zcripted.obx.OBX} bootstrap and its getters look them up. This
 * decouples service ownership (a feature module) from service access (anywhere in
 * the codebase via {@code plugin.getXService()}), so the bootstrap no longer needs
 * to know how to construct every subsystem.
 *
 * <p>Not thread-safe by design: registration happens on the main thread during
 * enable/disable, and lookups are reads of an already-populated map.
 */
public final class ServiceRegistry {

    private final Map<Class<?>, Object> services = new LinkedHashMap<>();

    /** Registers (or replaces) the instance stored under {@code type}. */
    public <T> void register(Class<T> type, T instance) {
        services.put(type, instance);
    }

    /** Returns the instance registered under {@code type}, or {@code null} if absent. */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        return (T) services.get(type);
    }

    /** Removes the instance registered under {@code type} (used on module disable). */
    public void unregister(Class<?> type) {
        services.remove(type);
    }

    /** Whether a service of {@code type} is currently registered. */
    public boolean has(Class<?> type) {
        return services.containsKey(type);
    }

    /** Clears every registered service (used on full plugin disable). */
    public void clear() {
        services.clear();
    }
}
