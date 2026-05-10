package dev.sergeantfuzzy.sfcore.gui.player;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.WarpService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WarpConfirmOverwriteMenu {

    private WarpConfirmOverwriteMenu() {
    }

    public static void open(Main plugin, Player player, String warpName, Location newLocation, WarpService.WarpEntry existing, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openConfirmOverwrite(plugin, player, warpName, newLocation, existing, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
