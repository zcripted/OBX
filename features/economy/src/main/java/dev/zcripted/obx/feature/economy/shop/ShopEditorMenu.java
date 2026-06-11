package dev.zcripted.obx.feature.economy.shop;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.gui.MenuHolder;
import dev.zcripted.obx.feature.economy.shop.ShopService.ShopCategory;
import dev.zcripted.obx.feature.economy.shop.ShopService.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Admin GUI editor for shop.yml categories and items. Accessed via /shop admin.
 * Admins can add/remove categories, add/remove items, and modify item prices.
 */
public final class ShopEditorMenu {

    public enum EditView { CATEGORIES, ITEMS }

    public static final int NAV_BACK = 45;
    public static final int NAV_ADD = 46;
    public static final int NAV_REMOVE = 48;
    public static final int NAV_SAVE = 49;
    public static final int NAV_CLOSE = 53;

    private ShopEditorMenu() {
    }

    public static final class Holder extends MenuHolder {
        private final EditView view;
        private final String categoryId;
        private final int page;

        Holder(EditView view, String categoryId, int page) {
            this.view = view;
            this.categoryId = categoryId;
            this.page = page;
        }

        public EditView editView() { return view; }
        public String categoryId() { return categoryId; }
        public int page() { return page; }
    }

    public static void openCategories(ObxPlugin plugin, Player player) {
        if (!player.hasPermission("obx.shop.admin")) {
            plugin.getLanguageManager().send(player, "core.no-permission");
            return;
        }
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop == null) {
            return;
        }
        List<ShopCategory> categories = shop.categories();
        Holder holder = new Holder(EditView.CATEGORIES, null, 0);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                plugin.getLanguageManager().get(player, "shop.editor.gui.title-categories"));
        holder.setInventory(inventory);

        int slot = 0;
        for (ShopCategory cat : categories) {
            if (slot >= 45) break;
            ItemStack iconItem = ShopMenu.icon(cat.iconMaterials(), "§6" + cat.id(), Collections.<String>emptyList());
            ItemMeta meta = iconItem.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add("§7Items: §f" + cat.items().size());
                lore.add("§7Slot: §f" + cat.slot());
                lore.add("");
                lore.add("§eClick to edit items");
                meta.setLore(lore);
                iconItem.setItemMeta(meta);
            }
            inventory.setItem(slot++, iconItem);
        }

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, ShopMenu.icon(new String[]{"GRAY_STAINED_GLASS_PANE",
                    "STAINED_GLASS_PANE", "THIN_GLASS", "STONE"}, " ", Collections.<String>emptyList()));
        }

        inventory.setItem(NAV_ADD, ShopMenu.icon(new String[]{"EMERALD"},
                plugin.getLanguageManager().get(player, "shop.editor.gui.add-category.name"),
                Collections.<String>emptyList()));
        inventory.setItem(NAV_SAVE, ShopMenu.icon(new String[]{"NETHER_STAR"},
                plugin.getLanguageManager().get(player, "shop.editor.gui.save.name"),
                plugin.getLanguageManager().list(player, "shop.editor.gui.save.lore",
                        Collections.<String, String>emptyMap())));
        inventory.setItem(NAV_CLOSE, ShopMenu.icon(new String[]{"BARRIER"},
                plugin.getLanguageManager().get(player, "shop.gui.close.name"),
                Collections.<String>emptyList()));

        player.openInventory(inventory);
    }

    public static void openItems(ObxPlugin plugin, Player player, String categoryId) {
        if (!player.hasPermission("obx.shop.admin")) {
            plugin.getLanguageManager().send(player, "core.no-permission");
            return;
        }
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop == null) {
            return;
        }
        ShopCategory cat = shop.categoryById(categoryId);
        if (cat == null) {
            plugin.getLanguageManager().send(player, "shop.editor.category-unknown");
            return;
        }
        Holder holder = new Holder(EditView.ITEMS, categoryId, 0);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                plugin.getLanguageManager().get(player, "shop.editor.gui.title-items",
                        Collections.singletonMap("category", categoryId)));
        holder.setInventory(inventory);

        int slot = 0;
        for (ShopItem shopItem : cat.items()) {
            if (slot >= 45) break;
            ItemStack display = ShopMenu.icon(new String[]{shopItem.material().name(), "STONE"},
                    "§f" + shopItem.material().name(), Collections.<String>emptyList());
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
                lore.add("§7Buy: §a" + (shopItem.buyPrice() > 0 ? String.format("%.2f", shopItem.buyPrice()) : "N/A"));
                lore.add("§7Sell: §a" + (shopItem.sellPrice() > 0 ? String.format("%.2f", shopItem.sellPrice()) : "N/A"));
                if (shopItem.amount() > 1) {
                    lore.add("§7Bundle size: §f" + shopItem.amount());
                }
                lore.add("");
                lore.add("§eLeft-click to set buy price");
                lore.add("§eRight-click to set sell price");
                lore.add("§eShift+click to remove");
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(slot++, display);
        }

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, ShopMenu.icon(new String[]{"GRAY_STAINED_GLASS_PANE",
                    "STAINED_GLASS_PANE", "THIN_GLASS", "STONE"}, " ", Collections.<String>emptyList()));
        }

        inventory.setItem(NAV_BACK, ShopMenu.icon(new String[]{"ARROW"},
                plugin.getLanguageManager().get(player, "shop.gui.back.name"),
                Collections.<String>emptyList()));
        inventory.setItem(NAV_ADD, ShopMenu.icon(new String[]{"EMERALD"},
                plugin.getLanguageManager().get(player, "shop.editor.gui.add-item.name"),
                Collections.<String>emptyList()));
        inventory.setItem(NAV_SAVE, ShopMenu.icon(new String[]{"NETHER_STAR"},
                plugin.getLanguageManager().get(player, "shop.editor.gui.save.name"),
                plugin.getLanguageManager().list(player, "shop.editor.gui.save.lore",
                        Collections.<String, String>emptyMap())));
        inventory.setItem(NAV_CLOSE, ShopMenu.icon(new String[]{"BARRIER"},
                plugin.getLanguageManager().get(player, "shop.gui.close.name"),
                Collections.<String>emptyList()));

        player.openInventory(inventory);
    }
}