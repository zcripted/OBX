package dev.sergeantfuzzy.sfcore.gui.player;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.WarpService;
import org.bukkit.entity.Player;

public final class WarpRenameInputFlow {

    private WarpRenameInputFlow() {
    }

    public static void start(Main plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm, WarpMenuHolder.AdminAction adminAction) {
        plugin.getWarpMenuInputManager().promptRename(player, entry, backTarget, returnPage, categoryFilter, searchTerm, adminAction);
    }
}
