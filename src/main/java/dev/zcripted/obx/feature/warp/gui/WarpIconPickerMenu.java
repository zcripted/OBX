package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.warp.service.WarpService;
import org.bukkit.entity.Player;

public final class WarpIconPickerMenu {

    private WarpIconPickerMenu() {
    }

    public static void open(OBX plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openIconPicker(plugin, player, entry, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
