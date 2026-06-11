package dev.zcripted.obx.feature.economy.shop;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the player shop from admin-editable YAML:
 *
 * <ul>
 *   <li>{@code shop.yml} — the main menu: title, rows, and one entry per category
 *       (slot, icon, name, lore, file id).</li>
 *   <li>{@code shops/<id>.yml} — one file per category: title plus an item list
 *       ({@code material}, {@code buy}, {@code sell}, optional {@code amount}).</li>
 * </ul>
 *
 * <p><b>Version safety (1.8.8 → latest):</b> every material is resolved through
 * {@link Material#matchMaterial(String)} at load time; entries the running server
 * doesn't know are skipped (and counted in the console summary) instead of erroring,
 * so a single bundled category set serves every supported version. {@code buy = 0}
 * means the item can't be bought, {@code sell = 0} means it can't be sold.
 *
 * <p>The bundled defaults are installed on first run via {@code saveResource}; admins
 * edit/extend them freely (files are never overwritten) and apply changes with
 * {@code /shop reload} or the Economy Control Panel's Shop tile.
 */
public final class ShopService {

    /** One purchasable/sellable shop entry (material resolved for THIS server version). */
    public static final class ShopItem {
        private final Material material;
        private final int amount;
        private final double buyPrice;
        private final double sellPrice;
        private final int stock;

        ShopItem(Material material, int amount, double buyPrice, double sellPrice, int stock) {
            this.material = material;
            this.amount = amount;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.stock = stock;
        }

        public Material material() { return material; }
        /** Units per purchase click (default 1). */
        public int amount() { return amount; }
        /** Price to buy {@link #amount()} units; {@code <= 0} = not buyable. */
        public double buyPrice() { return buyPrice; }
        /** Money per single unit sold; {@code <= 0} = not sellable here. */
        public double sellPrice() { return sellPrice; }
        /** Max units buyable per restock window; {@code <= 0} = unlimited. */
        public int stock() { return stock; }
    }

    /** One category: main-menu tile definition + its resolved item list. */
    public static final class ShopCategory {
        private final String id;
        private final int slot;
        private final String[] iconMaterials;
        private final String name;
        private final List<String> lore;
        private final String title;
        private final List<ShopItem> items;
        private final String file;

        ShopCategory(String id, int slot, String[] iconMaterials, String name,
                     List<String> lore, String title, List<ShopItem> items, String file) {
            this.id = id;
            this.slot = slot;
            this.iconMaterials = iconMaterials;
            this.name = name;
            this.lore = lore;
            this.title = title;
            this.items = items;
            this.file = file;
        }

        public String id() { return id; }
        public int slot() { return slot; }
        public String[] iconMaterials() { return iconMaterials; }
        public String name() { return name; }
        public List<String> lore() { return lore; }
        public String title() { return title; }
        public List<ShopItem> items() { return items; }
        /** The {@code shops/<file>.yml} basename this category's items live in. */
        public String file() { return file; }
    }

    private static final String[] BUNDLED_CATEGORY_FILES = {
            "blocks", "ores", "farming", "food", "mobdrops", "redstone"
    };

    private final ObxPlugin plugin;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();
    private String mainTitle = "&5Shop";
    private int mainRows = 4;
    private boolean enabled = true;

    // ── Finite stock (per restock window) ────────────────────────────────────
    /** Units left this window, keyed {@code categoryId:MATERIAL}; absent = unlimited. */
    private final Map<String, Integer> stockRemaining = new java.util.concurrent.ConcurrentHashMap<>();
    private int restockMinutes = 60;
    private long lastRestock;

    // ── Dynamic pricing config (consumed by ShopPricing) ─────────────────────
    private boolean dynamicEnabled;
    private double dynamicSensitivity = 0.0005;
    private double dynamicMaxDrift = 0.15;
    private double dynamicDailyDecay = 0.25;

    public ShopService(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        categories.clear();
        installDefaults();
        File mainFile = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration main = YamlConfiguration.loadConfiguration(mainFile);
        enabled = main.getBoolean("enabled", true);
        mainTitle = main.getString("title", "&5Shop");
        mainRows = Math.max(2, Math.min(6, main.getInt("rows", 4)));
        restockMinutes = Math.max(1, main.getInt("restock-minutes", 60));
        dynamicEnabled = main.getBoolean("dynamic-pricing.enabled", false);
        dynamicSensitivity = Math.max(0.0, main.getDouble("dynamic-pricing.sensitivity", 0.0005));
        dynamicMaxDrift = Math.max(0.0, Math.min(0.5, main.getDouble("dynamic-pricing.max-drift", 0.15)));
        dynamicDailyDecay = Math.max(0.0, Math.min(1.0, main.getDouble("dynamic-pricing.daily-decay", 0.25)));
        ConfigurationSection list = main.getConfigurationSection("categories");
        int skippedItems = 0;
        int loadedItems = 0;
        if (list != null) {
            for (String id : list.getKeys(false)) {
                ConfigurationSection entry = list.getConfigurationSection(id);
                if (entry == null) continue;
                String file = entry.getString("file", id);
                int slot = entry.getInt("slot", -1);
                String icon = entry.getString("icon", "CHEST");
                String name = entry.getString("name", id);
                List<String> lore = entry.getStringList("lore");
                LoadedCategory loaded = loadCategoryFile(file);
                categories.put(id.toLowerCase(Locale.ENGLISH), new ShopCategory(
                        id.toLowerCase(Locale.ENGLISH), slot,
                        new String[]{icon, "CHEST"}, name, lore, loaded.title, loaded.items, file));
                loadedItems += loaded.items.size();
                skippedItems += loaded.skipped;
            }
        }
        dev.zcripted.obx.util.message.ConsoleLog.info(plugin, "Shop",
                "Loaded §d" + categories.size() + "§7 categories · §d" + loadedItems + "§7 items"
                        + (skippedItems > 0
                        ? " §8(§e" + skippedItems + " skipped — not in this server version§8)" : ""));
        restockAll();
    }

    // ── Stock window ─────────────────────────────────────────────────────────

    /** Resets every finite-stock item to its configured maximum. */
    private void restockAll() {
        stockRemaining.clear();
        for (ShopCategory category : categories.values()) {
            for (ShopItem item : category.items()) {
                if (item.stock() > 0) {
                    stockRemaining.put(stockKey(category.id(), item.material()), item.stock());
                }
            }
        }
        lastRestock = System.currentTimeMillis();
    }

    /** Lazily restocks when the window ({@code restock-minutes}) has elapsed. */
    private void checkRestock() {
        if (System.currentTimeMillis() - lastRestock >= restockMinutes * 60_000L) {
            restockAll();
        }
    }

    /** Units still buyable this window ({@link Integer#MAX_VALUE} = unlimited). */
    public int stockRemaining(String categoryId, ShopItem item) {
        if (item.stock() <= 0) {
            return Integer.MAX_VALUE;
        }
        checkRestock();
        Integer remaining = stockRemaining.get(stockKey(categoryId, item.material()));
        return remaining == null ? item.stock() : Math.max(0, remaining);
    }

    /** Consumes {@code units} from the window's stock (no-op for unlimited items). */
    public void consumeStock(String categoryId, ShopItem item, int units) {
        if (item.stock() <= 0 || units <= 0) {
            return;
        }
        checkRestock();
        stockRemaining.merge(stockKey(categoryId, item.material()), -units,
                (current, delta) -> Math.max(0, current + delta));
    }

    /** Minutes until the next restock (display hint). */
    public long minutesToRestock() {
        long elapsed = System.currentTimeMillis() - lastRestock;
        return Math.max(0, restockMinutes - elapsed / 60_000L);
    }

    private static String stockKey(String categoryId, Material material) {
        return categoryId + ":" + material.name();
    }

    // ── Dynamic pricing config ───────────────────────────────────────────────

    public boolean dynamicPricingEnabled() { return dynamicEnabled; }
    /** Multiplier drift per net unit traded (buys push up, sells push down). */
    public double dynamicSensitivity() { return dynamicSensitivity; }
    /** Hard bound on how far prices drift from base (fraction, e.g. 0.15 = ±15%). */
    public double dynamicMaxDrift() { return dynamicMaxDrift; }
    /** Fraction of accumulated volume that evaporates per day (drifts back to base). */
    public double dynamicDailyDecay() { return dynamicDailyDecay; }

    public void reload() {
        load();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String mainTitle() {
        return mainTitle;
    }

    public int mainRows() {
        return mainRows;
    }

    public List<ShopCategory> categories() {
        return new ArrayList<>(categories.values());
    }

    public List<ShopCategory> getCategories() {
        return categories();
    }

    public ShopCategory categoryById(String id) {
        return id == null ? null : categories.get(id.toLowerCase(Locale.ENGLISH));
    }

    public ShopCategory getCategory(String id) {
        return categoryById(id);
    }

    private static final class LoadedCategory {
        final String title;
        final List<ShopItem> items;
        final int skipped;

        LoadedCategory(String title, List<ShopItem> items, int skipped) {
            this.title = title;
            this.items = items;
            this.skipped = skipped;
        }
    }

    private LoadedCategory loadCategoryFile(String file) {
        File yamlFile = new File(plugin.getDataFolder(), "shops/" + file + ".yml");
        if (!yamlFile.exists()) {
            return new LoadedCategory("&5Shop", Collections.<ShopItem>emptyList(), 0);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
        String title = yaml.getString("title", "&5Shop");
        List<ShopItem> items = new ArrayList<>();
        int skipped = 0;
        for (Map<?, ?> raw : yaml.getMapList("items")) {
            Object materialName = raw.get("material");
            if (materialName == null) continue;
            Material material = Material.matchMaterial(String.valueOf(materialName).toUpperCase(Locale.ENGLISH));
            if (material == null) {
                skipped++; // unknown on this server version — version-safe skip
                continue;
            }
            int amount = clampAmount(raw.get("amount"));
            double buy = toDouble(raw.get("buy"));
            double sell = toDouble(raw.get("sell"));
            int stock = raw.get("stock") instanceof Number
                    ? Math.max(0, ((Number) raw.get("stock")).intValue()) : 0;
            if (buy <= 0 && sell <= 0) continue; // inert entry
            items.add(new ShopItem(material, amount, buy, sell, stock));
        }
        return new LoadedCategory(title, items, skipped);
    }

    private static int clampAmount(Object raw) {
        if (raw instanceof Number) {
            return Math.max(1, Math.min(64, ((Number) raw).intValue()));
        }
        return 1;
    }

    private static double toDouble(Object raw) {
        if (raw instanceof Number) {
            double value = ((Number) raw).doubleValue();
            return Double.isFinite(value) && value > 0 ? value : 0.0;
        }
        return 0.0;
    }

    // ── Editor persistence (/shop admin GUI) ────────────────────────────────
    // Every mutator below writes the YAML on disk immediately and then reloads
    // the in-memory catalogue, so the editor GUI, /shop and /shop reload always
    // agree — there is no separate unsaved "draft" state to lose.

    /**
     * Sets the buy and/or sell price of an existing item ({@code null} = keep
     * that price unchanged). Returns false for unknown category/item or a
     * failed disk write.
     */
    public boolean writePrice(String categoryId, Material material, Double buy, Double sell) {
        ShopCategory category = categoryById(categoryId);
        if (category == null || material == null) {
            return false;
        }
        File yamlFile = categoryFile(category);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
        List<Map<String, Object>> items = mutableItems(yaml);
        boolean found = false;
        for (Map<String, Object> entry : items) {
            if (entryMaterial(entry) == material) {
                if (buy != null) {
                    entry.put("buy", buy);
                }
                if (sell != null) {
                    entry.put("sell", sell);
                }
                found = true;
            }
        }
        if (!found) {
            return false;
        }
        yaml.set("items", items);
        return saveAndReload(yaml, yamlFile);
    }

    /** Appends a new item entry. Returns false when it already exists or the write fails. */
    public boolean addItem(String categoryId, Material material, double buy, double sell) {
        ShopCategory category = categoryById(categoryId);
        if (category == null || material == null || (buy <= 0 && sell <= 0)) {
            return false;
        }
        File yamlFile = categoryFile(category);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
        List<Map<String, Object>> items = mutableItems(yaml);
        for (Map<String, Object> entry : items) {
            if (entryMaterial(entry) == material) {
                return false; // already listed — edit its price instead
            }
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("material", material.name());
        entry.put("buy", buy);
        entry.put("sell", sell);
        items.add(entry);
        yaml.set("items", items);
        return saveAndReload(yaml, yamlFile);
    }

    /** Removes an item entry. Returns false for unknown category/item or a failed write. */
    public boolean removeItem(String categoryId, Material material) {
        ShopCategory category = categoryById(categoryId);
        if (category == null || material == null) {
            return false;
        }
        File yamlFile = categoryFile(category);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(yamlFile);
        List<Map<String, Object>> items = mutableItems(yaml);
        boolean removed = false;
        for (java.util.Iterator<Map<String, Object>> it = items.iterator(); it.hasNext(); ) {
            if (entryMaterial(it.next()) == material) {
                it.remove();
                removed = true;
            }
        }
        if (!removed) {
            return false;
        }
        yaml.set("items", items);
        return saveAndReload(yaml, yamlFile);
    }

    /**
     * Creates a new category: a {@code shop.yml} main-menu entry (first free tile
     * slot, CHEST icon, the id as its name) plus an empty {@code shops/<id>.yml}.
     * Returns false when the id exists, no tile slot is free, or a write fails.
     */
    public boolean addCategory(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        String cleanId = id.trim().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9_-]", "");
        if (cleanId.isEmpty() || categories.containsKey(cleanId)) {
            return false;
        }
        int slot = firstFreeTileSlot();
        if (slot < 0) {
            return false; // main menu is full
        }
        File mainFile = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration main = YamlConfiguration.loadConfiguration(mainFile);
        main.set("categories." + cleanId + ".file", cleanId);
        main.set("categories." + cleanId + ".slot", slot);
        main.set("categories." + cleanId + ".icon", "CHEST");
        main.set("categories." + cleanId + ".name", "&f" + cleanId);
        File categoryFile = new File(plugin.getDataFolder(), "shops/" + cleanId + ".yml");
        YamlConfiguration category = new YamlConfiguration();
        category.set("title", "&5Shop &8· &f" + cleanId);
        category.set("items", new ArrayList<Map<String, Object>>());
        try {
            File parent = categoryFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return false;
            }
            main.save(mainFile);
            category.save(categoryFile);
        } catch (java.io.IOException ex) {
            plugin.getLogger().warning("Shop editor: could not write category '" + cleanId + "': " + ex.getMessage());
            return false;
        }
        reload();
        return true;
    }

    /** First main-menu tile slot not taken by a category (nav row excluded), or -1. */
    private int firstFreeTileSlot() {
        int tiles = mainRows * 9 - 9;
        for (int slot = 0; slot < tiles; slot++) {
            boolean taken = false;
            for (ShopCategory category : categories.values()) {
                if (category.slot() == slot) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                return slot;
            }
        }
        return -1;
    }

    private File categoryFile(ShopCategory category) {
        return new File(plugin.getDataFolder(), "shops/" + category.file() + ".yml");
    }

    /** Deep-copies the YAML item list into mutable string-keyed maps. */
    private static List<Map<String, Object>> mutableItems(YamlConfiguration yaml) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<?, ?> raw : yaml.getMapList("items")) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : raw.entrySet()) {
                copy.put(String.valueOf(e.getKey()), e.getValue());
            }
            items.add(copy);
        }
        return items;
    }

    private static Material entryMaterial(Map<String, Object> entry) {
        Object name = entry.get("material");
        return name == null ? null
                : Material.matchMaterial(String.valueOf(name).toUpperCase(Locale.ENGLISH));
    }

    private boolean saveAndReload(YamlConfiguration yaml, File file) {
        try {
            yaml.save(file);
        } catch (java.io.IOException ex) {
            plugin.getLogger().warning("Shop editor: could not write " + file.getName() + ": " + ex.getMessage());
            return false;
        }
        reload();
        return true;
    }

    /** Installs the bundled shop.yml + shops/*.yml defaults once (never overwrites). */
    private void installDefaults() {
        saveIfMissing("shop.yml");
        for (String file : BUNDLED_CATEGORY_FILES) {
            saveIfMissing("shops/" + file + ".yml");
        }
    }

    private void saveIfMissing(String path) {
        File target = new File(plugin.getDataFolder(), path);
        if (target.exists()) {
            return;
        }
        try {
            plugin.saveResource(path, false);
        } catch (IllegalArgumentException notBundled) {
            // No bundled default for this path — admin-created categories are fine.
        }
    }
}