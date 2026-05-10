package dev.sergeantfuzzy.sfcore.gui.player;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.WarpService;
import org.bukkit.entity.Player;

public final class WarpIconPickerMenu {

    private WarpIconPickerMenu() {
    }

    public static void open(Main plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openIconPicker(plugin, player, entry, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
