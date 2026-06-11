package dev.zcripted.obx.feature.economy.shop;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Click + chat-prompt behaviour for the {@code /shop admin} editor GUI
 * ({@link ShopEditorMenu}). All movement is cancelled; edits persist
 * immediately through {@link ShopService}'s YAML writers, so there is no
 * unsaved draft state to lose.
 *
 * <ul>
 *   <li><b>CATEGORIES</b> — click a tile to edit its items; the emerald
 *       prompts a chat input for a new category id.</li>
 *   <li><b>ITEMS</b> — left-click prompts a new buy price, right-click a new
 *       sell price, shift-click removes the entry; the emerald adds the item
 *       held in the admin's main hand (prompting {@code <buy> <sell>}).</li>
 *   <li>Chat prompts run at LOWEST priority so the line is swallowed before the
 *       chat pipeline ({@code ChatManagementListener}, HIGHEST) renders it, and
 *       apply the edit back on the main thread ({@code runAtEntity} — Folia-safe).
 *       Typing {@code cancel} (or quitting) abandons the prompt.</li>
 * </ul>
 */
public final class ShopEditorListener implements Listener {

    private enum Prompt { BUY_PRICE, SELL_PRICE, ADD_ITEM, ADD_CATEGORY }

    /** One awaited chat input: what kind, and which category/item it edits. */
    private static final class Pending {
        final Prompt type;
        final String categoryId;
        final Material material;

        Pending(Prompt type, String categoryId, Material material) {
            this.type = type;
            this.categoryId = categoryId;
            this.material = material;
        }
    }

    private final ObxPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ShopEditorListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    // ── GUI clicks ───────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof ShopEditorMenu.Holder)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("obx.shop.admin")) {
            return; // opened pre-revoke — view-only now
        }
        ShopEditorMenu.Holder holder = (ShopEditorMenu.Holder) top.getHolder();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return; // own-inventory clicks — already cancelled above
        }
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop == null) {
            return;
        }
        if (slot == ShopEditorMenu.NAV_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == ShopEditorMenu.NAV_SAVE) {
            // Edits already persisted on each action — this re-reads the YAML so
            // hand edits made alongside the GUI session show up too.
            shop.reload();
            plugin.getLanguageManager().send(player, "shop.reloaded",
                    Placeholders.with("count", shop.categories().size()));
            reopen(player, holder);
            return;
        }
        if (holder.editView() == ShopEditorMenu.EditView.CATEGORIES) {
            handleCategoriesClick(player, shop, slot);
        } else {
            handleItemsClick(player, shop, holder, slot, event.isShiftClick(), event.isRightClick());
        }
    }

    private void handleCategoriesClick(Player player, ShopService shop, int slot) {
        if (slot == ShopEditorMenu.NAV_ADD) {
            pending.put(player.getUniqueId(), new Pending(Prompt.ADD_CATEGORY, null, null));
            player.closeInventory();
            plugin.getLanguageManager().send(player, "shop.editor.prompt-category");
            return;
        }
        if (slot >= 45) {
            return; // filler
        }
        // Tiles are laid out in categories() order (see ShopEditorMenu.openCategories).
        java.util.List<ShopService.ShopCategory> categories = shop.categories();
        if (slot < categories.size()) {
            ShopEditorMenu.openItems(plugin, player, categories.get(slot).id());
        }
    }

    private void handleItemsClick(Player player, ShopService shop, ShopEditorMenu.Holder holder,
                                  int slot, boolean shift, boolean right) {
        if (slot == ShopEditorMenu.NAV_BACK) {
            ShopEditorMenu.openCategories(plugin, player);
            return;
        }
        ShopService.ShopCategory category = shop.categoryById(holder.categoryId());
        if (category == null) {
            ShopEditorMenu.openCategories(plugin, player);
            return;
        }
        if (slot == ShopEditorMenu.NAV_ADD) {
            ItemStack held = dev.zcripted.obx.util.compat.InventoryCompat.mainHand(player);
            if (held == null || held.getType() == Material.AIR) {
                plugin.getLanguageManager().send(player, "shop.editor.no-held-item");
                return;
            }
            for (ShopService.ShopItem item : category.items()) {
                if (item.material() == held.getType()) {
                    plugin.getLanguageManager().send(player, "shop.editor.item-exists",
                            Placeholders.with("material", ShopMenu.prettyName(held.getType())));
                    return;
                }
            }
            pending.put(player.getUniqueId(), new Pending(Prompt.ADD_ITEM, category.id(), held.getType()));
            player.closeInventory();
            plugin.getLanguageManager().send(player, "shop.editor.prompt-add",
                    Placeholders.with("material", ShopMenu.prettyName(held.getType())));
            return;
        }
        if (slot >= 45 || slot >= category.items().size()) {
            return; // filler / empty tile
        }
        ShopService.ShopItem item = category.items().get(slot);
        if (shift) {
            if (shop.removeItem(category.id(), item.material())) {
                plugin.getLanguageManager().send(player, "shop.editor.item-removed", Placeholders.with(
                        "material", ShopMenu.prettyName(item.material()), "category", category.id()));
            } else {
                plugin.getLanguageManager().send(player, "shop.editor.write-failed");
            }
            ShopEditorMenu.openItems(plugin, player, category.id());
            return;
        }
        Prompt prompt = right ? Prompt.SELL_PRICE : Prompt.BUY_PRICE;
        pending.put(player.getUniqueId(), new Pending(prompt, category.id(), item.material()));
        player.closeInventory();
        plugin.getLanguageManager().send(player,
                right ? "shop.editor.prompt-sell" : "shop.editor.prompt-buy",
                Placeholders.with("material", ShopMenu.prettyName(item.material())));
    }

    private void reopen(Player player, ShopEditorMenu.Holder holder) {
        if (holder.editView() == ShopEditorMenu.EditView.ITEMS) {
            ShopEditorMenu.openItems(plugin, player, holder.categoryId());
        } else {
            ShopEditorMenu.openCategories(plugin, player);
        }
    }

    // ── Chat prompts ─────────────────────────────────────────────────────────

    // LOWEST: must run before the chat feature's ChatManagementListener (HIGHEST),
    // which dispatches the formatted line to recipients itself — by the time a
    // later handler cancels the event the message is already in everyone's chat.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Pending input = pending.remove(player.getUniqueId());
        if (input == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            plugin.getLanguageManager().send(player, "shop.editor.cancelled");
            return;
        }
        // Chat arrives async — apply the edit (YAML write + GUI reopen) on the
        // player's main thread (Folia: their entity scheduler).
        plugin.getSchedulerAdapter().runAtEntity(player, () -> apply(player, input, message));
    }

    private void apply(Player player, Pending input, String message) {
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop == null || !player.hasPermission("obx.shop.admin")) {
            return;
        }
        switch (input.type) {
            case BUY_PRICE:
            case SELL_PRICE: {
                Double price = parsePrice(player, message);
                if (price == null) {
                    return;
                }
                boolean buy = input.type == Prompt.BUY_PRICE;
                if (shop.writePrice(input.categoryId, input.material,
                        buy ? price : null, buy ? null : price)) {
                    plugin.getLanguageManager().send(player,
                            buy ? "shop.editor.price-set-buy" : "shop.editor.price-set-sell",
                            Placeholders.with("material", ShopMenu.prettyName(input.material),
                                    "price", String.format(Locale.ENGLISH, "%.2f", price),
                                    "category", input.categoryId));
                } else {
                    plugin.getLanguageManager().send(player, "shop.editor.write-failed");
                }
                ShopEditorMenu.openItems(plugin, player, input.categoryId);
                return;
            }
            case ADD_ITEM: {
                String[] parts = message.split("\\s+");
                Double buy = parsePrice(player, parts[0]);
                Double sell = parts.length > 1 ? parsePrice(player, parts[1]) : Double.valueOf(0.0);
                if (buy == null || sell == null) {
                    return;
                }
                if (buy <= 0 && sell <= 0) {
                    plugin.getLanguageManager().send(player, "shop.editor.invalid-number",
                            Placeholders.with("input", message));
                    return;
                }
                if (shop.addItem(input.categoryId, input.material, buy, sell)) {
                    Map<String, String> info = new java.util.HashMap<>();
                    info.put("material", ShopMenu.prettyName(input.material));
                    info.put("category", input.categoryId);
                    info.put("buy", String.format(Locale.ENGLISH, "%.2f", buy));
                    info.put("sell", String.format(Locale.ENGLISH, "%.2f", sell));
                    plugin.getLanguageManager().send(player, "shop.editor.item-added", info);
                } else {
                    plugin.getLanguageManager().send(player, "shop.editor.write-failed");
                }
                ShopEditorMenu.openItems(plugin, player, input.categoryId);
                return;
            }
            case ADD_CATEGORY: {
                if (shop.addCategory(message)) {
                    plugin.getLanguageManager().send(player, "shop.editor.category-added",
                            Placeholders.with("category",
                                    message.trim().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9_-]", "")));
                } else {
                    plugin.getLanguageManager().send(player, "shop.editor.category-failed");
                }
                ShopEditorMenu.openCategories(plugin, player);
                return;
            }
            default:
        }
    }

    /** Parses a non-negative finite price; bad input gets a message and {@code null}. */
    private Double parsePrice(Player player, String raw) {
        try {
            double value = Double.parseDouble(raw);
            if (Double.isFinite(value) && value >= 0.0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // fall through to the error message
        }
        plugin.getLanguageManager().send(player, "shop.editor.invalid-number",
                Placeholders.with("input", raw));
        return null;
    }

    // ── Housekeeping ─────────────────────────────────────────────────────────

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof ShopEditorMenu.Holder) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * Drops any pending prompt when the player leaves. Without this the entry
     * leaked forever AND the HIGHEST-priority chat hook above would swallow the
     * player's first chat line when they rejoined.
     */
    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}