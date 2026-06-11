package dev.zcripted.obx.feature.enchant.scroll;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.gui.EnchantMenuHolder;
import dev.zcripted.obx.feature.enchant.item.EnchantItems;
import dev.zcripted.obx.feature.enchant.item.ScrollKind;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Drag-and-drop scroll interactions: pick a scroll up onto the cursor and click
 * the thing it targets.
 * <ul>
 *   <li>Enchant scroll / book → a matching gear item: apply (convenience path,
 *       costs extra XP vs. the anvil).</li>
 *   <li>Protection / Success scroll → an enchant scroll: imbue it.</li>
 *   <li>Extraction scroll → enchanted gear: pull one enchant back into a scroll.</li>
 *   <li>Transmutation scroll → enchanted gear: re-roll one enchant in-category.</li>
 * </ul>
 */
public final class ScrollDragListener implements Listener {

    private final ObxPlugin plugin;
    private final EnchantService service;
    private final EnchantItems items;
    private final ScrollApplyService scrolls;

    public ScrollDragListener(ObxPlugin plugin, ScrollApplyService scrolls) {
        this.plugin = plugin;
        this.service = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.service.EnchantService.class);
        this.items = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.item.EnchantItems.class);
        this.scrolls = scrolls;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        // Leave the Arcanum GUI clicks to its own listener.
        Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof EnchantMenuHolder) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (cursor == null || cursor.getType() == Material.AIR) {
            return;
        }
        ScrollKind kind = items.kindOf(cursor);
        if (kind == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        switch (kind) {
            case ENCHANT_SCROLL:
            case BOOK:
                if (isEnchantable(current)) {
                    ScrollResult res = scrolls.applyScroll(player, current, cursor, false);
                    finish(event, res, current);
                }
                return;
            case PROTECTION:
                if (isSingle(current) && items.kindOf(current) == ScrollKind.ENCHANT_SCROLL) {
                    ScrollResult res = scrolls.imbue(player, current, true);
                    finish(event, res, current);
                }
                return;
            case SUCCESS:
                if (isSingle(current) && items.kindOf(current) == ScrollKind.ENCHANT_SCROLL) {
                    ScrollResult res = scrolls.imbue(player, current, false);
                    finish(event, res, current);
                }
                return;
            case EXTRACTION:
                if (isEnchanted(current)) {
                    ScrollResult res = scrolls.extract(player, current);
                    finish(event, res, current);
                }
                return;
            case TRANSMUTATION:
                if (isEnchanted(current)) {
                    ScrollResult res = scrolls.transmute(player, current);
                    finish(event, res, current);
                }
                return;
            default:
        }
    }

    /** True only for a single item (stacks are rejected so one scroll can't enchant many). */
    private boolean isSingle(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getAmount() == 1;
    }

    /** A single gear item that isn't itself an Arcanum scroll/book. */
    private boolean isEnchantable(ItemStack item) {
        return isSingle(item) && items.kindOf(item) == null;
    }

    private boolean isEnchanted(ItemStack item) {
        return isEnchantable(item) && !service.getStorage().read(item).isEmpty();
    }

    private void finish(InventoryClickEvent event, ScrollResult result, ItemStack current) {
        event.setCancelled(true);
        if (result.consumesScroll()) {
            consumeCursor(event);
        }
        if (result.destroysTarget()) {
            event.setCurrentItem(null);
        } else {
            event.setCurrentItem(current);
        }
        if (event.getWhoClicked() instanceof Player) {
            ((Player) event.getWhoClicked()).updateInventory();
        }
    }

    @SuppressWarnings("deprecation")
    private void consumeCursor(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        if (cursor == null) {
            return;
        }
        if (cursor.getAmount() > 1) {
            cursor.setAmount(cursor.getAmount() - 1);
            event.setCursor(cursor);
        } else {
            event.setCursor(null);
        }
    }
}