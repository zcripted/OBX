package dev.zcripted.obx.hologram.render;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;

/**
 * Tiny lifecycle wrapper around the renderer's per-tick callback. Schedules
 * itself via OBX's existing {@link SchedulerAdapter} so Folia's
 * region/global threading model is respected automatically — the renderer
 * does not need to know whether it's running on the main thread or a
 * region thread.
 *
 * <p>Period is read from {@code systems/holograms.yml → view-update-ticks}
 * (default 5 ticks = 4 Hz, matching the hub-launchpad cooldown action-bar
 * refresh rate). Lower values give snappier visibility transitions at the
 * cost of more per-tick work; the renderer's per-iteration cost is the
 * dominant factor so this is the main perf knob.
 */
public final class TickLoop {

    private final Main plugin;
    private final HologramRenderer renderer;
    private final long periodTicks;
    private SchedulerAdapter.CancellableTask handle;

    public TickLoop(Main plugin, HologramRenderer renderer, long periodTicks) {
        this.plugin = plugin;
        this.renderer = renderer;
        this.periodTicks = Math.max(1L, periodTicks);
    }

    public void start() {
        stop();
        handle = plugin.getSchedulerAdapter().runRepeating(new Runnable() {
            @Override
            public void run() {
                try {
                    renderer.tick();
                } catch (Throwable throwable) {
                    plugin.getLogger().warning("[Holograms] Tick loop error: " + throwable.getMessage());
                }
            }
        }, periodTicks, periodTicks);
    }

    public void stop() {
        if (handle != null) {
            try {
                handle.cancel();
            } catch (Throwable ignored) {
            }
            handle = null;
        }
    }
}
