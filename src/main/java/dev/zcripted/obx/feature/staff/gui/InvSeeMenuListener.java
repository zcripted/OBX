package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.staff.gui.InvSeeMenuHolder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes interactions on the {@code /invsee} live-mirror GUI:
 *
 * <ul>
 *   <li>Every click on the mirror is cancelled (view-only) so the operator
 *       can observe the target's inventory updating in real time without
 *       accidentally pulling items out of the mirror or shoving items in
 *       from their own hotbar.</li>
 *   <li>Clicking the red-X close head at the bottom-right closes the GUI.</li>
 *   <li>{@link InventoryDragEvent} on the mirror is also cancelled so a
 *       carelessly-held mouse drag can't shuffle the displayed items.</li>
 *   <li>{@link InventoryCloseEvent} drops the viewer from
 *       {@link dev.zcripted.obx.feature.staff.gui.InvSeeMenuManager} so the
 *       refresh task stops touching the now-discarded chest inventory.</li>
 * </ul>
 */
public final class InvSeeMenuListener implements Listener {

    private final ObxPlugin plugin;

    public InvSeeMenuListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null) {
            return;
        }
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof InvSeeMenuHolder)) {
            return;
        }
        // View-only: cancel every click so neither the GUI mirror nor the
        // viewer's own inventory can move items into / out of the chest.
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) {
            return;
        }
        InvSeeMenuHolder mirror = (InvSeeMenuHolder) holder;
        if (event.getRawSlot() == mirror.getCloseSlot()) {
            ((Player) clicker).closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null) {
            return;
        }
        if (top.getHolder() instanceof InvSeeMenuHolder) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getInventory();
        if (top == null) {
            return;
        }
        if (!(top.getHolder() instanceof InvSeeMenuHolder)) {
            return;
        }
        if (plugin.getInvSeeMenuManager() != null) {
            plugin.getInvSeeMenuManager().unregister(event.getPlayer().getUniqueId());
        }
    }
}
