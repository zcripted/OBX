package dev.zcripted.obx.feature.enchant.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.model.ItemTag;
import dev.zcripted.obx.feature.enchant.registry.EnchantRegistry;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Central orchestrator for the Arcanum enchantment module. Owns the
 * {@link EnchantRegistry} and {@link EnchantStorage}, loads {@code config.yml}
 * (master toggle, apply caps, conflict groups, PvP rules), and exposes the
 * validated {@link #apply} entry point used by commands, the admin GUI, and the
 * scroll system.
 *
 * <p>Dormant by default: when {@code enabled: false} in config the module's
 * listeners early-exit, so registration cost is negligible.
 */
public final class EnchantService {

    private static final String CONFIG_RESOURCE = "enchants/config.yml";

    private final ObxPlugin plugin;
    private final EnchantRegistry registry;
    private final EnchantStorage storage;
    private final ScrollSettings scrollSettings;
    private final CombatPrefs combatPrefs;
    private final File configFile;

    private volatile boolean enabled;
    private volatile boolean debug;
    private volatile int maxEnchantsPerItem;
    private volatile boolean cursedCountsTowardCap;
    private volatile boolean destroyOnFailure;
    private volatile boolean cursedInPvp;
    private volatile boolean mysticInPvp;
    private volatile boolean styleVanilla;
    private volatile CombatSettings combatSettings = new CombatSettings(null);

    /** enchantId → set of enchantIds it cannot coexist with (own + group derived). */
    private volatile Map<String, Set<String>> conflicts = new HashMap<String, Set<String>>();
    /** Admins with proc-debug logging enabled. */
    private final Set<java.util.UUID> debugListeners = new CopyOnWriteArraySet<java.util.UUID>();

    public EnchantService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.registry = new EnchantRegistry(plugin);
        this.storage = new EnchantStorage(registry);
        this.scrollSettings = new ScrollSettings(plugin);
        this.combatPrefs = new CombatPrefs(plugin);
        this.configFile = new File(plugin.getDataFolder(), CONFIG_RESOURCE);
    }

    public void load() {
        if (!configFile.exists()) {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try {
                plugin.saveResource(CONFIG_RESOURCE, false);
            } catch (IllegalArgumentException ignored) {
                // not bundled — defaults below
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("enabled", true);
        debug = config.getBoolean("debug", false);
        maxEnchantsPerItem = Math.max(1, config.getInt("apply.max_enchants_per_item", 6));
        cursedCountsTowardCap = config.getBoolean("apply.cursed_count_toward_cap", false);
        destroyOnFailure = config.getBoolean("apply.destroy_item_on_failure", true);
        cursedInPvp = config.getBoolean("pvp.cursed_in_pvp", true);
        mysticInPvp = config.getBoolean("pvp.mystic_in_pvp", true);
        styleVanilla = config.getBoolean("lore.style_vanilla_enchants", true);
        storage.setStyleVanilla(styleVanilla);
        // Anti-forge: stamp/verify an invisible HMAC signature on enchanted gear. Default
        // trusts unsigned lore (back-compat); owners flip this to false for strict enforcement.
        boolean trustUnsignedLore = config.getBoolean("security.trust_unsigned_lore", true);
        storage.setSecurity(trustUnsignedLore, loadOrCreateSigningSecret());
        combatSettings = new CombatSettings(config.getConfigurationSection("combat_global"));

        registry.load();
        scrollSettings.load(destroyOnFailure);
        combatPrefs.load();
        buildConflicts(config.getConfigurationSection("conflict_groups"));
    }

    public void reload() {
        load();
    }

    /**
     * Returns the server-local HMAC secret used to sign enchanted gear, generating and
     * persisting a fresh random key on first run. Kept in a separate {@code enchant-signing.key}
     * file (not the shared config) so it isn't casually copied between servers — a server's
     * signatures only validate on that server. An unreadable/unwritable key fails open
     * (returns "" → signing disabled) rather than bricking enchant reads.
     */
    private String loadOrCreateSigningSecret() {
        try {
            File keyFile = new File(plugin.getDataFolder(), "enchant-signing.key");
            if (keyFile.isFile()) {
                String existing = new String(java.nio.file.Files.readAllBytes(keyFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!existing.isEmpty()) {
                    return existing;
                }
            }
            File parent = keyFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            byte[] raw = new byte[32];
            new java.security.SecureRandom().nextBytes(raw);
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            String secret = hex.toString();
            java.nio.file.Files.write(keyFile.toPath(),
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return secret;
        } catch (Throwable failure) {
            plugin.getLogger().warning("Could not load/create enchant signing key (anti-forge signing disabled): "
                    + failure.getMessage());
            return "";
        }
    }

    private void buildConflicts(ConfigurationSection groups) {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        // Each enchant's own declared conflicts.
        for (CustomEnchant enchant : registry.all()) {
            Set<String> set = map.get(enchant.getId());
            if (set == null) {
                set = new HashSet<String>();
                map.put(enchant.getId(), set);
            }
            for (String other : enchant.getConflicts()) {
                set.add(other);
                Set<String> reverse = map.get(other);
                if (reverse == null) {
                    reverse = new HashSet<String>();
                    map.put(other, reverse);
                }
                reverse.add(enchant.getId());
            }
        }
        // Mutually-exclusive conflict groups from config.
        if (groups != null) {
            for (String groupKey : groups.getKeys(false)) {
                List<String> members = groups.getStringList(groupKey);
                if (members == null) {
                    continue;
                }
                for (String a : members) {
                    String ai = a.toLowerCase(Locale.ENGLISH);
                    Set<String> set = map.get(ai);
                    if (set == null) {
                        set = new HashSet<String>();
                        map.put(ai, set);
                    }
                    for (String b : members) {
                        String bi = b.toLowerCase(Locale.ENGLISH);
                        if (!ai.equals(bi)) {
                            set.add(bi);
                        }
                    }
                }
            }
        }
        this.conflicts = map;
    }

    // ── Accessors ───────────────────────────────────────────────────────────

    public EnchantRegistry getRegistry() {
        return registry;
    }

    public EnchantStorage getStorage() {
        return storage;
    }

    public ScrollSettings getScrollSettings() {
        return scrollSettings;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getMaxEnchantsPerItem() {
        return maxEnchantsPerItem;
    }

    public boolean isDestroyOnFailure() {
        return destroyOnFailure;
    }

    public boolean isCursedInPvp() {
        return cursedInPvp;
    }

    public boolean isMysticInPvp() {
        return mysticInPvp;
    }

    /** Whether vanilla enchantments are restyled into the themed Vanilla lore section. */
    public boolean isStyleVanilla() {
        return styleVanilla;
    }

    /** Presentation/safety toggles for the Combat enchant category ({@code combat_global}). */
    public CombatSettings getCombatSettings() {
        return combatSettings;
    }

    /** Per-player combat-FX opt-outs (set via {@code /enchants settings}). */
    public CombatPrefs getCombatPrefs() {
        return combatPrefs;
    }

    // ── Debug listeners ───────────────────────────────────────────────────────

    public void setDebug(Player player, boolean on) {
        if (on) {
            debugListeners.add(player.getUniqueId());
        } else {
            debugListeners.remove(player.getUniqueId());
        }
    }

    public boolean isDebugListener(Player player) {
        return debugListeners.contains(player.getUniqueId());
    }

    // ── Conflict checks ───────────────────────────────────────────────────────

    public Set<String> conflictsFor(String enchantId) {
        Set<String> set = conflicts.get(enchantId == null ? null : enchantId.toLowerCase(Locale.ENGLISH));
        return set == null ? Collections.<String>emptySet() : set;
    }

    /** Returns the first enchant already on the item that conflicts with {@code enchant}, or {@code null}. */
    public CustomEnchant findConflict(ItemStack item, CustomEnchant enchant) {
        Set<String> conflictIds = conflictsFor(enchant.getId());
        if (conflictIds.isEmpty()) {
            return null;
        }
        for (String presentId : storage.read(item).keySet()) {
            if (conflictIds.contains(presentId)) {
                return registry.get(presentId);
            }
        }
        return null;
    }

    /**
     * Counts the enchantments on an item against the configurable cap. Cursed
     * enchants optionally don't count (incentivizing risk).
     */
    public int countTowardCap(ItemStack item) {
        int count = 0;
        for (String id : storage.read(item).keySet()) {
            CustomEnchant enchant = registry.get(id);
            if (enchant == null) {
                continue;
            }
            if (enchant.isCursed() && !cursedCountsTowardCap) {
                continue;
            }
            count++;
        }
        return count;
    }

    /**
     * Validates and applies an enchantment to the item, mutating it in place on
     * success. Returns a rich {@link ApplyResult} for the feedback layer.
     */
    public ApplyResult apply(ItemStack item, CustomEnchant enchant, int level) {
        if (!enabled) {
            return ApplyResult.of(ApplyStatus.DISABLED, enchant, level);
        }
        if (item == null || item.getType() == Material.AIR) {
            return ApplyResult.of(ApplyStatus.EMPTY_HAND, enchant, level);
        }
        if (enchant == null || !enchant.hasLevel(level)) {
            return ApplyResult.of(ApplyStatus.INVALID_LEVEL, enchant, level);
        }
        if (!ItemTag.matchesAny(item, enchant.getTags())) {
            return ApplyResult.of(ApplyStatus.WRONG_TYPE, enchant, level);
        }
        int existing = storage.level(item, enchant.getId());
        if (existing == level) {
            return ApplyResult.of(ApplyStatus.ALREADY_APPLIED, enchant, level);
        }
        CustomEnchant conflict = findConflict(item, enchant);
        if (conflict != null) {
            return ApplyResult.conflict(enchant, level, conflict);
        }
        // Cap check only applies when adding a NEW enchant (upgrades are fine).
        if (existing == 0) {
            boolean counts = !(enchant.isCursed() && !cursedCountsTowardCap);
            if (counts && countTowardCap(item) >= maxEnchantsPerItem) {
                return ApplyResult.cap(enchant, level, maxEnchantsPerItem);
            }
        }
        storage.apply(item, enchant, level);
        if (existing > 0) {
            return ApplyResult.upgraded(enchant, level, existing);
        }
        return ApplyResult.of(ApplyStatus.APPLIED, enchant, level);
    }

    /** Removes an enchant from the item. Returns true if something was removed. */
    public boolean remove(ItemStack item, CustomEnchant enchant) {
        if (item == null || enchant == null) {
            return false;
        }
        if (!storage.has(item, enchant.getId())) {
            return false;
        }
        storage.remove(item, enchant.getId());
        return true;
    }

    public void saveConfigUnchanged() {
        // The config is only ever edited on disk by owners; nothing in-memory to persist here.
        // Method kept for symmetry with other services and future toggles (e.g. loot toggles).
        if (!configFile.exists()) {
            try {
                plugin.saveResource(CONFIG_RESOURCE, false);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /** Used by the loot-toggle command to flip a chest type and persist it. */
    public void persistLootToggle(String chestType, boolean value) throws IOException {
        YamlConfiguration loot = loadLoot();
        loot.set("loot." + chestType + ".enabled", value);
        loot.save(lootFile());
    }

    /** Chest-type keys declared in loot.yml (for tab-completion / validation). */
    public List<String> lootChestTypes() {
        ConfigurationSection section = loadLoot().getConfigurationSection("loot");
        if (section == null) {
            return Collections.emptyList();
        }
        return new java.util.ArrayList<String>(section.getKeys(false));
    }

    public boolean isLootChestKnown(String chestType) {
        return chestType != null && lootChestTypes().contains(chestType.toLowerCase(Locale.ENGLISH));
    }

    public boolean isLootChestEnabled(String chestType) {
        return loadLoot().getBoolean("loot." + chestType + ".enabled", false);
    }

    private File lootFile() {
        File file = new File(plugin.getDataFolder(), "enchants/loot.yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("enchants/loot.yml", false);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return file;
    }

    private YamlConfiguration loadLoot() {
        return YamlConfiguration.loadConfiguration(lootFile());
    }
}