package dev.sergeantfuzzy.sfcore.gui.player;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker holder for the SF-Core main menu inventory so listeners can identify it reliably.
 */
public final class MainMenuHolder implements InventoryHolder {

    private Inventory inventory;

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
