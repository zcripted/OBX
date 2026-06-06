package dev.zcripted.obx.feature.enchant.gui;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes clicks in the Arcanum GUI screens. Every interaction with the menu is
 * cancelled (icons are read-only); clicks inside the top inventory are
 * dispatched to {@link EnchantAdminMenu#handleClick}.
 */
public final class EnchantMenuListener implements Listener {

    private final ObxPlugin plugin;

    public EnchantMenuListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null) {
            return;
        }
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof EnchantMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player)) {
            return;
        }
        Player player = (Player) who;
        EnchantMenuHolder menuHolder = (EnchantMenuHolder) holder;
        // Re-verify the admin permission on EVERY click of an admin (non-browse) menu. The
        // authorization was captured at open time as the holder's browse flag; without this re-check a
        // player who loses obx.enchants.admin while the console is open (or is handed the inventory)
        // could keep minting max-level scrolls/books and applying enchants. Browse menus are read-only.
        if (!menuHolder.isBrowse() && !player.hasPermission("obx.enchants.admin")) {
            player.closeInventory();
            return;
        }
        // Only act on clicks within the menu itself (not the player's own inventory).
        if (event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) {
            return;
        }
        if (plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.gui.EnchantAdminMenu.class) == null) {
            return;
        }
        plugin.getServiceRegistry().get(dev.zcripted.obx.feature.enchant.gui.EnchantAdminMenu.class).handleClick(player, menuHolder, event.getRawSlot(), event.getClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof EnchantMenuHolder) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }
}
