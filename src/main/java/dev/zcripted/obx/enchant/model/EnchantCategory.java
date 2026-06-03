package dev.zcripted.obx.enchant.model;

import org.bukkit.ChatColor;

import java.util.Locale;

/**
 * The seven Arcanum enchantment categories. Each category drives admin-GUI
 * navigation, in-config grouping, and the themed color used when rendering an
 * enchantment's name. Icon materials are stored as plain names and resolved at
 * runtime via {@code Material.matchMaterial(...)} so the single jar keeps
 * working across 1.8.8 → 1.21.x (some icons did not exist in 1.12.2).
 */
public enum EnchantCategory {

    COMBAT("Combat", ChatColor.RED, "DIAMOND_SWORD"),
    DEFENSE("Defense", ChatColor.BLUE, "DIAMOND_CHESTPLATE"),
    TOOLS("Tools", ChatColor.YELLOW, "DIAMOND_PICKAXE"),
    FARMING("Farming", ChatColor.GREEN, "DIAMOND_HOE"),
    UTILITY("Utility", ChatColor.AQUA, "DIAMOND_BOOTS"),
    MYSTIC("Mystic", ChatColor.DARK_PURPLE, "ENDER_EYE"),
    CURSED("Cursed", ChatColor.DARK_RED, "WITHER_SKELETON_SKULL");

    private final String displayName;
    private final ChatColor color;
    private final String iconMaterial;

    EnchantCategory(String displayName, ChatColor color, String iconMaterial) {
        this.displayName = displayName;
        this.color = color;
        this.iconMaterial = iconMaterial;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    /** Plain material name for the GUI icon; resolve with {@code Material.matchMaterial}. */
    public String getIconMaterial() {
        return iconMaterial;
    }

    /** Lowercase id used in config files and commands (e.g. {@code combat}). */
    public String getId() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    /** Tolerant lookup by id; returns {@code null} if no category matches. */
    public static EnchantCategory fromId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.trim().toUpperCase(Locale.ENGLISH);
        for (EnchantCategory category : values()) {
            if (category.name().equals(normalized)) {
                return category;
            }
        }
        return null;
    }
}
