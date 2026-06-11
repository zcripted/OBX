package dev.zcripted.obx.feature.hologram.text;

import dev.zcripted.obx.feature.hologram.model.HologramId;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player page cursor for hologram lines that contain {@code !nextpage!}
 * delimiters. Indexed by player UUID + hologram id so two players viewing the
 * same hologram see different pages.
 *
 * <p>Stored in a single static {@link ConcurrentHashMap} — light state that
 * doesn't need its own service, and the entries don't outlive a player's
 * session in any meaningful way (no save/load).
 */
public final class PageState {

    private static final ConcurrentHashMap<Key, Integer> CURSORS = new ConcurrentHashMap<>();

    private PageState() {
    }

    public static int current(UUID viewer, HologramId hologram) {
        if (viewer == null || hologram == null) {
            return 0;
        }
        Integer value = CURSORS.get(new Key(viewer, hologram));
        return value == null ? 0 : value;
    }

    public static void set(UUID viewer, HologramId hologram, int page) {
        if (viewer == null || hologram == null) {
            return;
        }
        CURSORS.put(new Key(viewer, hologram), Math.max(0, page));
    }

    public static int next(UUID viewer, HologramId hologram, int pageCount) {
        if (pageCount <= 1) {
            return 0;
        }
        int cur = current(viewer, hologram);
        int updated = (cur + 1) % pageCount;
        set(viewer, hologram, updated);
        return updated;
    }

    public static int prev(UUID viewer, HologramId hologram, int pageCount) {
        if (pageCount <= 1) {
            return 0;
        }
        int cur = current(viewer, hologram);
        int updated = (cur - 1 + pageCount) % pageCount;
        set(viewer, hologram, updated);
        return updated;
    }

    public static void clear(UUID viewer) {
        if (viewer == null) {
            return;
        }
        CURSORS.keySet().removeIf(key -> key.viewer.equals(viewer));
    }

    private static final class Key {
        private final UUID viewer;
        private final HologramId hologram;

        private Key(UUID viewer, HologramId hologram) {
            this.viewer = viewer;
            this.hologram = hologram;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) {
                return false;
            }
            Key that = (Key) other;
            return viewer.equals(that.viewer) && hologram.equals(that.hologram);
        }

        @Override
        public int hashCode() {
            return viewer.hashCode() * 31 + hologram.hashCode();
        }
    }
}