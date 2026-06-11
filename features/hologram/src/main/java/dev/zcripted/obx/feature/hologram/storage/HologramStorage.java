package dev.zcripted.obx.feature.hologram.storage;

import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.model.HologramId;

import java.util.Collection;

/**
 * Persistence boundary for the hologram subsystem. Implementations decide how
 * holograms are stored on disk; the rest of the module never sees IO. Mirrors
 * the {@code WarpService} / {@code MotdService} pattern of "one service-owned
 * file, atomic writes, safe defaults on missing keys".
 *
 * <p>Phase 2 ships {@link YamlHologramStorage}; an optional SQL backend behind
 * the same interface is left for a future phase per the plan.
 */
public interface HologramStorage {

    /** Read every hologram from disk. Returns an empty collection on first run. */
    Collection<Hologram> loadAll();

    /** Insert / overwrite a single hologram. */
    void save(Hologram hologram);

    /** Flush every supplied hologram in a single write. */
    void saveAll(Collection<Hologram> holograms);

    /** Remove a hologram from disk. */
    void delete(HologramId id);
}