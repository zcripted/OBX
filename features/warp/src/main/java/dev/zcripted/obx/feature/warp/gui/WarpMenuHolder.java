package dev.zcripted.obx.feature.warp.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.gui.MenuHolder;
import org.bukkit.Location;

import java.util.List;

public class WarpMenuHolder extends MenuHolder {

    public enum View {
        MAIN,
        CATEGORIES,
        DETAILS,
        MANAGE,
        MANAGE_SELECT,
        CONFIRM_DELETE,
        CONFIRM_MOVE,
        CONFIRM_OVERWRITE,
        ICON_PICKER,
        VISIBILITY
    }

    public enum BackTarget {
        MAIN_MENU,
        ADMIN_MENU,
        WARP_MAIN,
        WARP_MANAGE,
        CATEGORIES,
        DETAILS
    }

    public enum AdminAction {
        NONE,
        DELETE,
        MOVE,
        RENAME,
        ICON,
        VISIBILITY,
        OVERWRITE
    }

    private final ObxPlugin plugin;
    private final View view;
    private final int page;
    private final String categoryFilter;
    private final String searchTerm;
    private final List<String> warpKeys;
    private final List<String> categories;
    private final String warpKey;
    private final String warpName;
    private final boolean adminMode;
    private final BackTarget backTarget;
    private final AdminAction adminAction;
    private final int returnPage;
    private final Location pendingLocation;

    public WarpMenuHolder(ObxPlugin plugin,
                          View view,
                          int page,
                          String categoryFilter,
                          String searchTerm,
                          List<String> warpKeys,
                          List<String> categories,
                          String warpKey,
                          String warpName,
                          boolean adminMode,
                          BackTarget backTarget,
                          AdminAction adminAction,
                          int returnPage,
                          Location pendingLocation) {
        this.plugin = plugin;
        this.view = view;
        this.page = page;
        this.categoryFilter = categoryFilter;
        this.searchTerm = searchTerm;
        this.warpKeys = warpKeys;
        this.categories = categories;
        this.warpKey = warpKey;
        this.warpName = warpName;
        this.adminMode = adminMode;
        this.backTarget = backTarget;
        this.adminAction = adminAction == null ? AdminAction.NONE : adminAction;
        this.returnPage = returnPage;
        this.pendingLocation = pendingLocation;
    }

    public ObxPlugin getPlugin() {
        return plugin;
    }

    public int getPage() {
        return page;
    }

    public String getCategoryFilter() {
        return categoryFilter;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public List<String> getWarpKeys() {
        return warpKeys;
    }

    public List<String> getCategories() {
        return categories;
    }

    public String getWarpKey() {
        return warpKey;
    }

    public String getWarpName() {
        return warpName;
    }

    public boolean isAdminMode() {
        return adminMode;
    }

    public View getView() {
        return view;
    }

    public BackTarget getBackTarget() {
        return backTarget;
    }

    public AdminAction getAdminAction() {
        return adminAction;
    }

    public int getReturnPage() {
        return returnPage;
    }

    public Location getPendingLocation() {
        return pendingLocation;
    }
}