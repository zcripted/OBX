package dev.zcripted.obx.feature.kit.service;

import dev.zcripted.obx.feature.kit.model.Kit;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KitService {

    private final ObxPlugin plugin;
    private final SqliteDataStore store;
    private final File kitsFile;
    private YamlConfiguration kitsConfig;

    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
    }

    public void load() {
        try {
            if (!kitsFile.exists()) {
                if (kitsFile.getParentFile() != null && !kitsFile.getParentFile().exists()) {
                    kitsFile.getParentFile().mkdirs();
                }
                try {
                    plugin.saveResource("kits.yml", false);
                } catch (IllegalArgumentException ignored) {
                    kitsFile.createNewFile();
                }
            }
            kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to load kits.yml: " + exception.getMessage());
        }
        if (store.isAvailable()) {
            store.execute("CREATE TABLE IF NOT EXISTS kit_cooldowns (" +
                    "uuid TEXT NOT NULL," +
                    "kit_name TEXT NOT NULL," +
                    "last_used INTEGER NOT NULL," +
                    "PRIMARY KEY (uuid, kit_name))");
            store.execute("CREATE TABLE IF NOT EXISTS kit_first_join (" +
                    "uuid TEXT PRIMARY KEY," +
                    "claimed INTEGER NOT NULL DEFAULT 1)");
            migrateLegacyYaml();
        }
        parseKits();
    }

    public void reload() { load(); }
    public void save() { /* auto-commit */ }

    private void migrateLegacyYaml() {
        File legacy = new File(plugin.getDataFolder(), "kits-data.yml");
        if (!legacy.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) {
            renameMigrated(legacy);
            return;
        }
        int cooldownsMigrated = 0;
        int firstJoinMigrated = 0;
        for (String uuidKey : players.getKeys(false)) {
            ConfigurationSection entry = players.getConfigurationSection(uuidKey);
            if (entry == null) continue;
            UUID uuid;
            try { uuid = UUID.fromString(uuidKey); } catch (IllegalArgumentException ignored) { continue; }
            ConfigurationSection cooldowns = entry.getConfigurationSection("cooldowns");
            if (cooldowns != null) {
                for (String kitName : cooldowns.getKeys(false)) {
                    long lastUsed = cooldowns.getLong(kitName);
                    store.executeUpdate("INSERT OR REPLACE INTO kit_cooldowns(uuid, kit_name, last_used) VALUES (?, ?, ?)",
                            uuid, kitName, lastUsed);
                    cooldownsMigrated++;
                }
            }
            if (entry.getBoolean("first-join-claimed", false)) {
                store.executeUpdate("INSERT OR IGNORE INTO kit_first_join(uuid, claimed) VALUES (?, 1)", uuid);
                firstJoinMigrated++;
            }
        }
        plugin.getLogger().info("Migrated " + cooldownsMigrated + " kit cooldown(s) and " +
                firstJoinMigrated + " first-join flag(s) from kits-data.yml.");
        renameMigrated(legacy);
    }

    private void renameMigrated(File legacy) {
        File renamed = new File(legacy.getParentFile(), legacy.getName() + ".migrated");
        if (!legacy.renameTo(renamed)) {
            plugin.getLogger().warning("Could not rename " + legacy.getName() + " after migration.");
        }
    }

    private void parseKits() {
        kits.clear();
        if (kitsConfig == null) return;
        ConfigurationSection section = kitsConfig.getConfigurationSection("kits");
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            ConfigurationSection kitSection = section.getConfigurationSection(name);
            if (kitSection == null) continue;
            String display = kitSection.getString("display-name", name);
            long cooldown = kitSection.getLong("cooldown-seconds", 0L);
            boolean firstJoin = kitSection.getBoolean("first-join", false);
            String iconMaterial = kitSection.getString("icon", "CHEST");
            List<ItemStack> items = readItems(kitSection.getList("items"));
            kits.put(name.toLowerCase(), new Kit(name, display, cooldown, items, firstJoin, iconMaterial));
        }
    }

    private List<ItemStack> readItems(List<?> raw) {
        if (raw == null) return Collections.emptyList();
        List<ItemStack> items = new ArrayList<>();
        for (Object entry : raw) {
            if (entry == null) continue;
            ItemStack stack = readItem(entry);
            if (stack != null) items.add(stack);
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private ItemStack readItem(Object raw) {
        if (raw instanceof ItemStack) return (ItemStack) raw;
        if (raw instanceof String) return parseShortItem((String) raw);
        if (raw instanceof Map) return parseMapItem((Map<String, Object>) raw);
        return null;
    }

    private ItemStack parseShortItem(String spec) {
        if (spec == null || spec.isEmpty()) return null;
        String[] parts = spec.split(":", 2);
        Material material = matchMaterial(parts[0]);
        if (material == null) return null;
        int amount = 1;
        if (parts.length > 1) {
            try { amount = Math.max(1, Integer.parseInt(parts[1].trim())); } catch (NumberFormatException ignored) {}
        }
        return new ItemStack(material, amount);
    }

    @SuppressWarnings("unchecked")
    private ItemStack parseMapItem(Map<String, Object> map) {
        Object materialRaw = map.get("material");
        if (!(materialRaw instanceof String)) return null;
        Material material = matchMaterial((String) materialRaw);
        if (material == null) return null;
        int amount = 1;
        Object amountRaw = map.get("amount");
        if (amountRaw instanceof Number) amount = Math.max(1, ((Number) amountRaw).intValue());
        ItemStack stack = new ItemStack(material, amount);
        Object nameRaw = map.get("name");
        Object loreRaw = map.get("lore");
        if (nameRaw instanceof String || loreRaw instanceof List) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                if (nameRaw instanceof String) {
                    meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', (String) nameRaw));
                }
                if (loreRaw instanceof List) {
                    List<String> lore = new ArrayList<>();
                    for (Object line : (List<Object>) loreRaw) {
                        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', String.valueOf(line)));
                    }
                    meta.setLore(lore);
                }
                stack.setItemMeta(meta);
            }
        }
        Object enchants = map.get("enchants");
        if (enchants instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) enchants).entrySet()) {
                Enchantment enchantment = matchEnchantment(entry.getKey());
                int level = 1;
                if (entry.getValue() instanceof Number) level = ((Number) entry.getValue()).intValue();
                if (enchantment != null) {
                    stack.addUnsafeEnchantment(enchantment, level);
                }
            }
        }
        return stack;
    }

    private Material matchMaterial(String input) {
        if (input == null) return null;
        try { return Material.valueOf(input.trim().toUpperCase()); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    @SuppressWarnings("deprecation")
    private Enchantment matchEnchantment(String name) {
        if (name == null) return null;
        return Enchantment.getByName(name.trim().toUpperCase());
    }

    public Kit getKit(String name) {
        if (name == null) return null;
        return kits.get(name.toLowerCase());
    }

    public java.util.Collection<Kit> getKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public long getCooldownRemaining(UUID uuid, Kit kit) {
        if (uuid == null || kit == null || kit.getCooldownSeconds() <= 0 || !store.isAvailable()) return 0L;
        long lastUsed = store.queryFirst("SELECT last_used FROM kit_cooldowns WHERE uuid = ? AND kit_name = ?",
                rs -> rs.getLong("last_used"), uuid, kit.getName().toLowerCase()).orElse(0L);
        if (lastUsed == 0L) return 0L;
        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000L;
        return Math.max(0L, kit.getCooldownSeconds() - elapsed);
    }

    public void markUsed(UUID uuid, Kit kit) {
        if (uuid == null || kit == null || !store.isAvailable()) return;
        store.executeUpdateAsync(
                "INSERT OR REPLACE INTO kit_cooldowns(uuid, kit_name, last_used) VALUES (?, ?, ?)",
                uuid, kit.getName().toLowerCase(), System.currentTimeMillis());
    }

    public boolean hasReceivedFirstJoinKit(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return false;
        return store.queryFirst("SELECT claimed FROM kit_first_join WHERE uuid = ?",
                rs -> rs.getInt("claimed"), uuid).orElse(0) == 1;
    }

    public void markFirstJoinClaimed(UUID uuid) {
        if (uuid == null || !store.isAvailable()) return;
        store.executeUpdateAsync(
                "INSERT OR REPLACE INTO kit_first_join(uuid, claimed) VALUES (?, 1)", uuid);
    }

    public Map<String, Object> giveItems(Player player, Kit kit) {
        Map<String, Object> result = new HashMap<>();
        int dropped = 0;
        for (ItemStack item : kit.getItems()) {
            if (item == null) continue;
            ItemStack copy = item.clone();
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(copy);
            if (overflow != null && !overflow.isEmpty()) {
                for (ItemStack remainder : overflow.values()) {
                    if (remainder == null) continue;
                    player.getWorld().dropItemNaturally(player.getLocation(), remainder);
                    dropped++;
                }
            }
        }
        result.put("droppedCount", dropped);
        return result;
    }
}
