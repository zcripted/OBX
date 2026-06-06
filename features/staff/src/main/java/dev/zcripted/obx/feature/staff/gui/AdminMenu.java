package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.gui.WarpMenuStyling;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

public final class AdminMenu {

    private static final int INFO_SLOT = 4;
    public static final int CLOSE_SLOT = 8;
    private static final boolean HEX_COLORS_SUPPORTED = supportsHexColors();
    private static final String GRADIENT_START = "f6b73c";
    private static final String GRADIENT_END = "fff289";
    private static final String ADMIN_TITLE_TEXT = gradientTitle("Admin Menu");
    // Each entry carries a stable localization key (NOT the display name) so menu
    // routing and per-player EN/DE/ES name+lore resolution never depend on language.
    private static final PlaceholderData[] PLACEHOLDERS = {
            new PlaceholderData(19, "server-control", new String[]{"REDSTONE_BLOCK"}),
            new PlaceholderData(21, "economy", new String[]{"LIME_BUNDLE", "BUNDLE", "GOLD_INGOT"}),
            new PlaceholderData(23, "moderation", new String[]{"PAPER", "BOOK_AND_QUILL", "BOOK"}),
            new PlaceholderData(25, "world-tools", new String[]{"DIAMOND_PICKAXE"}),
            new PlaceholderData(28, "roles", new String[]{"BOOK", "WRITABLE_BOOK", "BOOK_AND_QUILL"}),
            new PlaceholderData(30, "chat-settings", new String[]{"SIGN", "OAK_SIGN"}),
            new PlaceholderData(32, "fun-utilities", new String[]{"FIREWORK", "FIREWORK_ROCKET"}),
            new PlaceholderData(34, "diagnostics", new String[]{"COMPASS"})
    };

    private AdminMenu() {
    }

    public static void open(ObxPlugin plugin, Player player) {
        dev.zcripted.obx.core.language.LanguageManager lang = plugin.getLanguageManager();
        AdminMenuHolder holder = new AdminMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, ADMIN_TITLE_TEXT);
        holder.setInventory(inventory);

        ItemStack filler = createPane(" ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler.clone());
        }

        ItemStack infoItem = new ItemStack(resolveInfoMaterial());
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(lang.get(player, "admin.menu.info.name"));
            infoMeta.setLore(lang.list(player, "admin.menu.info.lore", java.util.Collections.<String, String>emptyMap()));
            hideAttributes(infoMeta);
            infoItem.setItemMeta(infoMeta);
        }
        inventory.setItem(INFO_SLOT, infoItem);

        ItemStack closeItem = new ItemStack(resolveMaterial(new String[]{"BARRIER"}));
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(lang.get(player, "admin.menu.close.name"));
            closeMeta.setLore(lang.list(player, "admin.menu.close.lore", java.util.Collections.<String, String>emptyMap()));
            hideAttributes(closeMeta);
            closeItem.setItemMeta(closeMeta);
        }
        inventory.setItem(CLOSE_SLOT, closeItem);

        for (PlaceholderData placeholderData : PLACEHOLDERS) {
            inventory.setItem(placeholderData.slot(), createPlaceholderItem(plugin, player, placeholderData));
        }

        // Server Control (slot 19) is the entry point to the whole server-control
        // panel, so its icon shows a live, categorized preview of the current server
        // settings instead of static placeholder text.
        inventory.setItem(SERVER_CONTROL_SLOT, buildServerControlItem(plugin, player));

        // Economy (slot 21) gets the same live categorized-preview treatment as
        // Server Control / Warp Manager / Fun Utilities: balances + market snapshot.
        inventory.setItem(ECONOMY_SLOT, buildEconomyItem(plugin, player));

        boolean canManageWarps = hasWarpManagePermission(player);
        ItemStack warpManager = new ItemStack(Material.NETHER_STAR);
        ItemMeta warpMeta = warpManager.getItemMeta();
        if (warpMeta != null) {
            if (canManageWarps) {
                warpMeta.setDisplayName(WarpMenuStyling.gradientTitle(lang.get(player, "admin.menu.warp.name")));
                warpMeta.setLore(lang.list(player, "admin.menu.warp.lore", java.util.Collections.<String, String>emptyMap()));
            } else {
                warpMeta.setDisplayName(lang.get(player, "admin.menu.warp.locked-name"));
                warpMeta.setLore(lang.list(player, "admin.menu.warp.locked-lore", java.util.Collections.<String, String>emptyMap()));
            }
            hideAttributes(warpMeta);
            warpManager.setItemMeta(warpMeta);
        }
        inventory.setItem(37, warpManager);

        // Hub / Lobby Controls — slot 43 sits symmetric with the Warp
        // Manager at slot 37, keeping the row visually balanced.
        boolean canManageHub = player.hasPermission("obx.hub.admin");
        Material hubMaterial = Material.matchMaterial("BEACON");
        if (hubMaterial == null) {
            hubMaterial = Material.matchMaterial("COMPASS");
        }
        if (hubMaterial == null) {
            hubMaterial = Material.STONE;
        }
        ItemStack hubControls = new ItemStack(hubMaterial);
        ItemMeta hubMeta = hubControls.getItemMeta();
        if (hubMeta != null) {
            boolean hubEnabled = plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class) != null && plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class).isEnabled();
            int worldCount = plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class) == null ? 0 : plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class).getHubWorlds().size();
            if (canManageHub) {
                java.util.Map<String, String> hubPlaceholders = new java.util.HashMap<>();
                hubPlaceholders.put("status", lang.get(player, hubEnabled ? "admin.menu.hub.enabled" : "admin.menu.hub.disabled"));
                hubPlaceholders.put("worlds", String.valueOf(worldCount));
                hubMeta.setDisplayName(WarpMenuStyling.gradientTitle(lang.get(player, "admin.menu.hub.name")));
                hubMeta.setLore(lang.list(player, "admin.menu.hub.lore", hubPlaceholders));
            } else {
                hubMeta.setDisplayName(lang.get(player, "admin.menu.hub.locked-name"));
                hubMeta.setLore(lang.list(player, "admin.menu.hub.locked-lore", java.util.Collections.<String, String>emptyMap()));
            }
            hideAttributes(hubMeta);
            hubControls.setItemMeta(hubMeta);
        }
        inventory.setItem(43, hubControls);

        player.openInventory(inventory);
    }

    /** Public so menu listeners can identify the Hub Controls slot. */
    public static final int HUB_CONTROLS_SLOT = 43;

    /** Slot of the Server Control entry item (also routed by {@code placeholderForSlot}). */
    private static final int SERVER_CONTROL_SLOT = 19;

    /** Slot of the Economy entry item (also routed by {@code placeholderForSlot}). */
    private static final int ECONOMY_SLOT = 21;

    /**
     * Builds the Server Control icon with a localized name and a live, categorized
     * preview of up to 15 current server settings. Each rendered line is capped at 45
     * visible characters (color codes excluded) with no mid-word breaks.
     */
    /**
     * Re-renders the live Server Control preview (players / uptime / current settings) on an
     * already-open Admin Menu, so its player count and uptime tick up while the menu is open.
     * Called by {@code AdminMenuRefreshTask} on the menu's refresh cadence.
     */
    public static void refreshLive(ObxPlugin plugin, Player player, Inventory inventory) {
        if (inventory == null) {
            return;
        }
        inventory.setItem(SERVER_CONTROL_SLOT, buildServerControlItem(plugin, player));
        inventory.setItem(ECONOMY_SLOT, buildEconomyItem(plugin, player));
    }

    private static ItemStack buildServerControlItem(ObxPlugin plugin, Player player) {
        ItemStack item = new ItemStack(resolveMaterial(new String[]{"REDSTONE_BLOCK"}));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            dev.zcripted.obx.core.language.LanguageManager lang = plugin.getLanguageManager();
            meta.setDisplayName(lang.get(player, "admin.scp.name"));
            meta.setLore(serverControlLore(plugin, player));
            hideAttributes(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static java.util.List<String> serverControlLore(ObxPlugin plugin, Player player) {
        dev.zcripted.obx.core.language.LanguageManager lang = plugin.getLanguageManager();
        org.bukkit.World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);

        String on = lang.get(player, "admin.scp.on");
        String off = lang.get(player, "admin.scp.off");
        String yes = lang.get(player, "admin.scp.yes");
        String no = lang.get(player, "admin.scp.no");

        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(lang.get(player, "admin.scp.header"));
        lore.add("");

        // Access
        lore.add(lang.get(player, "admin.scp.cat.access"));
        lore.add(scpRow(lang, player, "whitelist", Bukkit.hasWhitelist() ? on : off));
        lore.add(scpRow(lang, player, "players", Bukkit.getOnlinePlayers().size() + " / " + Bukkit.getMaxPlayers()));

        // Gameplay
        lore.add("");
        lore.add(lang.get(player, "admin.scp.cat.gameplay"));
        lore.add(scpRow(lang, player, "difficulty",
                world == null ? "-" : titleCase(world.getDifficulty().name())));
        lore.add(scpRow(lang, player, "gamemode", titleCase(Bukkit.getDefaultGameMode().name())));
        lore.add(scpRow(lang, player, "pvp", (world != null && world.getPVP()) ? on : off));
        lore.add(scpRow(lang, player, "hardcore", isHardcore() ? yes : no));

        // World
        lore.add("");
        lore.add(lang.get(player, "admin.scp.cat.world"));
        lore.add(scpRow(lang, player, "view-distance", String.valueOf(Bukkit.getViewDistance())));
        lore.add(scpRow(lang, player, "spawn-protect", String.valueOf(Bukkit.getSpawnRadius())));
        lore.add(scpRow(lang, player, "flight", Bukkit.getAllowFlight() ? on : off));
        lore.add(scpRow(lang, player, "nether", Bukkit.getAllowNether() ? on : off));
        lore.add(scpRow(lang, player, "end", Bukkit.getAllowEnd() ? on : off));

        // Server
        lore.add("");
        lore.add(lang.get(player, "admin.scp.cat.server"));
        lore.add(scpRow(lang, player, "version", shortServerVersion()));
        lore.add(scpRow(lang, player, "uptime",
                formatUptime(java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime())));

        lore.add("");
        lore.add(lang.get(player, "admin.scp.footer"));
        lore.add(lang.get(player, "admin.scp.click"));

        java.util.List<String> capped = new java.util.ArrayList<>(lore.size());
        for (String line : lore) {
            capped.add(truncateVisible(line, 45));
        }
        return capped;
    }

    private static String scpRow(dev.zcripted.obx.core.language.LanguageManager lang, Player player, String labelKey, String value) {
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("label", lang.get(player, "admin.scp.label." + labelKey));
        placeholders.put("value", value);
        return lang.get(player, "admin.scp.row", placeholders);
    }

    /**
     * Builds the Economy icon in the same live categorized-preview theme as the
     * Server Control tile: header → ▸ Balances / ▸ Market sections with
     * {@code label » value} rows → divider → click hint. Values are pulled fresh
     * from the economy/worth/shop services on every render (cheap, indexed queries),
     * with {@code —} placeholders when a service is unavailable.
     */
    private static ItemStack buildEconomyItem(ObxPlugin plugin, Player player) {
        ItemStack item = new ItemStack(resolveMaterial(new String[]{"LIME_BUNDLE", "BUNDLE", "GOLD_INGOT"}));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            dev.zcripted.obx.core.language.LanguageManager lang = plugin.getLanguageManager();
            meta.setDisplayName(lang.get(player, "admin.menu.item.economy.name"));
            meta.setLore(economyTileLore(plugin, player));
            hideAttributes(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static java.util.List<String> economyTileLore(ObxPlugin plugin, Player player) {
        dev.zcripted.obx.core.language.LanguageManager lang = plugin.getLanguageManager();
        String none = lang.get(player, "admin.ecp.none");

        String accounts = none;
        String supply = none;
        String average = none;
        String top = none;
        dev.zcripted.obx.api.economy.EconomyService economy = plugin.getEconomyService();
        if (economy != null) {
            // Cached snapshot (3s TTL) — the 0.5s refresh task must not hit SQLite
            // on every render for every viewer.
            EconomyStats.Snapshot stats = EconomyStats.get(economy);
            accounts = stats.accounts < 0 ? none : String.valueOf(stats.accounts);
            supply = stats.supply < 0 ? none : economy.format(stats.supply);
            average = (stats.supply < 0 || stats.accounts <= 0) ? none : economy.format(stats.supply / stats.accounts);
            if (!stats.top.isEmpty()) {
                top = stats.top.get(0).getName() + " (" + economy.format(stats.top.get(0).getBalance()) + ")";
            }
        }
        dev.zcripted.obx.feature.economy.shop.ShopService shop =
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.shop.ShopService.class);
        String categories = shop == null ? none : String.valueOf(shop.getCategories().size());
        dev.zcripted.obx.feature.economy.service.WorthService worth =
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.service.WorthService.class);
        String prices = worth == null ? none : String.valueOf(worth.pricedCount());

        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(lang.get(player, "admin.ecp.header"));
        lore.add("");
        lore.add(lang.get(player, "admin.ecp.cat.balances"));
        lore.add(ecpRow(lang, player, "accounts", accounts));
        lore.add(ecpRow(lang, player, "supply", supply));
        lore.add(ecpRow(lang, player, "average", average));
        lore.add("");
        lore.add(lang.get(player, "admin.ecp.cat.market"));
        lore.add(ecpRow(lang, player, "top", top));
        lore.add(ecpRow(lang, player, "categories", categories));
        lore.add(ecpRow(lang, player, "prices", prices));
        lore.add("");
        lore.add(lang.get(player, "admin.scp.footer"));
        lore.add(lang.get(player, "admin.ecp.click"));

        java.util.List<String> capped = new java.util.ArrayList<>(lore.size());
        for (String line : lore) {
            capped.add(truncateVisible(line, 45));
        }
        return capped;
    }

    /** {@code label » value} row in the shared preview-row format (same as Server Control). */
    private static String ecpRow(dev.zcripted.obx.core.language.LanguageManager lang, Player player, String labelKey, String value) {
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("label", lang.get(player, "admin.ecp.label." + labelKey));
        placeholders.put("value", value);
        return lang.get(player, "admin.scp.row", placeholders);
    }

    /** Reflective {@code Bukkit.isHardcore()} (1.13+); false on older APIs. */
    private static boolean isHardcore() {
        try {
            Object result = Bukkit.class.getMethod("isHardcore").invoke(null);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Short server version (e.g. {@code 1.21.4} from {@code 1.21.4-R0.1-SNAPSHOT}). */
    private static String shortServerVersion() {
        String full = Bukkit.getBukkitVersion();
        if (full == null || full.isEmpty()) {
            return "-";
        }
        int dash = full.indexOf('-');
        return dash > 0 ? full.substring(0, dash) : full;
    }

    private static String titleCase(String enumName) {
        if (enumName == null || enumName.isEmpty()) {
            return "-";
        }
        String[] words = enumName.toLowerCase(java.util.Locale.ENGLISH).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    /**
     * Caps a rendered line at {@code maxVisible} visible characters (legacy color codes
     * and the leading section bar are not counted), trimming on a word boundary and
     * appending an ellipsis when it overflows — so a line never breaks mid-word.
     */
    private static String truncateVisible(String line, int maxVisible) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        int visible = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ChatColor.COLOR_CHAR || c == '&') {
                i++; // skip the color code character that follows
                continue;
            }
            visible++;
        }
        if (visible <= maxVisible) {
            return line;
        }
        StringBuilder out = new StringBuilder();
        int count = 0;
        int lastSpace = -1;
        for (int i = 0; i < line.length() && count < maxVisible; i++) {
            char c = line.charAt(i);
            if (c == ChatColor.COLOR_CHAR || c == '&') {
                if (i + 1 < line.length()) {
                    out.append(c).append(line.charAt(i + 1));
                    i++;
                }
                continue;
            }
            if (c == ' ') {
                lastSpace = out.length();
            }
            out.append(c);
            count++;
        }
        if (lastSpace > 0) {
            out.setLength(lastSpace);
        }
        return out.append("…").toString();
    }

    private static String formatUptime(long millis) {
        long totalSeconds = millis / 1000L;
        long days = totalSeconds / 86400L;
        totalSeconds %= 86400L;
        long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            builder.append(minutes).append("m ");
        }
        builder.append(seconds).append("s");
        return builder.toString();
    }

    private static Material resolveInfoMaterial() {
        Material modern = Material.matchMaterial("WRITABLE_BOOK");
        if (modern != null) {
            return modern;
        }
        Material legacy = Material.matchMaterial("BOOK_AND_QUILL");
        if (legacy != null) {
            return legacy;
        }
        return Material.BOOK;
    }

    private static ItemStack createPane(String name, java.util.List<String> lore) {
        return createPane(name, lore, "GRAY_STAINED_GLASS_PANE", (short) 7);
    }

    private static ItemStack createPlaceholderItem(ObxPlugin plugin, Player player, PlaceholderData data) {
        dev.zcripted.obx.core.language.LanguageManager lang = plugin.getLanguageManager();
        Material material = resolveMaterial(data.materials());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang.get(player, "admin.menu.item." + data.key() + ".name"));
            meta.setLore(lang.list(player, "admin.menu.item." + data.key() + ".lore", java.util.Collections.<String, String>emptyMap()));
            hideAttributes(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Material resolveMaterial(String[] names) {
        if (names == null || names.length == 0) {
            return Material.STONE;
        }
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                return material;
            }
        }
        return Material.STONE;
    }

    private static ItemStack createPane(String name, java.util.List<String> lore, String preferredMaterial, short legacyData) {
        Material material = preferredMaterial != null ? Material.matchMaterial(preferredMaterial) : null;
        boolean legacy = material != null && material.name().equalsIgnoreCase("STAINED_GLASS_PANE");
        if (material == null) {
            material = Material.matchMaterial("STAINED_GLASS_PANE");
            legacy = material != null;
        }
        if (material == null) {
            material = Material.matchMaterial("GLASS_PANE");
        }
        if (material == null) {
            material = Material.matchMaterial("THIN_GLASS");
        }
        if (material == null) {
            material = Material.AIR;
        }
        ItemStack pane = legacy ? new ItemStack(material, 1, legacyData) : new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            hideAttributes(meta);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public static String gradientTitle(String text) {
        if (!HEX_COLORS_SUPPORTED) {
            return ChatColor.DARK_PURPLE.toString() + ChatColor.BOLD + text;
        }
        int totalLetters = (int) text.chars().filter(ch -> ch != ' ').count();
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (char character : text.toCharArray()) {
            if (character == ' ') {
                builder.append(' ');
                continue;
            }
            double ratio = totalLetters <= 1 ? 0 : (double) index / (totalLetters - 1);
            String hex = interpolateHex(GRADIENT_START, GRADIENT_END, ratio);
            builder.append(hexColorCode(hex)).append(ChatColor.BOLD).append(character);
            index++;
        }
        return builder.toString();
    }

    private static boolean hasWarpManagePermission(Player player) {
        if (player == null) {
            return false;
        }
        if (player.hasPermission("obx.warp.manage")) {
            return true;
        }
        String[] perms = new String[]{"obx.warp.set", "obx.warp.delete", "obx.warp.rename", "obx.warp.move", "obx.warp.icon", "obx.warp.public"};
        for (String perm : perms) {
            if (player.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }

    public static PlaceholderView placeholderForSlot(ObxPlugin plugin, Player player, int slot) {
        for (PlaceholderData data : PLACEHOLDERS) {
            if (data.slot() == slot) {
                dev.zcripted.obx.core.language.LanguageManager lang = plugin.getLanguageManager();
                String name = lang.get(player, "admin.menu.item." + data.key() + ".name");
                java.util.List<String> lore = lang.list(player, "admin.menu.item." + data.key() + ".lore",
                        java.util.Collections.<String, String>emptyMap());
                return new PlaceholderView(data.slot(), data.key(), name, lore, data.materials());
            }
        }
        return null;
    }

    private static void hideAttributes(ItemMeta meta) {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
    }

    private static int[] hexToRgb(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[]{
                Integer.parseInt(clean.substring(0, 2), 16),
                Integer.parseInt(clean.substring(2, 4), 16),
                Integer.parseInt(clean.substring(4, 6), 16)
        };
    }

    private static String interpolateHex(String startHex, String endHex, double ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        int[] start = hexToRgb(startHex);
        int[] end = hexToRgb(endHex);
        int r = (int) Math.round(start[0] + (end[0] - start[0]) * ratio);
        int g = (int) Math.round(start[1] + (end[1] - start[1]) * ratio);
        int b = (int) Math.round(start[2] + (end[2] - start[2]) * ratio);
        return String.format("%02x%02x%02x", r, g, b);
    }

    private static String hexColorCode(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        return new StringBuilder()
                .append(ChatColor.COLOR_CHAR).append('x')
                .append(ChatColor.COLOR_CHAR).append(clean.charAt(0))
                .append(ChatColor.COLOR_CHAR).append(clean.charAt(1))
                .append(ChatColor.COLOR_CHAR).append(clean.charAt(2))
                .append(ChatColor.COLOR_CHAR).append(clean.charAt(3))
                .append(ChatColor.COLOR_CHAR).append(clean.charAt(4))
                .append(ChatColor.COLOR_CHAR).append(clean.charAt(5))
                .toString();
    }

    private static boolean supportsHexColors() {
        String version = Bukkit.getBukkitVersion();
        String[] parts = version.split("-")[0].split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return major > 1 || (major == 1 && minor >= 16);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static final class PlaceholderData {
        private final int slot;
        private final String key;
        private final String[] materials;

        private PlaceholderData(int slot, String key, String[] materials) {
            this.slot = slot;
            this.key = key;
            this.materials = materials;
        }

        private int slot() {
            return slot;
        }

        private String key() {
            return key;
        }

        private String[] materials() {
            return materials;
        }
    }

    public static final class PlaceholderView {
        private final int slot;
        private final String key;
        private final String name;
        private final java.util.List<String> lore;
        private final String[] materials;

        public PlaceholderView(int slot, String key, String name, java.util.List<String> lore, String[] materials) {
            this.slot = slot;
            this.key = key;
            this.name = name;
            this.lore = lore;
            this.materials = materials;
        }

        public int slot() {
            return slot;
        }

        /** Stable, language-independent identifier used for menu routing. */
        public String key() {
            return key;
        }

        public String name() {
            return name;
        }

        public java.util.List<String> lore() {
            return lore;
        }

        public String[] materials() {
            return materials;
        }
    }
}
