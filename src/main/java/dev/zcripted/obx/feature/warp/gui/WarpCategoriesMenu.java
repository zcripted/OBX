package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.OBX;
import org.bukkit.entity.Player;

public final class WarpCategoriesMenu {

    private WarpCategoriesMenu() {
    }

    public static void open(OBX plugin, Player player, int page, boolean adminMode, WarpMenuHolder.BackTarget backTarget) {
        WarpMenu.openCategories(plugin, player, page, adminMode, backTarget);
    }
}
