package dev.sergeantfuzzy.sfcore.gui.player;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.WarpService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WarpConfirmMoveMenu {

    private WarpConfirmMoveMenu() {
    }

    public static void open(Main plugin, Player player, WarpService.WarpEntry entry, Location location, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openConfirmMove(plugin, player, entry, location, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
