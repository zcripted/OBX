package dev.zcripted.obx.feature.enchant.model;

import org.bukkit.ChatColor;

import java.util.Locale;

/**
 * Scroll / enchantment rarity tiers. Rarity controls the color of scroll text,
 * the default anvil success rate (see {@code scrolls.yml}), and the weighting
 * used by world-loot generation. Each tier maps loosely to the level an
 * enchantment scroll carries (Common = Lv1 … Mythic = Lv6+), though an
 * enchantment declares its own canonical rarity in config.
 */
public enum EnchantRarity {

    COMMON("Common", ChatColor.WHITE),
    UNCOMMON("Uncommon", ChatColor.GREEN),
    RARE("Rare", ChatColor.BLUE),
    EPIC("Epic", ChatColor.DARK_PURPLE),
    LEGENDARY("Legendary", ChatColor.GOLD),
    MYTHIC("Mythic", ChatColor.RED);

    private final String displayName;
    private final ChatColor color;

    EnchantRarity(String displayName, ChatColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getId() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    /** Rarity that best matches a scroll carrying the given enchantment level. */
    public static EnchantRarity forLevel(int level) {
        switch (Math.max(1, level)) {
            case 1:
                return COMMON;
            case 2:
                return UNCOMMON;
            case 3:
                return RARE;
            case 4:
                return EPIC;
            case 5:
                return LEGENDARY;
            default:
                return MYTHIC;
        }
    }

    /** Tolerant lookup by id; falls back to {@code COMMON} for unknown values. */
    public static EnchantRarity fromId(String id) {
        if (id == null) {
            return COMMON;
        }
        String normalized = id.trim().toUpperCase(Locale.ENGLISH);
        for (EnchantRarity rarity : values()) {
            if (rarity.name().equals(normalized)) {
                return rarity;
            }
        }
        return COMMON;
    }
}
