package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.feature.staff.gui.StaffMenuInputManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Cancels the chat event for staff members currently being prompted for a
 * player-search string in the {@code /staff} GUI, and forwards the message
 * body to {@link StaffMenuInputManager} so the search resolves on the
 * staff member's region thread.
 */
public final class StaffMenuInputListener implements Listener {

    private final StaffMenuInputManager inputManager;

    public StaffMenuInputListener(StaffMenuInputManager inputManager) {
        this.inputManager = inputManager;
    }

    // LOWEST: must run before the chat feature's ChatManagementListener (HIGHEST),
    // which dispatches the formatted line to recipients itself — a later cancel
    // would arrive after the prompt input is already in everyone's chat.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!inputManager.isPending(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        inputManager.handleInput(player, event.getMessage());
    }
}