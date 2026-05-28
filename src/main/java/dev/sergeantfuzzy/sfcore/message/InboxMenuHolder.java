package dev.sergeantfuzzy.sfcore.message;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Holder for the inbox GUI. Maps raw inventory slots to the {@link InboxMessage} shown
 * there so the click listener can open the exact message clicked (no index drift).
 */
public final class InboxMenuHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, InboxMessage> bySlot = new HashMap<Integer, InboxMessage>();

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    void map(int slot, InboxMessage message) {
        bySlot.put(slot, message);
    }

    public InboxMessage forSlot(int slot) {
        return bySlot.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
