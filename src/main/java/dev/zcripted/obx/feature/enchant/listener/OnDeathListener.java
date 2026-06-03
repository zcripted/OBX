package dev.zcripted.obx.feature.enchant.listener;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.enchant.item.EnchantItems;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import dev.zcripted.obx.feature.enchant.util.Particles;
import dev.zcripted.obx.feature.enchant.util.Potions;
import dev.zcripted.obx.feature.enchant.util.SoundPalette;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Death-triggered combat enchants. Currently handles Headsplitter's increased
 * head-drop chance; loot-multiplier and soul-drop effects (Plunderer, Reaper's
 * Toll) land in a later batch.
 */
public final class OnDeathListener implements Listener {

    private static final String[] SPLATTER = {"DAMAGE_INDICATOR", "CRIT", "SMOKE_NORMAL"};
    private static final String[] SOUL_PARTICLE = {"SOUL", "PORTAL", "SPELL_WITCH"};
    private static final String SOUL_MARKER = "Arcanum Soul L";
    private static final String[] SOUL_PICKUP_SOUND = {"ENTITY_VEX_DEATH", "ENTITY_EXPERIENCE_ORB_PICKUP", "ORB_PICKUP"};

    private final OBX plugin;
    private final EnchantService service;
    private final EnchantStorage storage;

    public OnDeathListener(OBX plugin) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.storage = service.getStorage();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) {
            return;
        }
        ItemStack weapon = CombatSupport.mainHand(killer);
        if (weapon == null) {
            return;
        }

        int headsplitter = storage.level(weapon, "headsplitter");
        if (headsplitter > 0) {
            CustomEnchant e = service.getRegistry().get("headsplitter");
            if (Math.random() < e.levelDouble(headsplitter, "head_chance", 0.0)) {
                ItemStack head = headFor(dead);
                if (head != null && dead.getWorld() != null) {
                    dead.getWorld().dropItemNaturally(dead.getLocation(), head);
                }
                Particles.at(dead.getEyeLocation(), SPLATTER, 12, 0.3);
            }
        }

        // Plunderer — multiply common drops, with a chance at extra/rare loot and scrolls.
        int plunderer = storage.level(weapon, "plunderer");
        if (plunderer > 0) {
            CustomEnchant e = service.getRegistry().get("plunderer");
            multiplyDrops(event, e.levelDouble(plunderer, "common_bonus", 0.0));
            if (Math.random() < e.levelDouble(plunderer, "rare_chance", 0.0)) {
                duplicateRandomDrop(event);
            }
            if (Math.random() < e.levelDouble(plunderer, "scroll_chance", 0.0)) {
                ItemStack scroll = randomScroll();
                if (scroll != null) {
                    event.getDrops().add(scroll);
                }
            }
        }

        // Reaper's Toll — chance to release a collectible soul that heals on pickup.
        int reaper = storage.level(weapon, "reapers_toll");
        if (reaper > 0) {
            CustomEnchant e = service.getRegistry().get("reapers_toll");
            if (Math.random() < e.levelDouble(reaper, "chance", 0.0) && dead.getWorld() != null) {
                dead.getWorld().dropItemNaturally(dead.getLocation().add(0, 0.5, 0), createSoul(reaper));
                Particles.at(dead.getEyeLocation(), SOUL_PARTICLE, 12, 0.3);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!service.isEnabled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        int level = soulLevel(stack);
        if (level <= 0) {
            return;
        }
        event.setCancelled(true);
        try {
            event.getItem().remove();
        } catch (Throwable ignored) {
            // item entity may already be gone
        }
        Player player = (Player) event.getEntity();
        CustomEnchant e = service.getRegistry().get("reapers_toll");
        if (e != null) {
            int hearts = e.levelInt(level, "heal_hearts", 1);
            int duration = e.levelInt(level, "duration_ticks", 40);
            Potions.applyLevel(player, Potions.REGENERATION, duration, Math.max(1, hearts));
            if (e.levelBoolean(level, "absorption", false)) {
                Potions.applyLevel(player, Potions.ABSORPTION, 200, 1);
            }
        }
        Particles.at(player.getEyeLocation(), SOUL_PARTICLE, 10, 0.3);
        SoundPalette.play(player, SOUL_PICKUP_SOUND, CombatSupport.volume(service, 0.6f), 1.4f);
    }

    // ── Plunderer helpers ────────────────────────────────────────────────────

    private void multiplyDrops(EntityDeathEvent event, double bonus) {
        if (bonus <= 0.0) {
            return;
        }
        int guaranteed = (int) Math.floor(bonus);
        double remainder = bonus - guaranteed;
        List<ItemStack> extra = new ArrayList<ItemStack>();
        for (ItemStack drop : event.getDrops()) {
            if (drop == null || drop.getType() == Material.AIR) {
                continue;
            }
            for (int i = 0; i < guaranteed; i++) {
                extra.add(drop.clone());
            }
            if (remainder > 0 && Math.random() < remainder) {
                extra.add(drop.clone());
            }
        }
        event.getDrops().addAll(extra);
    }

    private void duplicateRandomDrop(EntityDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        if (drops.isEmpty()) {
            return;
        }
        ItemStack pick = drops.get((int) (Math.random() * drops.size()));
        if (pick != null && pick.getType() != Material.AIR) {
            drops.add(pick.clone());
        }
    }

    private ItemStack randomScroll() {
        EnchantItems items = plugin.getEnchantItems();
        if (items == null) {
            return null;
        }
        List<CustomEnchant> all = service.getRegistry().all();
        if (all.isEmpty()) {
            return null;
        }
        CustomEnchant enchant = all.get((int) (Math.random() * all.size()));
        int level = 1 + (int) (Math.random() * enchant.getMaxLevel());
        try {
            return items.scroll(enchant, level, 1);
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ── Reaper's Toll soul item ────────────────────────────────────────────────

    private ItemStack createSoul(int level) {
        Material material = Material.matchMaterial("GHAST_TEAR");
        if (material == null) {
            material = Material.matchMaterial("GLOWSTONE_DUST");
        }
        ItemStack soul = new ItemStack(material == null ? Material.matchMaterial("EXPERIENCE_BOTTLE") : material);
        ItemMeta meta = soul.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "✦ Lost Soul");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Collect it to recover health.",
                    ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + SOUL_MARKER + level));
            dev.zcripted.obx.feature.enchant.util.Glow.apply(meta);
            soul.setItemMeta(meta);
        }
        return soul;
    }

    /** Reaper's Toll level encoded in a soul item's marker lore, or 0 if it isn't a soul. */
    private int soulLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return 0;
        }
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line).trim();
            if (stripped.startsWith(SOUL_MARKER)) {
                try {
                    return Integer.parseInt(stripped.substring(SOUL_MARKER.length()).trim());
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private ItemStack headFor(LivingEntity dead) {
        if (dead instanceof Player) {
            Material playerHead = Material.matchMaterial("PLAYER_HEAD");
            if (playerHead != null) {
                ItemStack head = new ItemStack(playerHead);
                applyOwner(head, (Player) dead);
                return head;
            }
            Material legacy = Material.matchMaterial("SKULL_ITEM");
            if (legacy != null) {
                ItemStack head = new ItemStack(legacy, 1, (short) 3);
                applyOwner(head, (Player) dead);
                return head;
            }
            return null;
        }
        String materialName = mobHeadMaterial(dead.getType().name());
        if (materialName == null) {
            return null;
        }
        Material material = Material.matchMaterial(materialName);
        return material == null ? null : new ItemStack(material);
    }

    private void applyOwner(ItemStack head, Player owner) {
        try {
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta == null) {
                return;
            }
            try {
                SkullMeta.class.getMethod("setOwningPlayer", OfflinePlayer.class).invoke(meta, owner);
            } catch (Throwable legacy) {
                meta.setOwner(owner.getName());
            }
            head.setItemMeta(meta);
        } catch (Throwable ignored) {
            // a head without a skin is still a valid drop
        }
    }

    private String mobHeadMaterial(String type) {
        switch (type) {
            case "ZOMBIE":
                return "ZOMBIE_HEAD";
            case "SKELETON":
                return "SKELETON_SKULL";
            case "WITHER_SKELETON":
                return "WITHER_SKELETON_SKULL";
            case "CREEPER":
                return "CREEPER_HEAD";
            case "ENDER_DRAGON":
                return "DRAGON_HEAD";
            case "PIGLIN":
                return "PIGLIN_HEAD";
            default:
                return null;
        }
    }
}
