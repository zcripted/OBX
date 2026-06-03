package dev.zcripted.obx.enchant.effect;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Persists a player's Satchel contents to disk the moment they close it, so the
 * stored items survive restarts/reloads (the satchel inventory is otherwise
 * in-memory only). A full save also runs on plugin disable via
 * {@link EnchantState#saveAll()}.
 */
public final class SatchelCloseListener implements Listener {

    private final EnchantState state;

    public SatchelCloseListener(EnchantState state) {
        this.state = state;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (state.isSatchel(player.getUniqueId(), event.getInventory())) {
            state.saveSatchel(player);
        }
    }
}
