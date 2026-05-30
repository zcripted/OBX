package dev.sergeantfuzzy.sfcore.hologram.model;

import dev.sergeantfuzzy.sfcore.hologram.anim.Animation;
import dev.sergeantfuzzy.sfcore.hologram.anim.AnimationConfig;
import dev.sergeantfuzzy.sfcore.hologram.anim.AnimationRegistry;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state for a single hologram. Mutable but addressed only through
 * the service / renderer pair, so callers never race on its fields directly.
 *
 * <p>The model carries:
 * <ul>
 *   <li>Identity ({@link HologramId});</li>
 *   <li>World position (a {@link Location} snapshot — mutating the returned
 *       location is harmless because mutators always clone);</li>
 *   <li>An ordered list of {@link HologramLine}s — the visual stack;</li>
 *   <li>{@link HologramSettings} (billboard / range / scale / visibility);</li>
 *   <li>Backend-allocated entity id list (filled by the chosen backend on spawn);</li>
 *   <li>Per-viewer state — which players currently see this hologram, and which
 *       players have explicitly hidden it (Phase 6).</li>
 * </ul>
 *
 * <p>{@code dirty} is the contract between command edits and the renderer: any
 * mutator flips it, the tick loop reads + clears it before pushing updates.
 */
public final class Hologram {

    private final HologramId id;
    private Location location;
    private final List<HologramLine> lines = new ArrayList<>();
    private final HologramSettings settings = new HologramSettings();

    /**
     * Bukkit entity ids allocated by the backend on {@link #spawn}. Used by
     * the packet layer to recognize click targets without touching the entity
     * objects themselves. Empty until the backend spawns the entities.
     */
    private final List<Integer> entityIds = new ArrayList<>();

    /** Players who currently see this hologram (snapshot, updated each tick). */
    private final Set<UUID> currentViewers = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    /** Players who have explicitly hidden this hologram via per-player toggle (Phase 6). */
    private final Set<UUID> personallyHidden = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    /** Configured animations (persisted) + live instances built from them. */
    private final List<AnimationConfig> animationConfigs = new ArrayList<>();
    private final List<Animation> liveAnimations = new ArrayList<>();
    private final long spawnedAtTick = 0L;
    private volatile long animationStartTick = 0L;

    private volatile boolean dirty = true;

    public Hologram(HologramId id, Location location) {
        if (id == null) {
            throw new IllegalArgumentException("HologramId required");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location required");
        }
        this.id = id;
        this.location = location.clone();
    }

    public HologramId getId() {
        return id;
    }

    public Location getLocation() {
        return location.clone();
    }

    public void setLocation(Location updated) {
        if (updated == null) {
            return;
        }
        this.location = updated.clone();
        this.dirty = true;
    }

    public List<HologramLine> getLines() {
        return Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public void addLine(HologramLine line) {
        if (line == null) {
            return;
        }
        lines.add(line);
        dirty = true;
    }

    public void insertLine(int index, HologramLine line) {
        if (line == null) {
            return;
        }
        int clamped = Math.max(0, Math.min(lines.size(), index));
        lines.add(clamped, line);
        dirty = true;
    }

    public boolean setLine(int index, HologramLine line) {
        if (line == null || index < 0 || index >= lines.size()) {
            return false;
        }
        lines.set(index, line);
        dirty = true;
        return true;
    }

    public boolean removeLine(int index) {
        if (index < 0 || index >= lines.size()) {
            return false;
        }
        lines.remove(index);
        dirty = true;
        return true;
    }

    public boolean swapLines(int a, int b) {
        if (a < 0 || b < 0 || a >= lines.size() || b >= lines.size() || a == b) {
            return false;
        }
        Collections.swap(lines, a, b);
        dirty = true;
        return true;
    }

    public void clearLines() {
        if (lines.isEmpty()) {
            return;
        }
        lines.clear();
        dirty = true;
    }

    public HologramSettings getSettings() {
        return settings;
    }

    public List<Integer> getEntityIds() {
        return Collections.unmodifiableList(new ArrayList<>(entityIds));
    }

    public void setEntityIds(List<Integer> ids) {
        entityIds.clear();
        if (ids != null) {
            entityIds.addAll(ids);
        }
    }

    public Set<UUID> getCurrentViewers() {
        return currentViewers;
    }

    public Set<UUID> getPersonallyHidden() {
        return personallyHidden;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    public List<AnimationConfig> getAnimationConfigs() {
        return Collections.unmodifiableList(new ArrayList<>(animationConfigs));
    }

    public void addAnimation(AnimationConfig config) {
        if (config == null) {
            return;
        }
        animationConfigs.add(config);
        if (config.isEnabled()) {
            Animation animation = AnimationRegistry.build(config);
            if (animation != null) {
                liveAnimations.add(animation);
            }
        }
        animationStartTick = 0L;
        dirty = true;
    }

    public boolean removeAnimation(int index) {
        if (index < 0 || index >= animationConfigs.size()) {
            return false;
        }
        animationConfigs.remove(index);
        // Rebuild the live list so it stays consistent with the (possibly
        // partially-disabled) config list — a 1:1 index removal would skew
        // when some configs are disabled.
        rebuildAnimations();
        dirty = true;
        return true;
    }

    public List<Animation> getLiveAnimations() {
        return Collections.unmodifiableList(new ArrayList<>(liveAnimations));
    }

    public void rebuildAnimations() {
        liveAnimations.clear();
        for (AnimationConfig cfg : animationConfigs) {
            if (!cfg.isEnabled()) {
                continue;
            }
            Animation animation = AnimationRegistry.build(cfg);
            if (animation != null) {
                liveAnimations.add(animation);
            }
        }
        animationStartTick = 0L;
    }

    public long getAnimationStartTick() {
        return animationStartTick;
    }

    public void setAnimationStartTick(long tick) {
        this.animationStartTick = tick;
    }
}
