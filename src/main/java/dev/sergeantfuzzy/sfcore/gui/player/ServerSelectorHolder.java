package dev.sergeantfuzzy.sfcore.gui.player;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Marker {@link InventoryHolder} used to identify the hub server-selector
 * inventory in click listeners (mirrors the existing
 * {@link MainMenuHolder} / {@link WarpMenuHolder} idiom).
 *
 * <p>Holds a mapping of inventory slot → server id so the click handler
 * can dispatch without re-parsing item NBT.
 */
public final class ServerSelectorHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, String> serverBySlot = new HashMap<>();
    private int closeSlot = -1;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void bindSlot(int slot, String serverId) {
        if (serverId == null) {
            return;
        }
        serverBySlot.put(slot, serverId);
    }

    public String serverFor(int slot) {
        return serverBySlot.get(slot);
    }

    public void setCloseSlot(int slot) {
        this.closeSlot = slot;
    }

    public boolean isCloseSlot(int slot) {
        return closeSlot >= 0 && slot == closeSlot;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
