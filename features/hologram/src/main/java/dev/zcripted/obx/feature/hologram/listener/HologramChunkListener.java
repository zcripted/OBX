package dev.zcripted.obx.feature.hologram.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hologram.HologramTag;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.render.HologramRenderer;
import dev.zcripted.obx.feature.hologram.service.HologramService;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Keeps live hologram entities in lock-step with chunk/world residency so they
 * never leak as orphans.
 *
 * <p>Hologram entities are spawned with {@code setRemoveWhenFarAway(false)} and
 * are persistent, so if we let a chunk unload while a hologram entity lives in
 * it, the server serializes that entity into the chunk data. The next time the
 * chunk loads, the saved copy reappears — untracked by the backend — while the
 * renderer respawns a fresh tracked copy, doubling up. Over a long-running
 * server with normal chunk churn this slowly accumulates orphans (the boot-time
 * {@link HologramTag#scrub()} only cleans them at restart).</p>
 *
 * <p>The fix: despawn a hologram's entities the instant its chunk/world unloads
 * (so nothing is serialized), and respawn them when the chunk/world loads again
 * (scrubbing any orphan that a previous, pre-fix session may already have saved
 * into that chunk). The model stays in the registry throughout; only the live
 * entities come and go.</p>
 */
public final class HologramChunkListener implements Listener {

    private final ObxPlugin plugin;
    private final HologramService service;

    public HologramChunkListener(ObxPlugin plugin, HologramService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        HologramRenderer renderer = activeRenderer();
        if (renderer == null) {
            return;
        }
        Chunk chunk = event.getChunk();
        for (Hologram hologram : service.getRegistry().all()) {
            if (isInChunk(hologram, chunk)) {
                // Despawn before the chunk is written so the entity is never saved.
                renderer.destroy(hologram);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        HologramRenderer renderer = activeRenderer();
        if (renderer == null) {
            return;
        }
        Chunk chunk = event.getChunk();
        // Drop any tagged orphan a previous session saved into this chunk, then
        // respawn the live tracked copy.
        HologramTag.scrubChunk(chunk);
        for (Hologram hologram : service.getRegistry().all()) {
            if (isInChunk(hologram, chunk)) {
                renderer.spawn(hologram);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        HologramRenderer renderer = activeRenderer();
        if (renderer == null) {
            return;
        }
        World world = event.getWorld();
        for (Hologram hologram : service.getRegistry().all()) {
            if (isInWorld(hologram, world)) {
                renderer.destroy(hologram);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        HologramRenderer renderer = activeRenderer();
        if (renderer == null) {
            return;
        }
        World world = event.getWorld();
        for (Hologram hologram : service.getRegistry().all()) {
            if (isInWorld(hologram, world)) {
                renderer.spawn(hologram);
            }
        }
    }

    private HologramRenderer activeRenderer() {
        if (service == null || !service.isActive()) {
            return null;
        }
        return service.getRenderer();
    }

    private static boolean isInChunk(Hologram hologram, Chunk chunk) {
        Location loc = hologram.getLocation();
        World world = loc == null ? null : loc.getWorld();
        if (world == null || !world.equals(chunk.getWorld())) {
            return false;
        }
        return (loc.getBlockX() >> 4) == chunk.getX() && (loc.getBlockZ() >> 4) == chunk.getZ();
    }

    private static boolean isInWorld(Hologram hologram, World world) {
        Location loc = hologram.getLocation();
        World holoWorld = loc == null ? null : loc.getWorld();
        return holoWorld != null && holoWorld.equals(world);
    }
}