package dev.zcripted.obx.gui.player;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.storage.WarpService;
import org.bukkit.entity.Player;

public final class WarpRenameInputFlow {

    private WarpRenameInputFlow() {
    }

    public static void start(Main plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm, WarpMenuHolder.AdminAction adminAction) {
        plugin.getWarpMenuInputManager().promptRename(player, entry, backTarget, returnPage, categoryFilter, searchTerm, adminAction);
    }
}
