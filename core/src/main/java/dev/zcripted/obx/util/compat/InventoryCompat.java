package dev.zcripted.obx.util.compat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Main-hand inventory access that degrades to the pre-1.9 single-hand API.
 *
 * <p>OBX compiles against a modern Bukkit API where {@code getItemInMainHand}/{@code setItemInMainHand}
 * exist, but on a true 1.8.x server those methods are absent at runtime and throw
 * {@link NoSuchMethodError}. We catch that and fall back to the legacy
 * {@code getItemInHand}/{@code setItemInHand}, so main-hand reads/writes work on 1.8 → latest.
 */
public final class InventoryCompat {

    private InventoryCompat() {
    }

    /** The item in the player's main hand, via the legacy single-hand API on 1.8. */
    public static ItemStack mainHand(Player player) {
        try {
            return player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError legacy) {
            return player.getInventory().getItemInHand();
        }
    }

    /** Sets the item in the player's main hand, via the legacy single-hand API on 1.8. */
    public static void setMainHand(Player player, ItemStack item) {
        try {
            player.getInventory().setItemInMainHand(item);
        } catch (NoSuchMethodError legacy) {
            player.getInventory().setItemInHand(item);
        }
    }
}