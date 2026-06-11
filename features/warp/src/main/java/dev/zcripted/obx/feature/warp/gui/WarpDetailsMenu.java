package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.warp.service.WarpService;
import org.bukkit.entity.Player;

public final class WarpDetailsMenu {

    private WarpDetailsMenu() {
    }

    public static void open(ObxPlugin plugin, Player player, WarpService.WarpEntry entry, boolean adminMode, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openDetails(plugin, player, entry, adminMode, backTarget, returnPage, categoryFilter, searchTerm);
    }
}