package dev.zcripted.obx.feature.enchant.gui;

import dev.zcripted.obx.feature.enchant.model.EnchantCategory;
import dev.zcripted.obx.core.gui.MenuHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Marker holder for the three Arcanum GUI screens (main menu, category list,
 * level selector). Carries the navigation context plus per-slot routing maps so
 * {@link EnchantMenuListener} can dispatch a click without re-parsing item NBT —
 * mirroring the hub {@code ServerSelectorHolder} idiom. Inventory plumbing is
 * inherited from {@link MenuHolder}.
 */
public final class EnchantMenuHolder extends MenuHolder {

    public enum Screen {
        MAIN, CATEGORY, LEVELS
    }

    public enum Action {
        OPEN_CATEGORY, OPEN_ENCHANT, APPLY_LEVEL, NEXT_PAGE, PREV_PAGE, BACK, CLOSE,
        GIVE_PROTECTION, GIVE_SUCCESS, GIVE_EXTRACTION, NOOP
    }

    private final Screen screen;
    private final boolean browse;

    private EnchantCategory category;
    private String enchantId;
    private int page = 1;

    private final Map<Integer, Action> actions = new HashMap<Integer, Action>();
    private final Map<Integer, EnchantCategory> categorySlots = new HashMap<Integer, EnchantCategory>();
    private final Map<Integer, String> enchantSlots = new HashMap<Integer, String>();
    private final Map<Integer, Integer> levelSlots = new HashMap<Integer, Integer>();

    public EnchantMenuHolder(Screen screen, boolean browse) {
        this.screen = screen;
        this.browse = browse;
    }

    public Screen getScreen() {
        return screen;
    }

    public boolean isBrowse() {
        return browse;
    }

    public EnchantCategory getCategory() {
        return category;
    }

    public void setCategory(EnchantCategory category) {
        this.category = category;
    }

    public String getEnchantId() {
        return enchantId;
    }

    public void setEnchantId(String enchantId) {
        this.enchantId = enchantId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void bindAction(int slot, Action action) {
        actions.put(slot, action);
    }

    public Action actionAt(int slot) {
        Action action = actions.get(slot);
        return action == null ? Action.NOOP : action;
    }

    public void bindCategory(int slot, EnchantCategory category) {
        categorySlots.put(slot, category);
        actions.put(slot, Action.OPEN_CATEGORY);
    }

    public EnchantCategory categoryAt(int slot) {
        return categorySlots.get(slot);
    }

    public void bindEnchant(int slot, String enchantId) {
        enchantSlots.put(slot, enchantId);
        actions.put(slot, Action.OPEN_ENCHANT);
    }

    public String enchantAt(int slot) {
        return enchantSlots.get(slot);
    }

    public void bindLevel(int slot, int level) {
        levelSlots.put(slot, level);
        actions.put(slot, Action.APPLY_LEVEL);
    }

    public int levelAt(int slot) {
        Integer level = levelSlots.get(slot);
        return level == null ? 0 : level;
    }
}
