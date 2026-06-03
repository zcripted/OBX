package dev.zcripted.obx.gui.player;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.storage.WarpService;
import org.bukkit.entity.Player;

public final class WarpIconPickerMenu {

    private WarpIconPickerMenu() {
    }

    public static void open(OBX plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openIconPicker(plugin, player, entry, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
