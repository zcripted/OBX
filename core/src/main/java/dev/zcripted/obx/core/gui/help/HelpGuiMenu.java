package dev.zcripted.obx.core.gui.help;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Paginated help GUI for the OBX /help override.
 *
 * <p>Displays every server command whose permission default is {@code TRUE} (or that has no
 * permission requirement at all) sorted alphabetically. The 6th row is reserved for the
 * pagination controls and the category filter; the previous-page button only appears on
 * pages 2 and beyond, and the next-page button is hidden on the final page so navigation
 * stays self-evident. The category button cycles through All / Admin / Core / Language /
 * Moderation / Teleport / Utility / Other to filter the visible commands.
 *
 * <p>Tooltip lines wrap at 45 visible characters without breaking words, mirroring the
 * Adventure / MiniMessage layout style used elsewhere in the plugin.
 */
public final class HelpGuiMenu {

    public static final int SIZE = 54;
    public static final int PAGE_SIZE = 45;
    public static final int PREV_SLOT = 45;
    public static final int CATEGORY_SLOT = 48;
    public static final int INFO_SLOT = 49;
    public static final int CLOSE_SLOT = 50;
    public static final int NEXT_SLOT = 53;
    public static final int HOVER_LINE_LIMIT = 45;

    public static final String CATEGORY_ALL = "All";
    public static final String CATEGORY_ADMIN = "Admin";
    public static final String CATEGORY_CORE = "Core";
    public static final String CATEGORY_LANGUAGE = "Language";
    public static final String CATEGORY_MESSAGING = "Messaging";
    public static final String CATEGORY_MODERATION = "Moderation";
    public static final String CATEGORY_TELEPORT = "Teleport";
    public static final String CATEGORY_UTILITY = "Utility";
    public static final String CATEGORY_OTHER = "Other";

    public static final List<String> CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            CATEGORY_ALL,
            CATEGORY_ADMIN,
            CATEGORY_CORE,
            CATEGORY_LANGUAGE,
            CATEGORY_MESSAGING,
            CATEGORY_MODERATION,
            CATEGORY_TELEPORT,
            CATEGORY_UTILITY,
            CATEGORY_OTHER
    ));

    private static final String SF_CORE_COMMAND_PACKAGE = "dev.zcripted.obx.command.";
    private static final String SF_CORE_PLUGIN_NAME = "OBX";

    /**
     * Authoritative mapping of every OBX command to its display category. Keyed by
     * lowercase command name (matches the value Bukkit returns from
     * {@link org.bukkit.command.PluginCommand#getName()}). This is the primary source of
     * truth because ProGuard renames the command package names (e.g.
     * {@code dev.zcripted.obx.command.admin} → {@code dev.zcripted.obx.b.a})
     * which would otherwise break a package-introspection heuristic.
     */
    private static final Map<String, String> SF_CORE_COMMAND_CATEGORIES;

    static {
        Map<String, String> map = new HashMap<>();
        // Admin / diagnostics
        map.put("kill", CATEGORY_ADMIN);
        map.put("crosskill", CATEGORY_ADMIN);
        map.put("aimkill", CATEGORY_ADMIN);
        map.put("targetkill", CATEGORY_ADMIN);
        map.put("tps", CATEGORY_ADMIN);
        map.put("lag", CATEGORY_ADMIN);
        map.put("mspt", CATEGORY_ADMIN);
        map.put("performance", CATEGORY_ADMIN);
        map.put("health", CATEGORY_ADMIN);
        map.put("healthcheck", CATEGORY_ADMIN);
        map.put("pl", CATEGORY_ADMIN);
        map.put("plugins", CATEGORY_ADMIN);
        // Core
        map.put("obx", CATEGORY_CORE);
        map.put("help", CATEGORY_CORE);
        // Language
        map.put("language", CATEGORY_LANGUAGE);
        map.put("sprache", CATEGORY_LANGUAGE);
        map.put("idioma", CATEGORY_LANGUAGE);
        // Moderation
        map.put("ban", CATEGORY_MODERATION);
        map.put("unban", CATEGORY_MODERATION);
        map.put("kick", CATEGORY_MODERATION);
        map.put("mute", CATEGORY_MODERATION);
        map.put("unmute", CATEGORY_MODERATION);
        map.put("tempban", CATEGORY_MODERATION);
        map.put("tban", CATEGORY_MODERATION);
        map.put("warn", CATEGORY_MODERATION);
        map.put("banlist", CATEGORY_MODERATION);
        map.put("blist", CATEGORY_MODERATION);
        map.put("status", CATEGORY_MODERATION);
        // Teleport
        map.put("home", CATEGORY_TELEPORT);
        map.put("sethome", CATEGORY_TELEPORT);
        map.put("delhome", CATEGORY_TELEPORT);
        map.put("homes", CATEGORY_TELEPORT);
        map.put("spawn", CATEGORY_TELEPORT);
        map.put("setspawn", CATEGORY_TELEPORT);
        map.put("back", CATEGORY_TELEPORT);
        map.put("top", CATEGORY_TELEPORT);
        map.put("above", CATEGORY_TELEPORT);
        map.put("ground", CATEGORY_TELEPORT);
        map.put("up", CATEGORY_TELEPORT);
        map.put("warp", CATEGORY_TELEPORT);
        map.put("warps", CATEGORY_TELEPORT);
        map.put("w", CATEGORY_TELEPORT);
        map.put("goto", CATEGORY_TELEPORT);
        map.put("go", CATEGORY_TELEPORT);
        map.put("travel", CATEGORY_TELEPORT);
        map.put("tp", CATEGORY_TELEPORT);
        map.put("teleport", CATEGORY_TELEPORT);
        map.put("tphere", CATEGORY_TELEPORT);
        map.put("tpa", CATEGORY_TELEPORT);
        map.put("tpaccept", CATEGORY_TELEPORT);
        map.put("tpyes", CATEGORY_TELEPORT);
        map.put("tpdeny", CATEGORY_TELEPORT);
        map.put("tpno", CATEGORY_TELEPORT);
        map.put("pos", CATEGORY_TELEPORT);
        map.put("position", CATEGORY_TELEPORT);
        // Messaging
        map.put("msg", CATEGORY_MESSAGING);
        map.put("tell", CATEGORY_MESSAGING);
        map.put("pm", CATEGORY_MESSAGING);
        map.put("whisper", CATEGORY_MESSAGING);
        map.put("rply", CATEGORY_MESSAGING);
        map.put("reply", CATEGORY_MESSAGING);
        map.put("r", CATEGORY_MESSAGING);
        map.put("inbox", CATEGORY_MESSAGING);
        map.put("inbound", CATEGORY_MESSAGING);
        // Utility
        map.put("gamemode", CATEGORY_UTILITY);
        map.put("gm", CATEGORY_UTILITY);
        map.put("gmode", CATEGORY_UTILITY);
        map.put("mode", CATEGORY_UTILITY);
        map.put("gms", CATEGORY_UTILITY);
        map.put("gmc", CATEGORY_UTILITY);
        map.put("gma", CATEGORY_UTILITY);
        map.put("gmsp", CATEGORY_UTILITY);
        map.put("heal", CATEGORY_UTILITY);
        map.put("backpack", CATEGORY_UTILITY);
        map.put("bp", CATEGORY_UTILITY);
        map.put("pack", CATEGORY_UTILITY);
        map.put("feed", CATEGORY_UTILITY);
        map.put("vital", CATEGORY_UTILITY);
        map.put("restore", CATEGORY_UTILITY);
        map.put("regen", CATEGORY_UTILITY);
        map.put("god", CATEGORY_UTILITY);
        map.put("godmode", CATEGORY_UTILITY);
        map.put("invincible", CATEGORY_UTILITY);
        map.put("immortal", CATEGORY_UTILITY);
        map.put("craft", CATEGORY_UTILITY);
        map.put("workbench", CATEGORY_UTILITY);
        map.put("crafting", CATEGORY_UTILITY);
        map.put("research", CATEGORY_UTILITY);
        map.put("discover", CATEGORY_UTILITY);
        map.put("itemprofile", CATEGORY_UTILITY);
        map.put("iteminfo", CATEGORY_UTILITY);
        map.put("anvil", CATEGORY_UTILITY);
        map.put("forge", CATEGORY_UTILITY);
        map.put("enchant", CATEGORY_UTILITY);
        map.put("enchanting", CATEGORY_UTILITY);
        map.put("enchanttable", CATEGORY_UTILITY);
        map.put("smith", CATEGORY_UTILITY);
        map.put("smithing", CATEGORY_UTILITY);
        map.put("smithtable", CATEGORY_UTILITY);
        map.put("stonecut", CATEGORY_UTILITY);
        map.put("chop", CATEGORY_UTILITY);
        map.put("cut", CATEGORY_UTILITY);
        map.put("scut", CATEGORY_UTILITY);
        map.put("loom", CATEGORY_UTILITY);
        map.put("grindstone", CATEGORY_UTILITY);
        map.put("gstone", CATEGORY_UTILITY);
        map.put("grind", CATEGORY_UTILITY);
        map.put("gs", CATEGORY_UTILITY);
        map.put("cartography", CATEGORY_UTILITY);
        map.put("ctable", CATEGORY_UTILITY);
        map.put("cartograph", CATEGORY_UTILITY);
        map.put("map", CATEGORY_UTILITY);
        SF_CORE_COMMAND_CATEGORIES = Collections.unmodifiableMap(map);
    }

    private HelpGuiMenu() {
    }

    public static void open(ObxPlugin plugin, Player player, int page) {
        open(plugin, player, page, CATEGORY_ALL);
    }

    public static void open(ObxPlugin plugin, Player player, int page, String category) {
        String resolvedCategory = normalizeCategory(category);
        List<HelpEntry> allEntries = collectVisibleCommands(player);
        List<HelpEntry> entries = filterByCategory(allEntries, resolvedCategory);
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int clamped = Math.max(1, Math.min(page, totalPages));
        LanguageManager languages = plugin.getLanguageManager();

        Map<String, String> titlePlaceholders = new LinkedHashMap<>();
        titlePlaceholders.put("page", String.valueOf(clamped));
        titlePlaceholders.put("pages", String.valueOf(totalPages));
        titlePlaceholders.put("category", resolveCategoryDisplay(languages, player, resolvedCategory));
        String rawTitle = languages.get(player, "commands.help.gui.title", titlePlaceholders);
        String title = ChatColor.translateAlternateColorCodes('&', rawTitle);

        HelpGuiHolder holder = new HelpGuiHolder(clamped, totalPages, resolvedCategory);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inventory);

        ItemStack filler = createPane(" ");
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler.clone());
        }

        int start = (clamped - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());
        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, buildCommandItem(languages, player, entries.get(i)));
        }

        if (clamped > 1) {
            inventory.setItem(PREV_SLOT, buildNavItem(languages, player, "commands.help.gui.button.previous", "commands.help.gui.button.previous-lore", clamped - 1, totalPages));
        }
        if (clamped < totalPages) {
            inventory.setItem(NEXT_SLOT, buildNavItem(languages, player, "commands.help.gui.button.next", "commands.help.gui.button.next-lore", clamped + 1, totalPages));
        }
        inventory.setItem(INFO_SLOT, buildInfoItem(languages, player, clamped, totalPages, resolvedCategory));
        inventory.setItem(CATEGORY_SLOT, buildCategoryItem(languages, player, resolvedCategory, entries.size(), allEntries.size()));
        inventory.setItem(CLOSE_SLOT, buildCloseItem(languages, player));

        player.openInventory(inventory);
    }

    public static String commandAt(Inventory inventory, int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        String displayName = meta == null ? null : meta.getDisplayName();
        if (displayName == null) {
            return null;
        }
        String stripped = ChatColor.stripColor(displayName);
        if (stripped == null || !stripped.startsWith("/")) {
            return null;
        }
        return stripped.substring(1).trim();
    }

    public static String nextCategory(String current) {
        return offsetCategory(current, 1);
    }

    public static String previousCategory(String current) {
        return offsetCategory(current, -1);
    }

    public static String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return CATEGORY_ALL;
        }
        for (String known : CATEGORIES) {
            if (known.equalsIgnoreCase(category)) {
                return known;
            }
        }
        return CATEGORY_ALL;
    }

    private static String offsetCategory(String current, int offset) {
        String normalized = normalizeCategory(current);
        int idx = CATEGORIES.indexOf(normalized);
        if (idx < 0) {
            idx = 0;
        }
        int size = CATEGORIES.size();
        int next = ((idx + offset) % size + size) % size;
        return CATEGORIES.get(next);
    }

    private static List<HelpEntry> collectVisibleCommands(Player viewer) {
        Set<String> seen = new HashSet<>();
        List<HelpEntry> entries = new ArrayList<>();
        for (Plugin loaded : Bukkit.getPluginManager().getPlugins()) {
            if (loaded == null || loaded.getDescription() == null) {
                continue;
            }
            Map<String, Map<String, Object>> commandMap = loaded.getDescription().getCommands();
            if (commandMap == null || commandMap.isEmpty()) {
                continue;
            }
            for (String name : commandMap.keySet()) {
                String key = name.toLowerCase(Locale.ENGLISH);
                if (!seen.add(key)) {
                    continue;
                }
                PluginCommand command = Bukkit.getPluginCommand(name);
                if (command == null) {
                    continue;
                }
                if (!canViewerRun(command, viewer)) {
                    continue;
                }
                entries.add(buildEntry(name, command));
            }
        }
        Collections.sort(entries, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return entries;
    }

    private static List<HelpEntry> filterByCategory(List<HelpEntry> entries, String category) {
        if (category == null || CATEGORY_ALL.equalsIgnoreCase(category)) {
            return entries;
        }
        List<HelpEntry> filtered = new ArrayList<>();
        for (HelpEntry entry : entries) {
            if (category.equalsIgnoreCase(entry.category)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    /**
     * Decides whether a given command should be listed in the GUI for the supplied viewer.
     * The rule is intentionally generous: show the command if the viewer has the listed
     * permission, OR the permission is unset, OR the permission resolves to a default of
     * {@code TRUE}. This means OPs see every command they can actually run (including the
     * virtual workstations and admin utilities), while regular players still see the
     * default-true subset.
     */
    private static boolean canViewerRun(PluginCommand command, Player viewer) {
        String permission = command.getPermission();
        if (permission == null || permission.trim().isEmpty()) {
            return true;
        }
        if (viewer != null && viewer.hasPermission(permission)) {
            return true;
        }
        Permission perm = Bukkit.getPluginManager().getPermission(permission);
        if (perm == null) {
            return false;
        }
        return perm.getDefault() == PermissionDefault.TRUE;
    }

    private static HelpEntry buildEntry(String name, PluginCommand command) {
        HelpEntry entry = new HelpEntry();
        entry.name = name;
        String description = command.getDescription();
        entry.description = description == null || description.trim().isEmpty() ? "No description provided." : description.trim();
        String usage = command.getUsage();
        entry.usage = usage == null || usage.trim().isEmpty() ? ("/" + name) : usage.replace("<command>", name);
        String permission = command.getPermission();
        entry.permission = permission == null || permission.isEmpty() ? "(none)" : permission;
        entry.category = deriveCategory(name, command);
        return entry;
    }

    /**
     * Determines the help GUI category for a command. Lookup priority:
     * <ol>
     *   <li>The static {@link #SF_CORE_COMMAND_CATEGORIES} map keyed by command name and
     *   each declared alias. This is authoritative for OBX commands and survives
     *   ProGuard's package renaming.</li>
     *   <li>If the registered command originates from the ObxPlugin plugin (matched by the
     *   owning plugin's name), the executor package is inspected as a fallback in case a
     *   future OBX command is added without updating the map.</li>
     *   <li>Anything else falls into {@link #CATEGORY_OTHER}.</li>
     * </ol>
     */
    private static String deriveCategory(String commandName, PluginCommand command) {
        if (commandName != null) {
            String mapped = SF_CORE_COMMAND_CATEGORIES.get(commandName.toLowerCase(Locale.ENGLISH));
            if (mapped != null) {
                return mapped;
            }
        }
        if (command != null) {
            List<String> aliases = command.getAliases();
            if (aliases != null) {
                for (String alias : aliases) {
                    if (alias == null) {
                        continue;
                    }
                    String mapped = SF_CORE_COMMAND_CATEGORIES.get(alias.toLowerCase(Locale.ENGLISH));
                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
            // Best-effort fallback for commands registered by OBX that aren't in the map
            // yet. We treat any command whose owning plugin reports name "OBX" as ours
            // and bucket it under the most generic OBX category (Core).
            try {
                if (command.getPlugin() != null && SF_CORE_PLUGIN_NAME.equalsIgnoreCase(command.getPlugin().getName())) {
                    CommandExecutor executor = command.getExecutor();
                    if (executor != null) {
                        Package pkg = executor.getClass().getPackage();
                        String packageName = pkg == null ? executor.getClass().getName() : pkg.getName();
                        if (packageName != null && packageName.startsWith(SF_CORE_COMMAND_PACKAGE)) {
                            String tail = packageName.substring(SF_CORE_COMMAND_PACKAGE.length());
                            int dot = tail.indexOf('.');
                            if (dot >= 0) {
                                tail = tail.substring(0, dot);
                            }
                            return mapPackageSegmentToCategory(tail);
                        }
                    }
                    return CATEGORY_CORE;
                }
            } catch (Throwable ignored) {
                // Plugin lookup failed - treat as foreign
            }
        }
        return CATEGORY_OTHER;
    }

    private static String mapPackageSegmentToCategory(String segment) {
        if (segment == null || segment.isEmpty()) {
            return CATEGORY_OTHER;
        }
        switch (segment.toLowerCase(Locale.ENGLISH)) {
            case "admin":
                return CATEGORY_ADMIN;
            case "core":
                return CATEGORY_CORE;
            case "language":
                return CATEGORY_LANGUAGE;
            case "moderation":
                return CATEGORY_MODERATION;
            case "teleportation":
            case "teleport":
                return CATEGORY_TELEPORT;
            case "utility":
                return CATEGORY_UTILITY;
            default:
                return CATEGORY_OTHER;
        }
    }

    private static String resolveCategoryDisplay(LanguageManager languages, Player player, String category) {
        String key = "commands.help.gui.category." + category.toLowerCase(Locale.ENGLISH);
        String resolved = languages.get(player, key, Collections.<String, String>emptyMap());
        if (resolved == null || resolved.isEmpty() || resolved.contains("commands.help.gui.category.")) {
            return category;
        }
        return resolved;
    }

    private static ItemStack buildCommandItem(LanguageManager languages, Player player, HelpEntry entry) {
        ItemStack stack = new ItemStack(resolveMaterial("BOOK", "WRITTEN_BOOK", "PAPER"));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("command", entry.name);
        placeholders.put("description", entry.description);
        placeholders.put("permission", entry.permission);
        placeholders.put("usage", entry.usage);
        placeholders.put("category", resolveCategoryDisplay(languages, player, entry.category));

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', languages.get(player, "commands.help.gui.item-name", placeholders)));
        List<String> lore = new ArrayList<>();
        lore.add(colorize(languages.get(player, "commands.help.gui.hover-command", placeholders)));
        lore.add(colorize(languages.get(player, "commands.help.gui.hover-usage", placeholders)));
        lore.add(colorize(languages.get(player, "commands.help.gui.hover-category", placeholders)));
        lore.add(colorize(languages.get(player, "commands.help.gui.hover-permission", placeholders)));
        lore.add(" ");
        lore.add(colorize(languages.get(player, "commands.help.gui.hover-description", placeholders)));
        for (String line : wrapWords(entry.description, HOVER_LINE_LIMIT)) {
            lore.add(ChatColor.WHITE + line);
        }
        lore.add(" ");
        lore.add(colorize(languages.get(player, "commands.help.gui.hover-click", placeholders)));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack buildNavItem(LanguageManager languages, Player player, String labelKey, String loreKey, int targetPage, int totalPages) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("page", String.valueOf(targetPage));
        placeholders.put("pages", String.valueOf(totalPages));
        ItemStack stack = new ItemStack(resolveMaterial("ARROW"));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(colorize(languages.get(player, labelKey, placeholders)));
        meta.setLore(Collections.singletonList(colorize(languages.get(player, loreKey, placeholders))));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack buildInfoItem(LanguageManager languages, Player player, int page, int totalPages, String category) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("page", String.valueOf(page));
        placeholders.put("pages", String.valueOf(totalPages));
        placeholders.put("category", resolveCategoryDisplay(languages, player, category));
        ItemStack stack = new ItemStack(resolveMaterial("NETHER_STAR", "BOOK"));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(colorize(languages.get(player, "commands.help.gui.button.info", placeholders)));
        List<String> lore = new ArrayList<>();
        for (String line : languages.list(player, "commands.help.gui.button.info-lore", placeholders)) {
            lore.add(colorize(line));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack buildCloseItem(LanguageManager languages, Player player) {
        ItemStack stack = new ItemStack(resolveMaterial("BARRIER", "RED_STAINED_GLASS_PANE", "STAINED_GLASS_PANE"));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(colorize(languages.get(player, "commands.help.gui.button.close")));
        List<String> lore = new ArrayList<>();
        for (String line : languages.list(player, "commands.help.gui.button.close-lore", Collections.<String, String>emptyMap())) {
            lore.add(colorize(line));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack buildCategoryItem(LanguageManager languages, Player player, String currentCategory, int filteredCount, int totalCount) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("category", resolveCategoryDisplay(languages, player, currentCategory));
        placeholders.put("next", resolveCategoryDisplay(languages, player, nextCategory(currentCategory)));
        placeholders.put("previous", resolveCategoryDisplay(languages, player, previousCategory(currentCategory)));
        placeholders.put("count", String.valueOf(filteredCount));
        placeholders.put("total", String.valueOf(totalCount));

        ItemStack stack = new ItemStack(resolveMaterial("HOPPER", "CHEST", "BOOKSHELF"));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(colorize(languages.get(player, "commands.help.gui.button.category", placeholders)));
        List<String> lore = new ArrayList<>();
        for (String line : languages.list(player, "commands.help.gui.button.category-lore", placeholders)) {
            lore.add(colorize(line));
        }
        for (String entry : CATEGORIES) {
            Map<String, String> linePlaceholders = new LinkedHashMap<>(placeholders);
            linePlaceholders.put("entry", resolveCategoryDisplay(languages, player, entry));
            String key = entry.equalsIgnoreCase(currentCategory)
                    ? "commands.help.gui.button.category-line-active"
                    : "commands.help.gui.button.category-line";
            lore.add(colorize(languages.get(player, key, linePlaceholders)));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    static List<String> wrapWords(String input, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return lines;
        }
        String[] words = input.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (current.length() == 0) {
                current.append(word);
                continue;
            }
            if (current.length() + 1 + word.length() <= maxLength) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static ItemStack createPane(String name) {
        Material material = resolveMaterial("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "GLASS_PANE", "THIN_GLASS");
        boolean legacy = material != null && material.name().equalsIgnoreCase("STAINED_GLASS_PANE");
        if (material == null) {
            material = Material.AIR;
        }
        ItemStack stack = legacy ? new ItemStack(material, 1, (short) 7) : new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(new ArrayList<String>());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static Material resolveMaterial(String... names) {
        for (String candidate : names) {
            Material material = Material.matchMaterial(candidate);
            if (material != null) {
                return material;
            }
        }
        return Material.AIR;
    }

    private static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    /** Lightweight value type for a single help row entry. */
    public static final class HelpEntry {
        String name;
        String description;
        String usage;
        String permission;
        String category;

        public String name() {
            return name;
        }

        public String category() {
            return category;
        }
    }

    /** Static reference to the supported aliases for the override command. */
    public static final List<String> COMMAND_ALIASES = Collections.unmodifiableList(Arrays.asList("help", "?", "bukkit:?", "bukkit:help"));
}