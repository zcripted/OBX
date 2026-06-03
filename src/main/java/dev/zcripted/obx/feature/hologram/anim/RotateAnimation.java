package dev.zcripted.obx.feature.hologram.anim;

import dev.zcripted.obx.feature.hologram.backend.HologramBackend;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.Location;

/**
 * Rotates the hologram around its Y axis at the configured rate. On the
 * display-entity backend this manifests as a continuous left-rotation
 * quaternion update. On the armor-stand backend, rotation is emulated by
 * teleporting the stands at small yaw increments (lower quality — same
 * effect, less smooth).
 */
public final class RotateAnimation implements Animation {

    private final double degreesPerTick;
    private final long periodTicks;

    public RotateAnimation(AnimationConfig config) {
        this.periodTicks = Math.max(1L, config.getLong("period-ticks", 1L));
        this.degreesPerTick = config.getDouble("degrees-per-tick", 4.5);
    }

    @Override
    public void tick(Hologram hologram, HologramBackend backend, long ticksSinceStart) {
        if (hologram == null) {
            return;
        }
        if (ticksSinceStart % periodTicks != 0) {
            return;
        }
        Location current = hologram.getLocation();
        if (current == null) {
            return;
        }
        Location updated = current.clone();
        updated.setYaw((float) ((current.getYaw() + degreesPerTick) % 360.0));
        hologram.setLocation(updated);
        // markDirty already happens inside setLocation.
    }

    @Override
    public String name() {
        return "rotate";
    }
}
