package dev.zcripted.obx.feature.economy.shop;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.economy.service.WorthService;
import dev.zcripted.obx.feature.economy.shop.ShopService.ShopCategory;
import dev.zcripted.obx.feature.economy.shop.ShopService.ShopItem;
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

import java.util.HashMap;
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
        handleCategoryClick(player, holder, slot, event.isShiftClick(), event.isRightClick());
    }

    private void handleMainClick(Player player, Inventory top, int slot) {
        int size = top.getSize();
        int bottom = size - 9;
        if (slot == size - 1) {
            player.closeInventory();
            return;
        }
        if (slot == bottom + 2) {
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
        for (ShopCategory category : shop.getCategories()) {
            if (category.slot() == slot) {
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
        if (right) {
            sell(player, holder, category, item, shift);
        } else {
            buy(player, holder, category, item, shift);
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

    private void buy(Player player, ShopMenu.Holder holder, ShopCategory category, ShopItem item, boolean stack) {
        if (item.buyPrice() <= 0) {
            plugin.getLanguageManager().send(player, "shop.not-buyable");
            return;
        }
        EconomyService economy = plugin.getEconomyService();
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (economy == null || shop == null) {
            return;
        }
        // Left = one bundle; shift-left = a full stack's worth of bundles.
        int bundles = stack ? Math.max(1, 64 / Math.max(1, item.amount())) : 1;
        int units = bundles * item.amount();
        // Finite stock: clamp/deny BEFORE money moves.
        int inStock = shop.stockRemaining(category.id(), item);
        if (inStock < units) {
            plugin.getLanguageManager().send(player, "shop.out-of-stock", Placeholders.with(
                    "material", ShopMenu.prettyName(item.material()),
                    "remaining", String.valueOf(Math.max(0, inStock)),
                    "minutes", String.valueOf(shop.minutesToRestock())));
            return;
        }
        ShopPricing pricing = pricing();
        double multiplier = pricing == null ? 1.0 : pricing.buyMultiplier(item.material());
        double total = EconomyService.sanitize(item.buyPrice() * bundles * multiplier);
        // Pay first (atomic, races-safe), then deliver — a failed withdraw delivers nothing.
        if (!economy.withdraw(player.getUniqueId(), player.getName(), total)) {
            plugin.getLanguageManager().send(player, "shop.cannot-afford", Placeholders.with(
                    "price", economy.format(total),
                    "balance", economy.format(economy.getBalance(player.getUniqueId()))));
            return;
        }
        shop.consumeStock(category.id(), item, units);
        if (pricing != null) {
            pricing.recordBuy(item.material(), units);
        }
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(item.material(), units));
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        double balanceAfter = economy.getBalance(player.getUniqueId());
        economy.logTransaction(player.getName(), player.getUniqueId(), player.getName(),
                "SHOP_BUY", total, balanceAfter);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(units));
        placeholders.put("material", ShopMenu.prettyName(item.material()));
        placeholders.put("price", economy.format(total));
        placeholders.put("balance", economy.format(balanceAfter));
        plugin.getLanguageManager().send(player, "shop.bought", placeholders);
        ShopMenu.refreshBalance(plugin, player, holder);
    }

    private void sell(Player player, ShopMenu.Holder holder, ShopCategory category, ShopItem item, boolean all) {
        if (item.sellPrice() <= 0) {
            plugin.getLanguageManager().send(player, "shop.not-sellable");
            return;
        }
        EconomyService economy = plugin.getEconomyService();
        if (economy == null) {
            return;
        }
        int carried = countCarried(player, item.material());
        int toSell = all ? carried : Math.min(carried, item.amount());
        if (toSell <= 0) {
            plugin.getLanguageManager().send(player, "shop.sell-none", Placeholders.with(
                    "material", ShopMenu.prettyName(item.material())));
            return;
        }
        ShopPricing pricing = pricing();
        double unitPrice = item.sellPrice()
                * (pricing == null ? 1.0 : pricing.sellMultiplier(item.material()))
                * dev.zcripted.obx.feature.economy.service.SellBoost.multiplier(player);
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
        plugin.getLanguageManager().send(player, "shop.sold", placeholders);
        ShopMenu.refreshBalance(plugin, player, holder);
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
        economy.deposit(player.getUniqueId(), player.getName(), total);
        recordSold(player, total);
        double balanceAfter = economy.getBalance(player.getUniqueId());
        economy.logTransaction(player.getName(), player.getUniqueId(), player.getName(),
                "SELL", total, balanceAfter);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("total", economy.format(total));
        placeholders.put("stacks", String.valueOf(soldStacks));
        placeholders.put("balance", economy.format(balanceAfter));
        plugin.getLanguageManager().send(player, "shop.sell-gui.result", placeholders);
    }
}
