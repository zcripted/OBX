package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.entity.Player;

public final class WarpManageMenu {

    private WarpManageMenu() {
    }

    public static void open(ObxPlugin plugin, Player player, int page, String categoryFilter, String searchTerm, WarpMenuHolder.BackTarget backTarget) {
        WarpMenu.openManage(plugin, player, page, categoryFilter, searchTerm, backTarget);
    }
}