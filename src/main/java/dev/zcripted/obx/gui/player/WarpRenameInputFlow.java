package dev.zcripted.obx.gui.player;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.storage.WarpService;
import org.bukkit.entity.Player;

public final class WarpRenameInputFlow {

    private WarpRenameInputFlow() {
    }

    public static void start(OBX plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm, WarpMenuHolder.AdminAction adminAction) {
        plugin.getWarpMenuInputManager().promptRename(player, entry, backTarget, returnPage, categoryFilter, searchTerm, adminAction);
    }
}
