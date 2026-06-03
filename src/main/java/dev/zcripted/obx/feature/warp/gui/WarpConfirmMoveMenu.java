package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.warp.service.WarpService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WarpConfirmMoveMenu {

    private WarpConfirmMoveMenu() {
    }

    public static void open(OBX plugin, Player player, WarpService.WarpEntry entry, Location location, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openConfirmMove(plugin, player, entry, location, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
