package dev.zcripted.obx.feature.enchant.scroll;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.item.EnchantItems;
import dev.zcripted.obx.feature.enchant.item.ScrollKind;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.service.ApplyResult;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * The traditional anvil path for the scroll system: place gear in the first
 * slot and an enchant scroll / book in the second, take the result. The preview
 * is shown via {@link PrepareAnvilEvent}; the actual roll, XP cost, protection,
 * and destroy-on-failure happen when the result is taken, routed through
 * {@link ScrollApplyService}.
 */
public final class AnvilEnchantListener implements org.bukkit.event.Listener {

    private final ObxPlugin plugin;
    private final EnchantService service;
    private final EnchantItems items;
    private final ScrollApplyService scrolls;

    public AnvilEnchantListener(ObxPlugin plugin, ScrollApplyService scrolls) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.items = plugin.getEnchantItems();
        this.scrolls = scrolls;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareAnvilEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        AnvilInventory inv = event.getInventory();
        ItemStack gear = inv.getItem(0);
        ItemStack scroll = inv.getItem(1);
        if (!isApplicable(scroll) || gear == null || gear.getType() == Material.AIR) {
            return;
        }
        CustomEnchant enchant = items.payloadEnchant(scroll);
        int level = items.payloadLevel(scroll);
        if (enchant == null || level <= 0) {
            return;
        }
        ApplyResult probe = service.apply(gear.clone(), enchant, level);
        if (!probe.isSuccess()) {
            return;
        }
        ItemStack preview = gear.clone();
        service.getStorage().apply(preview, enchant, level);
        event.setResult(preview);
        try {
            inv.setRepairCost(0); // XP is charged by ScrollApplyService, not the vanilla anvil.
        } catch (Throwable ignored) {
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top == null || top.getType() != InventoryType.ANVIL || !(top instanceof AnvilInventory)) {
            return;
        }
        if (event.getRawSlot() != 2) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        AnvilInventory inv = (AnvilInventory) top;
        ItemStack gear = inv.getItem(0);
        ItemStack scroll = inv.getItem(1);
        ItemStack result = inv.getItem(2);
        if (result == null || result.getType() == Material.AIR || !isApplicable(scroll)
                || gear == null || gear.getType() == Material.AIR) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        ScrollResult outcome = scrolls.applyScroll(player, gear, scroll, true);
        if (outcome == ScrollResult.REJECTED || outcome == ScrollResult.NO_XP) {
            return;
        }
        // Consume one scroll from the second slot.
        reduceOne(inv, 1);
        inv.setItem(2, null);
        if (outcome.destroysTarget()) {
            inv.setItem(0, null);
        } else {
            // gear was mutated in place (enchanted, or untouched on fail-kept) — hand it back.
            inv.setItem(0, null);
            giveOrDrop(player, gear);
        }
        player.updateInventory();
    }

    private boolean isApplicable(ItemStack scroll) {
        ScrollKind kind = items.kindOf(scroll);
        return kind == ScrollKind.ENCHANT_SCROLL || kind == ScrollKind.BOOK;
    }

    private void reduceOne(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item == null) {
            return;
        }
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            inv.setItem(slot, item);
        } else {
            inv.setItem(slot, null);
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
}
