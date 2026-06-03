package dev.zcripted.obx.feature.enchant.registry;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.model.EnchantCategory;
import dev.zcripted.obx.feature.enchant.model.EnchantRarity;
import dev.zcripted.obx.feature.enchant.model.ItemTag;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads and indexes every {@link CustomEnchant} from the per-category config
 * files under {@code <datafolder>/enchants/}. Bundled defaults (combat.yml,
 * defense.yml, …) are written out on first run; server owners may edit them or
 * drop additional {@code .yml} files into the folder.
 *
 * <p>Builds three indexes: by id, by category (insertion order), and by
 * color-stripped display name — the last is what {@code EnchantStorage} uses to
 * parse enchantments back out of an item's lore on the PDC-less storage model.
 */
public final class EnchantRegistry {

    private static final String DIR = "enchants";
    private static final String[] DEFAULT_FILES = {
            "combat.yml", "defense.yml", "tools.yml", "farming.yml",
            "utility.yml", "mystic.yml", "cursed.yml"
    };
    /**
     * Files in {@code enchants/} that are module configuration, NOT enchantment
     * rosters. Their top-level keys ({@code apply}, {@code pvp},
     * {@code conflict_groups}, loot/scroll tuning, …) must never be parsed as
     * enchantments — doing so produced the spurious "unknown category; skipping"
     * warnings on boot. Compared case-insensitively against the file name.
     */
    private static final Set<String> RESERVED_FILES = new HashSet<String>(Arrays.asList(
            "config.yml", "scrolls.yml", "loot.yml"));

    private final ObxPlugin plugin;
    private final Map<String, CustomEnchant> byId = new LinkedHashMap<String, CustomEnchant>();
    private final Map<String, String> byPlainName = new LinkedHashMap<String, String>();
    private final Map<EnchantCategory, List<CustomEnchant>> byCategory = new EnumMap<EnchantCategory, List<CustomEnchant>>(EnchantCategory.class);

    public EnchantRegistry(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        byId.clear();
        byPlainName.clear();
        byCategory.clear();
        for (EnchantCategory category : EnchantCategory.values()) {
            byCategory.put(category, new ArrayList<CustomEnchant>());
        }

        File dir = new File(plugin.getDataFolder(), DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        for (String fileName : DEFAULT_FILES) {
            File target = new File(dir, fileName);
            if (!target.exists()) {
                try {
                    plugin.saveResource(DIR + "/" + fileName, false);
                } catch (IllegalArgumentException ignored) {
                    // Resource not bundled — skip silently.
                }
            }
        }

        File[] files = dir.listFiles();
        if (files != null) {
            // Stable order: bundled category files first (combat → cursed), then extras alphabetically.
            Arrays.sort(files, new java.util.Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    int ia = indexOfDefault(a.getName());
                    int ib = indexOfDefault(b.getName());
                    if (ia != ib) {
                        return Integer.compare(ia, ib);
                    }
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
            for (File file : files) {
                String name = file.getName().toLowerCase(Locale.ENGLISH);
                if (file.isFile() && name.endsWith(".yml") && !RESERVED_FILES.contains(name)) {
                    loadFile(file);
                }
            }
        }

        dev.zcripted.obx.util.message.ConsoleLog.info(plugin, "Arcanum",
                "Loaded " + byId.size() + " custom enchantments across "
                        + EnchantCategory.values().length + " categories.");
    }

    private static int indexOfDefault(String name) {
        for (int i = 0; i < DEFAULT_FILES.length; i++) {
            if (DEFAULT_FILES[i].equalsIgnoreCase(name)) {
                return i;
            }
        }
        return DEFAULT_FILES.length;
    }

    private void loadFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                CustomEnchant enchant = parse(key.toLowerCase(Locale.ENGLISH), section);
                if (enchant != null) {
                    register(enchant);
                }
            } catch (Exception exception) {
                plugin.getLogger().warning("[Arcanum] Failed to load enchantment '" + key
                        + "' from " + file.getName() + ": " + exception.getMessage());
            }
        }
    }

    private CustomEnchant parse(String id, ConfigurationSection section) {
        String displayName = section.getString("display_name", "&7" + id);
        EnchantCategory category = EnchantCategory.fromId(section.getString("category", ""));
        if (category == null) {
            plugin.getLogger().warning("[Arcanum] Enchantment '" + id + "' has an unknown category; skipping.");
            return null;
        }
        EnchantRarity rarity = EnchantRarity.fromId(section.getString("rarity", "common"));
        int maxLevel = Math.max(1, section.getInt("max_level", 1));

        List<ItemTag> tags = new ArrayList<ItemTag>();
        for (String raw : section.getStringList("applies_to")) {
            ItemTag tag = ItemTag.fromId(raw);
            if (tag != null) {
                tags.add(tag);
            }
        }
        if (tags.isEmpty()) {
            tags.add(ItemTag.ANY);
        }

        List<String> conflicts = new ArrayList<String>();
        for (String raw : section.getStringList("conflicts_with")) {
            if (raw != null && !raw.trim().isEmpty()) {
                conflicts.add(raw.trim().toLowerCase(Locale.ENGLISH));
            }
        }

        List<String> description = new ArrayList<String>();
        if (section.isList("description")) {
            description.addAll(section.getStringList("description"));
        } else {
            String desc = section.getString("description", "");
            if (desc != null && !desc.isEmpty()) {
                for (String line : desc.split("\\r?\\n")) {
                    description.add(line);
                }
            }
        }

        Map<Integer, ConfigurationSection> levels = new LinkedHashMap<Integer, ConfigurationSection>();
        ConfigurationSection levelsSection = section.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String levelKey : levelsSection.getKeys(false)) {
                int level;
                try {
                    level = Integer.parseInt(levelKey);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                ConfigurationSection levelData = levelsSection.getConfigurationSection(levelKey);
                if (levelData != null) {
                    levels.put(level, levelData);
                }
            }
        }

        boolean glow = false;
        int customModelData = 0;
        ConfigurationSection scroll = section.getConfigurationSection("scroll");
        if (scroll != null) {
            glow = scroll.getBoolean("common_book_glow", false);
            customModelData = scroll.getInt("custom_model_data", 0);
        }

        return new CustomEnchant(id, displayName, category, rarity, maxLevel, tags, conflicts, description,
                levels, glow, customModelData);
    }

    private void register(CustomEnchant enchant) {
        if (byId.containsKey(enchant.getId())) {
            plugin.getLogger().warning("[Arcanum] Duplicate enchantment id '" + enchant.getId() + "' — keeping the first.");
            return;
        }
        byId.put(enchant.getId(), enchant);
        byPlainName.put(normalizeName(enchant.plainName()), enchant.getId());
        byCategory.get(enchant.getCategory()).add(enchant);
    }

    private static String normalizeName(String name) {
        return ChatColor.stripColor(name == null ? "" : name).trim().toLowerCase(Locale.ENGLISH);
    }

    public CustomEnchant get(String id) {
        return id == null ? null : byId.get(id.toLowerCase(Locale.ENGLISH));
    }

    /** Reverse lookup used by lore parsing: color-stripped display name → enchant. */
    public CustomEnchant byDisplayName(String plainName) {
        String id = byPlainName.get(normalizeName(plainName));
        return id == null ? null : byId.get(id);
    }

    public List<CustomEnchant> all() {
        return Collections.unmodifiableList(new ArrayList<CustomEnchant>(byId.values()));
    }

    public List<CustomEnchant> byCategory(EnchantCategory category) {
        List<CustomEnchant> list = byCategory.get(category);
        return list == null ? Collections.<CustomEnchant>emptyList() : Collections.unmodifiableList(list);
    }

    public int size() {
        return byId.size();
    }
}
