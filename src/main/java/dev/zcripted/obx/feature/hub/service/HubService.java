package dev.zcripted.obx.feature.hub.service;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Loads, persists, and exposes the hub / lobby configuration stored in
 * {@code systems/hub.yml}. Mirrors the existing service pattern used by
 * {@link dev.zcripted.obx.feature.chat.service.ChatService} and
 * {@link dev.zcripted.obx.feature.tablist.service.TablistService} — typed
 * getters return safe defaults if the file is missing or malformed so hub
 * features degrade silently rather than crashing the plugin.
 *
 * <p>Hub-mode is per-world (the {@code worlds} list). Outside those worlds
 * the entire system is dormant — listeners early-exit before doing work.
 */
public final class HubService {

    private static final String RESOURCE_PATH = "systems/hub.yml";

    private final ObxPlugin plugin;
    private final File configFile;
    private volatile YamlConfiguration config;

    /** Cached lowercase world names for fast {@link #isHubWorld(String)} checks. */
    private final CopyOnWriteArraySet<String> hubWorldNames = new CopyOnWriteArraySet<>();

    public HubService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), RESOURCE_PATH);
    }

    public void load() {
        if (!configFile.exists()) {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try {
                plugin.saveResource(RESOURCE_PATH, false);
            } catch (IllegalArgumentException ignored) {
                // Resource not bundled — load empty defaults below.
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        cacheHubWorlds();
        warnCompassConflict();
    }

    public void reload() {
        load();
    }

    /**
     * Warns if the server-selector item is a COMPASS while WorldEdit is installed.
     * WorldEdit's navigation wand is a compass (right-click {@code /thru},
     * left-click {@code /jumpto}), so players holding {@code worldedit.navigation.*}
     * are teleported to their crosshairs when clicking the selector — in addition
     * to the menu opening. This is the most common "selector teleports me" report.
     */
    private void warnCompassConflict() {
        if (config == null || !config.getBoolean("enabled", false)) {
            return;
        }
        try {
            String material = config.getString("items.server-selector.material", "");
            boolean isCompass = material != null && material.trim().equalsIgnoreCase("COMPASS");
            boolean worldEditPresent = plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null;
            if (isCompass && worldEditPresent) {
                plugin.getLogger().warning("[Hub] Server-selector material is COMPASS and WorldEdit is "
                        + "installed. WorldEdit's navigation wand is also a compass, so anyone with "
                        + "worldedit.navigation.* (ops by default) gets teleported (/thru, /jumpto) when "
                        + "clicking the selector. Fix: set items.server-selector.material in systems/hub.yml "
                        + "to a non-compass item (e.g. NETHER_STAR) and run '/hub give', OR disable WorldEdit's "
                        + "navigation wand.");
            }
        } catch (Throwable ignored) {
        }
    }

    /** Synchronous flush used by command/GUI toggle paths and {@code onDisable}. */
    public void save() {
        if (config == null) {
            return;
        }
        try {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(configFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("[Hub] Failed to save hub.yml: " + exception.getMessage());
        }
    }

    // ── Master enable ──────────────────────────────────────────────────

    public boolean isEnabled() {
        return config != null && config.getBoolean("enabled", false);
    }

    public void setEnabled(boolean value) {
        ensureConfig();
        config.set("enabled", value);
        save();
    }

    public boolean toggleEnabled() {
        boolean next = !isEnabled();
        setEnabled(next);
        return next;
    }

    // ── World whitelist ────────────────────────────────────────────────

    public List<String> getHubWorlds() {
        if (config == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(config.getStringList("worlds"));
    }

    public boolean isHubWorld(String worldName) {
        if (worldName == null) {
            return false;
        }
        return hubWorldNames.contains(worldName.toLowerCase(Locale.ENGLISH));
    }

    public boolean isInHubWorld(Player player) {
        return player != null && player.getWorld() != null
                && isHubWorld(player.getWorld().getName());
    }

    /** True when hub-mode is active AND the player is currently in a hub world. */
    public boolean appliesTo(Player player) {
        return isEnabled() && isInHubWorld(player);
    }

    public boolean addHubWorld(String worldName) {
        ensureConfig();
        if (worldName == null || worldName.isEmpty()) {
            return false;
        }
        List<String> list = new ArrayList<>(config.getStringList("worlds"));
        for (String existing : list) {
            if (existing.equalsIgnoreCase(worldName)) {
                return false;
            }
        }
        list.add(worldName);
        config.set("worlds", list);
        cacheHubWorlds();
        save();
        return true;
    }

    public boolean removeHubWorld(String worldName) {
        ensureConfig();
        if (worldName == null || worldName.isEmpty()) {
            return false;
        }
        List<String> list = new ArrayList<>(config.getStringList("worlds"));
        boolean removed = list.removeIf(existing -> existing.equalsIgnoreCase(worldName));
        if (removed) {
            config.set("worlds", list);
            cacheHubWorlds();
            save();
        }
        return removed;
    }

    private void cacheHubWorlds() {
        hubWorldNames.clear();
        if (config == null) {
            return;
        }
        for (String name : config.getStringList("worlds")) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            hubWorldNames.add(name.toLowerCase(Locale.ENGLISH));
        }
    }

    // ── Kit options ────────────────────────────────────────────────────

    public boolean kitClearInventory() {
        return config == null || config.getBoolean("kit.clear-inventory", true);
    }

    public boolean kitGiveOnRespawn() {
        return config == null || config.getBoolean("kit.give-on-respawn", true);
    }

    public boolean kitLockHotbar() {
        return config == null || config.getBoolean("kit.lock-hotbar", true);
    }

    // ── Item access ────────────────────────────────────────────────────

    public boolean isItemEnabled(String itemId) {
        if (config == null || itemId == null) {
            return false;
        }
        return config.getBoolean("items." + itemId + ".enabled", false);
    }

    public int itemSlot(String itemId, int fallback) {
        if (config == null || itemId == null) {
            return fallback;
        }
        int raw = config.getInt("items." + itemId + ".slot", fallback);
        return Math.max(0, Math.min(8, raw));
    }

    public Material itemMaterial(String itemId, Material fallback) {
        return resolveMaterial("items." + itemId, fallback);
    }

    public Material itemMaterial(String pathPrefix, String fallbackName) {
        Material fallback = Material.matchMaterial(fallbackName);
        if (fallback == null) {
            fallback = Material.STONE;
        }
        return resolveMaterial(pathPrefix, fallback);
    }

    public String itemName(String pathPrefix, String fallback) {
        if (config == null) {
            return fallback;
        }
        return config.getString(pathPrefix + ".name", fallback);
    }

    public List<String> itemLore(String pathPrefix) {
        if (config == null) {
            return Collections.emptyList();
        }
        List<String> list = config.getStringList(pathPrefix + ".lore");
        return list == null ? Collections.<String>emptyList() : list;
    }

    /**
     * Tries {@code path.material}, then each entry in {@code path.material-fallback}.
     * Returns {@code fallback} if none resolve. Used to keep a single JAR working
     * across 1.8.8 → 26.1 where item names changed.
     */
    public Material resolveMaterial(String pathPrefix, Material fallback) {
        if (config == null || pathPrefix == null) {
            return fallback;
        }
        String primary = config.getString(pathPrefix + ".material");
        if (primary != null) {
            Material match = Material.matchMaterial(primary);
            if (match != null) {
                return match;
            }
        }
        List<String> alts = config.getStringList(pathPrefix + ".material-fallback");
        if (alts != null) {
            for (String alt : alts) {
                if (alt == null) {
                    continue;
                }
                Material match = Material.matchMaterial(alt);
                if (match != null) {
                    return match;
                }
            }
        }
        return fallback;
    }

    // ── Per-item helpers ───────────────────────────────────────────────

    public int jumpRodMaxDistance() {
        if (config == null) {
            return 60;
        }
        return Math.max(8, config.getInt("items.jump-rod.max-distance", 60));
    }

    public int launchpadCooldownSeconds() {
        if (config == null) {
            return 3;
        }
        return Math.max(0, config.getInt("items.launchpad.cooldown-seconds", 3));
    }

    public double launchpadForwardPower() {
        if (config == null) {
            return 1.4;
        }
        return config.getDouble("items.launchpad.forward-power", 1.4);
    }

    public double launchpadUpPower() {
        if (config == null) {
            return 1.0;
        }
        return config.getDouble("items.launchpad.up-power", 1.0);
    }

    // ── Selector / servers ─────────────────────────────────────────────

    public String selectorTitle() {
        if (config == null) {
            return "&5&lServer Selector";
        }
        return config.getString("selector.title", "&5&lServer Selector");
    }

    public int selectorRows() {
        if (config == null) {
            return 3;
        }
        int rows = config.getInt("selector.rows", 3);
        return Math.max(1, Math.min(6, rows));
    }

    public int selectorPingCacheSeconds() {
        if (config == null) {
            return 5;
        }
        return Math.max(1, config.getInt("selector.ping-cache-seconds", 5));
    }

    public boolean selectorOutlineEnabled() {
        return config != null && config.getBoolean("selector.outline.enabled", true);
    }

    public Material selectorOutlineMaterial() {
        Material fallback = Material.matchMaterial("GRAY_STAINED_GLASS_PANE");
        if (fallback == null) {
            fallback = Material.matchMaterial("STAINED_GLASS_PANE");
        }
        if (fallback == null) {
            fallback = Material.matchMaterial("GLASS_PANE");
        }
        if (fallback == null) {
            fallback = Material.AIR;
        }
        return resolveMaterial("selector.outline", fallback);
    }

    public boolean selectorCloseEnabled() {
        return config == null || config.getBoolean("selector.close-button.enabled", true);
    }

    public Material selectorCloseMaterial() {
        Material fallback = Material.matchMaterial("BARRIER");
        if (fallback == null) {
            fallback = Material.matchMaterial("RED_STAINED_GLASS_PANE");
        }
        if (fallback == null) {
            fallback = Material.matchMaterial("BEDROCK");
        }
        if (fallback == null) {
            fallback = Material.STONE;
        }
        return resolveMaterial("selector.close-button", fallback);
    }

    /** Configured close-button slot, or {@code -1} to auto-place at the bottom-center. */
    public int selectorCloseSlot() {
        if (config == null) {
            return -1;
        }
        return config.getInt("selector.close-button.slot", -1);
    }

    /** Returns the configured server entries in insertion order. */
    public List<ServerEntry> getSelectorServers() {
        if (config == null) {
            return Collections.emptyList();
        }
        ConfigurationSection section = config.getConfigurationSection("selector.servers");
        if (section == null) {
            return Collections.emptyList();
        }
        Map<String, ServerEntry> ordered = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection entrySection = section.getConfigurationSection(key);
            if (entrySection == null) {
                continue;
            }
            String id = entrySection.getString("id", key);
            String displayName = entrySection.getString("display-name", "&f" + id);
            int slot = entrySection.getInt("slot", -1);
            String pathPrefix = "selector.servers." + key;
            Material material = resolveMaterial(pathPrefix, Material.STONE);
            List<String> lore = entrySection.getStringList("lore");
            if (lore == null) {
                lore = Collections.emptyList();
            }
            ordered.put(key, new ServerEntry(id, displayName, slot, material, lore));
        }
        return new ArrayList<>(ordered.values());
    }

    private void ensureConfig() {
        if (config == null) {
            load();
            if (config == null) {
                config = new YamlConfiguration();
            }
        }
    }

    /** Immutable snapshot of a single selector server entry. */
    public static final class ServerEntry {
        private final String id;
        private final String displayName;
        private final int slot;
        private final Material material;
        private final List<String> lore;

        public ServerEntry(String id, String displayName, int slot, Material material, List<String> lore) {
            this.id = id;
            this.displayName = displayName;
            this.slot = slot;
            this.material = material;
            this.lore = lore;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getSlot() {
            return slot;
        }

        public Material getMaterial() {
            return material;
        }

        public List<String> getLore() {
            return lore;
        }
    }
}
