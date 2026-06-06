package dev.zcripted.obx.feature.hologram.render;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hologram.backend.HologramBackend;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.service.HologramRegistry;
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

    private final ObxPlugin plugin;
    private final HologramRegistry registry;
    private final HologramBackend backend;

    public HologramRenderer(ObxPlugin plugin, HologramRegistry registry, HologramBackend backend) {
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
        int orphans = dev.zcripted.obx.feature.hologram.HologramTag.scrub();
        if (orphans > 0) {
            plugin.getLogger().info("[Holograms] Removed " + orphans
                    + " orphaned hologram entit" + (orphans == 1 ? "y" : "ies") + " from a previous session.");
        }
        final Collection<? extends Player> online = plugin.getServer().getOnlinePlayers();
        for (Hologram hologram : registry.all()) {
            final Hologram h = hologram;
            runForHologram(hologram, () -> {
                backend.spawn(h, snapshotViewers(h, online));
                registry.rebuildEntityIndex(h);
            });
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
        final Collection<? extends Player> online = plugin.getServer().getOnlinePlayers();
        final long phaseTick = ++tickCounter;
        for (Hologram hologram : registry.all()) {
            final Hologram h = hologram;
            // Each hologram's animations/mutations/visibility (which spawn, move and remove backend
            // entities) run on THAT hologram's region thread under Folia, where entity mutation is
            // legal — the global tick loop only fans the work out, it never touches entities itself.
            runForHologram(hologram, () -> tickOne(h, online, phaseTick));
        }
    }

    /** Per-hologram tick body, run on the hologram's region thread under Folia. */
    private void tickOne(Hologram hologram, Collection<? extends Player> online, long phaseTick) {
        // Animations run before mutations so opacity/yaw changes land in the same tick they're requested.
        if (!hologram.getLiveAnimations().isEmpty()) {
            if (hologram.getAnimationStartTick() == 0L) {
                hologram.setAnimationStartTick(phaseTick);
            }
            long phase = phaseTick - hologram.getAnimationStartTick();
            for (dev.zcripted.obx.feature.hologram.anim.Animation anim : hologram.getLiveAnimations()) {
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

    /**
     * Runs entity-touching {@code work} for {@code hologram} on the hologram's own region thread under
     * Folia (where spawn/remove/teleport are legal), or inline on a regular Bukkit/Paper server. The
     * armor-stand backend creates real entities, so without this the global tick loop would hit Folia's
     * cross-region thread check and the holograms would silently never render.
     */
    private void runForHologram(Hologram hologram, Runnable work) {
        if (hologram == null) {
            return;
        }
        org.bukkit.Location loc = hologram.getLocation();
        if (plugin.getSchedulerAdapter() != null && plugin.getSchedulerAdapter().isFolia()
                && loc != null && loc.getWorld() != null) {
            plugin.getSchedulerAdapter().runAtLocation(loc, work);
        } else {
            work.run();
        }
    }

    private volatile long tickCounter = 0L;

    /**
     * Spawn (or re-spawn) a single hologram's entities now, with the correct
     * initial viewer set. Used by the chunk/world listener to bring a hologram
     * back when its chunk loads, without re-spawning the whole registry.
     */
    public void spawn(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        runForHologram(hologram, () -> {
            backend.spawn(hologram, snapshotViewers(hologram, plugin.getServer().getOnlinePlayers()));
            registry.rebuildEntityIndex(hologram);
        });
    }

    /**
     * Destroy a single hologram's live entities (without unregistering the model),
     * so the entities are not serialized into chunk data when the chunk unloads.
     */
    public void destroy(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        runForHologram(hologram, () -> backend.destroy(hologram));
    }

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
