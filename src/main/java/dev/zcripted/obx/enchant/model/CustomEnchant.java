package dev.zcripted.obx.enchant.model;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable definition of one custom enchantment, built from an entry in a
 * category config file (e.g. {@code enchants/combat.yml}). Per-level parameters
 * are kept as raw {@link ConfigurationSection}s and read through typed helpers,
 * which keeps the system fully data-driven — effect handlers ask the enchant for
 * {@code levelDouble(level, "chance", 0)} rather than each enchant needing a
 * bespoke model class.
 */
public final class CustomEnchant {

    private final String id;
    private final String displayName;
    private final EnchantCategory category;
    private final EnchantRarity rarity;
    private final int maxLevel;
    private final List<ItemTag> tags;
    private final List<String> conflicts;
    private final List<String> description;
    private final Map<Integer, ConfigurationSection> levels;
    private final boolean glow;
    private final int customModelData;

    public CustomEnchant(String id, String displayName, EnchantCategory category, EnchantRarity rarity,
                         int maxLevel, List<ItemTag> tags, List<String> conflicts, List<String> description,
                         Map<Integer, ConfigurationSection> levels, boolean glow, int customModelData) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.rarity = rarity;
        this.maxLevel = Math.max(1, maxLevel);
        this.tags = Collections.unmodifiableList(new ArrayList<ItemTag>(tags));
        this.conflicts = Collections.unmodifiableList(new ArrayList<String>(conflicts));
        this.description = Collections.unmodifiableList(new ArrayList<String>(description));
        this.levels = Collections.unmodifiableMap(new LinkedHashMap<Integer, ConfigurationSection>(levels));
        this.glow = glow;
        this.customModelData = customModelData;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EnchantCategory getCategory() {
        return category;
    }

    public EnchantRarity getRarity() {
        return rarity;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public List<ItemTag> getTags() {
        return tags;
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public List<String> getDescription() {
        return description;
    }

    public boolean isGlow() {
        return glow;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean isCursed() {
        return category == EnchantCategory.CURSED;
    }

    /** Color used for the enchant name in lore/menus: category color, but rarity color for cursed-neutral display. */
    public ChatColor displayColor() {
        return category != null ? category.getColor() : ChatColor.GRAY;
    }

    /** Display name without any custom prefix color, color-stripped, for parsing. */
    public String plainName() {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', displayName));
    }

    public boolean hasLevel(int level) {
        return level >= 1 && level <= maxLevel;
    }

    public ConfigurationSection levelSection(int level) {
        return levels.get(level);
    }

    public double levelDouble(int level, String key, double def) {
        ConfigurationSection section = levels.get(level);
        return section == null ? def : section.getDouble(key, def);
    }

    public int levelInt(int level, String key, int def) {
        ConfigurationSection section = levels.get(level);
        return section == null ? def : section.getInt(key, def);
    }

    public boolean levelBoolean(int level, String key, boolean def) {
        ConfigurationSection section = levels.get(level);
        return section == null ? def : section.getBoolean(key, def);
    }

    public String levelString(int level, String key, String def) {
        ConfigurationSection section = levels.get(level);
        return section == null ? def : section.getString(key, def);
    }

    /** Roman numeral for a level (1→I … up to a sensible cap, then plain number). */
    public static String roman(int level) {
        switch (level) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return Integer.toString(level);
        }
    }

    /** Parses a roman numeral (I–X) or plain integer back to a level; 0 if unparseable. */
    public static int parseRoman(String token) {
        if (token == null) {
            return 0;
        }
        String t = token.trim().toUpperCase(Locale.ENGLISH);
        switch (t) {
            case "I": return 1;
            case "II": return 2;
            case "III": return 3;
            case "IV": return 4;
            case "V": return 5;
            case "VI": return 6;
            case "VII": return 7;
            case "VIII": return 8;
            case "IX": return 9;
            case "X": return 10;
            default:
                try {
                    return Integer.parseInt(t);
                } catch (NumberFormatException ignored) {
                    return 0;
                }
        }
    }
}
