package dev.zcripted.obx.feature.warp.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.warp.gui.WarpMenu;
import dev.zcripted.obx.feature.warp.gui.WarpMenuHolder;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.core.storage.DataService;
import dev.zcripted.obx.feature.warp.service.WarpService;
import dev.zcripted.obx.api.teleport.TeleportManager;
import dev.zcripted.obx.feature.warp.service.WarpAccess;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Read-only / navigation warp subcommands extracted from {@link WarpCommand}:
 * {@code tp}, {@code info}, {@code list}, {@code category}, {@code categories},
 * and {@code gui}. Bodies are moved verbatim.
 */
final class WarpQueryCommands {

    private static final int LIST_PAGE_SIZE = 10;

    private final ObxPlugin plugin;
    private final WarpService warpService;
    private final LanguageManager languages;
    private final TeleportManager teleportManager;
    private final DataService dataService;

    WarpQueryCommands(ObxPlugin plugin, WarpService warpService, LanguageManager languages,
                      TeleportManager teleportManager, DataService dataService) {
        this.plugin = plugin;
        this.warpService = warpService;
        this.languages = languages;
        this.teleportManager = teleportManager;
        this.dataService = dataService;
    }

    boolean handleTeleport(CommandSender sender, WarpService.WarpEntry warpOverride, List<String> args) {
        String warpName = warpOverride != null ? warpOverride.getKey() : (args.isEmpty() ? null : args.get(0));
        if (warpName == null) {
            languages.send(sender, "teleport.warp.usage.tp");
            return true;
        }
        WarpService.WarpEntry entry = warpOverride != null ? warpOverride : warpService.getWarp(warpName);
        if (entry == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", warpName));
            return true;
        }

        Player target = null;
        if (warpOverride != null) {
            target = args.isEmpty() ? null : resolvePlayer(args.get(0));
        } else if (args.size() > 1) {
            target = resolvePlayer(args.get(1));
        }

        if (target == null) {
            if (!(sender instanceof Player)) {
                languages.send(sender, "teleport.warp.usage.tp-console");
                return true;
            }
            target = (Player) sender;
        }

        boolean self = sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());
        if (self) {
            if (!sender.hasPermission("obx.warp.tp")) {
                languages.send(sender, "core.no-permission");
                return true;
            }
        } else if (!sender.hasPermission("obx.warp.tp.others")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        if (!WarpAccess.canUse(entry, sender)) {
            languages.send(sender, "teleport.warp.no-access", Placeholders.with("warp", entry.getName()));
            return true;
        }
        if (!WarpAccess.canUse(entry, target) && !sender.hasPermission("obx.warp.manage")) {
            languages.send(sender, "teleport.warp.target-no-access", Placeholders.with("warp", entry.getName(), "target", target.getName()));
            return true;
        }
        if (entry.getLocation() == null) {
            languages.send(sender, "teleport.warp.invalid-location", Placeholders.with("warp", entry.getName()));
            return true;
        }

        dataService.setBack(target.getUniqueId(), target.getLocation());
        teleportManager.teleportPlayer(target, entry.getLocation(), "teleport.warp.teleporting", Placeholders.with("warp", entry.getName()));
        if (!self) {
            Map<String, String> placeholders = Placeholders.with("warp", entry.getName(), "target", target.getName());
            placeholders.put("sender", sender.getName());
            languages.send(sender, "teleport.warp.sent-other", placeholders);
            languages.send(target, "teleport.warp.sent-target", placeholders);
        }
        return true;
    }

    boolean handleInfo(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("obx.warp.info") && !sender.hasPermission("obx.warp.manage")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.info");
            return true;
        }
        WarpService.WarpEntry entry = warpService.getWarp(args.get(0));
        if (entry == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", args.get(0)));
            return true;
        }
        Map<String, String> placeholders = new HashMap<>();
        if (entry.getLocation() != null && entry.getLocation().getWorld() != null) {
            placeholders.put("world", entry.getLocation().getWorld().getName());
        } else {
            placeholders.put("world", "unknown");
        }
        placeholders.put("x", entry.getLocation() == null ? "?" : format(entry.getLocation().getX()));
        placeholders.put("y", entry.getLocation() == null ? "?" : format(entry.getLocation().getY()));
        placeholders.put("z", entry.getLocation() == null ? "?" : format(entry.getLocation().getZ()));
        placeholders.put("yaw", entry.getLocation() == null ? "?" : format(entry.getLocation().getYaw()));
        placeholders.put("pitch", entry.getLocation() == null ? "?" : format(entry.getLocation().getPitch()));
        placeholders.put("warp", entry.getName());
        placeholders.put("category", entry.getCategory());
        placeholders.put("icon", entry.getIcon() == null ? "none" : entry.getIcon());
        placeholders.put("visibility", entry.isPublic() ? languages.get(sender, "teleport.warp.visibility.public") : languages.get(sender, "teleport.warp.visibility.hidden"));
        placeholders.put("permission", entry.getPermission() == null ? languages.get(sender, "teleport.warp.permission.none") : entry.getPermission());
        placeholders.put("setBy", entry.getSetByName() != null ? entry.getSetByName() : (entry.getSetBy() != null ? entry.getSetBy().toString() : languages.get(sender, "teleport.warp.info-unknown")));
        placeholders.put("setAt", entry.getSetAt() == null ? languages.get(sender, "teleport.warp.info-unknown") : entry.getSetAt());
        languages.send(sender, "teleport.warp.info", placeholders);
        return true;
    }

    boolean handleList(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("obx.warp.list")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        int page = 1;
        String category = null;
        for (String arg : args) {
            Integer numeric = parseInt(arg);
            if (numeric != null) {
                page = Math.max(1, numeric);
            } else {
                category = arg;
            }
        }
        boolean includeHidden = !(sender instanceof Player) || sender.hasPermission("obx.warp.hidden.view");
        List<WarpService.WarpEntry> warps = filterForSender(sender, includeHidden, category);
        if (warps.isEmpty()) {
            languages.send(sender, category == null ? "teleport.warp.list.empty" : "teleport.warp.list.empty-category", Placeholders.with("category", category));
            return true;
        }
        int pages = (int) Math.ceil(warps.size() / (double) LIST_PAGE_SIZE);
        page = Math.max(1, Math.min(page, pages));
        int start = (page - 1) * LIST_PAGE_SIZE;
        int end = Math.min(start + LIST_PAGE_SIZE, warps.size());
        List<String> names = new ArrayList<>();
        for (int i = start; i < end; i++) {
            names.add(warps.get(i).getName());
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("page", String.valueOf(page));
        placeholders.put("pages", String.valueOf(pages));
        placeholders.put("warps", String.join(", ", names));
        if (category != null) {
            placeholders.put("category", category);
        }
        languages.send(sender, category == null ? "teleport.warp.list.page" : "teleport.warp.list.page-category", placeholders);
        return true;
    }

    boolean handleCategory(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("obx.warp.list")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.category");
            return true;
        }
        String category = args.get(0);
        return handleList(sender, Collections.singletonList(category));
    }

    boolean handleCategories(CommandSender sender) {
        if (!sender.hasPermission("obx.warp.list")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        boolean includeHidden = !(sender instanceof Player) || sender.hasPermission("obx.warp.hidden.view");
        List<WarpService.WarpEntry> warps = filterForSender(sender, includeHidden, null);
        Map<String, Integer> counts = new HashMap<>();
        for (WarpService.WarpEntry entry : warps) {
            counts.put(entry.getCategory(), counts.getOrDefault(entry.getCategory(), 0) + 1);
        }
        if (counts.isEmpty()) {
            languages.send(sender, "teleport.warp.categories.none");
            return true;
        }
        List<String> categories = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            categories.add(entry.getKey() + " (" + entry.getValue() + ")");
        }
        categories.sort(String.CASE_INSENSITIVE_ORDER);
        languages.send(sender, "teleport.warp.categories.list", Placeholders.with("categories", String.join(", ", categories)));
        return true;
    }

    boolean handleGui(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "teleport.warp.gui.console");
            return true;
        }
        if (!sender.hasPermission("obx.warp.gui")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        String category = args.isEmpty() ? null : args.get(0);
        WarpMenu.openMain(plugin, (Player) sender, 0, category, null, false, WarpMenuHolder.BackTarget.MAIN_MENU);
        return true;
    }

    List<WarpService.WarpEntry> filterForSender(CommandSender sender, boolean includeHidden, String category) {
        List<WarpService.WarpEntry> filtered = new ArrayList<>();
        for (WarpService.WarpEntry entry : warpService.getWarps().values()) {
            if (category != null && !category.equalsIgnoreCase(entry.getCategory())) {
                continue;
            }
            if (!WarpAccess.canView(entry, sender, includeHidden)) {
                continue;
            }
            filtered.add(entry);
        }
        filtered.sort(Comparator.comparing(WarpService.WarpEntry::getName, String.CASE_INSENSITIVE_ORDER));
        return filtered;
    }

    List<String> categoriesForSender(CommandSender sender, boolean includeHidden) {
        Set<String> categories = new HashSet<>();
        for (WarpService.WarpEntry entry : filterForSender(sender, includeHidden, null)) {
            categories.add(entry.getCategory());
        }
        List<String> list = new ArrayList<>(categories);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    private Player resolvePlayer(String name) {
        if (name == null) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String format(double value) {
        return String.format(Locale.ENGLISH, "%.2f", value);
    }
}