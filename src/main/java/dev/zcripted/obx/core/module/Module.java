package dev.zcripted.obx.core.module;

import dev.zcripted.obx.OBX;

/**
 * A self-contained feature of the plugin.
 *
 * <p>Each feature package (e.g. {@code feature/economy}) ships exactly one
 * {@code Module}. The module owns its feature's construction and wiring: it builds
 * the feature's services, registers them in the {@link dev.zcripted.obx.core.service.ServiceRegistry},
 * and registers its listeners and commands. The {@link ModuleManager} drives the
 * lifecycle in dependency order and can enable/disable a module at runtime.
 *
 * <p>Implementations should extend {@link AbstractModule}, which provides
 * book-keeping helpers ({@code listener(...)}, {@code command(...)}, {@code task(...)},
 * {@code service(...)}) and an automatic teardown so {@link #disable(OBX)} rarely
 * needs to be written by hand.
 */
public interface Module {

    /** Stable, lowercase identifier (e.g. {@code "economy"}). Used in config and the toggle UI. */
    String id();

    /** Human-facing label for the toggle UI (e.g. {@code "Economy"}). */
    default String displayName() {
        String s = id();
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Ids of modules that must be enabled before this one. The manager enables in this order. */
    default String[] dependsOn() {
        return new String[0];
    }

    /**
     * Whether this module is enabled when no explicit toggle exists in config.
     * Features gated behind their own {@code enabled: false} config flag (hub,
     * holograms) still load their module but stay dormant internally, so this
     * stays {@code true} for them.
     */
    default boolean enabledByDefault() {
        return true;
    }

    /** Construct services, register listeners/commands, start tasks. */
    void enable(OBX plugin);

    /** Tear down: unregister listeners, stop tasks, save state, drop services. */
    void disable(OBX plugin);

    /** Re-read configuration for a live module. Default is a no-op. */
    default void reload(OBX plugin) {
    }
}
