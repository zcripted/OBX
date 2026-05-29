package dev.sergeantfuzzy.sfcore.hologram.service;

import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import dev.sergeantfuzzy.sfcore.hologram.model.HologramId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory registry of every loaded hologram, plus the entity-id allocator
 * the backends draw from. Lookup by {@link HologramId} is concurrent-safe.
 *
 * <p>The entity-id allocator starts at {@link Integer#MAX_VALUE} / 2 and
 * decrements — keeping our packet-side ids in a range Bukkit-managed entities
 * are extremely unlikely to occupy in normal play. This is the same offset
 * trick the major hologram plugins use to avoid id collisions with real
 * entities. (Backends that spawn real entities ignore this allocator — they
 * use the entity ids the server assigns; the allocator is for backends that
 * would synthesize packet-only entities in a future tier.)
 */
public final class HologramRegistry {

    private final Map<HologramId, Hologram> holograms = new ConcurrentHashMap<>();
    private final Map<Integer, HologramId> entityIndex = new ConcurrentHashMap<>();
    private final AtomicInteger nextSyntheticEntityId = new AtomicInteger(Integer.MAX_VALUE / 2);

    public void register(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        holograms.put(hologram.getId(), hologram);
        rebuildEntityIndex(hologram);
    }

    public Hologram unregister(HologramId id) {
        if (id == null) {
            return null;
        }
        Hologram removed = holograms.remove(id);
        if (removed != null) {
            for (Integer entityId : removed.getEntityIds()) {
                entityIndex.remove(entityId);
            }
        }
        return removed;
    }

    public Hologram get(HologramId id) {
        return id == null ? null : holograms.get(id);
    }

    public boolean contains(HologramId id) {
        return id != null && holograms.containsKey(id);
    }

    public Collection<Hologram> all() {
        return Collections.unmodifiableCollection(new ArrayList<>(holograms.values()));
    }

    public int size() {
        return holograms.size();
    }

    /**
     * Resolve a Bukkit entity id to the hologram that owns it, or {@code null}
     * if the id belongs to an unrelated entity. Used by the packet layer
     * (Phase 4) to dispatch click events without scanning every hologram.
     */
    public HologramId resolveByEntityId(int entityId) {
        return entityIndex.get(entityId);
    }

    /** Re-index a hologram's current entity ids — call after backend spawn. */
    public void rebuildEntityIndex(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        List<Integer> ids = hologram.getEntityIds();
        for (Integer id : ids) {
            entityIndex.put(id, hologram.getId());
        }
    }

    public int allocateSyntheticEntityId() {
        return nextSyntheticEntityId.getAndDecrement();
    }

    public void clear() {
        holograms.clear();
        entityIndex.clear();
    }
}
