package dev.zcripted.obx.gui.player;

import dev.zcripted.obx.Main;
import org.bukkit.entity.Player;

public final class WarpMainMenu {

    private WarpMainMenu() {
    }

    public static void open(Main plugin, Player player, int page, String categoryFilter, String searchTerm, boolean adminMode, WarpMenuHolder.BackTarget backTarget) {
        WarpMenu.openMain(plugin, player, page, categoryFilter, searchTerm, adminMode, backTarget);
    }
}
