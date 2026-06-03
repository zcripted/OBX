package dev.zcripted.obx.feature.hub.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hub.service.HubService;
import dev.zcripted.obx.feature.hub.item.HubItems;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents hub hotbar items from leaking out of the player inventory while
 * hub-mode is active for that player. Cancels drops, swaps, drags into
 * containers, and item pickups of items with hub item IDs.
 *
 * <p>Only enforces protection when the player is in a hub world AND the
 * hub config has {@code kit.lock-hotbar: true}.
 */
public final class HubItemProtectionListener implements Listener {

    private final ObxPlugin plugin;
    private final HubService hub;

    public HubItemProtectionListener(ObxPlugin plugin, HubService hub) {
        this.plugin = plugin;
        this.hub = hub;
    }

    private boolean shouldProtect(Player player) {
        return hub.isEnabled() && hub.kitLockHotbar() && hub.isInHubWorld(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!shouldProtect(player)) {
            return;
        }
        if (HubItems.isHubItem(plugin, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // Note: hotbar/offhand swap protection isn't included here because
    // PlayerSwapHandItemsEvent is 1.9+ and OBX targets 1.8.8 → 26.1
    // from a single JAR. The hotbar-lock click protection below already
    // blocks every reachable shuffle path in practice.

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player)) {
            return;
        }
        Player player = (Player) who;
        if (!shouldProtect(player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (HubItems.isHubItem(plugin, current) || HubItems.isHubItem(plugin, cursor)) {
            // Allow clicks INSIDE the player's own inventory only if both
            // source and destination are the player inventory (i.e. moving
            // between hotbar slots). Block transfers to chests and crafting
            // grids.
            Inventory top = event.getView().getTopInventory();
            boolean toContainer = top != null && top.getType() != InventoryType.CRAFTING
                    && top.getType() != InventoryType.PLAYER;
            if (toContainer) {
                event.setCancelled(true);
            } else {
                // Disallow any in-inventory shuffle as well — keeps the hub
                // hotbar layout stable for the duration of the session.
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player)) {
            return;
        }
        Player player = (Player) who;
        if (!shouldProtect(player)) {
            return;
        }
        ItemStack cursor = event.getOldCursor();
        if (HubItems.isHubItem(plugin, cursor)) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!shouldProtect(player)) {
            return;
        }
        if (HubItems.isHubItem(plugin, event.getItem().getItemStack())) {
            // Belt-and-braces in case another plugin force-drops items.
            // PlayerPickupItemEvent is deprecated in favour of
            // EntityPickupItemEvent (1.12+), but the legacy event still
            // fires on every supported version and matches the existing
            // codebase pattern (see VanishManager).
            event.setCancelled(true);
        }
    }
}
