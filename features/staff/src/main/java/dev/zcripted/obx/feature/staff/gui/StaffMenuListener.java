package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.staff.gui.StaffActionMenu;
import dev.zcripted.obx.feature.staff.gui.StaffActionMenuHolder;
import dev.zcripted.obx.feature.staff.gui.StaffMenu;
import dev.zcripted.obx.feature.staff.gui.StaffMenuHolder;
import dev.zcripted.obx.feature.staff.gui.StaffMenuInputManager;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Routes clicks on the {@code /staff} main menu and per-player action
 * sub-menu. All clicks on these menus are cancelled (no item movement) and
 * dispatched by raw slot. Drag events are also cancelled so a careless
 * shift-drag can't shuffle staff heads around.
 */
public final class StaffMenuListener implements Listener {

    private final ObxPlugin plugin;

    public StaffMenuListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null) {
            return;
        }
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof StaffMenuHolder) && !(holder instanceof StaffActionMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) {
            return;
        }
        Player viewer = (Player) clicker;

        // Defense-in-depth: the staff menu and its per-player action sub-menu
        // trigger moderation flows (warn/mute/kick/tempban/ban). Re-verify the
        // gating permission on every click rather than trusting it held since the
        // menu opened — it may have been revoked, or the inventory handed off.
        if (!viewer.hasPermission("obx.staff.menu")) {
            plugin.getLanguageManager().send(viewer, "core.no-permission");
            viewer.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        if (holder instanceof StaffMenuHolder) {
            handleStaffMenuClick(viewer, (StaffMenuHolder) holder, slot);
            return;
        }
        if (holder instanceof StaffActionMenuHolder) {
            handleActionMenuClick(viewer, (StaffActionMenuHolder) holder, slot);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null) {
            return;
        }
        InventoryHolder holder = top.getHolder();
        if (holder instanceof StaffMenuHolder || holder instanceof StaffActionMenuHolder) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    private void handleStaffMenuClick(Player viewer, StaffMenuHolder holder, int slot) {
        if (slot == holder.getCloseSlot()) {
            viewer.closeInventory();
            return;
        }
        if (slot == holder.getSearchSlot()) {
            // Close first so the chat prompt isn't obscured by the open GUI;
            // the input manager will reopen the menu on cancel/empty input
            // or hand off to the action menu on a successful match.
            viewer.closeInventory();
            StaffMenuInputManager inputManager = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.staff.gui.StaffMenuInputManager.class);
            if (inputManager != null) {
                inputManager.promptSearch(viewer, holder.getCurrentPage());
            }
            return;
        }
        if (slot == holder.getPrevPageSlot() && slot != StaffMenuHolder.NO_SLOT) {
            StaffMenu.open(plugin, viewer, holder.getCurrentPage() - 1);
            return;
        }
        if (slot == holder.getNextPageSlot() && slot != StaffMenuHolder.NO_SLOT) {
            StaffMenu.open(plugin, viewer, holder.getCurrentPage() + 1);
            return;
        }
        if (slot == holder.getViewerSlot()) {
            // Viewer's own head is purely an accountability marker — clicking
            // it is intentionally a no-op so it never opens an action flow
            // against the operator themselves.
            return;
        }
        UUID targetUuid = holder.playerAt(slot);
        if (targetUuid == null) {
            return;
        }
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            // Player logged off between GUI render and click; reopen the
            // staff menu at the same page so the listing refreshes without
            // the stale entry but the operator stays where they were.
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetUuid);
            String fallbackName = offline != null && offline.getName() != null ? offline.getName() : "?";
            plugin.getLanguageManager().send(viewer, "admin.staff.search.not-online",
                    Placeholders.with("player", fallbackName));
            StaffMenu.open(plugin, viewer, holder.getCurrentPage());
            return;
        }
        StaffActionMenu.open(plugin, viewer, target.getUniqueId(), target.getName());
    }

    private void handleActionMenuClick(Player viewer, StaffActionMenuHolder holder, int slot) {
        if (slot == StaffActionMenu.SLOT_CLOSE) {
            viewer.closeInventory();
            return;
        }
        if (slot == StaffActionMenu.SLOT_BACK) {
            StaffMenu.open(plugin, viewer);
            return;
        }
        String action = actionForSlot(slot);
        if (action == null) {
            return;
        }
        // Per-action gate: don't prompt for a reason the viewer can't act on.
        // The command re-checks too, but this avoids a dead-end prompt.
        if (!viewer.hasPermission("obx.moderation." + action)) {
            plugin.getLanguageManager().send(viewer, "core.no-permission");
            return;
        }
        // Close the GUI and prompt for a reason in chat; the captured reason runs
        // the matching /warn|/mute|/kick|/tempban|/ban command (existing box-style
        // response + permission checks). Falls back to a plain message if the
        // input manager is unavailable.
        StaffMenuInputManager inputManager = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.staff.gui.StaffMenuInputManager.class);
        if (inputManager == null) {
            return;
        }
        viewer.closeInventory();
        inputManager.promptAction(viewer, holder.getTargetUuid(), holder.getTargetName(), action);
    }

    private String actionForSlot(int slot) {
        if (slot == StaffActionMenu.SLOT_WARN) return "warn";
        if (slot == StaffActionMenu.SLOT_MUTE) return "mute";
        if (slot == StaffActionMenu.SLOT_KICK) return "kick";
        if (slot == StaffActionMenu.SLOT_TEMPBAN) return "tempban";
        if (slot == StaffActionMenu.SLOT_BAN) return "ban";
        return null;
    }
}