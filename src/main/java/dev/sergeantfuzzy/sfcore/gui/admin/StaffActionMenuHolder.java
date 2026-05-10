package dev.sergeantfuzzy.sfcore.gui.admin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Marker holder for the {@code /staff} per-player action sub-menu. The
 * {@link #targetUuid} is the player the moderator action would apply to;
 * {@link #targetName} is the cached display name (so the click handler
 * doesn't need a re-lookup in case the target logs off mid-menu).
 */
public final class StaffActionMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private final UUID targetUuid;
    private final String targetName;

    public StaffActionMenuHolder(UUID targetUuid, String targetName) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }
}
