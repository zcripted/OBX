package dev.zcripted.obx.feature.item.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.command.PlayerActionCommand;
import org.bukkit.entity.Player;

/**
 * Opens a virtual enchanting table at the player's current location. Uses the
 * universally-available {@link Player#openEnchanting(org.bukkit.Location, boolean)}
 * API. The {@code force} flag bypasses the bookshelf-power requirement so the
 * level-30 enchantment slot is always reachable.
 */
public class EnchantCommand extends PlayerActionCommand {

    public EnchantCommand(ObxPlugin plugin) {
        super(plugin, "obx.enchant");
    }

    @Override
    protected void run(Player player, String[] args) {
        try {
            player.openEnchanting(null, true);
            languages.send(player, "utility.enchant.opened");
        } catch (Throwable throwable) {
            languages.send(player, "utility.enchant.unsupported");
        }
    }
}