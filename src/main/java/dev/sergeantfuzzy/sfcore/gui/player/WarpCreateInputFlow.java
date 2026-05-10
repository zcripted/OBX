package dev.sergeantfuzzy.sfcore.gui.player;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.entity.Player;

public final class WarpCreateInputFlow {

    private WarpCreateInputFlow() {
    }

    public static void start(Main plugin, Player player, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        plugin.getWarpMenuInputManager().promptCreate(player, backTarget, returnPage, categoryFilter, searchTerm);
    }
}
