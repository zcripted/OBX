package dev.zcripted.obx.core.gui.help;

import dev.zcripted.obx.core.gui.MenuHolder;

/**
 * Marker holder for the OBX help GUI inventory. Carries the current page,
 * total page count, and active category filter so the click listener can
 * compute pagination targets without parsing the inventory title.
 */
public final class HelpGuiHolder extends MenuHolder {

    private final int page;
    private final int totalPages;
    private final String category;

    public HelpGuiHolder(int page, int totalPages, String category) {
        this.page = page;
        this.totalPages = totalPages;
        this.category = category == null ? HelpGuiMenu.CATEGORY_ALL : category;
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
}
