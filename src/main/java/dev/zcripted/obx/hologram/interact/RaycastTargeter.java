package dev.zcripted.obx.hologram.interact;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.hologram.model.Hologram;
import dev.zcripted.obx.hologram.packet.InteractDecoder;
import dev.zcripted.obx.hologram.packet.PacketAvailability;
import dev.zcripted.obx.hologram.service.HologramService;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback interaction trigger used when the Netty packet layer is
 * unavailable (or as the only path on older / non-Paper forks). Runs a tick
 * loop at ~4 Hz, raycasts each online player's look direction against the
 * configured interaction box of every hologram, and dispatches an
 * {@code INTERACT} action when a fresh hover lasts more than 250 ms.
 *
 * <p>This is the documented ~0.25 s "look-tracking latency" called out in
 * the plan (§F final bullet, §9 row 4).
 */
public final class RaycastTargeter {

    private final Main plugin;
    private final HologramService service;
    private final long periodTicks;
    private SchedulerAdapter.CancellableTask handle;

    // Tracks the timestamp at which each (player, hologram) hover started.
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Long>> hoverStart = new ConcurrentHashMap<>();

    public RaycastTargeter(Main plugin, HologramService service, long periodTicks) {
        this.plugin = plugin;
        this.service = service;
        this.periodTicks = Math.max(1L, periodTicks);
    }

    public void start() {
        stop();
        handle = plugin.getSchedulerAdapter().runRepeating(new Runnable() {
            @Override
            public void run() {
                try {
                    tick();
                } catch (Throwable throwable) {
                    PacketAvailability.noteFailure(plugin, throwable);
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

    private void tick() {
        if (service == null || !service.isActive()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ConcurrentHashMap<UUID, Long> perPlayer = hoverStart.computeIfAbsent(player.getUniqueId(),
                    uuid -> new ConcurrentHashMap<>());
            Location eye = player.getEyeLocation();
            Vector dir = eye.getDirection().normalize();
            for (Hologram hologram : service.getRegistry().all()) {
                if (!hologram.getSettings().isInteractionEnabled()) {
                    continue;
                }
                if (!matches(hologram, eye, dir)) {
                    perPlayer.remove(hologramUuid(hologram));
                    continue;
                }
                UUID key = hologramUuid(hologram);
                Long start = perPlayer.get(key);
                if (start == null) {
                    perPlayer.put(key, now);
                } else if (now - start > 250L) {
                    perPlayer.remove(key);
                    // Synthesize an INTERACT action — raycast can't tell left from right.
                    InteractDecoder.Decoded decoded = new InteractDecoder.Decoded(
                            hologram.getEntityIds().isEmpty() ? -1 : hologram.getEntityIds().get(0),
                            InteractDecoder.Action.INTERACT,
                            player.isSneaking());
                    InteractionDispatcher.dispatch(plugin, service, player, hologram.getId(), decoded);
                }
            }
        }
    }

    private boolean matches(Hologram hologram, Location eye, Vector dir) {
        Location target = hologram.getLocation();
        if (target.getWorld() == null || !target.getWorld().equals(eye.getWorld())) {
            return false;
        }
        Vector delta = target.toVector().subtract(eye.toVector());
        double along = delta.dot(dir);
        if (along < 0.0 || along > Math.max(2.0, hologram.getSettings().getShowRange())) {
            return false;
        }
        Vector projected = dir.clone().multiply(along);
        double perp = delta.clone().subtract(projected).length();
        double radius = Math.max(hologram.getSettings().getInteractionWidth(),
                hologram.getSettings().getInteractionHeight()) / 2.0;
        return perp <= radius;
    }

    private UUID hologramUuid(Hologram hologram) {
        return UUID.nameUUIDFromBytes(hologram.getId().value().getBytes());
    }
}
