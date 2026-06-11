package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.warp.service.WarpService;
import org.bukkit.entity.Player;

public final class WarpRenameInputFlow {

    private WarpRenameInputFlow() {
    }

    public static void start(ObxPlugin plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm, WarpMenuHolder.AdminAction adminAction) {
        plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager.class).promptRename(player, entry, backTarget, returnPage, categoryFilter, searchTerm, adminAction);
    }
}