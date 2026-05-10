package dev.sergeantfuzzy.sfcore.gui.player;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.WarpService;
import org.bukkit.entity.Player;

public final class WarpDetailsMenu {

    private WarpDetailsMenu() {
    }

    public static void open(Main plugin, Player player, WarpService.WarpEntry entry, boolean adminMode, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenu.openDetails(plugin, player, entry, adminMode, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
