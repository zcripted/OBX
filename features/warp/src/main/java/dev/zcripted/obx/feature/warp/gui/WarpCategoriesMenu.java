package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.entity.Player;

public final class WarpCategoriesMenu {

    private WarpCategoriesMenu() {
    }

    public static void open(ObxPlugin plugin, Player player, int page, boolean adminMode, WarpMenuHolder.BackTarget backTarget) {
        WarpMenu.openCategories(plugin, player, page, adminMode, backTarget);
    }
}