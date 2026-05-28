package dev.sergeantfuzzy.sfcore.enchant.service;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.enchant.model.EnchantRarity;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loads scroll-system tuning from {@code enchants/scrolls.yml}: per-rarity
 * success rates and anvil XP costs, the protection save chance, the success-scroll
 * boost, the drag-and-drop XP surcharge, and the extraction level penalty.
 */
public final class ScrollSettings {

    private final Main plugin;

    private final Map<EnchantRarity, Double> successRates = new EnumMap<EnchantRarity, Double>(EnchantRarity.class);
    private final Map<EnchantRarity, Integer> anvilXpCost = new EnumMap<EnchantRarity, Integer>(EnchantRarity.class);
    private double protectSaveChance = 1.0;
    private double successBoost = 0.20;
    private double dragDropXpMultiplier = 1.5;
    private int extractionLevelPenalty = 1;
    private boolean destroyOnFailure = true;

    public ScrollSettings(Main plugin) {
        this.plugin = plugin;
    }

    public void load(boolean destroyOnFailure) {
        this.destroyOnFailure = destroyOnFailure;
        File file = new File(plugin.getDataFolder(), "enchants/scrolls.yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("enchants/scrolls.yml", false);
            } catch (IllegalArgumentException ignored) {
            }
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        double[] rateDefaults = {0.95, 0.85, 0.70, 0.55, 0.40, 0.30};
        int[] costDefaults = {5, 10, 18, 28, 40, 55};
        EnchantRarity[] tiers = EnchantRarity.values();
        for (int i = 0; i < tiers.length; i++) {
            String id = tiers[i].getId();
            successRates.put(tiers[i], cfg.getDouble("scrolls.default_success_rates." + id, rateDefaults[i]));
            anvilXpCost.put(tiers[i], cfg.getInt("scrolls.anvil_xp_cost." + id, costDefaults[i]));
        }
        protectSaveChance = cfg.getDouble("scrolls.protect_scroll_save_chance", 1.0);
        successBoost = cfg.getDouble("scrolls.success_scroll_boost", 0.20);
        dragDropXpMultiplier = cfg.getDouble("scrolls.dragdrop_xp_multiplier", 1.5);
        extractionLevelPenalty = cfg.getInt("scrolls.extraction_level_penalty", 1);
    }

    public double successRate(EnchantRarity rarity) {
        Double value = successRates.get(rarity);
        return value == null ? 0.5 : value;
    }

    public int anvilXpCost(EnchantRarity rarity) {
        Integer value = anvilXpCost.get(rarity);
        return value == null ? 10 : value;
    }

    public int dragDropXpCost(EnchantRarity rarity) {
        return (int) Math.ceil(anvilXpCost(rarity) * dragDropXpMultiplier);
    }

    public double getProtectSaveChance() {
        return protectSaveChance;
    }

    public double getSuccessBoost() {
        return successBoost;
    }

    public int getExtractionLevelPenalty() {
        return extractionLevelPenalty;
    }

    public boolean isDestroyOnFailure() {
        return destroyOnFailure;
    }
}
