package dev.sergeantfuzzy.sfcore.gui.admin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AdminMenuHolder implements InventoryHolder {

    private Inventory inventory;

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
