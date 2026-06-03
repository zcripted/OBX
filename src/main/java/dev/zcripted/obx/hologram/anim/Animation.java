package dev.zcripted.obx.hologram.anim;

import dev.zcripted.obx.hologram.backend.HologramBackend;
import dev.zcripted.obx.hologram.model.Hologram;

/**
 * Runtime interface for a hologram animation. Each tick, the renderer calls
 * {@link #tick(Hologram, HologramBackend, long)} with the absolute tick count
 * since the animation started; implementations translate the tick into a
 * concrete backend mutation (rotate transformation, change opacity,
 * shift Y offset, etc.).
 *
 * <p>Animations are stateless beyond the configuration captured by their
 * {@link AnimationConfig} — all per-instance state lives in the model
 * ({@code Hologram} dirty flag, transformation cache) so the renderer can
 * recreate them after a reload without losing their phase.
 */
public interface Animation {

    void tick(Hologram hologram, HologramBackend backend, long ticksSinceStart);

    String name();
}
