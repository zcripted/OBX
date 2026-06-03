package dev.zcripted.obx.gui.player;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.storage.WarpService;
import org.bukkit.entity.Player;

public final class WarpVisibilityMenu {

    private WarpVisibilityMenu() {
    }

    public static void open(Main plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openVisibilityMenu(plugin, player, entry, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
