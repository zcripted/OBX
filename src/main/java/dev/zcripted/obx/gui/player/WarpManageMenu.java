package dev.zcripted.obx.gui.player;

import dev.zcripted.obx.OBX;
import org.bukkit.entity.Player;

public final class WarpManageMenu {

    private WarpManageMenu() {
    }

    public static void open(OBX plugin, Player player, int page, String categoryFilter, String searchTerm, WarpMenuHolder.BackTarget backTarget) {
        WarpMenu.openManage(plugin, player, page, categoryFilter, searchTerm, backTarget);
    }
}
