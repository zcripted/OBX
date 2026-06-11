package dev.zcripted.obx.feature.economy.sink;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;

import java.util.Map;

/**
 * Charges a configurable fee when a player takes an item out of an anvil.
 * Fee is a flat amount per repair operation, configured at
 * {@code economy.sinks.anvil-repair-fee}. 0 or missing = disabled.
 */
public final class RepairFeeListener implements Listener {

    private final ObxPlugin plugin;

    public RepairFeeListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvilTake(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getInventory() == null || !(event.getInventory() instanceof AnvilInventory)) {
            return;
        }
        if (event.getSlotType() != InventoryType.SlotType.RESULT) {
            return;
        }
        double fee = plugin.getConfig().getDouble("economy.sinks.anvil-repair-fee", 0.0);
        if (fee <= 0.0) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        EconomyService economy = plugin.getEconomyService();
        if (economy == null) {
            return;
        }
        if (!economy.withdraw(player.getUniqueId(), player.getName(), fee)) {
            event.setCancelled(true);
            plugin.getLanguageManager().send(player, "economy.sink.anvil.cant-afford",
                    Placeholders.with("fee", economy.format(fee)));
            return;
        }
        economy.logTransaction(player.getName(), player.getUniqueId(), player.getName(),
                "ANVIL_REPAIR", fee, economy.getBalance(player.getUniqueId()));
        // Fee → the visible server account (not burned).
        ServerAccountService account = plugin.getServiceRegistry().get(ServerAccountService.class);
        if (account != null) {
            account.deposit(player.getName(), "ANVIL_REPAIR", fee);
        }
    }
}