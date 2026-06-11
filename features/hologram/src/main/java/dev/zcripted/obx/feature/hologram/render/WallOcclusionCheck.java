package dev.zcripted.obx.feature.hologram.render;

import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional line-of-sight check used when {@code hide-behind-walls = true}.
 * Walks a discretised ray from the viewer's eye to the hologram origin and
 * returns {@code true} if every step is in air / non-occluding material.
 *
 * <p>To avoid running a full raycast every tick per (player, hologram) pair,
 * the result is cached for a configurable window (default 5 ticks). Cache
 * entries are bounded by player count × hologram count so memory stays sane
 * even on large servers.
 */
public final class WallOcclusionCheck {

    private static final ConcurrentHashMap<Key, Cached> CACHE = new ConcurrentHashMap<>();

    private WallOcclusionCheck() {
    }

    public static boolean canSee(Player player, Hologram hologram, int cacheTicks) {
        if (player == null || hologram == null) {
            return true;
        }
        Key key = new Key(player.getUniqueId(), hologram.getId().value());
        long now = System.currentTimeMillis();
        long ttlMs = Math.max(50L, cacheTicks * 50L);
        Cached cached = CACHE.get(key);
        if (cached != null && now - cached.timestamp < ttlMs) {
            return cached.value;
        }
        boolean result = raycast(player, hologram);
        CACHE.put(key, new Cached(result, now));
        return result;
    }

    private static boolean raycast(Player player, Hologram hologram) {
        Location eye = player.getEyeLocation();
        Location target = hologram.getLocation();
        if (eye.getWorld() == null || target.getWorld() == null
                || !eye.getWorld().equals(target.getWorld())) {
            return false;
        }
        Vector delta = new Vector(target.getX() - eye.getX(),
                target.getY() - eye.getY(),
                target.getZ() - eye.getZ());
        double distance = delta.length();
        if (distance <= 0.1) {
            return true;
        }
        Vector step = delta.clone().multiply(1.0 / Math.max(1, (int) Math.ceil(distance * 2.0)));
        int steps = (int) Math.ceil(distance * 2.0);
        Location probe = eye.clone();
        for (int i = 0; i < steps; i++) {
            probe.add(step);
            try {
                if (probe.getBlock().getType().isOccluding()) {
                    return false;
                }
            } catch (Throwable ignored) {
                // Old API — fall back to "isSolid". Either result is
                // acceptable; a missing API just means more lenient occlusion.
                if (probe.getBlock().getType().isSolid()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void evict(UUID player) {
        if (player == null) {
            return;
        }
        CACHE.keySet().removeIf(k -> k.player.equals(player));
    }

    private static final class Key {
        final UUID player;
        final String hologramId;

        Key(UUID player, String hologramId) {
            this.player = player;
            this.hologramId = hologramId;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) {
                return false;
            }
            Key that = (Key) other;
            return player.equals(that.player) && hologramId.equals(that.hologramId);
        }

        @Override
        public int hashCode() {
            return player.hashCode() * 31 + hologramId.hashCode();
        }
    }

    private static final class Cached {
        final boolean value;
        final long timestamp;

        Cached(boolean value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}