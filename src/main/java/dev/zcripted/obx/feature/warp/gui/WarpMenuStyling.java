package dev.zcripted.obx.feature.warp.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Shared styling helpers for warp-related menus (titles, filler panes, lore builders).
 */
public final class WarpMenuStyling {

    public static final int INVENTORY_SIZE = 54;
    public static final int CONTENT_START = 9;
    public static final int CONTENT_END = 44;
    public static final int SLOT_BACK = 45;
    public static final int SLOT_CLOSE = 49;
    public static final int SLOT_PREVIOUS = 52;
    public static final int SLOT_NEXT = 53;
    private static final String GRADIENT_START = "#A855F7";
    private static final String GRADIENT_END = "#6A1B9A";

    private WarpMenuStyling() {
    }

    public static ItemStack createFiller() {
        Material glass = resolveMaterial("GRAY_STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "GLASS_PANE", "THIN_GLASS");
        boolean legacy = glass != null && glass.name().equalsIgnoreCase("STAINED_GLASS_PANE");
        ItemStack stack = legacy ? new ItemStack(glass, 1, (short) 7) : new ItemStack(glass);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            hide(meta);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material == null ? Material.STONE : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            hide(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static List<String> lore(String... lines) {
        List<String> lore = new ArrayList<>();
        if (lines != null) {
            Collections.addAll(lore, lines);
        }
        return lore;
    }

    public static String separator() {
        return ChatColor.DARK_GRAY + "────────────────────────";
    }

    public static String gradientTitle(String text) {
        if (!supportsHex()) {
            return ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + text;
        }
        int visible = (int) text.chars().filter(ch -> ch != ' ').count();
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (char character : text.toCharArray()) {
            if (character == ' ') {
                builder.append(' ');
                continue;
            }
            double ratio = visible <= 1 ? 0 : (double) index / (visible - 1);
            builder.append(hexColor(interpolate(GRADIENT_START, GRADIENT_END, ratio))).append(ChatColor.BOLD).append(character);
            index++;
        }
        return builder.toString();
    }

    public static Material resolveMaterial(String preferred, String... fallbacks) {
        Material material = preferred == null ? null : Material.matchMaterial(preferred);
        if (material != null) {
            return material;
        }
        if (fallbacks != null) {
            for (String fallback : fallbacks) {
                material = Material.matchMaterial(fallback);
                if (material != null) {
                    return material;
                }
            }
        }
        return Material.STONE;
    }

    private static void hide(ItemMeta meta) {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
    }

    private static boolean supportsHex() {
        try {
            Class<?> chat = Class.forName("net.md_5.bungee.api.ChatColor");
            chat.getMethod("of", String.class);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String interpolate(String start, String end, double ratio) {
        int[] a = hexToRgb(start);
        int[] b = hexToRgb(end);
        int r = (int) Math.round(a[0] + (b[0] - a[0]) * ratio);
        int g = (int) Math.round(a[1] + (b[1] - a[1]) * ratio);
        int bl = (int) Math.round(a[2] + (b[2] - a[2]) * ratio);
        return String.format(Locale.ENGLISH, "#%02X%02X%02X", r, g, bl);
    }

    private static String hexColor(String hex) {
        try {
            Class<?> chat = Class.forName("net.md_5.bungee.api.ChatColor");
            java.lang.reflect.Method of = chat.getMethod("of", String.class);
            Object color = of.invoke(null, hex);
            return color == null ? ChatColor.DARK_PURPLE.toString() : color.toString();
        } catch (Exception ignored) {
            return ChatColor.DARK_PURPLE.toString();
        }
    }

    private static int[] hexToRgb(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[]{
                Integer.valueOf(clean.substring(0, 2), 16),
                Integer.valueOf(clean.substring(2, 4), 16),
                Integer.valueOf(clean.substring(4, 6), 16)
        };
    }
}
