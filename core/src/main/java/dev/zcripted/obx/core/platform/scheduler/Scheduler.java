package dev.zcripted.obx.core.platform.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Platform-agnostic scheduling abstraction — the seam that unifies global
 * (Bukkit/Paper) and region/entity-aware (Folia) scheduling. The active
 * implementation is chosen by {@link dev.zcripted.obx.core.bootstrap.PlatformResolver}.
 *
 * <p>The shipped implementation, {@link SchedulerAdapter}, detects Folia at
 * runtime and dispatches reflectively, so a single jar runs on Spigot 1.8.8
 * through Folia 1.21+. Native per-platform implementations (compiled against the
 * Folia/Paper APIs) can be added as drop-in {@code Scheduler}s without touching
 * callers.
 */
public interface Scheduler {

    /** Whether this scheduler is running region-threaded (Folia). */
    boolean isFolia();

    CancellableTask runNow(Runnable task);

    CancellableTask runLater(Runnable task, long delayTicks);

    CancellableTask runRepeating(Runnable task, long initialDelayTicks, long periodTicks);

    CancellableTask runAsync(Runnable task);

    CancellableTask runAsyncLater(Runnable task, long delayTicks);

    CancellableTask runAtLocation(Location location, Runnable task);

    CancellableTask runAtEntity(Entity entity, Runnable task, Runnable retired);

    CancellableTask runAtEntity(Entity entity, Runnable task);

    CancellableTask runAtEntityLater(Entity entity, Runnable task, Runnable retired, long delayTicks);

    void cancelAll();
}