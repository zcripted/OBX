package dev.zcripted.obx.core.bootstrap;

import dev.zcripted.obx.core.platform.Platform;
import dev.zcripted.obx.core.platform.PlatformInfo;
import dev.zcripted.obx.core.platform.scheduler.Scheduler;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import org.bukkit.plugin.Plugin;

/**
 * Detects the running server software/version and selects the matching platform
 * implementations ({@link Platform} + {@link Scheduler}). This is the single
 * place that decides "Spigot vs Paper vs Folia vs Purpur".
 *
 * <p>Today it returns the universal reflective {@link SchedulerAdapter} (one jar,
 * Spigot 1.8.8 → Folia 1.21+). When native per-platform modules are introduced
 * (e.g. a Folia-API-compiled scheduler), this resolver is the only code that
 * needs to change to hand them out — callers already program against the
 * {@link Scheduler} / {@link Platform} interfaces.
 */
public final class PlatformResolver {

    private PlatformResolver() {
    }

    /** The detected platform capabilities/type. */
    public static Platform platform() {
        return PlatformInfo.get();
    }

    /** The scheduler implementation for the running platform. */
    public static Scheduler scheduler(Plugin plugin) {
        // Region-threaded (Folia) and global (Bukkit/Paper/Purpur) dispatch is
        // handled inside SchedulerAdapter via runtime detection.
        return new SchedulerAdapter(plugin);
    }
}