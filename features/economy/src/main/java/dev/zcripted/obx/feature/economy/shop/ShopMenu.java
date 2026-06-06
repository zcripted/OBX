package dev.zcripted.obx.feature.economy.shop;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.gui.MenuHolder;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.feature.economy.shop.ShopService.ShopCategory;
import dev.zcripted.obx.feature.economy.shop.ShopService.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Player shop GUIs (EcoShop-style), rendered from {@link ShopService}'s YAML config:
 *
 * <ul>
 *   <li><b>MAIN</b> — category tiles (slot/icon/name/lore from {@code shop.yml}) plus a
 *       Sell-Items tile, a live balance card, and close.</li>
 *   <li><b>CATEGORY</b> — paginated 54-slot view (45 items per page): left-click buys one
 *       unit bundle, shift-left buys a stack, right-click sells one bundle,
 *       shift-right sells all you carry. Nav row: back · prev · balance · next · close.</li>
 *   <li><b>SELL</b> — an open 45-slot dump inventory; closing it sells everything
 *       priced in {@code worth.yml} and returns the rest.</li>
 * </ul>
 *
 * <p>Click dispatch + payout live in {@link ShopListener}.
 */
public final class ShopMenu {

    public enum ViewType { MAIN, CATEGORY, SELL }

    public static final int CATEGORY_PAGE_SIZE = 45;
    public static final int NAV_BACK = 45;
    public static final int NAV_PREV = 48;
    public static final int NAV_BALANCE = 49;
    public static final int NAV_NEXT = 50;
    public static final int NAV_CLOSE = 53;

    private ShopMenu() {
    }

    public static final class Holder extends MenuHolder {
        private final ViewType view;
        private final String categoryId;
        private final int page;

        Holder(ViewType view, String categoryId, int page) {
            this.view = view;
            this.categoryId = categoryId;
            this.page = page;
        }

        public ViewType view() { return view; }
        public String categoryId() { return categoryId; }
        public int page() { return page; }
    }

    private static LanguageManager languages(ObxPlugin plugin) {
        return plugin.getLanguageManager();
    }

    private static String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    // ── MAIN view ────────────────────────────────────────────────────────────

    public static void openMain(ObxPlugin plugin, Player player) {
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop == null || !shop.isEnabled()) {
            languages(plugin).send(player, "shop.disabled");
            return;
        }
        int size = shop.mainRows() * 9;
        Holder holder = new Holder(ViewType.MAIN, null, 0);
        Inventory inventory = Bukkit.createInventory(holder, size, color(shop.mainTitle()));
        holder.setInventory(inventory);
        fill(inventory);

        for (ShopCategory category : shop.getCategories()) {
            int slot = category.slot();
            if (slot < 0 || slot >= size - 9) {
                continue; // bottom row is reserved for nav
            }
            List<String> lore = new ArrayList<>();
            for (String line : category.lore()) {
                lore.add(color(line));
            }
            lore.addAll(languages(plugin).list(player, "shop.gui.category.hint",
                    java.util.Collections.singletonMap("count", String.valueOf(category.items().size()))));
            inventory.setItem(slot, icon(category.iconMaterials(), color(category.name()), lore));
        }

        // Bottom nav row: sell tile · balance card · close.
        int bottom = size - 9;
        inventory.setItem(bottom + 2, icon(new String[]{"CHEST"},
                languages(plugin).get(player, "shop.gui.sell-tile.name"),
                languages(plugin).list(player, "shop.gui.sell-tile.lore",
                        java.util.Collections.<String, String>emptyMap())));
        inventory.setItem(bottom + 4, balanceItem(plugin, player));
        inventory.setItem(size - 1, icon(new String[]{"BARRIER"},
                languages(plugin).get(player, "shop.gui.close.name"),
                languages(plugin).list(player, "shop.gui.close.lore",
                        java.util.Collections.<String, String>emptyMap())));
        player.openInventory(inventory);
    }

    // ── CATEGORY view ────────────────────────────────────────────────────────

    public static void openCategory(ObxPlugin plugin, Player player, String categoryId, int page) {
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop == null || !shop.isEnabled()) {
            languages(plugin).send(player, "shop.disabled");
            return;
        }
        ShopCategory category = shop.getCategory(categoryId);
        if (category == null) {
            languages(plugin).send(player, "shop.unknown-category",
                    java.util.Collections.singletonMap("category", String.valueOf(categoryId)));
            return;
        }
        int pages = Math.max(1, (int) Math.ceil(category.items().size() / (double) CATEGORY_PAGE_SIZE));
        int current = Math.max(0, Math.min(page, pages - 1));
        Holder holder = new Holder(ViewType.CATEGORY, category.id(), current);
        Inventory inventory = Bukkit.createInventory(holder, 54, color(category.title()));
        holder.setInventory(inventory);

        int start = current * CATEGORY_PAGE_SIZE;
        for (int i = 0; i < CATEGORY_PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= category.items().size()) {
                break;
            }
            inventory.setItem(i, shopItemStack(plugin, player, category, category.items().get(index)));
        }

        for (int slot = 45; slot < 54; slot++) {
            inventory.setItem(slot, filler());
        }
        inventory.setItem(NAV_BACK, icon(new String[]{"ARROW", "SPECTRAL_ARROW"},
                languages(plugin).get(player, "shop.gui.back.name"),
                languages(plugin).list(player, "shop.gui.back.lore",
                        java.util.Collections.<String, String>emptyMap())));
        Map<String, String> pageInfo = new HashMap<>();
        pageInfo.put("page", String.valueOf(current + 1));
        pageInfo.put("pages", String.valueOf(pages));
        if (current > 0) {
            inventory.setItem(NAV_PREV, icon(new String[]{"PAPER"},
                    languages(plugin).get(player, "shop.gui.prev.name", pageInfo),
                    languages(plugin).list(player, "shop.gui.prev.lore", pageInfo)));
        }
        inventory.setItem(NAV_BALANCE, balanceItem(plugin, player));
        if (current < pages - 1) {
            inventory.setItem(NAV_NEXT, icon(new String[]{"PAPER"},
                    languages(plugin).get(player, "shop.gui.next.name", pageInfo),
                    languages(plugin).list(player, "shop.gui.next.lore", pageInfo)));
        }
        inventory.setItem(NAV_CLOSE, icon(new String[]{"BARRIER"},
                languages(plugin).get(player, "shop.gui.close.name"),
                languages(plugin).list(player, "shop.gui.close.lore",
                        java.util.Collections.<String, String>emptyMap())));
        player.openInventory(inventory);
    }

    /** The item index behind a CATEGORY view slot, or {@code -1} for nav/filler. */
    public static int itemIndexAt(Holder holder, int slot) {
        if (holder.view() != ViewType.CATEGORY || slot < 0 || slot >= CATEGORY_PAGE_SIZE) {
            return -1;
        }
        return holder.page() * CATEGORY_PAGE_SIZE + slot;
    }

    // ── SELL view (dump inventory; payout on close in ShopListener) ──────────

    public static void openSellGui(ObxPlugin plugin, Player player) {
        Holder holder = new Holder(ViewType.SELL, null, 0);
        Inventory inventory = Bukkit.createInventory(holder, 45,
                languages(plugin).get(player, "shop.gui.sell.title"));
        holder.setInventory(inventory);
        languages(plugin).send(player, "shop.sell-gui.opened");
        player.openInventory(inventory);
    }

    // ── shared rendering helpers ─────────────────────────────────────────────

    private static ItemStack shopItemStack(ObxPlugin plugin, Player player,
                                           ShopService.ShopCategory category, ShopItem item) {
        dev.zcripted.obx.api.economy.EconomyService economy = plugin.getEconomyService();
        ItemStack stack = new ItemStack(item.material(), Math.max(1, item.amount()));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(languages(plugin).get(player, "shop.gui.item.name",
                java.util.Collections.singletonMap("material", prettyName(item.material()))));
        List<String> lore = new ArrayList<>(languages(plugin).list(player, "shop.gui.item.header",
                java.util.Collections.singletonMap("amount", String.valueOf(item.amount()))));
        if (item.buyPrice() > 0) {
            lore.addAll(languages(plugin).list(player, "shop.gui.item.buy", priceMap(plugin, economy, item, true)));
        }
        if (item.sellPrice() > 0) {
            lore.addAll(languages(plugin).list(player, "shop.gui.item.sell", priceMap(plugin, economy, item, false)));
        }
        // Finite stock: show what's left this restock window.
        if (item.stock() > 0) {
            ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
            int remaining = shop == null ? item.stock() : shop.stockRemaining(category.id(), item);
            Map<String, String> stockInfo = new HashMap<>();
            stockInfo.put("remaining", String.valueOf(remaining == Integer.MAX_VALUE ? item.stock() : remaining));
            stockInfo.put("max", String.valueOf(item.stock()));
            stockInfo.put("minutes", String.valueOf(shop == null ? 0 : shop.minutesToRestock()));
            lore.addAll(languages(plugin).list(player, "shop.gui.item.stock", stockInfo));
        }
        lore.add("");
        if (item.buyPrice() > 0) {
            lore.addAll(languages(plugin).list(player, "shop.gui.item.hint-buy",
                    java.util.Collections.<String, String>emptyMap()));
        }
        if (item.sellPrice() > 0) {
            lore.addAll(languages(plugin).list(player, "shop.gui.item.hint-sell",
                    java.util.Collections.<String, String>emptyMap()));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static Map<String, String> priceMap(ObxPlugin plugin,
                                                dev.zcripted.obx.api.economy.EconomyService economy,
                                                ShopItem item, boolean buy) {
        Map<String, String> map = new HashMap<>();
        // Display the EFFECTIVE price (dynamic-pricing multiplier applied) so what the
        // player sees is exactly what the transaction will charge/pay.
        ShopPricing pricing = plugin.getServiceRegistry().get(ShopPricing.class);
        double multiplier = pricing == null ? 1.0
                : (buy ? pricing.buyMultiplier(item.material()) : pricing.sellMultiplier(item.material()));
        double price = (buy ? item.buyPrice() : item.sellPrice() * item.amount()) * multiplier;
        price = dev.zcripted.obx.api.economy.EconomyService.sanitize(price);
        map.put("price", economy == null ? String.valueOf(price) : economy.format(price));
        map.put("amount", String.valueOf(item.amount()));
        return map;
    }

    private static ItemStack balanceItem(ObxPlugin plugin, Player player) {
        dev.zcripted.obx.api.economy.EconomyService economy = plugin.getEconomyService();
        String balance = economy == null ? "?" : economy.format(economy.getBalance(player.getUniqueId()));
        return icon(new String[]{"GOLD_INGOT"},
                languages(plugin).get(player, "shop.gui.balance.name"),
                languages(plugin).list(player, "shop.gui.balance.lore",
                        java.util.Collections.singletonMap("balance", balance)));
    }

    /** Re-renders the live balance card after a buy/sell without reopening the view. */
    public static void refreshBalance(ObxPlugin plugin, Player player, Holder holder) {
        Inventory inventory = holder.getInventory();
        if (inventory == null) {
            return;
        }
        if (holder.view() == ViewType.CATEGORY) {
            inventory.setItem(NAV_BALANCE, balanceItem(plugin, player));
        } else if (holder.view() == ViewType.MAIN) {
            inventory.setItem(inventory.getSize() - 9 + 4, balanceItem(plugin, player));
        }
    }

    /** Public: the auction GUI reuses this icon factory for its nav row. */
    public static ItemStack icon(String[] materials, String name, List<String> lore) {
        Material material = Material.STONE;
        for (String candidate : materials) {
            Material resolved = Material.matchMaterial(candidate);
            if (resolved != null) {
                material = resolved;
                break;
            }
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack filler() {
        Material material = Material.matchMaterial("GRAY_STAINED_GLASS_PANE");
        if (material == null) {
            material = Material.matchMaterial("STAINED_GLASS_PANE");
        }
        if (material == null) {
            material = Material.matchMaterial("THIN_GLASS");
        }
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static void fill(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler());
        }
    }

    public static String prettyName(Material material) {
        String[] words = material.name().toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }
}