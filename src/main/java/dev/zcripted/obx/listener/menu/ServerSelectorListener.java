package dev.zcripted.obx.listener.menu;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.gui.player.ServerSelectorHolder;
import dev.zcripted.obx.hub.messaging.BungeeMessenger;
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
 * Routes clicks in the hub server-selector GUI. Cancels every interaction
 * (icons are read-only) and dispatches the BungeeCord {@code Connect}
 * message when the clicker hits a slot that's bound to a server id by the
 * holder.
 */
public final class ServerSelectorListener implements Listener {

    private final Main plugin;

    public ServerSelectorListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null) {
            return;
        }
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof ServerSelectorHolder)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player)) {
            return;
        }
        Player player = (Player) who;

        ServerSelectorHolder selector = (ServerSelectorHolder) holder;
        if (selector.isCloseSlot(event.getRawSlot())) {
            player.closeInventory();
            return;
        }

        String serverId = selector.serverFor(event.getRawSlot());
        if (serverId == null) {
            return;
        }

        BungeeMessenger messenger = plugin.getBungeeMessenger();
        if (messenger == null || !messenger.isRegistered()) {
            plugin.getLanguageManager().send(player, "hub.selector.proxy-unavailable");
            return;
        }
        player.closeInventory();
        messenger.connect(player, serverId);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null) {
            return;
        }
        if (top.getHolder() instanceof ServerSelectorHolder) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }
}
