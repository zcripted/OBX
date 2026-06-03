package dev.zcripted.obx.feature.hologram.api;

import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired immediately after a hologram is spawned in-world by the active
 * backend. Carries the model snapshot; downstream plugins can inspect the
 * id / location / lines but should not mutate the hologram from this
 * handler — mutations belong on the {@code HologramService} entry points
 * so dirty / save / re-spawn cycles stay consistent.
 *
 * <p>Part of the public Phase 7 dev API (plan §J).
 */
public final class HologramSpawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Hologram hologram;

    public HologramSpawnEvent(Hologram hologram) {
        this.hologram = hologram;
    }

    public Hologram getHologram() {
        return hologram;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
