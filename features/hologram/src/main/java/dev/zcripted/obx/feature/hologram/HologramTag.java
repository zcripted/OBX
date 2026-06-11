package dev.zcripted.obx.feature.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Restart-surviving marker for OBX-spawned hologram entities, plus a startup
 * orphan scrub.
 *
 * <p>Hologram entities are tracked in-memory by the backends; a hard crash (no
 * clean {@code onDisable}) loses that map but the entities — armor stands spawn
 * with {@code setRemoveWhenFarAway(false)} — persist in the world, so the next
 * boot's {@code spawnAll()} would spawn a duplicate set. To prevent that, every
 * spawned entity is tagged here, and {@link #scrub()} removes any tagged entity
 * still present before a fresh spawn.</p>
 *
 * <p>Uses {@code Entity#addScoreboardTag}/{@code getScoreboardTags} (1.13+),
 * which persist in entity NBT across restarts. Resolved reflectively because the
 * plugin compiles against the 1.12.2 API. On 1.8–1.12 (no scoreboard tags) this
 * degrades to a no-op — tagging and scrubbing simply do nothing there.</p>
 */
public final class HologramTag {

    public static final String TAG = "obx_hologram";

    private static final Method ADD;   // Entity#addScoreboardTag(String)
    private static final Method GET;   // Entity#getScoreboardTags()

    static {
        Method add = null;
        Method get = null;
        try {
            add = Entity.class.getMethod("addScoreboardTag", String.class);
        } catch (Throwable ignored) {
            // pre-1.13
        }
        try {
            get = Entity.class.getMethod("getScoreboardTags");
        } catch (Throwable ignored) {
            // pre-1.13
        }
        ADD = add;
        GET = get;
    }

    private HologramTag() {
    }

    /** Marks an entity as OBX-owned (no-op on pre-1.13). */
    public static void tag(Entity entity) {
        if (entity == null || ADD == null) {
            return;
        }
        try {
            ADD.invoke(entity, TAG);
        } catch (Throwable ignored) {
            // non-fatal
        }
    }

    public static boolean isTagged(Entity entity) {
        if (entity == null || GET == null) {
            return false;
        }
        try {
            Object tags = GET.invoke(entity);
            return tags instanceof Collection && ((Collection<?>) tags).contains(TAG);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Removes every OBX-tagged entity from every loaded world — orphans left by a
     * prior session/crash. Returns the count removed. No-op on pre-1.13.
     */
    public static int scrub() {
        if (GET == null) {
            return 0;
        }
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isTagged(entity)) {
                    try {
                        entity.remove();
                        removed++;
                    } catch (Throwable ignored) {
                        // skip
                    }
                }
            }
        }
        return removed;
    }

    /**
     * Removes every OBX-tagged entity from a single chunk. Cheap enough to run on
     * {@code ChunkLoadEvent} (unlike {@link #scrub()}, which walks all worlds), so a
     * hologram entity that was saved into chunk data by a previous session can't
     * reappear as an untracked duplicate when the chunk loads. Returns the count
     * removed. No-op on pre-1.13.
     */
    public static int scrubChunk(Chunk chunk) {
        if (GET == null || chunk == null) {
            return 0;
        }
        int removed = 0;
        for (Entity entity : chunk.getEntities()) {
            if (isTagged(entity)) {
                try {
                    entity.remove();
                    removed++;
                } catch (Throwable ignored) {
                    // skip
                }
            }
        }
        return removed;
    }
}