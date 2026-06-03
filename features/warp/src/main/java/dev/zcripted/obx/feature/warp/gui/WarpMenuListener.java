
package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.staff.gui.AdminMenu;
import dev.zcripted.obx.core.gui.main.MainMenu;
import dev.zcripted.obx.feature.warp.gui.WarpMenu;
import dev.zcripted.obx.feature.warp.gui.WarpMenuHolder;
import dev.zcripted.obx.core.gui.WarpMenuStyling;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.feature.warp.service.WarpService;
import dev.zcripted.obx.feature.warp.service.WarpAccess;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class WarpMenuListener implements Listener {

    private static final int PAGE_SIZE = WarpMenuStyling.CONTENT_END - WarpMenuStyling.CONTENT_START + 1;
    private static final int SLOT_MANAGER_CREATE = 10;
    private static final int SLOT_MANAGER_DELETE = 12;
    private static final int SLOT_MANAGER_RENAME = 14;
    private static final int SLOT_MANAGER_MOVE = 16;
    private static final int SLOT_MANAGER_ICON = 28;
    private static final int SLOT_MANAGER_VISIBILITY = 30;
    private static final int SLOT_MANAGER_HIDDEN_TOGGLE = 32;

    private final ObxPlugin plugin;

    public WarpMenuListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WarpMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        WarpMenuHolder holder = (WarpMenuHolder) event.getInventory().getHolder();
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) {
            return;
        }

        switch (holder.getView()) {
            case MAIN:
                handleMainClick(event.getClick(), rawSlot, player, holder);
                break;
            case CATEGORIES:
                handleCategoriesClick(rawSlot, player, holder);
                break;
            case DETAILS:
                handleDetailsClick(event.getClick(), rawSlot, player, holder);
                break;
            case MANAGE:
                handleManageClick(event.getClick(), rawSlot, player, holder);
                break;
            case MANAGE_SELECT:
                handleManageSelect(event.getClick(), rawSlot, player, holder);
                break;
            case CONFIRM_DELETE:
                handleConfirmDelete(rawSlot, player, holder);
                break;
            case CONFIRM_MOVE:
                handleConfirmMove(rawSlot, player, holder);
                break;
            case CONFIRM_OVERWRITE:
                handleConfirmOverwrite(rawSlot, player, holder);
                break;
            case ICON_PICKER:
                handleIconPicker(rawSlot, player, holder);
                break;
            case VISIBILITY:
                handleVisibility(rawSlot, player, holder);
                break;
            default:
                break;
        }
    }
    private void handleMainClick(ClickType click, int slot, Player player, WarpMenuHolder holder) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            handleBack(player, holder);
            return;
        }
        if (slot == 52 && holder.getPage() > 0) {
            WarpMenu.openMain(plugin, player, holder.getPage() - 1, holder.getCategoryFilter(), holder.getSearchTerm(), holder.isAdminMode(), holder.getBackTarget(), holder.getAdminAction());
            return;
        }
        if (slot == 53) {
            WarpMenu.openMain(plugin, player, holder.getPage() + 1, holder.getCategoryFilter(), holder.getSearchTerm(), holder.isAdminMode(), holder.getBackTarget(), holder.getAdminAction());
            return;
        }
        if (slot == 4) {
            if (holder.getCategoryFilter() == null) {
                WarpMenu.openCategories(plugin, player, 0, holder.isAdminMode(), holder.getBackTarget());
            } else {
                WarpMenu.openMain(plugin, player, 0, null, holder.getSearchTerm(), holder.isAdminMode(), holder.getBackTarget(), holder.getAdminAction());
            }
            return;
        }
        if (slot == 7) {
            plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager.class).promptSearch(player, holder.isAdminMode(), holder.getBackTarget(), holder.getCategoryFilter());
            player.closeInventory();
            return;
        }
        if (slot == 8 && holder.isAdminMode()) {
            WarpMenu.openManage(plugin, player, 0, holder.getCategoryFilter(), holder.getSearchTerm(), WarpMenuHolder.BackTarget.WARP_MAIN);
            return;
        }

        if (slot < 9 || slot > 44 || holder.getWarpKeys() == null) {
            return;
        }
        int offset = slot - 9;
        int index = holder.getPage() * PAGE_SIZE + offset;
        if (index < 0 || index >= holder.getWarpKeys().size()) {
            return;
        }
        String warpKey = holder.getWarpKeys().get(index);
        WarpService.WarpEntry entry = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(warpKey);
        if (entry == null) {
            languages.send(player, "teleport.warp.not-found", Placeholders.with("warp", warpKey));
            WarpMenu.openMain(plugin, player, holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.isAdminMode(), holder.getBackTarget(), holder.getAdminAction());
            return;
        }
        boolean manageBypass = holder.isAdminMode() && player.hasPermission("obx.warp.manage");

        if (click == ClickType.SHIFT_RIGHT && holder.isAdminMode()) {
            WarpMenu.openConfirmDelete(plugin, player, entry, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
            return;
        }
        if (click == ClickType.SHIFT_LEFT && holder.isAdminMode()) {
            WarpMenu.openConfirmMove(plugin, player, entry, player.getLocation(), holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
            return;
        }
        if (click.isRightClick()) {
            WarpMenu.openDetails(plugin, player, entry, holder.isAdminMode(), holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
            return;
        }

        switch (holder.getAdminAction()) {
            case DELETE:
                WarpMenu.openConfirmDelete(plugin, player, entry, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                return;
            case MOVE:
                WarpMenu.openConfirmMove(plugin, player, entry, player.getLocation(), holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                return;
            case RENAME:
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager.class).promptRename(player, entry, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getAdminAction());
                player.closeInventory();
                return;
            case ICON:
                WarpMenu.openIconPicker(plugin, player, entry, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                return;
            case VISIBILITY:
                WarpMenu.openVisibilityMenu(plugin, player, entry, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                return;
            default:
                break;
        }

        if (!manageBypass && !WarpAccess.canUse(entry, player)) {
            languages.send(player, "teleport.warp.no-access", Placeholders.with("warp", entry.getName()));
            return;
        }
        Location location = entry.getLocation();
        if (location == null) {
            languages.send(player, "teleport.warp.invalid-location", Placeholders.with("warp", entry.getName()));
            return;
        }
        plugin.getDataService().setBack(player.getUniqueId(), player.getLocation());
        plugin.getTeleportManager().teleportPlayer(player, location, "teleport.warp.teleporting", Placeholders.with("warp", entry.getName()));
        player.closeInventory();
    }

    private void handleCategoriesClick(int slot, Player player, WarpMenuHolder holder) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            handleBack(player, holder);
            return;
        }
        if (slot == 4) {
            WarpMenu.openMain(plugin, player, 0, null, null, holder.isAdminMode(), holder.getBackTarget(), WarpMenuHolder.AdminAction.NONE);
            return;
        }
        if (slot < 9 || slot > 44 || holder.getCategories() == null) {
            return;
        }
        int index = slot - 9;
        if (index >= holder.getCategories().size()) {
            return;
        }
        String category = holder.getCategories().get(index);
        WarpMenu.openMain(plugin, player, 0, category, null, holder.isAdminMode(), WarpMenuHolder.BackTarget.CATEGORIES, WarpMenuHolder.AdminAction.NONE);
    }

    private void handleDetailsClick(ClickType click, int slot, Player player, WarpMenuHolder holder) {
        WarpService.WarpEntry entry = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(holder.getWarpKey());
        if (entry == null) {
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 31) {
            WarpMenu.openMain(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.isAdminMode(), holder.getBackTarget(), WarpMenuHolder.AdminAction.NONE);
            return;
        }
        if (slot == 13) {
            if (!WarpAccess.canUse(entry, player)) {
                plugin.getLanguageManager().send(player, "teleport.warp.no-access", Placeholders.with("warp", entry.getName()));
                return;
            }
            Location location = entry.getLocation();
            if (location == null) {
                plugin.getLanguageManager().send(player, "teleport.warp.invalid-location", Placeholders.with("warp", entry.getName()));
                return;
            }
            plugin.getDataService().setBack(player.getUniqueId(), player.getLocation());
            plugin.getTeleportManager().teleportPlayer(player, location, "teleport.warp.teleporting", Placeholders.with("warp", entry.getName()));
            player.closeInventory();
            return;
        }
        if (!holder.isAdminMode()) {
            return;
        }
        switch (slot) {
            case 45:
                WarpMenu.openConfirmMove(plugin, player, entry, player.getLocation(), holder.getBackTarget(), holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                break;
            case 46:
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager.class).promptRename(player, entry, holder.getBackTarget(), holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), WarpMenuHolder.AdminAction.RENAME);
                player.closeInventory();
                break;
            case 47:
                WarpMenu.openIconPicker(plugin, player, entry, holder.getBackTarget(), holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                break;
            case 48:
                WarpMenu.openVisibilityMenu(plugin, player, entry, holder.getBackTarget(), holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                break;
            case 49:
                WarpMenu.openConfirmDelete(plugin, player, entry, holder.getBackTarget(), holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                break;
            default:
                break;
        }
    }
    private void handleManageClick(ClickType click, int slot, Player player, WarpMenuHolder holder) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            handleBack(player, holder);
            return;
        }
        if (slot == 52 && holder.getPage() > 0) {
            WarpMenu.openManage(plugin, player, holder.getPage() - 1, holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
            return;
        }
        if (slot == 53) {
            WarpMenu.openManage(plugin, player, holder.getPage() + 1, holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
            return;
        }
        if (slot == 4) {
            if (holder.getCategoryFilter() == null) {
                WarpMenu.openCategories(plugin, player, 0, true, holder.getBackTarget());
            } else {
                WarpMenu.openManage(plugin, player, 0, null, holder.getSearchTerm(), holder.getBackTarget());
            }
            return;
        }
        if (slot == 7) {
            plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager.class).promptSearch(player, true, holder.getBackTarget(), holder.getCategoryFilter());
            player.closeInventory();
            return;
        }
        if (slot == 8) {
            WarpMenu.openManage(plugin, player, 0, holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
            return;
        }
        if (slot == SLOT_MANAGER_HIDDEN_TOGGLE) {
            WarpMenu.toggleHiddenView(player);
            WarpMenu.openManage(plugin, player, holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
            return;
        }
        if (slot == SLOT_MANAGER_CREATE) {
            plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager.class).promptCreate(player, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
            player.closeInventory();
            return;
        }
        if (slot == SLOT_MANAGER_DELETE) {
            WarpMenu.openManageSelection(plugin, player, holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget(), WarpMenuHolder.AdminAction.DELETE);
            return;
        }
        if (slot == SLOT_MANAGER_RENAME) {
            WarpMenu.openManageSelection(plugin, player, holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget(), WarpMenuHolder.AdminAction.RENAME);
            return;
        }
        if (slot == SLOT_MANAGER_MOVE) {
            WarpMenu.openManageSelection(plugin, player, holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget(), WarpMenuHolder.AdminAction.MOVE);
            return;
        }
        if (slot == SLOT_MANAGER_ICON) {
            WarpMenu.openManageSelection(plugin, player, holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget(), WarpMenuHolder.AdminAction.ICON);
            return;
        }
        if (slot == SLOT_MANAGER_VISIBILITY) {
            WarpMenu.openManageSelection(plugin, player, holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget(), WarpMenuHolder.AdminAction.VISIBILITY);
            return;
        }

        if (slot < 9 || slot > 44 || holder.getWarpKeys() == null) {
            return;
        }
        int offset = slot - 9;
        int index = holder.getPage() * PAGE_SIZE + offset;
        if (index < 0 || index >= holder.getWarpKeys().size()) {
            return;
        }
        WarpService.WarpEntry entry = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(holder.getWarpKeys().get(index));
        if (entry == null) {
            return;
        }
        if (click == ClickType.SHIFT_RIGHT) {
            WarpMenu.openConfirmDelete(plugin, player, entry, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
            return;
        }
        if (click == ClickType.SHIFT_LEFT) {
            WarpMenu.openConfirmMove(plugin, player, entry, player.getLocation(), holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
            return;
        }
        if (click.isRightClick()) {
            WarpMenu.openDetails(plugin, player, entry, true, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
            return;
        }
        if (!WarpAccess.canUse(entry, player)) {
            plugin.getLanguageManager().send(player, "teleport.warp.no-access", Placeholders.with("warp", entry.getName()));
            return;
        }
        if (entry.getLocation() == null) {
            plugin.getLanguageManager().send(player, "teleport.warp.invalid-location", Placeholders.with("warp", entry.getName()));
            return;
        }
        plugin.getTeleportManager().teleportPlayer(player, entry.getLocation(), "teleport.warp.teleporting", Placeholders.with("warp", entry.getName()));
        player.closeInventory();
    }

    private void handleManageSelect(ClickType click, int slot, Player player, WarpMenuHolder holder) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            WarpMenu.openManage(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
            return;
        }
        if (slot == 52 && holder.getPage() > 0) {
            WarpMenu.openManageSelection(plugin, player, holder.getPage() - 1, holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget(), holder.getAdminAction());
            return;
        }
        if (slot == 53) {
            WarpMenu.openManageSelection(plugin, player, holder.getPage() + 1, holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget(), holder.getAdminAction());
            return;
        }
        if (slot == 4) {
            if (holder.getCategoryFilter() == null) {
                WarpMenu.openCategories(plugin, player, 0, true, holder.getBackTarget());
            } else {
                WarpMenu.openManageSelection(plugin, player, 0, null, holder.getSearchTerm(), holder.getBackTarget(), holder.getAdminAction());
            }
            return;
        }
        if (slot == 7) {
            plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager.class).promptSearch(player, true, holder.getBackTarget(), holder.getCategoryFilter());
            player.closeInventory();
            return;
        }
        if (slot == 8) {
            WarpMenu.openManage(plugin, player, 0, holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
            return;
        }

        if (slot < 9 || slot > 44 || holder.getWarpKeys() == null) {
            return;
        }
        int offset = slot - 9;
        int index = holder.getPage() * PAGE_SIZE + offset;
        if (index < 0 || index >= holder.getWarpKeys().size()) {
            return;
        }
        WarpService.WarpEntry entry = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(holder.getWarpKeys().get(index));
        if (entry == null) {
            return;
        }
        switch (holder.getAdminAction()) {
            case DELETE:
                WarpMenu.openConfirmDelete(plugin, player, entry, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                break;
            case MOVE:
                WarpMenu.openConfirmMove(plugin, player, entry, player.getLocation(), holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                break;
            case RENAME:
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager.class).promptRename(player, entry, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getAdminAction());
                player.closeInventory();
                break;
            case ICON:
                WarpMenu.openIconPicker(plugin, player, entry, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                break;
            case VISIBILITY:
                WarpMenu.openVisibilityMenu(plugin, player, entry, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                break;
            default:
                WarpMenu.openDetails(plugin, player, entry, true, holder.getBackTarget(), holder.getPage(), holder.getCategoryFilter(), holder.getSearchTerm());
                break;
        }
    }

    private void handleConfirmDelete(int slot, Player player, WarpMenuHolder holder) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 24) {
            WarpMenu.openManage(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
            return;
        }
        if (slot == 45) {
            handleBack(player, holder);
            return;
        }
        if (slot != 20) {
            return;
        }
        WarpService.WarpEntry entry = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(holder.getWarpKey());
        if (entry == null) {
            return;
        }
        plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).deleteWarp(entry.getKey());
        plugin.getLanguageManager().send(player, "teleport.warp.deleted", Placeholders.with("warp", entry.getName()));
        WarpMenu.openManage(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
    }

    private void handleConfirmMove(int slot, Player player, WarpMenuHolder holder) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 24) {
            WarpMenu.openManage(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
            return;
        }
        if (slot == 45) {
            handleBack(player, holder);
            return;
        }
        if (slot != 20) {
            return;
        }
        WarpService.WarpEntry entry = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(holder.getWarpKey());
        if (entry == null) {
            return;
        }
        Location target = holder.getPendingLocation() != null ? holder.getPendingLocation() : player.getLocation();
        plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).moveWarp(entry.getKey(), target, player.getUniqueId(), player.getName());
        plugin.getLanguageManager().send(player, "teleport.warp.move.success", Placeholders.with("warp", entry.getName()));
        WarpMenu.openManage(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
    }

    private void handleConfirmOverwrite(int slot, Player player, WarpMenuHolder holder) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 24) {
            WarpMenu.openManage(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
            return;
        }
        if (slot == 45) {
            handleBack(player, holder);
            return;
        }
        if (slot != 20) {
            return;
        }
        String warpName = holder.getWarpName() != null ? holder.getWarpName() : holder.getWarpKey();
        Location location = holder.getPendingLocation() != null ? holder.getPendingLocation() : player.getLocation();
        boolean existed = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(warpName) != null;
        plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).setWarp(warpName, location, "general", null, true, null, player.getUniqueId(), player.getName());
        plugin.getLanguageManager().send(player, existed ? "teleport.warp.set.updated" : "teleport.warp.set.created", Placeholders.with("warp", warpName));
        WarpMenu.openManage(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
    }

    private void handleIconPicker(int slot, Player player, WarpMenuHolder holder) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            handleBack(player, holder);
            return;
        }
        WarpService.WarpEntry entry = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(holder.getWarpKey());
        if (entry == null) {
            return;
        }
        if (slot == SLOT_MANAGER_VISIBILITY) {
            plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).setIcon(entry.getKey(), null);
            plugin.getLanguageManager().send(player, "teleport.warp.icon.cleared", Placeholders.with("warp", entry.getName()));
            reopenAfterEdit(player, holder, entry);
            return;
        }
        if (slot < 9 || slot > 44) {
            return;
        }
        org.bukkit.inventory.ItemStack clicked = player.getOpenInventory().getTopInventory().getItem(slot);
        if (clicked == null || clicked.getType() == org.bukkit.Material.AIR) {
            return;
        }
        plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).setIcon(entry.getKey(), clicked.getType().name());
        plugin.getLanguageManager().send(player, "teleport.warp.icon.updated", Placeholders.with("warp", entry.getName(), "icon", clicked.getType().name()));
        reopenAfterEdit(player, holder, plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(entry.getKey()));
    }

    private void handleVisibility(int slot, Player player, WarpMenuHolder holder) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            handleBack(player, holder);
            return;
        }
        WarpService.WarpEntry entry = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(holder.getWarpKey());
        if (entry == null) {
            return;
        }
        if (slot == 20 || slot == 24) {
            boolean newState = slot == 20;
            plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).setPublic(entry.getKey(), newState);
            String stateLabel = newState ? plugin.getLanguageManager().get(player, "teleport.warp.visibility.public") : plugin.getLanguageManager().get(player, "teleport.warp.visibility.hidden");
            plugin.getLanguageManager().send(player, "teleport.warp.public.updated", Placeholders.with("warp", entry.getName(), "state", stateLabel));
            reopenAfterEdit(player, holder, plugin.getServiceRegistry().get(dev.zcripted.obx.feature.warp.service.WarpService.class).getWarp(entry.getKey()));
        }
    }

    private void reopenAfterEdit(Player player, WarpMenuHolder holder, WarpService.WarpEntry entry) {
        if (holder.getBackTarget() == holder.getBackTarget()) {
            WarpMenu.openManage(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
        } else {
            WarpMenu.openDetails(plugin, player, entry, holder.isAdminMode(), holder.getBackTarget(), holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm());
        }
    }

    private void handleBack(Player player, WarpMenuHolder holder) {
        switch (holder.getBackTarget()) {
            case ADMIN_MENU:
                AdminMenu.open(plugin, player);
                return;
            case WARP_MANAGE:
                WarpMenu.openManage(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.getBackTarget());
                return;
            case CATEGORIES:
                WarpMenu.openCategories(plugin, player, 0, holder.isAdminMode(), holder.getBackTarget());
                return;
            case MAIN_MENU:
                MainMenu.open(plugin, player);
                return;
            case WARP_MAIN:
                WarpMenu.openMain(plugin, player, holder.getReturnPage(), holder.getCategoryFilter(), holder.getSearchTerm(), holder.isAdminMode(), WarpMenuHolder.BackTarget.MAIN_MENU, WarpMenuHolder.AdminAction.NONE);
                return;
            default:
                WarpMenu.openMain(plugin, player, 0, null, null, holder.isAdminMode(), holder.getBackTarget(), WarpMenuHolder.AdminAction.NONE);
        }
    }
}
