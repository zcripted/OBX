
package dev.sergeantfuzzy.sfcore.gui.player;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.gui.admin.AdminMenu;
import dev.sergeantfuzzy.sfcore.gui.shared.WarpMenuStyling;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.storage.WarpService;
import dev.sergeantfuzzy.sfcore.util.teleport.WarpAccess;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central warp menu builder and navigator. Handles main browsing, categories, details, manage hub,
 * admin confirmations, and optional icon/visibility editors.
 */
public final class WarpMenu {

    private static final int INVENTORY_SIZE = WarpMenuStyling.INVENTORY_SIZE;
    private static final int CONTENT_START = WarpMenuStyling.CONTENT_START;
    private static final int CONTENT_END = WarpMenuStyling.CONTENT_END;
    private static final int SLOT_CATEGORIES = 4;
    private static final int SLOT_SEARCH = 7;
    private static final int SLOT_MANAGE = 8;
    private static final int SLOT_BACK = WarpMenuStyling.SLOT_BACK;
    private static final int SLOT_CLOSE = WarpMenuStyling.SLOT_CLOSE;
    private static final int SLOT_PREVIOUS = WarpMenuStyling.SLOT_PREVIOUS;
    private static final int SLOT_NEXT = WarpMenuStyling.SLOT_NEXT;
    private static final int SLOT_TELEPORT = 13;
    private static final int SLOT_INFO = 22;
    private static final int SLOT_DETAILS_BACK = 31;
    private static final int SLOT_ADMIN_MOVE = 45;
    private static final int SLOT_ADMIN_RENAME = 46;
    private static final int SLOT_ADMIN_ICON = 47;
    private static final int SLOT_ADMIN_VISIBILITY = 48;
    private static final int SLOT_ADMIN_DELETE = 49;
    private static final int SLOT_MANAGER_CREATE = 10;
    private static final int SLOT_MANAGER_DELETE = 12;
    private static final int SLOT_MANAGER_RENAME = 14;
    private static final int SLOT_MANAGER_MOVE = 16;
    private static final int SLOT_MANAGER_ICON = 28;
    private static final int SLOT_MANAGER_VISIBILITY = 30;
    private static final int SLOT_MANAGER_HIDDEN_TOGGLE = 32;
    private static final int PAGE_SIZE = CONTENT_END - CONTENT_START + 1;
    private static final ItemStack FILLER = WarpMenuStyling.createFiller();
    private static final Set<UUID> hiddenViewDisabled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private WarpMenu() {
    }

    public static void openMain(Main plugin, Player player, int page, String categoryFilter, String searchTerm, boolean adminMode, WarpMenuHolder.BackTarget backTarget) {
        openMain(plugin, player, page, categoryFilter, searchTerm, adminMode, backTarget, WarpMenuHolder.AdminAction.NONE);
    }

    public static void openMain(Main plugin, Player player, int page, String categoryFilter, String searchTerm, boolean adminMode, WarpMenuHolder.BackTarget backTarget, WarpMenuHolder.AdminAction adminAction) {
        LanguageManager languages = plugin.getLanguageManager();
        boolean includeHidden = shouldIncludeHidden(player, adminMode);
        boolean manageBypass = adminMode && player.hasPermission("sfcore.warp.manage");
        List<WarpService.WarpEntry> warps = collectWarps(plugin.getWarpService(), player, categoryFilter, searchTerm, includeHidden, manageBypass);
        if (warps.isEmpty()) {
            if (searchTerm != null && !searchTerm.isEmpty()) {
                languages.send(player, "teleport.warp.gui.search-empty");
            } else if (categoryFilter == null) {
                languages.send(player, "teleport.warp.no-warps");
            } else {
                languages.send(player, "teleport.warp.no-warps-category", Placeholders.with("category", categoryFilter));
            }
            return;
        }

        int maxPages = Math.max(1, (int) Math.ceil(warps.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, maxPages - 1));
        List<String> warpKeys = new ArrayList<>();
        for (WarpService.WarpEntry entry : warps) {
            warpKeys.add(entry.getKey());
        }

        String title = WarpMenuStyling.gradientTitle(categoryFilter == null ? "Warps" : "Warps: " + categoryFilter);
        WarpMenuHolder holder = new WarpMenuHolder(plugin, WarpMenuHolder.View.MAIN, safePage, categoryFilter, searchTerm, warpKeys, null, null, null, adminMode, backTarget, adminAction, safePage, null);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inventory);

        fill(inventory);
        applyListControls(inventory, player, categoryFilter, searchTerm, adminMode, backTarget, maxPages, safePage);

        int startIndex = safePage * PAGE_SIZE;
        int slot = CONTENT_START;
        for (int index = startIndex; index < warps.size() && slot <= CONTENT_END; index++, slot++) {
            WarpService.WarpEntry entry = warps.get(index);
            inventory.setItem(slot, createWarpItem(entry, player, adminMode, adminAction, manageBypass));
        }

        player.openInventory(inventory);
    }

    public static void openCategories(Main plugin, Player player, int page, boolean adminMode, WarpMenuHolder.BackTarget backTarget) {
        List<String> categories = new ArrayList<>(plugin.getWarpService().categories());
        if (categories.isEmpty()) {
            plugin.getLanguageManager().send(player, "teleport.warp.categories.none");
            return;
        }
        int maxPages = Math.max(1, (int) Math.ceil(categories.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, maxPages - 1));
        String title = WarpMenuStyling.gradientTitle("Warp Categories");
        WarpMenuHolder holder = new WarpMenuHolder(plugin, WarpMenuHolder.View.CATEGORIES, safePage, null, null, null, categories, null, null, adminMode, backTarget, WarpMenuHolder.AdminAction.NONE, 0, null);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inventory);
        fill(inventory);

        inventory.setItem(SLOT_BACK, backButton(backTarget));
        inventory.setItem(SLOT_CLOSE, closeButton());
        inventory.setItem(SLOT_CATEGORIES, WarpMenuStyling.item(resolveMaterial("COMPASS", "BOOK"), goldBold("Show All Warps"), WarpMenuStyling.lore(ChatColor.GRAY + "View every warp without filters.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Show all")));

        int startIndex = safePage * PAGE_SIZE;
        int slot = CONTENT_START;
        Map<String, Integer> counts = plugin.getWarpService().categoryCounts();
        for (int index = startIndex; index < categories.size() && slot <= CONTENT_END; index++, slot++) {
            String category = categories.get(index);
            int count = counts.getOrDefault(category, 0);
            List<String> lore = WarpMenuStyling.lore(ChatColor.GRAY + "Warps:" + ChatColor.WHITE + " " + count, "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " View warps in this category");
            inventory.setItem(slot, WarpMenuStyling.item(resolveMaterial("PAPER", "BOOK"), goldBold(category), lore));
        }

        if (safePage > 0) {
            inventory.setItem(SLOT_PREVIOUS, pageButton(false, safePage));
        }
        if (safePage < maxPages - 1) {
            inventory.setItem(SLOT_NEXT, pageButton(true, safePage + 2));
        }

        player.openInventory(inventory);
    }

    public static void openDetails(Main plugin, Player player, WarpService.WarpEntry entry, boolean adminMode, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        if (entry == null) {
            return;
        }
        String title = WarpMenuStyling.gradientTitle("Warp: " + entry.getName());
        WarpMenuHolder holder = new WarpMenuHolder(plugin, WarpMenuHolder.View.DETAILS, 0, categoryFilter, searchTerm, null, null, entry.getKey(), entry.getName(), adminMode, backTarget, WarpMenuHolder.AdminAction.NONE, returnPage, null);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inventory);
        fill(inventory);

        inventory.setItem(SLOT_TELEPORT, WarpMenuStyling.item(resolveMaterial(entry.getIcon(), "ENDER_PEARL"), goldBold("Teleport"), WarpMenuStyling.lore(ChatColor.GRAY + "Teleport to this warp.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Teleport")));
        inventory.setItem(SLOT_INFO, createInfoItem(entry));
        inventory.setItem(SLOT_DETAILS_BACK, backButton(backTarget));
        inventory.setItem(SLOT_CLOSE, closeButton());

        if (adminMode && hasAnyManagePermission(player)) {
            inventory.setItem(SLOT_ADMIN_MOVE, WarpMenuStyling.item(resolveMaterial("EMERALD_BLOCK", "EMERALD"), ChatColor.GREEN + "" + ChatColor.BOLD + "Move Warp", WarpMenuStyling.lore(ChatColor.GRAY + "Update warp to your location.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Confirm move")));
            inventory.setItem(SLOT_ADMIN_RENAME, WarpMenuStyling.item(resolveMaterial("NAME_TAG", "PAPER"), ChatColor.YELLOW + "" + ChatColor.BOLD + "Rename Warp", WarpMenuStyling.lore(ChatColor.GRAY + "Keep metadata, change the name.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Rename in chat")));
            inventory.setItem(SLOT_ADMIN_ICON, WarpMenuStyling.item(resolveMaterial("ITEM_FRAME", "PAINTING"), ChatColor.GOLD + "" + ChatColor.BOLD + "Change Icon", WarpMenuStyling.lore(ChatColor.GRAY + "Set a new display material.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Open icon picker")));
            inventory.setItem(SLOT_ADMIN_VISIBILITY, WarpMenuStyling.item(resolveMaterial("REDSTONE_LAMP", "REDSTONE_LAMP_OFF"), ChatColor.YELLOW + "" + ChatColor.BOLD + "Toggle Visibility", WarpMenuStyling.lore(ChatColor.GRAY + "Public:" + ChatColor.WHITE + " " + entry.isPublic(), ChatColor.GRAY + "Permission:" + ChatColor.WHITE + " " + (entry.getPermission() == null ? "none" : entry.getPermission()), "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Toggle public/hidden")));
            inventory.setItem(SLOT_ADMIN_DELETE, WarpMenuStyling.item(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "Delete Warp", WarpMenuStyling.lore(ChatColor.RED + "Danger: permanently deletes.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Confirm delete")));
        }

        player.openInventory(inventory);
    }
    public static void openManage(Main plugin, Player player, int page, String categoryFilter, String searchTerm, WarpMenuHolder.BackTarget backTarget) {
        boolean includeHidden = shouldIncludeHidden(player, true);
        int safePage = 0;
        int maxPages = 1;

        WarpMenuHolder holder = new WarpMenuHolder(plugin, WarpMenuHolder.View.MANAGE, safePage, categoryFilter, searchTerm, null, null, null, null, true, backTarget, WarpMenuHolder.AdminAction.NONE, safePage, null);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, WarpMenuStyling.gradientTitle("Warp Manager"));
        holder.setInventory(inventory);
        fill(inventory);

        inventory.setItem(SLOT_BACK, backButton(backTarget));
        inventory.setItem(SLOT_CLOSE, closeButton());
        applyListControls(inventory, player, categoryFilter, searchTerm, true, backTarget, maxPages, safePage);

        inventory.setItem(SLOT_MANAGER_CREATE, WarpMenuStyling.item(resolveMaterial("LIME_CONCRETE", "EMERALD_BLOCK"), goldBold("Create / Set"), WarpMenuStyling.lore(ChatColor.GRAY + "Create a warp at your location.", ChatColor.GRAY + "Existing names prompt overwrite.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Start create")));
        inventory.setItem(SLOT_MANAGER_DELETE, WarpMenuStyling.item(resolveMaterial("RED_CONCRETE", "REDSTONE_BLOCK"), ChatColor.RED + "" + ChatColor.BOLD + "Delete Warp", WarpMenuStyling.lore(ChatColor.RED + "DANGER: Deletes permanently.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Select warp to delete")));
        inventory.setItem(SLOT_MANAGER_RENAME, WarpMenuStyling.item(resolveMaterial("YELLOW_CONCRETE", "ANVIL"), goldBold("Rename Warp"), WarpMenuStyling.lore(ChatColor.GRAY + "Keep metadata, change the name.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Select warp to rename")));
        inventory.setItem(SLOT_MANAGER_MOVE, WarpMenuStyling.item(resolveMaterial("CYAN_CONCRETE", "COMPASS"), goldBold("Move / Relocate"), WarpMenuStyling.lore(ChatColor.GRAY + "Update warp to your position.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Select warp to move")));
        inventory.setItem(SLOT_MANAGER_ICON, WarpMenuStyling.item(resolveMaterial("ITEM_FRAME", "PAINTING"), goldBold("Icon Editor"), WarpMenuStyling.lore(ChatColor.GRAY + "Choose a display material.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Select warp to edit icon")));
        inventory.setItem(SLOT_MANAGER_VISIBILITY, WarpMenuStyling.item(resolveMaterial("SEA_LANTERN", "GLOWSTONE"), goldBold("Visibility"), WarpMenuStyling.lore(ChatColor.GRAY + "Toggle public / hidden.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Select warp to toggle")));
        inventory.setItem(SLOT_MANAGER_HIDDEN_TOGGLE, WarpMenuStyling.item(resolveMaterial("OBSERVER", "REDSTONE_TORCH_ON"), goldBold("Hidden View"), WarpMenuStyling.lore(ChatColor.GRAY + "Hidden warps visible:" + ChatColor.WHITE + " " + includeHidden, ChatColor.DARK_GRAY + "Requires: sfcore.warp.hidden.view", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Toggle hidden visibility")));

        player.openInventory(inventory);
    }

    public static void openManageSelection(Main plugin, Player player, int page, String categoryFilter, String searchTerm, WarpMenuHolder.BackTarget backTarget, WarpMenuHolder.AdminAction action) {
        boolean includeHidden = shouldIncludeHidden(player, true);
        boolean manageBypass = player.hasPermission("sfcore.warp.manage");
        List<WarpService.WarpEntry> warps = collectWarps(plugin.getWarpService(), player, categoryFilter, searchTerm, includeHidden, manageBypass);
        if (warps.isEmpty()) {
            plugin.getLanguageManager().send(player, "teleport.warp.no-warps");
            return;
        }
        int maxPages = Math.max(1, (int) Math.ceil(warps.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, maxPages - 1));
        List<String> warpKeys = new ArrayList<>();
        for (WarpService.WarpEntry entry : warps) {
            warpKeys.add(entry.getKey());
        }
        WarpMenuHolder holder = new WarpMenuHolder(plugin, WarpMenuHolder.View.MANAGE_SELECT, safePage, categoryFilter, searchTerm, warpKeys, null, null, null, true, backTarget, action, safePage, null);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, WarpMenuStyling.gradientTitle("Warp Manager"));
        holder.setInventory(inventory);
        fill(inventory);

        applyListControls(inventory, player, categoryFilter, searchTerm, true, backTarget, maxPages, safePage);
        inventory.setItem(SLOT_BACK, backButton(WarpMenuHolder.BackTarget.WARP_MANAGE));
        inventory.setItem(SLOT_CLOSE, closeButton());
        inventory.setItem(SLOT_MANAGE, WarpMenuStyling.item(Material.NETHER_STAR, goldBold("Back to Manager"), WarpMenuStyling.lore(ChatColor.GRAY + "Return to warp manager hub.")));

        int startIndex = safePage * PAGE_SIZE;
        int slot = CONTENT_START;
        for (int index = startIndex; index < warps.size() && slot <= CONTENT_END; index++, slot++) {
            WarpService.WarpEntry entry = warps.get(index);
            inventory.setItem(slot, createWarpItem(entry, player, true, action, manageBypass));
        }

        player.openInventory(inventory);
    }

    public static void openConfirmDelete(Main plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        if (entry == null) {
            return;
        }
        WarpMenuHolder holder = new WarpMenuHolder(plugin, WarpMenuHolder.View.CONFIRM_DELETE, 0, categoryFilter, searchTerm, null, null, entry.getKey(), entry.getName(), true, backTarget, WarpMenuHolder.AdminAction.DELETE, returnPage, null);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, WarpMenuStyling.gradientTitle("Delete Warp"));
        holder.setInventory(inventory);
        fill(inventory);

        inventory.setItem(20, WarpMenuStyling.item(resolveMaterial("LIME_CONCRETE", "EMERALD_BLOCK"), ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm", WarpMenuStyling.lore(ChatColor.RED + "This cannot be undone.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Delete " + entry.getName())));
        inventory.setItem(24, WarpMenuStyling.item(resolveMaterial("RED_CONCRETE", "REDSTONE_BLOCK"), ChatColor.RED + "" + ChatColor.BOLD + "Cancel", WarpMenuStyling.lore(ChatColor.GRAY + "Go back.")));
        inventory.setItem(SLOT_BACK, backButton(backTarget));
        inventory.setItem(SLOT_CLOSE, closeButton());

        player.openInventory(inventory);
    }

    public static void openConfirmMove(Main plugin, Player player, WarpService.WarpEntry entry, Location targetLocation, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        if (entry == null || targetLocation == null) {
            return;
        }
        WarpMenuHolder holder = new WarpMenuHolder(plugin, WarpMenuHolder.View.CONFIRM_MOVE, 0, categoryFilter, searchTerm, null, null, entry.getKey(), entry.getName(), true, backTarget, WarpMenuHolder.AdminAction.MOVE, returnPage, targetLocation.clone());
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, WarpMenuStyling.gradientTitle("Move Warp"));
        holder.setInventory(inventory);
        fill(inventory);

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Current: " + formatLocation(entry.getLocation()));
        confirmLore.add(ChatColor.GRAY + "New: " + formatLocation(targetLocation));
        confirmLore.add("");
        confirmLore.add(ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Confirm move");
        inventory.setItem(20, WarpMenuStyling.item(resolveMaterial("LIME_CONCRETE", "EMERALD_BLOCK"), ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm", confirmLore));
        inventory.setItem(24, WarpMenuStyling.item(resolveMaterial("RED_CONCRETE", "REDSTONE_BLOCK"), ChatColor.RED + "" + ChatColor.BOLD + "Cancel", WarpMenuStyling.lore(ChatColor.GRAY + "Go back.")));
        inventory.setItem(SLOT_BACK, backButton(backTarget));
        inventory.setItem(SLOT_CLOSE, closeButton());

        player.openInventory(inventory);
    }

    public static void openConfirmOverwrite(Main plugin, Player player, String warpName, Location newLocation, WarpService.WarpEntry existing, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        WarpMenuHolder holder = new WarpMenuHolder(plugin, WarpMenuHolder.View.CONFIRM_OVERWRITE, 0, categoryFilter, searchTerm, null, null, existing == null ? warpName : existing.getKey(), warpName, true, backTarget, WarpMenuHolder.AdminAction.OVERWRITE, returnPage, newLocation == null ? null : newLocation.clone());
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, WarpMenuStyling.gradientTitle("Overwrite Warp"));
        holder.setInventory(inventory);
        fill(inventory);

        List<String> lore = new ArrayList<>();
        if (existing != null && existing.getLocation() != null) {
            lore.add(ChatColor.GRAY + "Existing: " + formatLocation(existing.getLocation()));
        }
        if (newLocation != null) {
            lore.add(ChatColor.GRAY + "New: " + formatLocation(newLocation));
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Overwrite warp");
        inventory.setItem(20, WarpMenuStyling.item(resolveMaterial("LIME_CONCRETE", "EMERALD_BLOCK"), ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm", lore));
        inventory.setItem(24, WarpMenuStyling.item(resolveMaterial("RED_CONCRETE", "REDSTONE_BLOCK"), ChatColor.RED + "" + ChatColor.BOLD + "Cancel", WarpMenuStyling.lore(ChatColor.GRAY + "Go back.")));
        inventory.setItem(SLOT_BACK, backButton(backTarget));
        inventory.setItem(SLOT_CLOSE, closeButton());

        player.openInventory(inventory);
    }

    public static void openIconPicker(Main plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        if (entry == null) {
            return;
        }
        WarpMenuHolder holder = new WarpMenuHolder(plugin, WarpMenuHolder.View.ICON_PICKER, 0, categoryFilter, searchTerm, null, null, entry.getKey(), entry.getName(), true, backTarget, WarpMenuHolder.AdminAction.ICON, returnPage, null);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, WarpMenuStyling.gradientTitle("Warp Icon"));
        holder.setInventory(inventory);
        fill(inventory);

        inventory.setItem(SLOT_BACK, backButton(backTarget));
        inventory.setItem(SLOT_CLOSE, closeButton());

        List<Material> choices = iconChoices();
        int slot = CONTENT_START;
        for (Material material : choices) {
            if (slot > CONTENT_END) {
                break;
            }
            List<String> lore = WarpMenuStyling.lore(ChatColor.GRAY + "Set icon to: " + ChatColor.WHITE + material.name(), "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Apply icon");
            inventory.setItem(slot++, WarpMenuStyling.item(material, goldBold(formatMaterial(material)), lore));
        }

        inventory.setItem(SLOT_MANAGER_VISIBILITY, WarpMenuStyling.item(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "Clear Icon", WarpMenuStyling.lore(ChatColor.GRAY + "Remove custom icon.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Clear")));

        player.openInventory(inventory);
    }

    public static void openVisibilityMenu(Main plugin, Player player, WarpService.WarpEntry entry, WarpMenuHolder.BackTarget backTarget, int returnPage, String categoryFilter, String searchTerm) {
        if (entry == null) {
            return;
        }
        WarpMenuHolder holder = new WarpMenuHolder(plugin, WarpMenuHolder.View.VISIBILITY, 0, categoryFilter, searchTerm, null, null, entry.getKey(), entry.getName(), true, backTarget, WarpMenuHolder.AdminAction.VISIBILITY, returnPage, null);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, WarpMenuStyling.gradientTitle("Warp Visibility"));
        holder.setInventory(inventory);
        fill(inventory);

        inventory.setItem(SLOT_BACK, backButton(backTarget));
        inventory.setItem(SLOT_CLOSE, closeButton());

        List<String> publicLore = WarpMenuStyling.lore(ChatColor.GRAY + "Make this warp public.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Set public");
        inventory.setItem(20, WarpMenuStyling.item(resolveMaterial("SEA_LANTERN", "GLOWSTONE"), goldBold("Public"), publicLore));

        List<String> hiddenLore = WarpMenuStyling.lore(ChatColor.GRAY + "Hide this warp (permission needed).", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Set hidden", ChatColor.DARK_GRAY + "Permission:" + ChatColor.GRAY + " sfcore.warp.hidden.view");
        inventory.setItem(24, WarpMenuStyling.item(resolveMaterial("GRAY_CONCRETE", "STONE"), goldBold("Hidden"), hiddenLore));

        player.openInventory(inventory);
    }

    public static void toggleHiddenView(Player player) {
        if (hiddenViewDisabled.contains(player.getUniqueId())) {
            hiddenViewDisabled.remove(player.getUniqueId());
        } else {
            hiddenViewDisabled.add(player.getUniqueId());
        }
    }

    public static boolean shouldIncludeHidden(Player player, boolean adminMode) {
        if (!adminMode || player == null) {
            return false;
        }
        if (!player.hasPermission("sfcore.warp.hidden.view")) {
            return false;
        }
        return !hiddenViewDisabled.contains(player.getUniqueId());
    }
    private static void applyListControls(Inventory inventory, Player player, String categoryFilter, String searchTerm, boolean adminMode, WarpMenuHolder.BackTarget backTarget, int maxPages, int page) {
        inventory.setItem(SLOT_CATEGORIES, categoryFilter == null
                ? WarpMenuStyling.item(resolveMaterial("COMPASS", "BOOK"), goldBold("Categories"), WarpMenuStyling.lore(ChatColor.GRAY + "Browse warps by category.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Open categories"))
                : WarpMenuStyling.item(resolveMaterial("BOOK", "PAPER"), goldBold("Clear Filter"), WarpMenuStyling.lore(ChatColor.GRAY + "Showing only " + ChatColor.WHITE + categoryFilter, "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Show all warps")));

        List<String> searchLore = new ArrayList<>();
        searchLore.add(ChatColor.GRAY + "Filter warps by name.");
        if (searchTerm != null && !searchTerm.isEmpty()) {
            searchLore.add(ChatColor.GRAY + "Current:" + ChatColor.WHITE + " " + searchTerm);
        }
        searchLore.add("");
        searchLore.add(ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Type a search term");
        searchLore.add(ChatColor.DARK_GRAY + "Type 'cancel' to abort / 'clear' to reset.");
        inventory.setItem(SLOT_SEARCH, WarpMenuStyling.item(resolveMaterial("OAK_SIGN", "SIGN"), ChatColor.YELLOW + "" + ChatColor.BOLD + "Search / Filter", searchLore));

        if (adminMode && hasAnyManagePermission(player)) {
            inventory.setItem(SLOT_MANAGE, WarpMenuStyling.item(Material.NETHER_STAR, goldBold("Manage"), WarpMenuStyling.lore(ChatColor.GRAY + "Create, edit, and remove warps.", "", ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Open manager")));
        }

        inventory.setItem(SLOT_BACK, backButton(backTarget));
        inventory.setItem(SLOT_CLOSE, closeButton());

        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, pageButton(false, page));
        }
        if (page < maxPages - 1) {
            inventory.setItem(SLOT_NEXT, pageButton(true, page + 2));
        }
    }

    private static List<WarpService.WarpEntry> collectWarps(WarpService warpService, Player viewer, String categoryFilter, String searchTerm, boolean includeHidden, boolean manageBypass) {
        List<WarpService.WarpEntry> visible = new ArrayList<>();
        String search = searchTerm == null ? null : searchTerm.toLowerCase(Locale.ENGLISH);
        for (WarpService.WarpEntry entry : warpService.getWarps().values()) {
            if (categoryFilter != null && !categoryFilter.equalsIgnoreCase(entry.getCategory())) {
                continue;
            }
            if (search != null && (entry.getName() == null || !entry.getName().toLowerCase(Locale.ENGLISH).contains(search))) {
                continue;
            }
            if (!includeHidden && !entry.isPublic()) {
                continue;
            }
            if (!manageBypass && !WarpAccess.canView(entry, viewer, includeHidden)) {
                continue;
            }
            visible.add(entry);
        }
        visible.sort(Comparator.comparing(WarpService.WarpEntry::getName, String.CASE_INSENSITIVE_ORDER));
        return visible;
    }

    private static ItemStack createWarpItem(WarpService.WarpEntry entry, Player viewer, boolean adminMode, WarpMenuHolder.AdminAction adminAction, boolean manageBypass) {
        Material material = resolveMaterial(entry.getIcon(), "ENDER_PEARL");
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean canUse = manageBypass || WarpAccess.canUse(entry, viewer);
            meta.setDisplayName(goldBold(entry.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(WarpMenuStyling.separator());
            Location location = entry.getLocation();
            if (location != null && location.getWorld() != null) {
                lore.add(ChatColor.GRAY + "Category:" + ChatColor.WHITE + " " + entry.getCategory());
                lore.add(ChatColor.GRAY + "World:" + ChatColor.WHITE + " " + location.getWorld().getName());
                lore.add(ChatColor.GRAY + "Coords:" + ChatColor.WHITE + " " + format(location.getX()) + ", " + format(location.getY()) + ", " + format(location.getZ()));
            } else {
                lore.add(ChatColor.RED + "Location missing.");
            }
            lore.add(WarpMenuStyling.separator());

            if (!canUse) {
                lore.add(ChatColor.RED + "Locked:" + ChatColor.GRAY + " You can't use this warp");
                if (entry.getPermission() != null && !entry.getPermission().isEmpty()) {
                    lore.add(ChatColor.DARK_GRAY + "Permission:" + ChatColor.GRAY + " " + entry.getPermission());
                }
            } else {
                switch (adminAction) {
                    case DELETE:
                        lore.add(ChatColor.GRAY + "Delete this warp.");
                        lore.add(ChatColor.YELLOW + "Left-click:" + ChatColor.GRAY + " Confirm delete");
                        lore.add(ChatColor.YELLOW + "Right-click:" + ChatColor.GRAY + " Details");
                        break;
                    case MOVE:
                        lore.add(ChatColor.GRAY + "Move warp to you.");
                        lore.add(ChatColor.YELLOW + "Left-click:" + ChatColor.GRAY + " Confirm move");
                        lore.add(ChatColor.YELLOW + "Right-click:" + ChatColor.GRAY + " Details");
                        break;
                    case RENAME:
                        lore.add(ChatColor.GRAY + "Rename this warp.");
                        lore.add(ChatColor.YELLOW + "Left-click:" + ChatColor.GRAY + " Type new name");
                        lore.add(ChatColor.YELLOW + "Right-click:" + ChatColor.GRAY + " Details");
                        break;
                    case ICON:
                        lore.add(ChatColor.GRAY + "Update display icon.");
                        lore.add(ChatColor.YELLOW + "Left-click:" + ChatColor.GRAY + " Open icon picker");
                        lore.add(ChatColor.YELLOW + "Right-click:" + ChatColor.GRAY + " Details");
                        break;
                    case VISIBILITY:
                        lore.add(ChatColor.GRAY + "Toggle public / hidden.");
                        lore.add(ChatColor.YELLOW + "Left-click:" + ChatColor.GRAY + " Visibility menu");
                        lore.add(ChatColor.YELLOW + "Right-click:" + ChatColor.GRAY + " Details");
                        break;
                    default:
                        lore.add(ChatColor.YELLOW + "Left-click:" + ChatColor.GRAY + " Teleport");
                        lore.add(ChatColor.YELLOW + "Right-click:" + ChatColor.GRAY + " Details");
                        if (adminMode && hasAnyManagePermission(viewer)) {
                            lore.add(ChatColor.YELLOW + "Shift-Left:" + ChatColor.GRAY + " Move here");
                            lore.add(ChatColor.YELLOW + "Shift-Right:" + ChatColor.GRAY + " Delete");
                        }
                        break;
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createInfoItem(WarpService.WarpEntry entry) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "────────── " + ChatColor.GOLD + ChatColor.BOLD + "Info" + ChatColor.DARK_GRAY + " ──────────");
        lore.add(ChatColor.GRAY + "Name:" + ChatColor.WHITE + " " + entry.getName());
        lore.add(ChatColor.GRAY + "Category:" + ChatColor.WHITE + " " + entry.getCategory());
        Location location = entry.getLocation();
        if (location != null && location.getWorld() != null) {
            lore.add(ChatColor.GRAY + "World:" + ChatColor.WHITE + " " + location.getWorld().getName());
            lore.add(ChatColor.GRAY + "X:" + ChatColor.WHITE + " " + format(location.getX()) + ChatColor.GRAY + " Y:" + ChatColor.WHITE + " " + format(location.getY()) + ChatColor.GRAY + " Z:" + ChatColor.WHITE + " " + format(location.getZ()));
            lore.add(ChatColor.GRAY + "Yaw:" + ChatColor.WHITE + " " + format(location.getYaw()) + ChatColor.GRAY + " Pitch:" + ChatColor.WHITE + " " + format(location.getPitch()));
        } else {
            lore.add(ChatColor.RED + "Location missing.");
        }
        lore.add(ChatColor.GRAY + "Public:" + ChatColor.WHITE + " " + entry.isPublic());
        lore.add(ChatColor.GRAY + "Permission:" + ChatColor.WHITE + " " + (entry.getPermission() == null ? "none" : entry.getPermission()));
        lore.add(ChatColor.GRAY + "Set By:" + ChatColor.WHITE + " " + (entry.getSetByName() != null ? entry.getSetByName() : entry.getSetBy() != null ? entry.getSetBy().toString() : "unknown"));
        lore.add(ChatColor.GRAY + "Set At:" + ChatColor.WHITE + " " + (entry.getSetAt() == null ? "unknown" : entry.getSetAt()));
        return WarpMenuStyling.item(resolveMaterial("MAP", "WRITABLE_BOOK"), goldBold("Warp Info"), lore);
    }

    private static ItemStack backButton(WarpMenuHolder.BackTarget target) {
        String label;
        switch (target) {
            case ADMIN_MENU:
                label = "Back to Admin";
                break;
            case WARP_MANAGE:
                label = "Back to Manager";
                break;
            case CATEGORIES:
                label = "Back to Categories";
                break;
            default:
                label = "Back";
                break;
        }
        return WarpMenuStyling.item(resolveMaterial("ARROW", "ARROW"), ChatColor.YELLOW + label, WarpMenuStyling.lore(ChatColor.GRAY + "Go back."));
    }

    private static ItemStack closeButton() {
        return WarpMenuStyling.item(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "Close", WarpMenuStyling.lore(ChatColor.GRAY + "Close this menu."));
    }

    private static ItemStack pageButton(boolean next, int targetPage) {
        String name = next ? ChatColor.YELLOW + "Next Page" : ChatColor.YELLOW + "Previous Page";
        String lore = ChatColor.GRAY + "Page " + targetPage;
        return WarpMenuStyling.item(Material.ARROW, name, WarpMenuStyling.lore(lore));
    }

    private static Material resolveMaterial(String preferred, String fallback) {
        Material match = preferred == null ? null : Material.matchMaterial(preferred);
        if (match != null) {
            return match;
        }
        match = fallback == null ? null : Material.matchMaterial(fallback);
        return match == null ? Material.STONE : match;
    }

    private static String goldBold(String text) {
        return ChatColor.GOLD + "" + ChatColor.BOLD + text;
    }

    private static String format(double value) {
        return String.format(Locale.ENGLISH, "%.1f", value);
    }

    private static String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }
        return location.getWorld().getName() + " " + format(location.getX()) + ", " + format(location.getY()) + ", " + format(location.getZ());
    }

    private static boolean hasAnyManagePermission(Player player) {
        if (player == null) {
            return false;
        }
        if (player.hasPermission("sfcore.warp.manage")) {
            return true;
        }
        String[] perms = new String[]{
                "sfcore.warp.set",
                "sfcore.warp.delete",
                "sfcore.warp.rename",
                "sfcore.warp.move",
                "sfcore.warp.icon",
                "sfcore.warp.public"
        };
        for (String perm : perms) {
            if (player.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }

    private static void fill(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, FILLER.clone());
        }
    }

    private static List<Material> iconChoices() {
        Set<Material> materials = new HashSet<>();
        String[] preferred = new String[]{
                "ENDER_PEARL", "BEACON", "COMPASS", "DIAMOND_SWORD", "SHIELD", "GOLDEN_APPLE",
                "ELYTRA", "BOOK", "CAMPFIRE", "EMERALD", "AMETHYST_SHARD", "HONEYCOMB", "SPYGLASS",
                "BLAZE_ROD", "TNT", "CHEST", "NETHER_STAR", "HEART_OF_THE_SEA"
        };
        for (String name : preferred) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                materials.add(material);
            }
        }
        List<Material> list = new ArrayList<>(materials);
        list.sort(Comparator.comparing(Enum::name));
        return list;
    }

    private static String formatMaterial(Material material) {
        String name = material.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return builder.toString().trim();
    }
}
