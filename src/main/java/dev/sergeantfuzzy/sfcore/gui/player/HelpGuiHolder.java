package dev.sergeantfuzzy.sfcore.gui.player;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker holder for the SF-Core help GUI inventory. Carries the current page,
 * total page count, and active category filter so the click listener can
 * compute pagination targets without parsing the inventory title.
 */
public final class HelpGuiHolder implements InventoryHolder {

    private final int page;
    private final int totalPages;
    private final String category;
    private Inventory inventory;

    public HelpGuiHolder(int page, int totalPages, String category) {
        this.page = page;
        this.totalPages = totalPages;
        this.category = category == null ? HelpGuiMenu.CATEGORY_ALL : category;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
