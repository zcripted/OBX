package dev.zcripted.obx.enchant.listener;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.enchant.item.EnchantItems;
import dev.zcripted.obx.enchant.service.EnchantService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Restyles <strong>vanilla</strong> enchantment tooltips into OBX's themed,
 * sectioned lore so that ordinary enchanted gear matches the look of Arcanum gear
 * (an "Enchantments / Curses / Vanilla" layout with tiered dot bars).
 *
 * <p>Custom Arcanum enchants are already styled by {@link
 * dev.zcripted.obx.enchant.storage.EnchantStorage} whenever they are
 * applied/removed; this listener covers the two ways a <em>vanilla</em>
 * enchantment lands on an item:
 * <ul>
 *   <li><b>Enchanting table</b> ({@link EnchantItemEvent}) — the chosen
 *   enchantments aren't on the item when the event fires, so they're passed to
 *   {@code refresh} explicitly via {@link EnchantItemEvent#getEnchantsToAdd()}.</li>
 *   <li><b>Anvil</b> ({@link PrepareAnvilEvent}) — the merged result already
 *   carries its enchantments, so the result preview is cloned and restyled.</li>
 * </ul>
 *
 * <p>Gated by {@code lore.style_vanilla_enchants} in {@code enchants/config.yml}.
 * Arcanum scrolls/books and the Arcanum anvil path are skipped so this never
 * interferes with the scroll system (which manages its own item layout).
 */
public final class EnchantLoreListener implements Listener {

    private final EnchantService service;
    private final EnchantItems items;

    public EnchantLoreListener(Main plugin) {
        this.service = plugin.getEnchantService();
        this.items = plugin.getEnchantItems();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!enabled()) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (items != null && items.isArcanumItem(item)) {
            return;
        }
        // Books store their enchantments separately (EnchantmentStorageMeta) and
        // HIDE_ENCHANTS doesn't hide those lines, so styling them would duplicate.
        if (item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK) {
            return;
        }
        // The enchantments are applied to the item after this event; merge the
        // pending set in so the styled lore matches what the player will receive.
        Map<Enchantment, Integer> toAdd = event.getEnchantsToAdd();
        service.getStorage().refresh(item, toAdd);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        if (!enabled()) {
            return;
        }
        ItemStack result = event.getResult();
        if (result == null || result.getType() == Material.AIR || !result.hasItemMeta()) {
            return;
        }
        AnvilInventory inventory = event.getInventory();
        // Leave the Arcanum scroll/book anvil path to its own listener.
        if (items != null && (items.isArcanumItem(inventory.getItem(0)) || items.isArcanumItem(inventory.getItem(1)))) {
            return;
        }
        boolean hasVanilla = result.getItemMeta() != null && result.getItemMeta().hasEnchants();
        boolean hasCustom = !service.getStorage().read(result).isEmpty();
        if (!hasVanilla && !hasCustom) {
            return; // a plain rename/repair — nothing to style
        }
        ItemStack styled = result.clone();
        service.getStorage().refresh(styled);
        event.setResult(styled);
    }

    private boolean enabled() {
        return service != null && service.isEnabled() && service.isStyleVanilla();
    }
}
