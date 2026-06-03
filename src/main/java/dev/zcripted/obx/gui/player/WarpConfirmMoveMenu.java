package dev.zcripted.obx.gui.player;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.storage.WarpService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WarpConfirmMoveMenu {

    private WarpConfirmMoveMenu() {
    }

    public static void open(OBX plugin, Player player, WarpService.WarpEntry entry, Location location, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openConfirmMove(plugin, player, entry, location, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
