package dev.zcripted.obx.feature.warp.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.command.AbstractObxCommand;
import dev.zcripted.obx.feature.warp.gui.WarpMenu;
import dev.zcripted.obx.feature.warp.gui.WarpMenuHolder;
import dev.zcripted.obx.feature.warp.service.WarpService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WarpCommand extends AbstractObxCommand implements TabCompleter {

    private static final Map<String, String> SUB_ALIASES = createAliasMap();

    private final WarpService warpService;
    private final WarpQueryCommands query;
    private final WarpAdminCommands admin;

    public WarpCommand(ObxPlugin plugin) {
        super(plugin);
        this.warpService = plugin.getWarpService();
        this.query = new WarpQueryCommands(plugin, warpService, languages,
                plugin.getTeleportManager(), plugin.getDataService());
        this.admin = new WarpAdminCommands(warpService, languages);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.warp")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player) {
                if (!sender.hasPermission("obx.warp.gui")) {
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
            return query.handleTeleport(sender, warpMatch, remaining);
        }

        if (sub != null) {
            remaining.remove(0);
        } else {
            languages.send(sender, "teleport.warp.unknown", Placeholders.with("warp", first));
            return true;
        }

        switch (sub) {
            case "tp":
                return query.handleTeleport(sender, null, remaining);
            case "set":
                return admin.handleSet(sender, remaining);
            case "delete":
                return admin.handleDelete(sender, remaining);
            case "info":
                return query.handleInfo(sender, remaining);
            case "list":
                return query.handleList(sender, remaining);
            case "category":
                return query.handleCategory(sender, remaining);
            case "categories":
                return query.handleCategories(sender);
            case "rename":
                return admin.handleRename(sender, remaining);
            case "move":
                return admin.handleMove(sender, remaining);
            case "icon":
                return admin.handleIcon(sender, remaining);
            case "public":
                return admin.handlePublic(sender, remaining);
            case "gui":
                return query.handleGui(sender, remaining);
            default:
                languages.send(sender, "teleport.warp.unknown", Placeholders.with("warp", first));
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        boolean includeHidden = sender.hasPermission("obx.warp.hidden.view") || sender.hasPermission("obx.warp.manage");
        List<WarpService.WarpEntry> visible = query.filterForSender(sender, includeHidden, null);
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
                        for (String category : query.categoriesForSender(sender, includeHidden)) {
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

    private String resolveSubcommand(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.toLowerCase(Locale.ENGLISH);
        return SUB_ALIASES.getOrDefault(normalized, null);
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
}
