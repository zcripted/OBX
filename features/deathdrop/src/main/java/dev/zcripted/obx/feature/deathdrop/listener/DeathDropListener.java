package dev.zcripted.obx.feature.deathdrop.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.deathdrop.util.EntityPdc;
import dev.zcripted.obx.feature.deathdrop.util.ItemSerialization;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combines a dying player's drops into a single "carry-all" item entity with a holographic name that
 * floats in sync with the item, and restores the items on pickup. Hardened for production:
 *
 * <ul>
 *   <li><b>No duplication</b> — pickup claims the contents via an atomic {@link ConcurrentHashMap#remove},
 *       so a re-fired {@link EntityPickupItemEvent} (fast re-walk, two near-simultaneous pickups) can
 *       never re-grant the same inventory.</li>
 *   <li><b>No merge loss</b> — two same-material carry-alls won't merge (their {@link ItemMergeEvent}
 *       is cancelled), so neither pile's contents is orphaned.</li>
 *   <li><b>No void/lava loss</b> — if the death spot is in the void or lava, grouping is skipped and
 *       vanilla scatters the drops normally (we never concentrate loot into one easily-destroyed
 *       entity after clearing vanilla's drops).</li>
 *   <li><b>Survives restart</b> — the contents are also serialized onto the dropped item's persistent
 *       data container (1.16+), so a restart doesn't void in-flight loot; older servers fall back to
 *       in-memory tracking.</li>
 * </ul>
 */
public final class DeathDropListener implements Listener {

    /** Scoreboard tag stamped on every hologram stand, so orphans can be found and removed. */
    private static final String HOLOGRAM_TAG = "obx_deathdrop_holo";
    /** PDC key holding the Base64-serialized carry-all contents on the item entity. */
    private static final String PDC_KEY = "deathdrop_contents";

    private final ObxPlugin plugin;
    /** Carry-all item UUID → the items it still holds (the in-memory fast path). */
    private final Map<UUID, List<ItemStack>> contents = new ConcurrentHashMap<>();
    /** Carry-all item UUID → the mounted hologram stand (for count updates + removal). */
    private final Map<UUID, ArmorStand> holograms = new ConcurrentHashMap<>();

    /** Resolved once: the container material used for the carry-all entity. */
    private volatile Material containerMaterial;

    public DeathDropListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    // ── lifecycle ──────────────────────────────────────────────────────────

    /** Clears any holograms orphaned by a prior crash. Called on module enable. */
    public void start() {
        sweepOrphanHolograms();
    }

    /** Removes every tracked hologram and drops all in-memory state. Called on module disable. */
    public void shutdown() {
        for (UUID id : new ArrayList<>(holograms.keySet())) {
            ArmorStand stand = holograms.remove(id);
            if (stand != null) {
                try { stand.remove(); } catch (Throwable ignored) { /* already gone */ }
            }
        }
        holograms.clear();
        contents.clear();
    }

    /** Soft reset alias retained for callers that only want the tracking dropped. */
    public void clear() {
        shutdown();
    }

    // ── events ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) {
            return; // keepInventory: nothing drops, nothing to group
        }
        List<ItemStack> drops = event.getDrops();
        if (drops == null || drops.isEmpty()) {
            return;
        }
        Player player = event.getEntity();
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        // Void/lava guard: if the death spot would destroy a concentrated entity, leave vanilla's
        // scattered drops untouched (matches vanilla behaviour; never makes a void/lava death worse).
        if (isUnsafeDropLocation(location)) {
            return;
        }
        List<ItemStack> stored = new ArrayList<>();
        for (ItemStack stack : drops) {
            if (stack != null && stack.getType() != Material.AIR && stack.getAmount() > 0) {
                stored.add(stack.clone());
            }
        }
        if (stored.isEmpty()) {
            return;
        }
        // Take the drops out of vanilla's hands so they don't scatter as loose stacks.
        drops.clear();

        int count = totalCount(stored);
        ItemStack container = buildContainer(count);
        Item entity;
        try {
            entity = world.dropItem(location, container);
        } catch (Throwable cannotDrop) {
            // Spawning failed — fall back to scattering the items so nothing is lost.
            for (ItemStack stack : stored) {
                try {
                    world.dropItemNaturally(location, stack);
                } catch (Throwable ignored) {
                    // give up on this one stack only
                }
            }
            return;
        }
        UUID id = entity.getUniqueId();
        contents.put(id, stored);
        persistContents(entity, stored);
        spawnHologram(entity, count);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        if (item == null) {
            return;
        }
        UUID id = item.getUniqueId();
        // Is this one of ours? (in-memory, or restored from the item's PDC after a restart)
        boolean ours = contents.containsKey(id);
        if (!ours && EntityPdc.getString(plugin, item, PDC_KEY) == null) {
            return; // not a carry-all
        }
        // Only players may collect a carry-all; block mobs/other entities from hoovering it.
        if (!(event.getEntity() instanceof Player)) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);

        // ATOMIC claim. Only the caller that removes a non-null list proceeds — a concurrently
        // re-fired pickup gets null and bails, so the inventory can never be granted twice.
        List<ItemStack> stored = contents.remove(id);
        if (stored == null) {
            // Not in memory — restore from the item's PDC (survives restart). Marking the PDC consumed
            // before distributing prevents a double-restore from two near-simultaneous pickups.
            String data = EntityPdc.getString(plugin, item, PDC_KEY);
            if (data == null) {
                return; // already claimed by another pickup this tick
            }
            EntityPdc.remove(plugin, item, PDC_KEY);
            stored = ItemSerialization.fromBase64(data);
            if (stored == null || stored.isEmpty()) {
                detachHologram(id, item);
                try { item.remove(); } catch (Throwable ignored) { /* gone */ }
                return;
            }
        }

        Player player = (Player) event.getEntity();
        Map<Integer, ItemStack> leftoverMap = player.getInventory().addItem(stored.toArray(new ItemStack[0]));
        if (leftoverMap.isEmpty()) {
            EntityPdc.remove(plugin, item, PDC_KEY);
            detachHologram(id, item);
            try {
                item.remove();
            } catch (Throwable ignored) {
                // entity already gone
            }
            playPickup(player);
        } else {
            // Some items didn't fit — keep them in the entity (memory + PDC) and refresh the count.
            List<ItemStack> rest = new ArrayList<>(leftoverMap.values());
            contents.put(id, rest);
            persistContents(item, rest);
            updateHologram(id, totalCount(rest));
            playPickup(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        // Never let a carry-all merge with another item: a merge removes one entity without firing
        // pickup/despawn, which would orphan its contents (item loss) or conflate two players' loot.
        if (isCarryAll(event.getEntity()) || isCarryAll(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDespawn(ItemDespawnEvent event) {
        // The carry-all reached the item despawn timer (same as the death drops would have):
        // drop our tracking + hologram so nothing leaks. Contents expire exactly as vanilla items do.
        Item item = event.getEntity();
        UUID id = item.getUniqueId();
        contents.remove(id);
        detachHologram(id, item);
    }

    // ── hologram ─────────────────────────────────────────────────────────────

    /**
     * Spawns the floating name as an invisible marker armor stand and mounts it on the item so it
     * moves in perfect sync. Falls back to naming the item itself if the stand can't be mounted.
     */
    @SuppressWarnings("deprecation") // setPassenger(Entity): the one mount call valid across 1.12–1.21
    private void spawnHologram(Item item, int count) {
        String name = hologramName(count);
        ArmorStand stand = null;
        try {
            World world = item.getWorld();
            stand = (ArmorStand) world.spawnEntity(item.getLocation(), EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setMarker(true);          // no hitbox — never interferes with the item or players
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setCustomName(name);
            stand.setCustomNameVisible(true);
            try { stand.setGravity(false); } catch (Throwable ignored) {}
            try { stand.setRemoveWhenFarAway(false); } catch (Throwable ignored) {}
            try { stand.addScoreboardTag(HOLOGRAM_TAG); } catch (Throwable ignored) {}

            boolean mounted = false;
            try {
                mounted = item.setPassenger(stand);
            } catch (Throwable ignored) {
                mounted = false;
            }
            if (!mounted) {
                try { stand.remove(); } catch (Throwable ignored) {}
                try {
                    item.setCustomName(name);
                    item.setCustomNameVisible(true);
                } catch (Throwable ignored) {}
                return;
            }
            holograms.put(item.getUniqueId(), stand);
        } catch (Throwable cannotSpawn) {
            if (stand != null) {
                try { stand.remove(); } catch (Throwable ignored) {}
            }
            try {
                item.setCustomName(name);
                item.setCustomNameVisible(true);
            } catch (Throwable ignored) {}
        }
    }

    private void updateHologram(UUID id, int count) {
        ArmorStand stand = holograms.get(id);
        if (stand == null) {
            return;
        }
        try {
            stand.setCustomName(hologramName(count));
            stand.setCustomNameVisible(true);
        } catch (Throwable ignored) {
            // cosmetic
        }
    }

    /**
     * Removes the hologram for a carry-all: the tracked stand if we have one, plus any tagged armor
     * stand still mounted on the item (covers a hologram restored-but-untracked after a restart).
     */
    private void detachHologram(UUID id, Item item) {
        ArmorStand stand = holograms.remove(id);
        if (stand != null) {
            try { stand.remove(); } catch (Throwable ignored) {}
        }
        if (item != null) {
            try {
                for (Entity passenger : item.getPassengers()) {
                    if (passenger instanceof ArmorStand && hasHologramTag(passenger)) {
                        try { passenger.remove(); } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {
                // getPassengers unavailable / entity gone
            }
        }
    }

    /**
     * Best-effort removal of holograms orphaned by a hard crash (their tracking was lost but the
     * tagged stands persist). Skipped on Folia, where iterating world entities off the region thread
     * isn't safe — orphaned cosmetic stands there are rare and harmless.
     */
    private void sweepOrphanHolograms() {
        try {
            if (plugin.getSchedulerAdapter() != null && plugin.getSchedulerAdapter().isFolia()) {
                return;
            }
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof ArmorStand && hasHologramTag(entity)) {
                        try { entity.remove(); } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {
            // sweeping is best-effort
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean isCarryAll(Entity entity) {
        if (!(entity instanceof Item)) {
            return false;
        }
        UUID id = entity.getUniqueId();
        return contents.containsKey(id) || EntityPdc.getString(plugin, entity, PDC_KEY) != null;
    }

    private void persistContents(Item item, List<ItemStack> stored) {
        String data = ItemSerialization.toBase64(stored);
        if (data != null) {
            EntityPdc.setString(plugin, item, PDC_KEY, data);
        }
    }

    private static boolean hasHologramTag(Entity entity) {
        try {
            return entity.getScoreboardTags().contains(HOLOGRAM_TAG);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** True if dropping a single grouped entity here would likely destroy it (void or lava). */
    private boolean isUnsafeDropLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return true;
        }
        if (loc.getY() < worldMinY(world) + 1.0) {
            return true; // void / below the build floor — the entity would fall out of the world
        }
        try {
            if (isLavaLike(loc.getBlock().getType())
                    || isLavaLike(loc.clone().add(0, -1, 0).getBlock().getType())) {
                return true;
            }
        } catch (Throwable ignored) {
            // block lookup failed (unloaded) — treat as safe and let the drop proceed
        }
        return false;
    }

    private static boolean isLavaLike(Material material) {
        return material != null && material.name().contains("LAVA");
    }

    private static double worldMinY(World world) {
        try {
            Object min = World.class.getMethod("getMinHeight").invoke(world);
            return min instanceof Number ? ((Number) min).doubleValue() : 0.0;
        } catch (Throwable ignored) {
            return 0.0; // pre-1.17 worlds floor at y=0
        }
    }

    private ItemStack buildContainer(int count) {
        ItemStack item = new ItemStack(containerMaterial(), 1);
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(hologramName(count));
                item.setItemMeta(meta);
            }
        } catch (Throwable ignored) {
            // cosmetic only
        }
        return item;
    }

    private Material containerMaterial() {
        Material resolved = containerMaterial;
        if (resolved != null) {
            return resolved;
        }
        resolved = resolveContainerMaterial();
        containerMaterial = resolved;
        return resolved;
    }

    /** Bundle on 1.17+, chest on 1.13–1.16.5, always falling back to {@link Material#CHEST}. */
    private Material resolveContainerMaterial() {
        Material chosen = null;
        try {
            if (plugin.getPlatformInfo() != null && plugin.getPlatformInfo().isAtLeast(1, 17)) {
                chosen = Material.matchMaterial("BUNDLE");
            }
        } catch (Throwable ignored) {
            // version probe failed — fall through to chest
        }
        if (chosen == null) {
            chosen = Material.matchMaterial("CHEST");
        }
        if (chosen == null) {
            chosen = Material.CHEST; // compile-time constant present on every supported version
        }
        return chosen;
    }

    private String hologramName(int count) {
        Map<String, String> placeholders = Collections.singletonMap("count", Integer.toString(count));
        String name = null;
        try {
            if (plugin.getLanguageManager() != null) {
                name = plugin.getLanguageManager().formatConsole("deathdrop.hologram", placeholders);
            }
        } catch (Throwable ignored) {
            // fall through to default
        }
        if (name == null || name.isEmpty()) {
            name = "§5Death Items §7×" + count;
        }
        return name;
    }

    private void playPickup(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf("ENTITY_ITEM_PICKUP"), 0.6f, 1.4f);
        } catch (Throwable ignored) {
            // sound name varies / unavailable — pickup feedback is optional
        }
    }

    private static int totalCount(List<ItemStack> items) {
        int total = 0;
        for (ItemStack stack : items) {
            if (stack != null) {
                total += stack.getAmount();
            }
        }
        return total;
    }
}
