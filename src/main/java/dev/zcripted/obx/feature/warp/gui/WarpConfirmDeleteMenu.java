package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.warp.service.WarpService;
import org.bukkit.entity.Player;

public final class WarpConfirmDeleteMenu {

    private WarpConfirmDeleteMenu() {
    }

    public static void open(ObxPlugin plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openConfirmDelete(plugin, player, entry, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
