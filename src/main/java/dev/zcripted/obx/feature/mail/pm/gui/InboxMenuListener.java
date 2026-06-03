package dev.zcripted.obx.feature.mail.pm.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.mail.pm.gui.InboxMenu;
import dev.zcripted.obx.feature.mail.pm.gui.InboxMenuHolder;
import dev.zcripted.obx.feature.mail.pm.InboxMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Locks the inbox GUI (no item movement) and routes clicks: left-click reads a message
 * (marking it read), right-click deletes it, shift-click toggles a bookmark, and the
 * bottom Clear button removes all non-bookmarked messages.
 */
public final class InboxMenuListener implements Listener {

    private final ObxPlugin plugin;

    public InboxMenuListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof InboxMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot == InboxMenu.CLEAR_SLOT) {
            plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.pm.PrivateMessageService.class).clearInbox(player);
            return;
        }
        InboxMessage message = ((InboxMenuHolder) top.getHolder()).forSlot(slot);
        if (message == null) {
            return;
        }
        if (event.isShiftClick()) {
            plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.pm.PrivateMessageService.class).toggleBookmark(player, message);
        } else if (event.isRightClick()) {
            plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.pm.PrivateMessageService.class).deleteMessage(player, message);
        } else {
            plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.pm.PrivateMessageService.class).readMessage(player, message);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof InboxMenuHolder) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }
}
