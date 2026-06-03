package dev.zcripted.obx.feature.warp.service;

import dev.zcripted.obx.feature.warp.service.WarpService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import java.util.Locale;

public final class WarpAccess {

    private WarpAccess() {
    }

    public static boolean canView(WarpService.WarpEntry entry, CommandSender viewer, boolean includeHidden) {
        if (entry == null) {
            return false;
        }
        if (!includeHidden && !entry.isPublic()) {
            if (viewer == null || !viewer.hasPermission("obx.warp.hidden.view")) {
                return false;
            }
        }
        return hasWarpPermission(entry, viewer) && hasCategoryPermission(entry, viewer);
    }

    public static boolean canUse(WarpService.WarpEntry entry, CommandSender viewer) {
        if (entry == null) {
            return false;
        }
        return hasWarpPermission(entry, viewer) && hasCategoryPermission(entry, viewer);
    }

    private static boolean hasWarpPermission(WarpService.WarpEntry entry, CommandSender viewer) {
        if (entry == null || viewer == null) {
            return true;
        }
        String permission = entry.getPermission();
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        return viewer.hasPermission(permission);
    }

    private static boolean hasCategoryPermission(WarpService.WarpEntry entry, CommandSender viewer) {
        if (entry == null || viewer == null) {
            return true;
        }
        String category = entry.getCategory();
        if (category == null || category.isEmpty()) {
            return true;
        }
        String node = "obx.warp.category." + category.toLowerCase(Locale.ENGLISH);
        Permission permission = Bukkit.getPluginManager().getPermission(node);
        if (permission == null && !viewer.isPermissionSet(node)) {
            return true;
        }
        return viewer.hasPermission(node);
    }
}
