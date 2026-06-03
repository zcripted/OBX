package dev.zcripted.obx.gui.admin;

import dev.zcripted.obx.gui.MenuHolder;

import java.util.UUID;

/**
 * Marker holder for the {@code /invsee} live-mirror GUI. Carries the target
 * player UUID + a GUI-slot → target-slot map so the per-tick refresher can
 * copy individual slots from the target's {@link org.bukkit.entity.Player#getInventory()}
 * back into the viewer's chest without re-walking the layout each frame.
 *
 * <p>{@code slotMap[guiSlot] = -1} marks a non-mirrored cell (separator row,
 * filler glass, close button). Anything {@code >= 0} is the corresponding
 * raw {@link org.bukkit.inventory.PlayerInventory} slot index.
 */
public final class InvSeeMenuHolder extends MenuHolder {

    public static final int UNMAPPED = -1;

    private final UUID targetUuid;
    private final String targetName;
    private final int[] slotMap;
    private final int closeSlot;

    public InvSeeMenuHolder(UUID targetUuid, String targetName, int[] slotMap, int closeSlot) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.slotMap = slotMap;
        this.closeSlot = closeSlot;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public int[] getSlotMap() {
        return slotMap;
    }

    public int getCloseSlot() {
        return closeSlot;
    }
}
