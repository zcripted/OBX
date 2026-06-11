package dev.zcripted.obx.feature.hologram.backend;

import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Renderer-side contract. Implementations decide <em>how</em> a hologram is
 * realized in-world (display entities, armor stands, packets) but never own
 * per-hologram state — that lives on the {@link Hologram} model. This keeps
 * commands / persistence / animations / interactions agnostic to the chosen
 * backend so the plugin can pick at runtime via {@link BackendSelector}.
 *
 * <p>All operations execute on the main thread (or the entity / region thread
 * on Folia) — the {@code HologramRenderer} routes calls through the existing
 * {@code SchedulerAdapter}, so implementations may freely call Bukkit APIs.
 */
public interface HologramBackend {

    /**
     * Spawn the entities backing {@code hologram} and register their entity
     * ids back on the model via {@link Hologram#setEntityIds(java.util.List)}.
     * Implementations must show the new entities to every viewer in
     * {@code initialViewers} (and only to them).
     */
    void spawn(Hologram hologram, Collection<? extends Player> initialViewers);

    /** Flip a single viewer's visibility, idempotently. */
    void updateVisibility(Hologram hologram, Player viewer, boolean shouldSee);

    /**
     * Push any pending text / icon / scale / billboard changes to the live
     * entities. Implementations may early-exit when {@link Hologram#isDirty()}
     * is false; the renderer clears the flag after this call returns.
     */
    void applyMutations(Hologram hologram);

    /** Despawn for everyone, free entity ids. */
    void destroy(Hologram hologram);

    /**
     * True when the backend can render different text per viewer using its
     * own entity allocation strategy (the display-entity backend can, the
     * armor-stand backend cannot — see plan §7.4 / §8.2).
     */
    boolean supportsPerViewerText();

    /**
     * One-line diagnostic shown by {@code /holo info} and the startup
     * banner. Example: {@code "Display-entity backend (TextDisplay, Paper 1.21)"}.
     */
    String describe();
}