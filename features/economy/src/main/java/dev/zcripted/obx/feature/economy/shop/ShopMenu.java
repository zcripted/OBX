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
 *   <li><b>CATEGORY</b> — paginated 54-slot view (45 items per page): left-click opens
 *       the buy quantity menu, right-click the sell quantity menu; shift-left quick-buys
 *       a full stack, shift-right quick-sells everything carried.
 *       Nav row: back · prev · balance · next · close.</li>
 *   <li><b>QUANTITY</b> — per-item transaction menu: toggle buy/sell mode and adjust the
 *       amount by ±1/5/10/30/50, with a live receipt preview (unit price, total, balance,
 *       stock) on the centered item; confirm executes through the same guarded
 *       {@link ShopListener} paths as direct clicks.</li>
 *   <li><b>SELL</b> — an open 45-slot dump inventory; closing it sells everything
 *       priced in {@code worth.yml} and returns the rest.</li>
 * </ul>
 *
 * <p>Click dispatch + payout live in {@link ShopListener}.
 */
public final class ShopMenu {

    public enum ViewType { MAIN, CATEGORY, SELL, QUANTITY }

    public static final int CATEGORY_PAGE_SIZE = 45;
    public static final int NAV_BACK = 45;
    public static final int NAV_PREV = 48;
    public static final int NAV_BALANCE = 49;
    public static final int NAV_NEXT = 50;
    public static final int NAV_CLOSE = 53;

    // ── QUANTITY view slots (45-slot / 5-row layout) ─────────────────────────
    /** ±N step magnitudes, left → right (increase row 11–15, decrease row 29–33). */
    public static final int[] QTY_STEPS = {1, 5, 10, 30, 50};
    public static final int QTY_PLUS_FIRST = 11;
    public static final int QTY_MINUS_FIRST = 29;
    public static final int QTY_ITEM = 22;
    public static final int QTY_BACK = 36;
    public static final int QTY_TOGGLE = 38;
    public static final int QTY_CONFIRM = 40;
    public static final int QTY_BALANCE = 42;
    public static final int QTY_CLOSE = 44;
    /** Amount ceiling: a full 36-slot inventory of 64-stacks. */
    public static final int QTY_MAX = 2304;

    private ShopMenu() {
    }

    public static final class Holder extends MenuHolder {
        private final ViewType view;
        private final String categoryId;
        private final int page;
        // QUANTITY view state (mutated in place by ShopListener as buttons are clicked).
        private final int itemIndex;
        private int quantity;
        private boolean buying;

        Holder(ViewType view, String categoryId, int page) {
            this(view, categoryId, page, -1, 0, true);
        }

        Holder(ViewType view, String categoryId, int page, int itemIndex, int quantity, boolean buying) {
            this.view = view;
            this.categoryId = categoryId;
            this.page = page;
            this.itemIndex = itemIndex;
            this.quantity = quantity;
            this.buying = buying;
        }

        public ViewType view() { return view; }
        public String categoryId() { return categoryId; }
        public int page() { return page; }
        public int itemIndex() { return itemIndex; }
        public int quantity() { return quantity; }
        public boolean buying() { return buying; }

        void quantity(int value) { this.quantity = Math.max(1, Math.min(QTY_MAX, value)); }
        void buying(boolean value) { this.buying = value; }
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

        for (ShopCategory category : shop.categories()) {
            int slot = category.slot();
            if (slot < 0 || slot >= size - 9) {
                continue;
            }
            boolean hasPerm = player.hasPermission("obx.shop.category." + category.id());
            List<String> lore = new ArrayList<>();
            for (String line : category.lore()) {
                lore.add(color(line));
            }
            if (hasPerm) {
                lore.addAll(languages(plugin).list(player, "shop.gui.category.hint",
                        java.util.Collections.singletonMap("count", String.valueOf(category.items().size()))));
            } else {
                lore.add(color("&c&lLOCKED"));
                lore.add(color("&7You don't have access to this category"));
            }
            String displayName = hasPerm ? color(category.name()) : "&c&l" + category.name();
            inventory.setItem(slot, icon(category.iconMaterials(), displayName, lore));
        }

        // Bottom nav row: filler · filler · filler · sell tile · filler · balance card · filler · filler · close.
        int bottom = size - 9;
        inventory.setItem(bottom + 3, icon(new String[]{"CHEST"},
                languages(plugin).get(player, "shop.gui.sell-tile.name"),
                languages(plugin).list(player, "shop.gui.sell-tile.lore",
                        java.util.Collections.<String, String>emptyMap())));
        inventory.setItem(bottom + 5, balanceItem(plugin, player));
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

    // ── QUANTITY view (per-item buy/sell with ±1/5/10/30/50 amount steps) ─────

    public static void openQuantity(ObxPlugin plugin, Player player, String categoryId,
                                    int page, int itemIndex, boolean buying) {
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop == null || !shop.isEnabled()) {
            languages(plugin).send(player, "shop.disabled");
            return;
        }
        ShopCategory category = shop.getCategory(categoryId);
        if (category == null || itemIndex < 0 || itemIndex >= category.items().size()) {
            return;
        }
        ShopItem item = category.items().get(itemIndex);
        // Start at one bundle; mode falls back to whichever side is actually priced.
        if (buying && item.buyPrice() <= 0) buying = false;
        if (!buying && item.sellPrice() <= 0) buying = true;
        Holder holder = new Holder(ViewType.QUANTITY, categoryId, page, itemIndex,
                Math.max(1, item.amount()), buying);
        Inventory inventory = Bukkit.createInventory(holder, 45,
                languages(plugin).get(player, "shop.gui.qty.title",
                        java.util.Collections.singletonMap("material", prettyName(item.material()))));
        holder.setInventory(inventory);
        renderQuantity(plugin, player, holder);
        player.openInventory(inventory);
    }

    /** (Re-)renders the whole QUANTITY view in place — called on open and after every click. */
    public static void renderQuantity(ObxPlugin plugin, Player player, Holder holder) {
        Inventory inventory = holder.getInventory();
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (inventory == null || shop == null) {
            return;
        }
        ShopCategory category = shop.getCategory(holder.categoryId());
        if (category == null || holder.itemIndex() >= category.items().size()) {
            return;
        }
        ShopItem item = category.items().get(holder.itemIndex());
        LanguageManager lang = languages(plugin);
        fill(inventory);

        // ± step rows (stack size mirrors the step for at-a-glance scanning).
        for (int i = 0; i < QTY_STEPS.length; i++) {
            int step = QTY_STEPS[i];
            Map<String, String> n = java.util.Collections.singletonMap("n", String.valueOf(step));
            ItemStack plus = icon(new String[]{"LIME_STAINED_GLASS_PANE", "EMERALD"},
                    lang.get(player, "shop.gui.qty.plus.name", n),
                    lang.list(player, "shop.gui.qty.plus.lore", n));
            plus.setAmount(step > 64 ? 64 : step);
            inventory.setItem(QTY_PLUS_FIRST + i, plus);
            ItemStack minus = icon(new String[]{"RED_STAINED_GLASS_PANE", "REDSTONE"},
                    lang.get(player, "shop.gui.qty.minus.name", n),
                    lang.list(player, "shop.gui.qty.minus.lore", n));
            minus.setAmount(step > 64 ? 64 : step);
            inventory.setItem(QTY_MINUS_FIRST + i, minus);
        }

        // Centered item preview with the live receipt lore.
        dev.zcripted.obx.api.economy.EconomyService economy = plugin.getEconomyService();
        boolean buy = holder.buying();
        double unit = effectiveUnitPrice(plugin, player, item, buy);
        double total = dev.zcripted.obx.api.economy.EconomyService.sanitize(unit * holder.quantity());
        String mode = lang.get(player, buy ? "shop.gui.qty.mode.buy" : "shop.gui.qty.mode.sell");
        Map<String, String> info = new HashMap<>();
        info.put("material", prettyName(item.material()));
        info.put("amount", String.valueOf(holder.quantity()));
        info.put("mode", mode);
        info.put("unit", economy == null ? String.valueOf(unit) : economy.format(unit));
        info.put("total", (buy ? "&c-" : "&a+") + (economy == null ? String.valueOf(total) : economy.format(total)));
        info.put("balance", economy == null ? "?" : economy.format(economy.getBalance(player.getUniqueId())));
        List<String> lore = new ArrayList<>(lang.list(player, "shop.gui.qty.item.lore", info));
        if (item.stock() > 0) {
            int remaining = shop.stockRemaining(category.id(), item);
            Map<String, String> stockInfo = new HashMap<>();
            stockInfo.put("remaining", String.valueOf(remaining == Integer.MAX_VALUE ? item.stock() : remaining));
            stockInfo.put("max", String.valueOf(item.stock()));
            stockInfo.put("minutes", String.valueOf(shop.minutesToRestock()));
            lore.addAll(lang.list(player, "shop.gui.item.stock", stockInfo));
        }
        ItemStack preview = new ItemStack(item.material(),
                Math.max(1, Math.min(64, holder.quantity())));
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang.get(player, "shop.gui.qty.item.name", info));
            meta.setLore(lore);
            preview.setItemMeta(meta);
        }
        inventory.setItem(QTY_ITEM, preview);

        // Bottom row: back · mode toggle · confirm · balance · close.
        inventory.setItem(QTY_BACK, icon(new String[]{"ARROW", "SPECTRAL_ARROW"},
                lang.get(player, "shop.gui.back.name"),
                lang.list(player, "shop.gui.qty.back.lore", java.util.Collections.<String, String>emptyMap())));
        Map<String, String> toggleInfo = new HashMap<>(info);
        toggleInfo.put("other", lang.get(player, buy ? "shop.gui.qty.mode.sell" : "shop.gui.qty.mode.buy"));
        inventory.setItem(QTY_TOGGLE, icon(new String[]{"LEVER"},
                lang.get(player, "shop.gui.qty.toggle.name", toggleInfo),
                lang.list(player, "shop.gui.qty.toggle.lore", toggleInfo)));
        inventory.setItem(QTY_CONFIRM, icon(new String[]{"EMERALD_BLOCK"},
                lang.get(player, "shop.gui.qty.confirm.name"),
                lang.list(player, buy ? "shop.gui.qty.confirm.lore-buy" : "shop.gui.qty.confirm.lore-sell", info)));
        inventory.setItem(QTY_BALANCE, balanceItem(plugin, player));
        inventory.setItem(QTY_CLOSE, icon(new String[]{"BARRIER"},
                lang.get(player, "shop.gui.close.name"),
                lang.list(player, "shop.gui.close.lore", java.util.Collections.<String, String>emptyMap())));
    }

    /**
     * The effective per-single-unit price for {@code item}: buy = bundle price ÷ bundle size
     * × dynamic-pricing multiplier; sell = per-unit price × dynamic multiplier × sell boost.
     * Matches exactly what {@link ShopListener}'s transaction paths charge/pay.
     */
    public static double effectiveUnitPrice(ObxPlugin plugin, Player player, ShopItem item, boolean buy) {
        ShopPricing pricing = plugin.getServiceRegistry().get(ShopPricing.class);
        if (buy) {
            double perUnit = item.buyPrice() / Math.max(1, item.amount());
            return perUnit * (pricing == null ? 1.0 : pricing.buyMultiplier(item.material()));
        }
        return item.sellPrice()
                * (pricing == null ? 1.0 : pricing.sellMultiplier(item.material()))
                * dev.zcripted.obx.feature.economy.service.SellBoost.multiplier(player);
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
            inventory.setItem(inventory.getSize() - 9 + 5, balanceItem(plugin, player));
        } else if (holder.view() == ViewType.QUANTITY) {
            // Receipt totals, stock, and the balance card all change — redraw everything.
            renderQuantity(plugin, player, holder);
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