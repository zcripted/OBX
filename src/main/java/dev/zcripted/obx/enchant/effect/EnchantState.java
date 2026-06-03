package dev.zcripted.obx.enchant.effect;

import dev.zcripted.obx.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared runtime state for Arcanum effects that need memory across events:
 * per-player ability cooldowns, recall points (Beacon's Memory), satchel
 * inventories, and once-per-life flags (Last Stand, Phoenix Feather, Soulbound).
 *
 * <p>Cooldowns / recall points / life flags are session-scoped (intentional —
 * they reset on restart). <b>Satchel contents are persisted</b> to
 * {@code satchels.yml}: loaded on first open and saved on close and on plugin
 * disable, so a player's stored items survive restarts and reloads.
 */
public final class EnchantState {

    private final Main plugin;
    private final File satchelFile;
    private final YamlConfiguration satchelConfig;

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<UUID, Map<String, Long>>();
    private final Map<UUID, List<Location>> recallPoints = new ConcurrentHashMap<UUID, List<Location>>();
    private final Map<UUID, Inventory> satchels = new ConcurrentHashMap<UUID, Inventory>();

    public EnchantState(Main plugin) {
        this.plugin = plugin;
        this.satchelFile = new File(plugin.getDataFolder(), "satchels.yml");
        this.satchelConfig = YamlConfiguration.loadConfiguration(satchelFile);
    }

    // ── Cooldowns ─────────────────────────────────────────────────────────────

    public boolean onCooldown(Player player, String key) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return false;
        }
        Long until = map.get(key);
        return until != null && until > System.currentTimeMillis();
    }

    public long remainingSeconds(Player player, String key) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return 0;
        }
        Long until = map.get(key);
        if (until == null) {
            return 0;
        }
        return Math.max(0, (until - System.currentTimeMillis() + 999) / 1000);
    }

    public void setCooldown(Player player, String key, long seconds) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            map = new HashMap<String, Long>();
            cooldowns.put(player.getUniqueId(), map);
        }
        map.put(key, System.currentTimeMillis() + seconds * 1000L);
    }

    /** Clears the per-life flags (call on respawn). */
    public void clearLifeFlags(Player player) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map != null) {
            map.remove("last_stand_life");
        }
    }

    // ── Recall points (Beacon's Memory) ────────────────────────────────────────

    public void bindRecall(Player player, Location location, int maxSlots) {
        List<Location> list = recallPoints.get(player.getUniqueId());
        if (list == null) {
            list = new ArrayList<Location>();
            recallPoints.put(player.getUniqueId(), list);
        }
        list.add(0, location.clone());
        while (list.size() > Math.max(1, maxSlots)) {
            list.remove(list.size() - 1);
        }
    }

    public Location latestRecall(Player player) {
        List<Location> list = recallPoints.get(player.getUniqueId());
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    // ── Satchel storage (persisted) ────────────────────────────────────────────

    public Inventory satchel(Player player, int slots) {
        UUID uuid = player.getUniqueId();
        Inventory inv = satchels.get(uuid);
        if (inv == null) {
            int size = Math.max(9, ((slots + 8) / 9) * 9);
            inv = Bukkit.createInventory(player, size, org.bukkit.ChatColor.DARK_PURPLE + "Satchel");
            loadSatchelContents(uuid, inv, size);
            satchels.put(uuid, inv);
        }
        return inv;
    }

    /** True if {@code inv} is the cached satchel inventory for {@code uuid}. */
    public boolean isSatchel(UUID uuid, Inventory inv) {
        return inv != null && inv == satchels.get(uuid);
    }

    /** Persists one player's satchel contents (call on close). */
    public void saveSatchel(Player player) {
        Inventory inv = satchels.get(player.getUniqueId());
        if (inv == null) {
            return;
        }
        writeSatchel(player.getUniqueId(), inv);
        saveFile();
    }

    /** Persists every loaded satchel (call on plugin disable). */
    public void saveAll() {
        if (satchels.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Inventory> entry : satchels.entrySet()) {
            writeSatchel(entry.getKey(), entry.getValue());
        }
        saveFile();
    }

    private void loadSatchelContents(UUID uuid, Inventory inv, int size) {
        List<?> saved = satchelConfig.getList("satchels." + uuid);
        if (saved == null) {
            return;
        }
        int slot = 0;
        for (Object element : saved) {
            if (slot >= size) {
                break;
            }
            if (element instanceof ItemStack) {
                inv.setItem(slot, (ItemStack) element);
            }
            slot++;
        }
    }

    private void writeSatchel(UUID uuid, Inventory inv) {
        satchelConfig.set("satchels." + uuid, new ArrayList<ItemStack>(Arrays.asList(inv.getContents())));
    }

    private void saveFile() {
        try {
            satchelConfig.save(satchelFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save satchels.yml: " + ex.getMessage());
        }
    }
}
