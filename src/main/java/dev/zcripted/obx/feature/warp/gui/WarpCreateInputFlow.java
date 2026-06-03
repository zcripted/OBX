package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.OBX;
import org.bukkit.entity.Player;

public final class WarpCreateInputFlow {

    private WarpCreateInputFlow() {
    }

    public static void start(OBX plugin, Player player, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        plugin.getWarpMenuInputManager().promptCreate(player, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
