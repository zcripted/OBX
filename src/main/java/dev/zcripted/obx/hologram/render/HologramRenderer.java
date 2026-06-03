package dev.zcripted.obx.hologram.render;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.hologram.backend.HologramBackend;
import dev.zcripted.obx.hologram.model.Hologram;
import dev.zcripted.obx.hologram.service.HologramRegistry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Backend-agnostic per-tick entry point. Walks every registered hologram,
 * decides per-player visibility via {@link ViewerTracker}, and pushes mutation
 * updates whenever the model is dirty.
 *
 * <p>Renderer state lives entirely on the {@link Hologram} model and the
 * {@link HologramBackend} — the renderer itself is stateless across calls,
 * which is what lets the tick loop bind it to a region scheduler on Folia.
 */
public final class HologramRenderer {

    private final Main plugin;
    private final HologramRegistry registry;
    private final HologramBackend backend;

    public HologramRenderer(Main plugin, HologramRegistry registry, HologramBackend backend) {
        this.plugin = plugin;
        this.registry = registry;
        this.backend = backend;
    }

    public HologramBackend getBackend() {
        return backend;
    }

    /**
     * Spawn (or re-spawn) every registered hologram now. Called on service
     * activation and on reload.
     */
    public void spawnAll() {
        // Remove any OBX-tagged hologram entities left in the world by a prior
        // session/crash BEFORE spawning fresh, so a hard crash can't multiply
        // holograms across boots.
        int orphans = dev.zcripted.obx.hologram.HologramTag.scrub();
        if (orphans > 0) {
            plugin.getLogger().info("[Holograms] Removed " + orphans
                    + " orphaned hologram entit" + (orphans == 1 ? "y" : "ies") + " from a previous session.");
        }
        Collection<? extends Player> online = plugin.getServer().getOnlinePlayers();
        for (Hologram hologram : registry.all()) {
            backend.spawn(hologram, snapshotViewers(hologram, online));
            registry.rebuildEntityIndex(hologram);
        }
    }

    /**
     * Re-evaluate viewers for one hologram against a single player and push
     * the change to the backend. Used by join / respawn / world-change
     * listeners for immediate response without waiting for the next tick.
     */
    public void refreshFor(Hologram hologram, Player player) {
        if (hologram == null || player == null) {
            return;
        }
        boolean shouldSee = ViewerTracker.shouldSee(hologram, player);
        backend.updateVisibility(hologram, player, shouldSee);
    }

    /** Force-show / hide every hologram for one player (post-resource-pack reload). */
    public void resyncPlayer(Player player) {
        if (player == null) {
            return;
        }
        for (Hologram hologram : registry.all()) {
            refreshFor(hologram, player);
        }
    }

    /** Tick entry point — called by {@link TickLoop}. */
    public void tick() {
        Collection<? extends Player> online = plugin.getServer().getOnlinePlayers();
        tickCounter++;
        for (Hologram hologram : registry.all()) {
            // Animations run before mutations so opacity/yaw changes land in
            // the same tick they're requested.
            if (!hologram.getLiveAnimations().isEmpty()) {
                if (hologram.getAnimationStartTick() == 0L) {
                    hologram.setAnimationStartTick(tickCounter);
                }
                long phase = tickCounter - hologram.getAnimationStartTick();
                for (dev.zcripted.obx.hologram.anim.Animation anim : hologram.getLiveAnimations()) {
                    try {
                        anim.tick(hologram, backend, phase);
                    } catch (Throwable throwable) {
                        plugin.getLogger().warning("[Holograms] Animation "
                                + anim.name() + " errored: " + throwable.getMessage());
                    }
                }
            }
            if (hologram.isDirty()) {
                backend.applyMutations(hologram);
                registry.rebuildEntityIndex(hologram);
            }
            for (Player player : online) {
                boolean shouldSee = ViewerTracker.shouldSee(hologram, player);
                backend.updateVisibility(hologram, player, shouldSee);
            }
        }
    }

    private long tickCounter = 0L;

    /** Destroy every hologram's live entities. Called on service shutdown / reload. */
    public void destroyAll() {
        for (Hologram hologram : registry.all()) {
            backend.destroy(hologram);
        }
    }

    private Collection<Player> snapshotViewers(Hologram hologram, Collection<? extends Player> candidates) {
        List<Player> result = new ArrayList<>();
        for (Player player : candidates) {
            if (ViewerTracker.shouldSee(hologram, player)) {
                result.add(player);
            }
        }
        return result;
    }
}
