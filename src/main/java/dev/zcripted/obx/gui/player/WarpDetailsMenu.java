package dev.zcripted.obx.gui.player;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.storage.WarpService;
import org.bukkit.entity.Player;

public final class WarpDetailsMenu {

    private WarpDetailsMenu() {
    }

    public static void open(Main plugin, Player player, WarpService.WarpEntry entry, boolean adminMode, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openDetails(plugin, player, entry, adminMode, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
