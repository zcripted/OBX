package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.gui.player.WarpMenu;
import dev.sergeantfuzzy.sfcore.gui.player.WarpMenuHolder;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.storage.WarpService;
import dev.sergeantfuzzy.sfcore.util.teleport.TeleportManager;
import dev.sergeantfuzzy.sfcore.util.teleport.WarpAccess;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private static final Map<String, String> SUB_ALIASES = createAliasMap();
    private static final Set<String> CONFIRM_WORDS = new HashSet<>(Arrays.asList("confirm", "yes", "y"));
    private static final long CONFIRM_WINDOW_MS = 15_000L;
    private static final int LIST_PAGE_SIZE = 10;

    private final Main plugin;
    private final WarpService warpService;
    private final LanguageManager languages;
    private final TeleportManager teleportManager;
    private final DataService dataService;
    private final Map<UUID, PendingAction> deleteConfirmations = new HashMap<>();
    private final Map<UUID, PendingAction> overwriteConfirmations = new HashMap<>();

    public WarpCommand(Main plugin) {
        this.plugin = plugin;
        this.warpService = plugin.getWarpService();
        this.languages = plugin.getLanguageManager();
        this.teleportManager = plugin.getTeleportManager();
        this.dataService = plugin.getDataService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.warp")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player) {
                if (!sender.hasPermission("sfcore.warp.gui")) {
                    languages.send(sender, "core.no-permission");
                    return true;
                }
                WarpMenu.openMain(plugin, (Player) sender, 0, null, null, false, WarpMenuHolder.BackTarget.MAIN_MENU);
            } else {
                languages.send(sender, "teleport.warp.usage.console");
            }
            return true;
        }

        String first = args[0];
        WarpService.WarpEntry warpMatch = warpService.getWarp(first);
        String sub = resolveSubcommand(first);
        List<String> remaining = new ArrayList<>(Arrays.asList(args));

        if (warpMatch != null) {
            remaining.remove(0);
            return handleTeleport(sender, warpMatch, remaining);
        }

        if (sub != null) {
            remaining.remove(0);
        } else {
            languages.send(sender, "teleport.warp.unknown", Placeholders.with("warp", first));
            return true;
        }

        switch (sub) {
            case "tp":
                return handleTeleport(sender, null, remaining);
            case "set":
                return handleSet(sender, remaining);
            case "delete":
                return handleDelete(sender, remaining);
            case "info":
                return handleInfo(sender, remaining);
            case "list":
                return handleList(sender, remaining);
            case "category":
                return handleCategory(sender, remaining);
            case "categories":
                return handleCategories(sender);
            case "rename":
                return handleRename(sender, remaining);
            case "move":
                return handleMove(sender, remaining);
            case "icon":
                return handleIcon(sender, remaining);
            case "public":
                return handlePublic(sender, remaining);
            case "gui":
                return handleGui(sender, remaining);
            default:
                languages.send(sender, "teleport.warp.unknown", Placeholders.with("warp", first));
                return true;
        }
    }

    private boolean handleTeleport(CommandSender sender, WarpService.WarpEntry warpOverride, List<String> args) {
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
            if (!sender.hasPermission("sfcore.warp.tp")) {
                languages.send(sender, "core.no-permission");
                return true;
            }
        } else if (!sender.hasPermission("sfcore.warp.tp.others")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        if (!WarpAccess.canUse(entry, sender)) {
            languages.send(sender, "teleport.warp.no-access", Placeholders.with("warp", entry.getName()));
            return true;
        }
        if (!WarpAccess.canUse(entry, target) && !sender.hasPermission("sfcore.warp.manage")) {
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

    private boolean handleSet(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "teleport.warp.gui.console");
            return true;
        }
        if (!sender.hasPermission("sfcore.warp.set") && !sender.hasPermission("sfcore.warp.manage")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.set");
            return true;
        }
        Player player = (Player) sender;
        String nameInput = args.get(0);
        String normalized = warpService.normalizeName(nameInput);
        if (normalized == null) {
            languages.send(sender, "teleport.warp.invalid-name");
            return true;
        }
        WarpService.WarpEntry existing = warpService.getWarp(nameInput);
        boolean confirm = args.size() > 1 && CONFIRM_WORDS.contains(args.get(1).toLowerCase(Locale.ENGLISH));
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (existing != null && !confirm) {
            overwriteConfirmations.put(uuid, new PendingAction(normalized, now));
            languages.send(sender, "teleport.warp.set.confirm", Placeholders.with("warp", existing.getName()));
            return true;
        }
        if (existing != null) {
            PendingAction action = overwriteConfirmations.get(uuid);
            if (action == null || !action.warpKey.equalsIgnoreCase(normalized) || now - action.requestedAt > CONFIRM_WINDOW_MS) {
                overwriteConfirmations.put(uuid, new PendingAction(normalized, now));
                languages.send(sender, "teleport.warp.set.confirm", Placeholders.with("warp", existing.getName()));
                return true;
            }
        }
        warpService.setWarp(nameInput, player.getLocation(), "general", null, true, null, player.getUniqueId(), player.getName());
        languages.send(sender, existing == null ? "teleport.warp.set.created" : "teleport.warp.set.updated", Placeholders.with("warp", nameInput));
        return true;
    }

    private boolean handleDelete(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("sfcore.warp.delete") && !sender.hasPermission("sfcore.warp.manage")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.delete");
            return true;
        }
        String warpName = args.get(0);
        WarpService.WarpEntry entry = warpService.getWarp(warpName);
        if (entry == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", warpName));
            return true;
        }
        UUID key = sender instanceof Player ? ((Player) sender).getUniqueId() : UUID.randomUUID();
        long now = System.currentTimeMillis();
        boolean confirm = args.size() > 1 && CONFIRM_WORDS.contains(args.get(1).toLowerCase(Locale.ENGLISH));
        if (!confirm) {
            deleteConfirmations.put(key, new PendingAction(entry.getKey(), now));
            languages.send(sender, "teleport.warp.delete.confirm", Placeholders.with("warp", entry.getName()));
            return true;
        }
        PendingAction action = deleteConfirmations.get(key);
        if (action == null || !action.warpKey.equalsIgnoreCase(entry.getKey()) || now - action.requestedAt > CONFIRM_WINDOW_MS) {
            deleteConfirmations.put(key, new PendingAction(entry.getKey(), now));
            languages.send(sender, "teleport.warp.delete.confirm-needed", Placeholders.with("warp", entry.getName()));
            return true;
        }
        warpService.deleteWarp(entry.getKey());
        deleteConfirmations.remove(key);
        languages.send(sender, "teleport.warp.deleted", Placeholders.with("warp", entry.getName()));
        return true;
    }

    private boolean handleInfo(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("sfcore.warp.info") && !sender.hasPermission("sfcore.warp.manage")) {
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

    private boolean handleList(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("sfcore.warp.list")) {
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
        boolean includeHidden = !(sender instanceof Player) || sender.hasPermission("sfcore.warp.hidden.view");
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

    private boolean handleCategory(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("sfcore.warp.list")) {
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

    private boolean handleCategories(CommandSender sender) {
        if (!sender.hasPermission("sfcore.warp.list")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        boolean includeHidden = !(sender instanceof Player) || sender.hasPermission("sfcore.warp.hidden.view");
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

    private boolean handleRename(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("sfcore.warp.rename") && !sender.hasPermission("sfcore.warp.manage")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.size() < 2) {
            languages.send(sender, "teleport.warp.usage.rename");
            return true;
        }
        String oldName = args.get(0);
        String newName = args.get(1);
        WarpService.WarpEntry existing = warpService.getWarp(oldName);
        if (existing == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", oldName));
            return true;
        }
        if (warpService.getWarp(newName) != null) {
            languages.send(sender, "teleport.warp.rename.conflict", Placeholders.with("warp", newName));
            return true;
        }
        String normalized = warpService.normalizeName(newName);
        if (normalized == null) {
            languages.send(sender, "teleport.warp.invalid-name");
            return true;
        }
        warpService.renameWarp(oldName, newName);
        Map<String, String> placeholders = Placeholders.with("old", existing.getName(), "new", newName);
        languages.send(sender, "teleport.warp.rename.success", placeholders);
        return true;
    }

    private boolean handleMove(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "teleport.warp.gui.console");
            return true;
        }
        if (!sender.hasPermission("sfcore.warp.move") && !sender.hasPermission("sfcore.warp.manage")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.move");
            return true;
        }
        String warpName = args.get(0);
        WarpService.WarpEntry entry = warpService.getWarp(warpName);
        if (entry == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", warpName));
            return true;
        }
        Player player = (Player) sender;
        warpService.moveWarp(warpName, player.getLocation(), player.getUniqueId(), player.getName());
        languages.send(sender, "teleport.warp.move.success", Placeholders.with("warp", entry.getName()));
        return true;
    }

    private boolean handleIcon(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("sfcore.warp.manage") && !sender.hasPermission("sfcore.warp.icon")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.icon");
            return true;
        }
        WarpService.WarpEntry entry = warpService.getWarp(args.get(0));
        if (entry == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", args.get(0)));
            return true;
        }
        if (args.size() == 1) {
            warpService.setIcon(entry.getKey(), null);
            languages.send(sender, "teleport.warp.icon.cleared", Placeholders.with("warp", entry.getName()));
            return true;
        }
        String icon = args.get(1);
        if (org.bukkit.Material.matchMaterial(icon) == null) {
            languages.send(sender, "teleport.warp.icon.invalid", Placeholders.with("icon", icon));
            return true;
        }
        warpService.setIcon(entry.getKey(), icon);
        languages.send(sender, "teleport.warp.icon.updated", Placeholders.with("warp", entry.getName(), "icon", icon.toUpperCase(Locale.ENGLISH)));
        return true;
    }

    private boolean handlePublic(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("sfcore.warp.manage") && !sender.hasPermission("sfcore.warp.public")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.isEmpty()) {
            languages.send(sender, "teleport.warp.usage.public");
            return true;
        }
        WarpService.WarpEntry entry = warpService.getWarp(args.get(0));
        if (entry == null) {
            languages.send(sender, "teleport.warp.not-found", Placeholders.with("warp", args.get(0)));
            return true;
        }
        boolean newState = !entry.isPublic();
        if (args.size() > 1) {
            String raw = args.get(1).toLowerCase(Locale.ENGLISH);
            if (raw.equals("true") || raw.equals("yes") || raw.equals("on")) {
                newState = true;
            } else if (raw.equals("false") || raw.equals("no") || raw.equals("off")) {
                newState = false;
            } else {
                languages.send(sender, "teleport.warp.usage.public");
                return true;
            }
        }
        warpService.setPublic(entry.getKey(), newState);
        languages.send(sender, "teleport.warp.public.updated", Placeholders.with("warp", entry.getName(), "state", newState ? languages.get(sender, "teleport.warp.visibility.public") : languages.get(sender, "teleport.warp.visibility.hidden")));
        return true;
    }

    private boolean handleGui(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "teleport.warp.gui.console");
            return true;
        }
        if (!sender.hasPermission("sfcore.warp.gui")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        String category = args.isEmpty() ? null : args.get(0);
        WarpMenu.openMain(plugin, (Player) sender, 0, category, null, false, WarpMenuHolder.BackTarget.MAIN_MENU);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        boolean includeHidden = sender.hasPermission("sfcore.warp.hidden.view") || sender.hasPermission("sfcore.warp.manage");
        List<WarpService.WarpEntry> visible = filterForSender(sender, includeHidden, null);
        if (args.length == 0) {
            return suggestions;
        }
        String current = args[0].toLowerCase(Locale.ENGLISH);
        if (args.length == 1) {
            suggestions.addAll(canonicalSubs(current));
            for (WarpService.WarpEntry entry : visible) {
                if (entry.getName().toLowerCase(Locale.ENGLISH).startsWith(current)) {
                    suggestions.add(entry.getName());
                }
            }
        } else {
            String sub = resolveSubcommand(args[0]);
            if (sub == null) {
                WarpService.WarpEntry entry = warpService.getWarp(args[0]);
                if (entry != null) {
                    sub = "tp";
                }
            }
            if (sub == null) {
                return suggestions;
            }
            switch (sub) {
                case "tp":
                    if (args.length == 2) {
                        String partialWarp = args[1].toLowerCase(Locale.ENGLISH);
                        for (WarpService.WarpEntry entry : visible) {
                            if (entry.getName().toLowerCase(Locale.ENGLISH).startsWith(partialWarp)) {
                                suggestions.add(entry.getName());
                            }
                        }
                    } else if (args.length == 3) {
                        String partialPlayer = args[2].toLowerCase(Locale.ENGLISH);
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getName().toLowerCase(Locale.ENGLISH).startsWith(partialPlayer)) {
                                suggestions.add(player.getName());
                            }
                        }
                    }
                    break;
                case "set":
                case "delete":
                case "info":
                case "move":
                case "icon":
                case "public":
                case "rename":
                    if (args.length == 2) {
                        String partial = args[1].toLowerCase(Locale.ENGLISH);
                        for (WarpService.WarpEntry entry : visible) {
                            if (entry.getName().toLowerCase(Locale.ENGLISH).startsWith(partial)) {
                                suggestions.add(entry.getName());
                            }
                        }
                    } else if ("rename".equals(sub) && args.length == 3) {
                        suggestions.add(args[1]);
                    } else if ("icon".equals(sub) && args.length == 3) {
                        String partialMaterial = args[2].toLowerCase(Locale.ENGLISH);
                        for (org.bukkit.Material material : org.bukkit.Material.values()) {
                            String name = material.name().toLowerCase(Locale.ENGLISH);
                            if (name.startsWith(partialMaterial)) {
                                suggestions.add(material.name());
                            }
                        }
                    } else if ("public".equals(sub) && args.length == 3) {
                        suggestions.addAll(Arrays.asList("true", "false"));
                    }
                    break;
                case "list":
                case "category":
                    if (args.length == 2) {
                        String partialCategory = args[1].toLowerCase(Locale.ENGLISH);
                        for (String category : categoriesForSender(sender, includeHidden)) {
                            if (category.toLowerCase(Locale.ENGLISH).startsWith(partialCategory)) {
                                suggestions.add(category);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return suggestions;
    }

    private List<String> canonicalSubs(String current) {
        Set<String> subs = new HashSet<>(Arrays.asList("tp", "set", "delete", "info", "list", "category", "categories", "rename", "move", "gui", "icon", "public"));
        for (Map.Entry<String, String> alias : SUB_ALIASES.entrySet()) {
            if (subs.contains(alias.getValue())) {
                subs.add(alias.getKey());
            }
        }
        List<String> matches = new ArrayList<>();
        for (String option : subs) {
            if (option.toLowerCase(Locale.ENGLISH).startsWith(current)) {
                matches.add(option);
            }
        }
        Collections.sort(matches);
        return matches;
    }

    private List<WarpService.WarpEntry> filterForSender(CommandSender sender, boolean includeHidden, String category) {
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

    private List<String> categoriesForSender(CommandSender sender, boolean includeHidden) {
        Set<String> categories = new HashSet<>();
        for (WarpService.WarpEntry entry : filterForSender(sender, includeHidden, null)) {
            categories.add(entry.getCategory());
        }
        List<String> list = new ArrayList<>(categories);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    private String resolveSubcommand(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.toLowerCase(Locale.ENGLISH);
        return SUB_ALIASES.getOrDefault(normalized, null);
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

    private static Map<String, String> createAliasMap() {
        Map<String, String> map = new HashMap<>();
        map.put("tp", "tp");
        map.put("teleport", "tp");
        map.put("go", "tp");
        map.put("goto", "tp");
        map.put("warp", "tp");

        map.put("set", "set");
        map.put("create", "set");
        map.put("new", "set");
        map.put("setup", "set");
        map.put("add", "set");
        map.put("define", "set");

        map.put("delete", "delete");
        map.put("remove", "delete");
        map.put("del", "delete");
        map.put("clear", "delete");
        map.put("reset", "delete");
        map.put("unset", "delete");

        map.put("info", "info");
        map.put("information", "info");
        map.put("details", "info");
        map.put("about", "info");
        map.put("show", "info");

        map.put("list", "list");
        map.put("ls", "list");
        map.put("all", "list");
        map.put("browse", "list");
        map.put("page", "list");

        map.put("category", "category");
        map.put("cat", "category");
        map.put("group", "category");
        map.put("section", "category");

        map.put("categories", "categories");
        map.put("cats", "categories");
        map.put("groups", "categories");
        map.put("sections", "categories");

        map.put("rename", "rename");
        map.put("ren", "rename");
        map.put("name", "rename");
        map.put("setname", "rename");

        map.put("move", "move");
        map.put("reloc", "move");
        map.put("relocate", "move");
        map.put("update", "move");
        map.put("here", "move");

        map.put("gui", "gui");
        map.put("menu", "gui");
        map.put("open", "gui");
        map.put("view", "gui");

        map.put("icon", "icon");
        map.put("item", "icon");
        map.put("display", "icon");

        map.put("public", "public");
        map.put("publish", "public");
        map.put("visible", "public");
        map.put("visibility", "public");
        return map;
    }

    private static final class PendingAction {
        private final String warpKey;
        private final long requestedAt;

        private PendingAction(String warpKey, long requestedAt) {
            this.warpKey = warpKey;
            this.requestedAt = requestedAt;
        }
    }
}
