package dev.zcripted.obx.feature.backpack.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.backpack.service.BackpackService;
import dev.zcripted.obx.feature.backpack.service.BackpackService.BackpackHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Backpack runtime guards and persistence:
 *
 * <ul>
 *   <li><b>Use:</b> right-clicking a tagged backpack item opens the owner's storage —
 *       only for the owner, and only when the item's instance token matches the
 *       database (dupe-guard). Void copies are deleted on sight.</li>
 *   <li><b>Place guard:</b> the physical item can be a placeable block (chest/shulker
 *       on older servers) — placing it is cancelled so the tag can't be laundered
 *       into a world block.</li>
 *   <li><b>Nesting guard:</b> a backpack item can never be stored inside an open
 *       backpack view (click, shift-click, hotbar swap, drag).</li>
 *   <li><b>Persistence:</b> contents save on inventory close and on quit; module
 *       disable saves through {@link BackpackService#closeAndSaveAll()}.</li>
 * </ul>
 */
public class BackpackListener implements Listener {

    private final ObxPlugin plugin;
    private final BackpackService backpacks;

    public BackpackListener(ObxPlugin plugin, BackpackService backpacks) {
        this.plugin = plugin;
        this.backpacks = backpacks;
    }

    // ── open on use + dupe-guard ─────────────────────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!backpacks.isBackpackItem(item)) {
            return;
        }
        // Fires once per hand on 1.9+; only act on the main-hand event.
        try {
            if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
                event.setCancelled(true);
                return;
            }
        } catch (Throwable ignored) {
            // 1.8: single-hand servers have no getHand() — proceed.
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID owner = backpacks.itemOwner(item);
        if (owner == null) {
            return;
        }
        if (!owner.equals(player.getUniqueId())) {
            // Someone else's (inevitably void after a respawn) backpack — never opens.
            plugin.getLanguageManager().send(player, "backpack.not-yours");
            return;
        }
        if (!backpacks.isStorageAvailable()) {
            plugin.getLanguageManager().send(player, "backpack.storage-unavailable");
            return;
        }
        if (!backpacks.isValidItem(item)) {
            // Stale token (respawned elsewhere) or virtual mode: the copy is void.
            // Purge it so a duplicated item can't even linger as clutter.
            backpacks.removeStaleItems(player);
            plugin.getLanguageManager().send(player, "backpack.void-item");
            return;
        }
        // Creative middle-click can clone a VALID key (same token) or stack it —
        // collapse to exactly one key of amount 1 before opening.
        backpacks.normalizeKeyItem(player);
        backpacks.open(player);
    }

    // ── place guard ──────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (backpacks.isBackpackItem(event.getItemInHand())) {
            event.setCancelled(true);
            plugin.getLanguageManager().send(event.getPlayer(), "backpack.no-place");
        }
    }

    // ── nesting guard ────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof BackpackHolder)) {
            return;
        }
        boolean intoTop = event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize();
        // 1) Cursor drop into the backpack area.
        if (intoTop && backpacks.isBackpackItem(event.getCursor())) {
            deny(event);
            return;
        }
        // 2) Shift-click from the player inventory (moves into the top inventory).
        if (!intoTop && event.isShiftClick() && backpacks.isBackpackItem(event.getCurrentItem())) {
            deny(event);
            return;
        }
        // 3) Number-key hotbar swap targeting a backpack slot.
        if (intoTop && event.getHotbarButton() >= 0) {
            ItemStack hotbar = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (backpacks.isBackpackItem(hotbar)) {
                deny(event);
                return;
            }
        }
        // 4) F-key off-hand swap into a backpack slot (1.9+). The ClickType constant
        //    is compared by name so the class still loads on pre-1.16 API baselines.
        if (intoTop && "SWAP_OFFHAND".equals(event.getClick().name())) {
            try {
                ItemStack offHand = event.getWhoClicked().getInventory().getItemInOffHand();
                if (backpacks.isBackpackItem(offHand)) {
                    deny(event);
                }
            } catch (Throwable noOffHand) {
                // 1.8: no off-hand — nothing to guard.
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof BackpackHolder)
                || !backpacks.isBackpackItem(event.getOldCursor())) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < top.getSize()) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    plugin.getLanguageManager().send(event.getWhoClicked(), "backpack.no-nest");
                }
                return;
            }
        }
    }

    private void deny(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player) {
            plugin.getLanguageManager().send(event.getWhoClicked(), "backpack.no-nest");
        }
    }

    // ── persistence ──────────────────────────────────────────────────────────

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory != null && inventory.getHolder() instanceof BackpackHolder) {
            backpacks.saveContents(((BackpackHolder) inventory.getHolder()).owner(),
                    inventory.getContents());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Defensive double-save: most platforms fire InventoryCloseEvent on quit,
        // but saving here as well costs one cheap UPDATE and closes the gap on
        // forks that don't.
        try {
            Inventory top = event.getPlayer().getOpenInventory().getTopInventory();
            if (top != null && top.getHolder() instanceof BackpackHolder) {
                backpacks.saveContents(((BackpackHolder) top.getHolder()).owner(), top.getContents());
            }
        } catch (Throwable ignored) {
            // never block the quit path
        }
    }
}
