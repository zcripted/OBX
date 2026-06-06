package dev.zcripted.obx.core.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Shared base for OBX menu {@link InventoryHolder}s.
 *
 * <p>Every OBX menu holder used to repeat the same three lines — a private
 * {@code Inventory} field plus {@code setInventory}/{@code getInventory}. That
 * boilerplate now lives here once. Concrete subclasses keep only the state that
 * makes their menu unique (slot maps, page indices, target UUIDs, …); the
 * <em>type</em> of the subclass is still what click listeners match on with
 * {@code instanceof} to identify which menu was clicked.</p>
 */
public abstract class MenuHolder implements InventoryHolder {

    private Inventory inventory;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}