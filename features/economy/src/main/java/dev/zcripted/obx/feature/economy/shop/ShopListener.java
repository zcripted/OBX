package dev.zcripted.obx.feature.economy.shop;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.economy.service.WorthService;
import dev.zcripted.obx.feature.economy.shop.ShopService.ShopCategory;
import dev.zcripted.obx.feature.economy.shop.ShopService.ShopItem;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shop GUI behaviour:
 *
 * <ul>
 *   <li><b>MAIN / CATEGORY</b> — read-only menus (all movement cancelled).
 *       Category items: left-click buys one bundle, shift-left buys a full stack,
 *       right-click sells one bundle, shift-right sells everything carried. Money
 *       moves through {@link EconomyService} with remove-first dupe guards and
 *       every trade is written to the {@code economy_log} audit trail.</li>
 *   <li><b>SELL</b> — a free dump inventory; on close everything priced in
 *       {@code worth.yml} is sold (one payout, one audit entry) and unsellable
 *       items are handed back (overflow dropped at the player's feet).</li>
 * </ul>
 */
public final class ShopListener implements Listener {

    /** How many sold item types to surface in the bulk-sale hover tooltip. */
    private static final int BULK_HOVER_LIMIT = 8;

    private final ObxPlugin plugin;

    public ShopListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof ShopMenu.Holder)) {
            return;
        }
        ShopMenu.Holder holder = (ShopMenu.Holder) top.getHolder();
        if (holder.view() == ShopMenu.ViewType.SELL) {
            return; // free dump inventory — vanilla movement allowed
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (holder.view() == ShopMenu.ViewType.MAIN) {
            handleMainClick(player, top, slot);
            return;
        }
        if (holder.view() == ShopMenu.ViewType.QUANTITY) {
            handleQuantityClick(player, holder, slot);
            return;
        }
        handleCategoryClick(player, holder, slot, event.isShiftClick(), event.isRightClick());
    }

    private void handleMainClick(Player player, Inventory top, int slot) {
        int size = top.getSize();
        int bottom = size - 9;
        if (slot == size - 1) {
            player.closeInventory();
            return;
        }
        if (slot == bottom + 3) {
            if (!player.hasPermission("obx.shop.sell")) {
                plugin.getLanguageManager().send(player, "core.no-permission");
                return;
            }
            ShopMenu.openSellGui(plugin, player);
            return;
        }
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop == null || slot < 0 || slot >= bottom) {
            return;
        }
        for (ShopCategory category : shop.categories()) {
            if (category.slot() == slot) {
                if (!player.hasPermission("obx.shop.category." + category.id())) {
                    plugin.getLanguageManager().send(player, "shop.no-category-permission");
                    return;
                }
                ShopMenu.openCategory(plugin, player, category.id(), 0);
                return;
            }
        }
    }

    private void handleCategoryClick(Player player, ShopMenu.Holder holder, int slot,
                                     boolean shift, boolean right) {
        if (slot == ShopMenu.NAV_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == ShopMenu.NAV_BACK) {
            ShopMenu.openMain(plugin, player);
            return;
        }
        if (slot == ShopMenu.NAV_PREV) {
            ShopMenu.openCategory(plugin, player, holder.categoryId(), holder.page() - 1);
            return;
        }
        if (slot == ShopMenu.NAV_NEXT) {
            ShopMenu.openCategory(plugin, player, holder.categoryId(), holder.page() + 1);
            return;
        }
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop == null) {
            return;
        }
        ShopCategory category = shop.getCategory(holder.categoryId());
        int index = ShopMenu.itemIndexAt(holder, slot);
        if (category == null || index < 0 || index >= category.items().size()) {
            return;
        }
        ShopItem item = category.items().get(index);
        if (shift) {
            // Power-user shortcuts: shift-left quick-buys a full stack, shift-right
            // quick-sells everything carried (the pre-quantity-menu behaviour).
            if (right) {
                sellUnits(player, holder, category, item, Integer.MAX_VALUE);
            } else {
                int bundles = Math.max(1, 64 / Math.max(1, item.amount()));
                buyUnits(player, holder, category, item, bundles * item.amount());
            }
            return;
        }
        // Plain click: open the per-item quantity menu (left = buy mode, right = sell mode).
        if (item.buyPrice() <= 0 && item.sellPrice() <= 0) {
            plugin.getLanguageManager().send(player, "shop.not-buyable");
            return;
        }
        ShopMenu.openQuantity(plugin, player, category.id(), holder.page(), index, !right);
    }

    /** QUANTITY view clicks: ± steps, buy/sell mode toggle, confirm, back, close. */
    private void handleQuantityClick(Player player, ShopMenu.Holder holder, int slot) {
        if (slot == ShopMenu.QTY_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == ShopMenu.QTY_BACK) {
            ShopMenu.openCategory(plugin, player, holder.categoryId(), holder.page());
            return;
        }
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop == null) {
            return;
        }
        ShopCategory category = shop.getCategory(holder.categoryId());
        if (category == null || holder.itemIndex() < 0 || holder.itemIndex() >= category.items().size()) {
            return;
        }
        ShopItem item = category.items().get(holder.itemIndex());
        for (int i = 0; i < ShopMenu.QTY_STEPS.length; i++) {
            if (slot == ShopMenu.QTY_PLUS_FIRST + i) {
                holder.quantity(holder.quantity() + ShopMenu.QTY_STEPS[i]);
                ShopMenu.renderQuantity(plugin, player, holder);
                return;
            }
            if (slot == ShopMenu.QTY_MINUS_FIRST + i) {
                holder.quantity(holder.quantity() - ShopMenu.QTY_STEPS[i]);
                ShopMenu.renderQuantity(plugin, player, holder);
                return;
            }
        }
        if (slot == ShopMenu.QTY_TOGGLE) {
            // Only flip when the other side is actually priced.
            if (holder.buying() && item.sellPrice() <= 0) {
                plugin.getLanguageManager().send(player, "shop.not-sellable");
                return;
            }
            if (!holder.buying() && item.buyPrice() <= 0) {
                plugin.getLanguageManager().send(player, "shop.not-buyable");
                return;
            }
            holder.buying(!holder.buying());
            ShopMenu.renderQuantity(plugin, player, holder);
            return;
        }
        if (slot == ShopMenu.QTY_CONFIRM) {
            if (holder.buying()) {
                buyUnits(player, holder, category, item, holder.quantity());
            } else {
                sellUnits(player, holder, category, item, holder.quantity());
            }
            // buyUnits/sellUnits refresh the view via refreshBalance → renderQuantity.
        }
    }

    /** Pre-pay sell guards: daily sell cap, then MAX_BALANCE headroom (refuse > clamp). */
    private boolean blockedBySellGuards(Player player, EconomyService economy, double total) {
        dev.zcripted.obx.feature.economy.service.SellLimitTracker limits = plugin.getServiceRegistry()
                .get(dev.zcripted.obx.feature.economy.service.SellLimitTracker.class);
        if (limits != null && limits.dailyCap() > 0.0) {
            double remaining = limits.remaining(player.getUniqueId());
            if (total > remaining) {
                plugin.getLanguageManager().send(player, "economy.sell.daily-cap", Placeholders.with(
                        "remaining", economy.format(Math.max(0.0, remaining)),
                        "cap", economy.format(limits.dailyCap())));
                return true;
            }
        }
        if (economy.getBalance(player.getUniqueId()) + total > EconomyService.MAX_BALANCE) {
            plugin.getLanguageManager().send(player, "economy.sell.balance-full");
            return true;
        }
        return false;
    }

    /** Counts {@code total} toward the player's daily sell cap. */
    private void recordSold(Player player, double total) {
        dev.zcripted.obx.feature.economy.service.SellLimitTracker limits = plugin.getServiceRegistry()
                .get(dev.zcripted.obx.feature.economy.service.SellLimitTracker.class);
        if (limits != null) {
            limits.record(player.getUniqueId(), total);
        }
    }

    private ShopPricing pricing() {
        return plugin.getServiceRegistry().get(ShopPricing.class);
    }

    /** Buys exactly {@code units} single items (guards: stock, balance; pays first, then delivers). */
    private void buyUnits(Player player, ShopMenu.Holder holder, ShopCategory category, ShopItem item, int units) {
        if (item.buyPrice() <= 0) {
            plugin.getLanguageManager().send(player, "shop.not-buyable");
            return;
        }
        EconomyService economy = plugin.getEconomyService();
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (economy == null || shop == null || units <= 0) {
            return;
        }
        // Finite stock: clamp/deny BEFORE money moves.
        int inStock = shop.stockRemaining(category.id(), item);
        if (inStock < units) {
            plugin.getLanguageManager().send(player, "shop.out-of-stock", Placeholders.with(
                    "material", ShopMenu.prettyName(item.material()),
                    "remaining", String.valueOf(Math.max(0, inStock)),
                    "minutes", String.valueOf(shop.minutesToRestock())));
            return;
        }
        double total = EconomyService.sanitize(
                ShopMenu.effectiveUnitPrice(plugin, player, item, true) * units);
        // Inventory space check BEFORE money moves.
        int space = maxAddable(player, item.material());
        if (units > space) {
            plugin.getLanguageManager().send(player, "shop.inventory-full", Placeholders.with(
                    "material", ShopMenu.prettyName(item.material()),
                    "units", String.valueOf(units),
                    "space", String.valueOf(space)));
            return;
        }
        ShopPricing pricing = pricing();
        double balanceBefore = economy.getBalance(player.getUniqueId());
        // Pay first (atomic, races-safe), then deliver — a failed withdraw delivers nothing.
        if (!economy.withdraw(player.getUniqueId(), player.getName(), total)) {
            plugin.getLanguageManager().send(player, "shop.cannot-afford", Placeholders.with(
                    "price", economy.format(total),
                    "balance", economy.format(economy.getBalance(player.getUniqueId()))));
            return;
        }
        // Deliver items FIRST, then commit stock — if the inventory can't hold
        // everything, refund the player instead of dropping overflow on the ground.
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(item.material(), units));
        if (!overflow.isEmpty()) {
            economy.deposit(player.getUniqueId(), player.getName(), total);
            plugin.getLanguageManager().send(player, "shop.inventory-full", Placeholders.with(
                    "material", ShopMenu.prettyName(item.material()),
                    "units", String.valueOf(units),
                    "space", String.valueOf(units - overflow.values().stream()
                            .mapToInt(ItemStack::getAmount).sum())));
            return;
        }
        shop.consumeStock(category.id(), item, units);
        if (pricing != null) {
            pricing.recordBuy(item.material(), units);
        }
        double balanceAfter = economy.getBalance(player.getUniqueId());
        economy.logTransaction(player.getName(), player.getUniqueId(), player.getName(),
                "SHOP_BUY", total, balanceAfter);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(units));
        placeholders.put("material", ShopMenu.prettyName(item.material()));
        placeholders.put("price", economy.format(total));
        placeholders.put("balance", economy.format(balanceAfter));
        // Result line carries a detail hover: item, amount, unit price, cost, balance, stock.
        List<String> hover = new ArrayList<>();
        hover.add(lang(player, "shop.hover.bought.title"));
        hover.add(lang(player, "core.divider-line"));
        hover.add(lang(player, "shop.hover.item", "material", ShopMenu.prettyName(item.material())));
        hover.add(lang(player, "shop.hover.amount", "amount", String.valueOf(units)));
        hover.add(lang(player, "shop.hover.unit-price", "price", economy.format(total / units)));
        hover.add(lang(player, "shop.hover.cost", "total", economy.format(total)));
        hover.add(lang(player, "shop.hover.balance", Placeholders.with(
                "before", economy.format(balanceBefore), "after", economy.format(balanceAfter))));
        if (item.stock() > 0) {
            hover.add(lang(player, "shop.hover.stock", Placeholders.with(
                    "remaining", String.valueOf(shop.stockRemaining(category.id(), item)),
                    "minutes", String.valueOf(shop.minutesToRestock()))));
        }
        ComponentMessenger.sendHoverMessage(player,
                plugin.getLanguageManager().get(player, "shop.bought", placeholders), hover, null);
        ShopMenu.refreshBalance(plugin, player, holder);
    }

    /** Sells up to {@code desiredUnits} carried items ({@code Integer.MAX_VALUE} = everything carried). */
    private void sellUnits(Player player, ShopMenu.Holder holder, ShopCategory category, ShopItem item, int desiredUnits) {
        if (item.sellPrice() <= 0) {
            plugin.getLanguageManager().send(player, "shop.not-sellable");
            return;
        }
        EconomyService economy = plugin.getEconomyService();
        if (economy == null || desiredUnits <= 0) {
            return;
        }
        int carried = countCarried(player, item.material());
        if (carried <= 0) {
            plugin.getLanguageManager().send(player, "shop.sell-none", Placeholders.with(
                    "material", ShopMenu.prettyName(item.material())));
            return;
        }
        // Quantity-mode sell: refuse if the player doesn't have the configured amount.
        if (desiredUnits != Integer.MAX_VALUE && carried < desiredUnits) {
            plugin.getLanguageManager().send(player, "shop.sell-insufficient", Placeholders.with(
                    "material", ShopMenu.prettyName(item.material()),
                    "carried", String.valueOf(carried),
                    "desired", String.valueOf(desiredUnits)));
            return;
        }
        int toSell = Math.min(carried, desiredUnits);
        ShopPricing pricing = pricing();
        double unitPrice = ShopMenu.effectiveUnitPrice(plugin, player, item, false);
        // Guards BEFORE removal so a cap refusal costs the player nothing.
        if (blockedBySellGuards(player, economy, EconomyService.sanitize(unitPrice * toSell))) {
            return;
        }
        // Remove first, then pay — count what was ACTUALLY removed (dupe guard).
        Map<Integer, ItemStack> leftover = player.getInventory()
                .removeItem(new ItemStack(item.material(), toSell));
        int notRemoved = 0;
        for (ItemStack remaining : leftover.values()) {
            notRemoved += remaining.getAmount();
        }
        int sold = toSell - notRemoved;
        if (sold <= 0) {
            plugin.getLanguageManager().send(player, "shop.sell-none", Placeholders.with(
                    "material", ShopMenu.prettyName(item.material())));
            return;
        }
        double total = EconomyService.sanitize(unitPrice * sold);
        double balanceBefore = economy.getBalance(player.getUniqueId());
        economy.deposit(player.getUniqueId(), player.getName(), total);
        recordSold(player, total);
        if (pricing != null) {
            pricing.recordSell(item.material(), sold);
        }
        double balanceAfter = economy.getBalance(player.getUniqueId());
        economy.logTransaction(player.getName(), player.getUniqueId(), player.getName(),
                "SHOP_SELL", total, balanceAfter);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(sold));
        placeholders.put("material", ShopMenu.prettyName(item.material()));
        placeholders.put("price", economy.format(total));
        placeholders.put("balance", economy.format(balanceAfter));
        // Result line carries a detail hover: item, amount, unit price, boost, gain, balance.
        double boost = dev.zcripted.obx.feature.economy.service.SellBoost.multiplier(player);
        List<String> hover = new ArrayList<>();
        hover.add(lang(player, "shop.hover.sold.title"));
        hover.add(lang(player, "core.divider-line"));
        hover.add(lang(player, "shop.hover.item", "material", ShopMenu.prettyName(item.material())));
        hover.add(lang(player, "shop.hover.amount", "amount", String.valueOf(sold)));
        hover.add(lang(player, "shop.hover.unit-price", "price", economy.format(unitPrice)));
        if (boost > 1.0) {
            hover.add(lang(player, "shop.hover.boost",
                    "multiplier", String.format(java.util.Locale.ENGLISH, "%.2f", boost)));
        }
        hover.add(lang(player, "shop.hover.gain", "total", economy.format(total)));
        hover.add(lang(player, "shop.hover.balance", Placeholders.with(
                "before", economy.format(balanceBefore), "after", economy.format(balanceAfter))));
        ComponentMessenger.sendHoverMessage(player,
                plugin.getLanguageManager().get(player, "shop.sold", placeholders), hover, null);
        ShopMenu.refreshBalance(plugin, player, holder);
    }

    /** Shorthand: localized line for {@code player} with zero or one placeholder pair. */
    private String lang(Player player, String key) {
        return plugin.getLanguageManager().get(player, key);
    }

    private String lang(Player player, String key, String placeholder, String value) {
        return plugin.getLanguageManager().get(player, key, Placeholders.with(placeholder, value));
    }

    private String lang(Player player, String key, Map<String, String> placeholders) {
        return plugin.getLanguageManager().get(player, key, placeholders);
    }

    private static int countCarried(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    /** How many more of {@code material} could fit into the player's storage slots (0-35).
     *  The full {@code getContents()} array includes armor + offhand slots that
     *  {@code addItem()} cannot use; counting them would overestimate space and let
     *  purchases through that then overflow and drop items on the ground. */
    private static int maxAddable(Player player, Material material) {
        int maxStack = material.getMaxStackSize();
        int space = 0;
        ItemStack[] contents = player.getInventory().getContents();
        int storageSlots = Math.min(36, contents.length);
        for (int i = 0; i < storageSlots; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() == Material.AIR) {
                space += maxStack;
            } else if (stack.getType() == material && stack.getAmount() < maxStack) {
                space += maxStack - stack.getAmount();
            }
        }
        return space;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof ShopMenu.Holder)) {
            return;
        }
        if (((ShopMenu.Holder) top.getHolder()).view() == ShopMenu.ViewType.SELL) {
            return; // dumping into the sell inventory is the whole point
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
    }

    /**
     * Death with the sell inventory open: dump its contents into the normal death
     * drops. Without this the items rode through death inside the GUI and came back
     * at the respawn point — a keep-inventory bypass.
     */
    @EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Inventory top;
        try {
            top = event.getEntity().getOpenInventory().getTopInventory();
        } catch (Throwable unavailable) {
            return;
        }
        if (top == null || !(top.getHolder() instanceof ShopMenu.Holder)
                || ((ShopMenu.Holder) top.getHolder()).view() != ShopMenu.ViewType.SELL) {
            return;
        }
        for (int slot = 0; slot < top.getSize(); slot++) {
            ItemStack stack = top.getItem(slot);
            if (stack != null && stack.getType() != Material.AIR) {
                event.getDrops().add(stack);
                top.setItem(slot, null);
            }
        }
    }

    /** SELL view payout: sell what worth.yml prices, hand back the rest. */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory == null || !(inventory.getHolder() instanceof ShopMenu.Holder)) {
            return;
        }
        ShopMenu.Holder holder = (ShopMenu.Holder) inventory.getHolder();
        if (holder.view() != ShopMenu.ViewType.SELL || !(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        EconomyService economy = plugin.getEconomyService();
        WorthService worth = plugin.getServiceRegistry().get(WorthService.class);
        double multiplier = dev.zcripted.obx.feature.economy.service.SellBoost.multiplier(player);
        // Guard pass first (no mutation): estimate the payout, and if the daily cap or
        // MAX_BALANCE headroom refuses it, hand EVERYTHING back instead of selling.
        double estimate = 0.0;
        boolean anyItems = false;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            anyItems = true;
            double price = (worth == null || economy == null) ? 0.0 : worth.getPrice(stack.getType());
            if (price > 0.0) {
                estimate += price * stack.getAmount();
            }
        }
        if (!anyItems) {
            return; // closed an empty sell inventory — no message noise
        }
        boolean refused = economy != null && estimate > 0.0
                && blockedBySellGuards(player, economy, EconomyService.sanitize(estimate * multiplier));
        double total = 0.0;
        int soldStacks = 0;
        // Per-material tally for the result hover: amount + raw value (boost applied to the total).
        Map<Material, Integer> soldAmounts = new LinkedHashMap<>();
        Map<Material, Double> soldValues = new LinkedHashMap<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            double price = (refused || worth == null || economy == null) ? 0.0 : worth.getPrice(stack.getType());
            // Clear the slot first either way; sellable stacks convert to money,
            // unsellable (or refused) ones go straight back to the player (overflow drops).
            inventory.setItem(slot, null);
            if (price > 0.0) {
                total += price * stack.getAmount();
                soldStacks++;
                soldAmounts.merge(stack.getType(), stack.getAmount(), Integer::sum);
                soldValues.merge(stack.getType(), price * stack.getAmount(), Double::sum);
            } else {
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }
        if (refused) {
            return; // guard message already sent; items returned above
        }
        if (total <= 0.0 || economy == null) {
            plugin.getLanguageManager().send(player, "shop.sell-gui.nothing");
            return;
        }
        total = EconomyService.sanitize(total * multiplier);
        double balanceBefore = economy.getBalance(player.getUniqueId());
        economy.deposit(player.getUniqueId(), player.getName(), total);
        recordSold(player, total);
        double balanceAfter = economy.getBalance(player.getUniqueId());
        economy.logTransaction(player.getName(), player.getUniqueId(), player.getName(),
                "SELL", total, balanceAfter);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("total", economy.format(total));
        placeholders.put("stacks", String.valueOf(soldStacks));
        placeholders.put("balance", economy.format(balanceAfter));
        // Result line carries a detail hover: per-item breakdown, boost, gain, balance.
        List<String> hover = new ArrayList<>();
        hover.add(lang(player, "shop.hover.bulk.title"));
        hover.add(lang(player, "core.divider-line"));
        hover.add(lang(player, "shop.hover.bulk.count", Placeholders.with(
                "stacks", String.valueOf(soldStacks), "types", String.valueOf(soldAmounts.size()))));
        hover.add(lang(player, "shop.hover.bulk.items"));
        int shown = 0;
        for (Map.Entry<Material, Integer> entry : soldAmounts.entrySet()) {
            if (shown >= BULK_HOVER_LIMIT) {
                break;
            }
            hover.add(lang(player, "shop.hover.bulk.entry", Placeholders.with(
                    "material", ShopMenu.prettyName(entry.getKey()),
                    "amount", String.valueOf(entry.getValue()),
                    "value", economy.format(soldValues.getOrDefault(entry.getKey(), 0.0)))));
            shown++;
        }
        if (soldAmounts.size() > shown) {
            hover.add(lang(player, "shop.hover.bulk.more",
                    "count", String.valueOf(soldAmounts.size() - shown)));
        }
        if (multiplier > 1.0) {
            hover.add(lang(player, "shop.hover.boost",
                    "multiplier", String.format(java.util.Locale.ENGLISH, "%.2f", multiplier)));
        }
        hover.add(lang(player, "shop.hover.gain", "total", economy.format(total)));
        hover.add(lang(player, "shop.hover.balance", Placeholders.with(
                "before", economy.format(balanceBefore), "after", economy.format(balanceAfter))));
        ComponentMessenger.sendHoverMessage(player,
                plugin.getLanguageManager().get(player, "shop.sell-gui.result", placeholders), hover, null);
    }
}