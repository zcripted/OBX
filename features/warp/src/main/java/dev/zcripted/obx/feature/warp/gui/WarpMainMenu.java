package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.entity.Player;

public final class WarpMainMenu {

    private WarpMainMenu() {
    }

    public static void open(ObxPlugin plugin, Player player, int page, String categoryFilter, String searchTerm, boolean adminMode, WarpMenuHolder.BackTarget backTarget) {
        WarpMenu.openMain(plugin, player, page, categoryFilter, searchTerm, adminMode, backTarget);
    }
}