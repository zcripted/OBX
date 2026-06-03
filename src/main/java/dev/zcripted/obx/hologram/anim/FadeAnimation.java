package dev.zcripted.obx.hologram.anim;

import dev.zcripted.obx.hologram.backend.HologramBackend;
import dev.zcripted.obx.hologram.model.Hologram;
import dev.zcripted.obx.hologram.model.HologramSettings;

/**
 * Linearly ramps text opacity from 0 → 255 over the configured tick count
 * after the hologram spawns. Optional fade-out reverses the ramp after the
 * hologram's visible window.
 *
 * <p>Backend-aware: on the display-entity backend the opacity is a real
 * setting; on the armor-stand backend it's a no-op (the stand has no
 * opacity control — see plan §8.1).
 */
public final class FadeAnimation implements Animation {

    private final long fadeInTicks;
    private final long visibleTicks;
    private final long fadeOutTicks;

    public FadeAnimation(AnimationConfig config) {
        this.fadeInTicks = Math.max(1L, config.getLong("fade-in-ticks", 20L));
        this.visibleTicks = Math.max(0L, config.getLong("visible-ticks", 0L));
        this.fadeOutTicks = Math.max(0L, config.getLong("fade-out-ticks", 0L));
    }

    @Override
    public void tick(Hologram hologram, HologramBackend backend, long ticksSinceStart) {
        if (hologram == null) {
            return;
        }
        HologramSettings settings = hologram.getSettings();
        long phase = ticksSinceStart;
        int newOpacity;
        if (phase < fadeInTicks) {
            newOpacity = (int) ((255.0 * phase) / fadeInTicks);
        } else if (fadeOutTicks > 0 && phase >= fadeInTicks + visibleTicks
                && phase < fadeInTicks + visibleTicks + fadeOutTicks) {
            long fadePhase = phase - fadeInTicks - visibleTicks;
            newOpacity = (int) (255.0 - (255.0 * fadePhase) / fadeOutTicks);
        } else if (fadeOutTicks > 0 && phase >= fadeInTicks + visibleTicks + fadeOutTicks) {
            newOpacity = 0;
        } else {
            newOpacity = 255;
        }
        if (newOpacity != settings.getTextOpacity()) {
            settings.setTextOpacity(newOpacity);
            hologram.markDirty();
        }
    }

    @Override
    public String name() {
        return "fade";
    }
}
