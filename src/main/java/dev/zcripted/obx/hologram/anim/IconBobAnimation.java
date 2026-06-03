package dev.zcripted.obx.hologram.anim;

import dev.zcripted.obx.hologram.backend.HologramBackend;
import dev.zcripted.obx.hologram.model.Hologram;
import org.bukkit.Location;

/**
 * Sinusoidal Y-offset bob for icon lines. Implemented as a small position
 * delta applied to the hologram's stored location every {@code period-ticks}
 * ticks. Amplitude defaults to 0.15 blocks, frequency to one full cycle
 * every 40 ticks (2 seconds at 20 TPS).
 *
 * <p>Affects every line of the hologram (icons stack with text), which is
 * the intended behaviour for "bobbing welcome sign" style displays. Use
 * {@code period-ticks = 0} to lock the bob to once per tick.
 */
public final class IconBobAnimation implements Animation {

    private final double amplitude;
    private final long periodTicks;
    private final long cycleTicks;

    private Location anchor;

    public IconBobAnimation(AnimationConfig config) {
        this.amplitude = config.getDouble("amplitude", 0.15);
        this.periodTicks = Math.max(1L, config.getLong("period-ticks", 1L));
        this.cycleTicks = Math.max(1L, config.getLong("cycle-ticks", 40L));
    }

    @Override
    public void tick(Hologram hologram, HologramBackend backend, long ticksSinceStart) {
        if (hologram == null) {
            return;
        }
        if (ticksSinceStart % periodTicks != 0) {
            return;
        }
        if (anchor == null) {
            anchor = hologram.getLocation();
            if (anchor == null) {
                return;
            }
        }
        double phase = (2.0 * Math.PI * ticksSinceStart) / cycleTicks;
        double offset = Math.sin(phase) * amplitude;
        Location updated = anchor.clone();
        updated.setY(anchor.getY() + offset);
        hologram.setLocation(updated);
    }

    @Override
    public String name() {
        return "bob";
    }
}
