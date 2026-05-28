package dev.sergeantfuzzy.sfcore.enchant.loot;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.enchant.item.EnchantItems;
import dev.sergeantfuzzy.sfcore.enchant.model.CustomEnchant;
import dev.sergeantfuzzy.sfcore.enchant.model.EnchantCategory;
import dev.sergeantfuzzy.sfcore.enchant.model.EnchantRarity;
import dev.sergeantfuzzy.sfcore.enchant.service.EnchantService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * World-loot integration: injects Arcanum scrolls / books into world-generated
 * chests. Driven entirely by {@code enchants/loot.yml} (per-chest enable, weight,
 * cap, rarity distribution, category filter).
 *
 * <p>Hooks {@code org.bukkit.event.world.LootGenerateEvent}, which only exists on
 * Minecraft 1.13+. Because the plugin compiles against the 1.12.2 API, the event
 * is registered <em>reflectively</em> via an {@link EventExecutor} and accessed
 * through reflection — on 1.8–1.12 the registration simply no-ops.
 */
public final class EnchantLoot {

    private final Main plugin;
    private final EnchantService service;
    private final EnchantItems items;
    private final File lootFile;

    private volatile boolean masterEnabled;
    private volatile double globalRollChance;
    private volatile double bookChance;
    private final Map<String, ChestConfig> chests = new HashMap<String, ChestConfig>();
    private int[] defaultRarity = {30, 30, 22, 12, 5, 1};
    private final Map<String, int[]> rarityByChest = new HashMap<String, int[]>();

    private Method getLootTable;
    private Method getLoot;
    private Method setLoot;
    private boolean registered;

    public EnchantLoot(Main plugin) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.items = plugin.getEnchantItems();
        this.lootFile = new File(plugin.getDataFolder(), "enchants/loot.yml");
    }

    public void load() {
        chests.clear();
        rarityByChest.clear();
        if (!lootFile.exists()) {
            try {
                plugin.saveResource("enchants/loot.yml", false);
            } catch (IllegalArgumentException ignored) {
            }
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(lootFile);
        masterEnabled = cfg.getBoolean("enabled", false);
        globalRollChance = cfg.getDouble("global_roll_chance", 0.5);
        bookChance = cfg.getDouble("book_instead_of_scroll_chance", 0.35);

        ConfigurationSection lootSection = cfg.getConfigurationSection("loot");
        if (lootSection != null) {
            for (String key : lootSection.getKeys(false)) {
                ConfigurationSection cs = lootSection.getConfigurationSection(key);
                if (cs == null) {
                    continue;
                }
                ChestConfig cc = new ChestConfig();
                cc.enabled = cs.getBoolean("enabled", false);
                cc.weight = cs.getInt("weight", 10);
                cc.maxPerChest = Math.max(1, cs.getInt("max_per_chest", 1));
                List<String> cats = cs.getStringList("allowed_categories");
                if (cats != null && !cats.isEmpty()) {
                    cc.allowed = new HashSet<EnchantCategory>();
                    for (String cat : cats) {
                        EnchantCategory category = EnchantCategory.fromId(cat);
                        if (category != null) {
                            cc.allowed.add(category);
                        }
                    }
                }
                chests.put(key.toLowerCase(Locale.ENGLISH), cc);
            }
        }

        defaultRarity = readDist(cfg.getConfigurationSection("default_rarity_distribution"), defaultRarity);
        ConfigurationSection distSection = cfg.getConfigurationSection("rarity_distribution");
        if (distSection != null) {
            for (String key : distSection.getKeys(false)) {
                rarityByChest.put(key.toLowerCase(Locale.ENGLISH),
                        readDist(distSection.getConfigurationSection(key), defaultRarity));
            }
        }
    }

    private int[] readDist(ConfigurationSection section, int[] fallback) {
        if (section == null) {
            return fallback.clone();
        }
        EnchantRarity[] tiers = EnchantRarity.values();
        int[] dist = new int[tiers.length];
        for (int i = 0; i < tiers.length; i++) {
            dist[i] = Math.max(0, section.getInt(tiers[i].getId(), fallback[i]));
        }
        return dist;
    }

    /** Registers the LootGenerateEvent hook reflectively (1.13+ only). Safe to call once. */
    public void register() {
        load();
        if (registered) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName("org.bukkit.event.world.LootGenerateEvent");
            getLootTable = eventClass.getMethod("getLootTable");
            getLoot = eventClass.getMethod("getLoot");
            setLoot = eventClass.getMethod("setLoot", Collection.class);
            Listener dummy = new Listener() {
            };
            EventExecutor executor = new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) {
                    handle(event);
                }
            };
            plugin.getServer().getPluginManager().registerEvent(eventClass, dummy, EventPriority.NORMAL, executor, plugin);
            registered = true;
            dev.sergeantfuzzy.sfcore.util.message.ConsoleLog.info(plugin, "Arcanum",
                    "World-loot integration active (LootGenerateEvent).");
        } catch (ClassNotFoundException notSupported) {
            dev.sergeantfuzzy.sfcore.util.message.ConsoleLog.info(plugin, "Arcanum",
                    "World-loot integration needs Minecraft 1.13+; disabled on this version.");
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[Arcanum] Failed to register loot listener: " + throwable.getMessage());
        }
    }

    public void reload() {
        load();
    }

    public boolean isMasterEnabled() {
        return masterEnabled;
    }

    @SuppressWarnings("unchecked")
    private void handle(Event event) {
        if (!service.isEnabled() || !masterEnabled) {
            return;
        }
        try {
            Object lootTable = getLootTable.invoke(event);
            String chestType = chestTypeFromKey(keyOf(lootTable));
            if (chestType == null) {
                return;
            }
            ChestConfig cc = chests.get(chestType);
            if (cc == null || !cc.enabled) {
                return;
            }
            if (Math.random() >= globalRollChance) {
                return;
            }
            List<ItemStack> injected = roll(chestType, cc);
            if (injected.isEmpty()) {
                return;
            }
            List<ItemStack> loot = (List<ItemStack>) getLoot.invoke(event);
            List<ItemStack> merged = new ArrayList<ItemStack>(loot);
            merged.addAll(injected);
            setLoot.invoke(event, merged);
        } catch (Throwable throwable) {
            // Never let loot injection break world generation.
            if (service.isDebug()) {
                plugin.getLogger().warning("[Arcanum] Loot inject error: " + throwable.getMessage());
            }
        }
    }

    private List<ItemStack> roll(String chestType, ChestConfig cc) {
        List<ItemStack> result = new ArrayList<ItemStack>();
        int[] dist = rarityByChest.containsKey(chestType) ? rarityByChest.get(chestType) : defaultRarity;
        for (int i = 0; i < cc.maxPerChest; i++) {
            if (Math.random() * 100.0 >= cc.weight) {
                continue;
            }
            EnchantRarity rarity = pickRarity(dist);
            CustomEnchant enchant = pickEnchant(rarity, cc.allowed);
            if (enchant == null) {
                continue;
            }
            int level = Math.min(enchant.getMaxLevel(), Math.max(1, rarity.ordinal() + 1));
            boolean book = Math.random() < bookChance;
            result.add(book ? items.book(enchant, level, 1) : items.scroll(enchant, level, 1));
        }
        return result;
    }

    private EnchantRarity pickRarity(int[] dist) {
        int total = 0;
        for (int weight : dist) {
            total += weight;
        }
        if (total <= 0) {
            return EnchantRarity.COMMON;
        }
        int roll = (int) (Math.random() * total);
        EnchantRarity[] tiers = EnchantRarity.values();
        for (int i = 0; i < tiers.length; i++) {
            roll -= dist[i];
            if (roll < 0) {
                return tiers[i];
            }
        }
        return EnchantRarity.COMMON;
    }

    private CustomEnchant pickEnchant(EnchantRarity rarity, Set<EnchantCategory> allowed) {
        List<CustomEnchant> exact = new ArrayList<CustomEnchant>();
        List<CustomEnchant> anyRarity = new ArrayList<CustomEnchant>();
        for (CustomEnchant enchant : service.getRegistry().all()) {
            if (allowed != null && !allowed.contains(enchant.getCategory())) {
                continue;
            }
            anyRarity.add(enchant);
            if (enchant.getRarity() == rarity) {
                exact.add(enchant);
            }
        }
        List<CustomEnchant> pool = !exact.isEmpty() ? exact : anyRarity;
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get((int) (Math.random() * pool.size()));
    }

    private String keyOf(Object lootTable) {
        if (lootTable == null) {
            return null;
        }
        try {
            Object key = lootTable.getClass().getMethod("getKey").invoke(lootTable);
            if (key != null) {
                return key.toString();
            }
        } catch (Throwable ignored) {
        }
        return lootTable.toString();
    }

    /**
     * Maps a vanilla loot-table key (e.g. {@code minecraft:chests/ancient_city},
     * {@code minecraft:chests/village/village_weaponsmith}) to one of our config
     * chest-type names, applying the renames where vanilla differs.
     */
    private String chestTypeFromKey(String key) {
        if (key == null) {
            return null;
        }
        String path = key.toLowerCase(Locale.ENGLISH);
        int idx = path.indexOf("chests/");
        if (idx < 0) {
            return null;
        }
        path = path.substring(idx + "chests/".length());
        path = path.replace("village/", "");
        // Direct matches first.
        if (chests.containsKey(path)) {
            return path;
        }
        // Known vanilla → config renames / groupings.
        if (path.equals("simple_dungeon")) {
            return "dungeon";
        }
        if (path.equals("nether_bridge")) {
            return "nether_fortress";
        }
        if (path.equals("igloo_chest")) {
            return "igloo_basement";
        }
        if (path.startsWith("bastion")) {
            return "bastion_remnant";
        }
        if (path.startsWith("trail_ruins")) {
            return "trail_ruins";
        }
        if (path.contains("trial_chambers") && (path.contains("ominous") || path.contains("vault"))) {
            return "ominous_vault";
        }
        if (path.contains("trial_chambers")) {
            return "trial_chamber_reward";
        }
        if (path.equals("spawn_bonus_chest")) {
            return null;
        }
        return chests.containsKey(path) ? path : null;
    }

    private static final class ChestConfig {
        private boolean enabled;
        private int weight;
        private int maxPerChest;
        private Set<EnchantCategory> allowed;
    }
}
