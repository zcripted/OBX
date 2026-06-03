package dev.zcripted.obx.gui.admin;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.gui.shared.WarpMenuStyling;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class AdminMenu {

    private static final int INFO_SLOT = 4;
    public static final int CLOSE_SLOT = 8;
    private static final boolean HEX_COLORS_SUPPORTED = supportsHexColors();
    private static final String GRADIENT_START = "f6b73c";
    private static final String GRADIENT_END = "fff289";
    private static final String ADMIN_TITLE_TEXT = gradientTitle("Admin Menu");
    private static final PlaceholderData[] PLACEHOLDERS = {
            new PlaceholderData(19, ChatColor.YELLOW + "Server Control",
                    Arrays.asList(ChatColor.GRAY + "Future slot for restarts", ChatColor.GRAY + "and scheduled tasks."),
                    new String[]{"REDSTONE_BLOCK"}),
            new PlaceholderData(21, ChatColor.YELLOW + "Economy",
                    Arrays.asList(ChatColor.GRAY + "Reserved for balance tools", ChatColor.GRAY + "and payouts."),
                    new String[]{"LIME_BUNDLE", "BUNDLE", "GOLD_INGOT"}),
            new PlaceholderData(23, ChatColor.YELLOW + "Moderation",
                    Arrays.asList(ChatColor.GRAY + "Live staff commands for bans,", ChatColor.GRAY + "mutes, warns, and Discord logs."),
                    new String[]{"PAPER", "BOOK_AND_QUILL", "BOOK"}),
            new PlaceholderData(25, ChatColor.YELLOW + "World Tools",
                    Arrays.asList(ChatColor.GRAY + "Slot for world editing", ChatColor.GRAY + "and rollbacks."),
                    new String[]{"DIAMOND_PICKAXE"}),
            new PlaceholderData(28, ChatColor.YELLOW + "Roles & Permissions",
                    Arrays.asList(ChatColor.GRAY + "Future GUI to assign", ChatColor.GRAY + "staff/player roles."),
                    new String[]{"BOOK", "WRITABLE_BOOK", "BOOK_AND_QUILL"}),
            new PlaceholderData(30, ChatColor.YELLOW + "Chat Settings",
                    Arrays.asList(ChatColor.GRAY + "Placeholder for chat", ChatColor.GRAY + "formatting helpers."),
                    new String[]{"SIGN", "OAK_SIGN"}),
            new PlaceholderData(32, ChatColor.YELLOW + "Fun Utilities",
                    Arrays.asList(ChatColor.GRAY + "Reserved for cosmetic", ChatColor.GRAY + "toggles and events."),
                    new String[]{"FIREWORK", "FIREWORK_ROCKET"}),
            new PlaceholderData(34, ChatColor.YELLOW + "Diagnostics",
                    Arrays.asList(ChatColor.GRAY + "Future log viewers", ChatColor.GRAY + "and profiling tools."),
                    new String[]{"COMPASS"})
    };

    private AdminMenu() {
    }

    public static void open(Main plugin, Player player) {
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
            infoMeta.setDisplayName(ChatColor.DARK_PURPLE + "Admin Panel");
            infoMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Your control center for OBX.",
                    ChatColor.GRAY + "Manage your server's tools and",
                    ChatColor.GRAY + "settings from the options here."
            ));
            hideAttributes(infoMeta);
            infoItem.setItemMeta(infoMeta);
        }
        inventory.setItem(INFO_SLOT, infoItem);

        ItemStack closeItem = new ItemStack(resolveMaterial(new String[]{"BARRIER"}));
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(ChatColor.RED + "Close");
            closeMeta.setLore(Arrays.asList(ChatColor.DARK_RED + "✖ " + ChatColor.GRAY + "Click to close this menu"));
            hideAttributes(closeMeta);
            closeItem.setItemMeta(closeMeta);
        }
        inventory.setItem(CLOSE_SLOT, closeItem);

        for (PlaceholderData placeholderData : PLACEHOLDERS) {
            inventory.setItem(placeholderData.slot(), createPlaceholderItem(placeholderData));
        }

        boolean canManageWarps = hasWarpManagePermission(player);
        ItemStack warpManager = new ItemStack(Material.NETHER_STAR);
        ItemMeta warpMeta = warpManager.getItemMeta();
        if (warpMeta != null) {
            warpMeta.setDisplayName(WarpMenuStyling.gradientTitle("Warp Manager"));
            if (canManageWarps) {
                warpMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Create, edit, and remove warps.",
                        "",
                        ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Open Warp Manager"
                ));
            } else {
                warpMeta.setDisplayName(ChatColor.DARK_GRAY + "Warp Manager");
                warpMeta.setLore(Arrays.asList(
                        ChatColor.RED + "Locked",
                        ChatColor.DARK_GRAY + "Requires: obx.warp.manage"
                ));
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
            boolean hubEnabled = plugin.getHubService() != null && plugin.getHubService().isEnabled();
            int worldCount = plugin.getHubService() == null ? 0 : plugin.getHubService().getHubWorlds().size();
            if (canManageHub) {
                hubMeta.setDisplayName(WarpMenuStyling.gradientTitle("Hub / Lobby Controls"));
                hubMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Configure the hub / lobby system:",
                        ChatColor.GRAY + "items, kit, server selector, and per-world scope.",
                        "",
                        (hubEnabled ? ChatColor.GREEN + "● Enabled" : ChatColor.RED + "○ Disabled"),
                        ChatColor.YELLOW + "Worlds: " + ChatColor.WHITE + worldCount,
                        "",
                        ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Open Hub Panel"
                ));
            } else {
                hubMeta.setDisplayName(ChatColor.DARK_GRAY + "Hub / Lobby Controls");
                hubMeta.setLore(Arrays.asList(
                        ChatColor.RED + "Locked",
                        ChatColor.DARK_GRAY + "Requires: obx.hub.admin"
                ));
            }
            hideAttributes(hubMeta);
            hubControls.setItemMeta(hubMeta);
        }
        inventory.setItem(43, hubControls);

        player.openInventory(inventory);
    }

    /** Public so menu listeners can identify the Hub Controls slot. */
    public static final int HUB_CONTROLS_SLOT = 43;

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

    private static ItemStack createPlaceholderItem(PlaceholderData data) {
        Material material = resolveMaterial(data.materials());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(data.name());
            meta.setLore(data.lore());
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

    public static PlaceholderView placeholderForSlot(int slot) {
        for (PlaceholderData data : PLACEHOLDERS) {
            if (data.slot() == slot) {
                return new PlaceholderView(data.slot(), data.name(), data.lore(), data.materials());
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
        private final String name;
        private final java.util.List<String> lore;
        private final String[] materials;

        private PlaceholderData(int slot, String name, java.util.List<String> lore, String[] materials) {
            this.slot = slot;
            this.name = name;
            this.lore = lore;
            this.materials = materials;
        }

        private int slot() {
            return slot;
        }

        private String name() {
            return name;
        }

        private java.util.List<String> lore() {
            return lore;
        }

        private String[] materials() {
            return materials;
        }
    }

    public static final class PlaceholderView {
        private final int slot;
        private final String name;
        private final java.util.List<String> lore;
        private final String[] materials;

        public PlaceholderView(int slot, String name, java.util.List<String> lore, String[] materials) {
            this.slot = slot;
            this.name = name;
            this.lore = lore;
            this.materials = materials;
        }

        public int slot() {
            return slot;
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
