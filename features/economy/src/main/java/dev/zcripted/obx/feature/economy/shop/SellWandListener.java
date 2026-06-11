package dev.zcripted.obx.feature.economy.shop;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.economy.service.WorthService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Sell-all-from-chest wand: a named item (configurable material) that, when a
 * player right-clicks a chest, sells every item in the chest that has a worth
 * price and drops unsellable items at the player's feet.
 *
 * <p>Not a persistent "wand" in the plugin sense — any item with the correct
 * display name in any player's hand triggers the behaviour, so admins can give
 * themselves the wand at any time via {@code /obx givewand} or creative.
 *
 * <p>Config: {@code economy.sell.wand-item} (material name) and
 * {@code economy.sell.wand-name} (display name with &amp; codes).
 * Permission: {@code obx.shop.sellwand}.
 */
public final class SellWandListener implements Listener {

    private final ObxPlugin plugin;

    public SellWandListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Material chestType = event.getClickedBlock().getType();
        if (chestType != Material.matchMaterial("CHEST") && chestType != Material.matchMaterial("TRAPPED_CHEST")) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("obx.shop.sellwand")) {
            return;
        }
        ItemStack held = dev.zcripted.obx.util.compat.InventoryCompat.mainHand(player);
        if (!isWandItem(held)) {
            return;
        }
        event.setCancelled(true);

        Inventory chestInv;
        if (event.getClickedBlock().getState() instanceof Chest) {
            Chest chest = (Chest) event.getClickedBlock().getState();
            if (chest.getInventory().getHolder() instanceof DoubleChest) {
                chestInv = ((DoubleChest) chest.getInventory().getHolder()).getInventory();
            } else {
                chestInv = chest.getInventory();
            }
        } else {
            return;
        }

        EconomyService economy = plugin.getEconomyService();
        WorthService worth = plugin.getServiceRegistry().get(WorthService.class);
        if (economy == null) {
            return;
        }

        double total = 0.0;
        int soldCount = 0;
        int unsellableCount = 0;

        for (int i = 0; i < chestInv.getSize(); i++) {
            ItemStack item = chestInv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            double price = worth != null ? worth.getPrice(item.getType()) : 0.0;
            if (price <= 0.0) {
                unsellableCount += item.getAmount();
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                chestInv.setItem(i, null);
                continue;
            }
            double value = EconomyService.sanitize(price * item.getAmount());
            economy.deposit(player.getUniqueId(), player.getName(), value);
            total += value;
            soldCount += item.getAmount();
            chestInv.setItem(i, null);
            economy.logTransaction("SELL_WAND", player.getUniqueId(), player.getName(),
                    "SELL_WAND", value, economy.getBalance(player.getUniqueId()));
        }

        java.util.Map<String, String> placeholders = new HashMap<>();
        if (soldCount > 0) {
            placeholders.put("amount", economy.format(total));
            placeholders.put("count", String.valueOf(soldCount));
            placeholders.put("unsold", String.valueOf(unsellableCount));
            plugin.getLanguageManager().send(player, "economy.sell.wand-sold", placeholders);
        } else if (unsellableCount > 0) {
            placeholders.put("count", String.valueOf(unsellableCount));
            plugin.getLanguageManager().send(player, "economy.sell.wand-nothing-sold", placeholders);
        } else {
            plugin.getLanguageManager().send(player, "economy.sell.wand-empty");
        }
    }

    private boolean isWandItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        String wandMaterial = plugin.getConfig().getString("economy.sell.wand-item", "BLAZE_ROD");
        Material expected = Material.matchMaterial(wandMaterial);
        if (expected == null) {
            expected = Material.BLAZE_ROD;
        }
        if (item.getType() != expected) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String wandName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("economy.sell.wand-name", "&6&lSell &e&lWand"));
        return meta.getDisplayName().equals(wandName);
    }
}