package dev.zcripted.obx.core.platform.scheduler;

/**
 * Minimal task handle returned by all {@link Scheduler} methods. The Bukkit and
 * Folia task types share no common interface, so this exposes only the operations
 * OBX needs (cancel + isCancelled), letting each platform wrap its native handle.
 */
public interface CancellableTask {

    void cancel();

    boolean isCancelled();
}
