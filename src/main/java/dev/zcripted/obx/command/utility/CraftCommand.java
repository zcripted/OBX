package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.command.PlayerActionCommand;
import org.bukkit.entity.Player;

/**
 * Opens a virtual crafting table (3x3 workbench) at the player's current location
 * without requiring a real crafting table block. Uses the universal
 * {@link Player#openWorkbench(org.bukkit.Location, boolean)} API which is
 * available on every supported Spigot/Paper version.
 */
public class CraftCommand extends PlayerActionCommand {

    public CraftCommand(OBX plugin) {
        super(plugin, "obx.craft");
    }

    @Override
    protected void run(Player player, String[] args) {
        try {
            player.openWorkbench(null, true);
            languages.send(player, "utility.craft.opened");
        } catch (Throwable throwable) {
            languages.send(player, "utility.craft.unsupported");
        }
    }
}
