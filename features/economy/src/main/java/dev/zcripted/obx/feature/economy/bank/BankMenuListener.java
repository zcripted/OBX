package dev.zcripted.obx.feature.economy.bank;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class BankMenuListener implements Listener {

    private final ObxPlugin plugin;

    public BankMenuListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof BankMenu.Holder)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        BankMenu.Holder holder = (BankMenu.Holder) top.getHolder();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }
        switch (slot) {
            case BankMenu.NAV_CLOSE:
                player.closeInventory();
                return;
            case BankMenu.NAV_DEPOSIT:
                player.closeInventory();
                plugin.getLanguageManager().send(player, "economy.bank.gui.deposit-hint");
                return;
            case BankMenu.NAV_WITHDRAW:
                player.closeInventory();
                plugin.getLanguageManager().send(player, "economy.bank.gui.withdraw-hint");
                return;
            case BankMenu.NAV_PREV:
                BankMenu.open(plugin, player, holder.page() - 1);
                return;
            case BankMenu.NAV_NEXT:
                BankMenu.open(plugin, player, holder.page() + 1);
                return;
            case BankMenu.NAV_BALANCE:
                BankMenu.open(plugin, player, holder.page());
                return;
            default:
                break;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof BankMenu.Holder) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }
}