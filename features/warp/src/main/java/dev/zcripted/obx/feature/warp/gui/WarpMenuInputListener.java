package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class WarpMenuInputListener implements Listener {

    private final WarpMenuInputManager inputManager;

    public WarpMenuInputListener(WarpMenuInputManager inputManager) {
        this.inputManager = inputManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        WarpMenuInputManager.PendingInput input = inputManager.get(player.getUniqueId());
        if (input == null) {
            return;
        }
        event.setCancelled(true);
        inputManager.handleInput(player, input, event.getMessage());
    }

    /**
     * Drops any pending prompt when the player leaves. Without this the entry
     * leaked forever AND the HIGHEST-priority chat hook above would swallow the
     * player's first chat line when they rejoined.
     */
    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        inputManager.clear(event.getPlayer().getUniqueId());
    }
}
