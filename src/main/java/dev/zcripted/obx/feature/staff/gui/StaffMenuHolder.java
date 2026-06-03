package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.core.gui.MenuHolder;

import java.util.List;
import java.util.UUID;

/**
 * Marker holder for the {@code /staff} main menu inventory. The
 * {@link #playerSlots} list maps inventory slot indices to the UUID of the
 * online player rendered at that slot, so the click handler can route a
 * click to the correct player without re-walking the GUI.
 */
public final class StaffMenuHolder extends MenuHolder {

    public static final int NO_SLOT = -1;

    private final List<UUID> playerSlots;
    private final int searchSlot;
    private final int closeSlot;
    private final int viewerSlot;
    private final int prevPageSlot;
    private final int nextPageSlot;
    private final int currentPage;
    private final int totalPages;

    public StaffMenuHolder(List<UUID> playerSlots,
                           int searchSlot,
                           int closeSlot,
                           int viewerSlot,
                           int prevPageSlot,
                           int nextPageSlot,
                           int currentPage,
                           int totalPages) {
        this.playerSlots = playerSlots;
        this.searchSlot = searchSlot;
        this.closeSlot = closeSlot;
        this.viewerSlot = viewerSlot;
        this.prevPageSlot = prevPageSlot;
        this.nextPageSlot = nextPageSlot;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }

    /**
     * Returns the UUID of the player rendered at {@code slot}, or {@code null}
     * if the slot doesn't carry a player head (filler glass, search head,
     * close head, viewer head, prev/next page button, or empty).
     */
    public UUID playerAt(int slot) {
        if (slot < 0 || slot >= playerSlots.size()) {
            return null;
        }
        return playerSlots.get(slot);
    }

    /**
     * Re-points a content slot to a player UUID (or {@code null} to clear it).
     * Used by the live refresh so clicks keep routing to the correct player
     * after the online list is re-rendered in place.
     */
    public void setPlayerAt(int slot, UUID uuid) {
        if (slot >= 0 && slot < playerSlots.size()) {
            playerSlots.set(slot, uuid);
        }
    }

    public int getSearchSlot() {
        return searchSlot;
    }

    public int getCloseSlot() {
        return closeSlot;
    }

    public int getViewerSlot() {
        return viewerSlot;
    }

    /** Slot of the previous-page button, or {@link #NO_SLOT} when not paginated / on page 0. */
    public int getPrevPageSlot() {
        return prevPageSlot;
    }

    /** Slot of the next-page button, or {@link #NO_SLOT} when not paginated / on the last page. */
    public int getNextPageSlot() {
        return nextPageSlot;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
