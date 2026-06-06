package dev.zcripted.obx.feature.economy.service;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class WorthService {

    private final ObxPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public WorthService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "worth.yml");
    }

    public void load() {
        try {
            if (!file.exists()) {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                try {
                    plugin.saveResource("worth.yml", false);
                } catch (IllegalArgumentException ignored) {
                    file.createNewFile();
                }
            }
            config = YamlConfiguration.loadConfiguration(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to load worth.yml: " + exception.getMessage());
        }
    }

    public void reload() { load(); }

    public double getPrice(Material material) {
        if (material == null || config == null) return 0.0;
        ConfigurationSection section = config.getConfigurationSection("worth");
        if (section == null) return 0.0;
        String key = material.name().toLowerCase(Locale.ROOT);
        return section.getDouble(key, 0.0);
    }

    public boolean hasPrice(Material material) {
        return getPrice(material) > 0.0;
    }

    /**
     * Number of worth.yml entries with a positive price that resolve to a material
     * ON THIS SERVER VERSION — the bundled database spans 1.8.8 → latest, so entries
     * for materials this version doesn't know are simply not counted (or sellable).
     */
    public int pricedCount() {
        if (config == null) return 0;
        ConfigurationSection section = config.getConfigurationSection("worth");
        if (section == null) return 0;
        int count = 0;
        for (String key : section.getKeys(false)) {
            if (section.getDouble(key, 0.0) <= 0.0) continue;
            if (Material.matchMaterial(key.toUpperCase(Locale.ROOT)) != null) count++;
        }
        return count;
    }
}
