package dev.sergeantfuzzy.sfcore.enchant.effect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
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
 * <p>State is in-memory for the session — appropriate for cooldowns and combat
 * saves. Recall points and satchel contents reset on restart (documented); a
 * future phase can persist them through {@code DataService} if desired.
 */
public final class EnchantState {

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<UUID, Map<String, Long>>();
    private final Map<UUID, List<Location>> recallPoints = new ConcurrentHashMap<UUID, List<Location>>();
    private final Map<UUID, Inventory> satchels = new ConcurrentHashMap<UUID, Inventory>();

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

    // ── Satchel storage ─────────────────────────────────────────────────────

    public Inventory satchel(Player player, int slots) {
        Inventory inv = satchels.get(player.getUniqueId());
        if (inv == null) {
            inv = Bukkit.createInventory(player, Math.max(9, ((slots + 8) / 9) * 9),
                    org.bukkit.ChatColor.DARK_PURPLE + "Satchel");
            satchels.put(player.getUniqueId(), inv);
        }
        return inv;
    }
}
