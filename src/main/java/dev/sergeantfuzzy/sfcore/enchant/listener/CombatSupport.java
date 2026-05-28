package dev.sergeantfuzzy.sfcore.enchant.listener;

import dev.sergeantfuzzy.sfcore.enchant.model.CustomEnchant;
import dev.sergeantfuzzy.sfcore.enchant.model.EnchantCategory;
import dev.sergeantfuzzy.sfcore.enchant.service.EnchantService;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stateless helpers shared by the split combat listeners (attacker resolution,
 * main-hand access, health math, nearby-hostile counting, crit approximation,
 * and config-gated action-bar/sound output). Kept separate so each listener
 * stays focused on its own enchant family.
 */
public final class CombatSupport {

    private CombatSupport() {
    }

    /** The player behind a damage source — direct melee or a projectile they shot. */
    public static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            ProjectileSource source = ((Projectile) damager).getShooter();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        return null;
    }

    /** True if {@code item} carries at least one Combat-category Arcanum enchant. */
    public static boolean hasCombatEnchant(EnchantService service, ItemStack item) {
        if (service == null || item == null) {
            return false;
        }
        for (String id : service.getStorage().read(item).keySet()) {
            CustomEnchant enchant = service.getRegistry().get(id);
            if (enchant != null && enchant.getCategory() == EnchantCategory.COMBAT) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack mainHand(Player player) {
        try {
            return player.getInventory().getItemInMainHand();
        } catch (Throwable modern) {
            try {
                return player.getInventory().getItem(player.getInventory().getHeldItemSlot());
            } catch (Throwable legacy) {
                return null;
            }
        }
    }

    public static double healthFraction(LivingEntity entity) {
        try {
            double max = entity.getMaxHealth();
            return max <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, entity.getHealth() / max));
        } catch (Throwable ignored) {
            return 1.0;
        }
    }

    public static void heal(LivingEntity entity, double amount) {
        if (entity == null || amount <= 0.0) {
            return;
        }
        try {
            double max = entity.getMaxHealth();
            entity.setHealth(Math.min(max, entity.getHealth() + amount));
        } catch (Throwable ignored) {
            // setHealth can throw if amount math overshoots on odd forks
        }
    }

    public static int countNearbyHostiles(Player player, double radius) {
        int count = 0;
        try {
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof Monster) {
                    count++;
                }
            }
        } catch (Throwable ignored) {
            // getNearbyEntities is safe on all supported versions; guard anyway
        }
        return count;
    }

    /** Approximates vanilla critical-hit conditions (airborne descent, not sprinting). */
    public static boolean isVanillaCrit(Player player) {
        try {
            return player.getFallDistance() > 0.0f && !player.isOnGround()
                    && !player.isInsideVehicle() && !player.isSprinting();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Sends an action-bar line (translating {@code &} codes) when feedback is enabled. */
    public static void actionBar(EnchantService service, Player player, String message) {
        if (service != null && service.getCombatSettings() != null && !service.getCombatSettings().actionBarFeedback()) {
            return;
        }
        if (player == null || message == null) {
            return;
        }
        // Honor the player's personal opt-out from /enchants settings.
        if (service != null && service.getCombatPrefs() != null && !service.getCombatPrefs().fxEnabled(player.getUniqueId())) {
            return;
        }
        ComponentMessenger.sendActionBar(player, ChatColor.translateAlternateColorCodes('&', message));
    }

    /** Sends a combat title/subtitle (translating {@code &} codes), honoring the player's FX opt-out. */
    public static void title(EnchantService service, Player player, String title, String subtitle) {
        if (player == null) {
            return;
        }
        if (service != null && service.getCombatPrefs() != null && !service.getCombatPrefs().fxEnabled(player.getUniqueId())) {
            return;
        }
        String top = title == null ? "" : ChatColor.translateAlternateColorCodes('&', title);
        String bottom = subtitle == null ? "" : ChatColor.translateAlternateColorCodes('&', subtitle);
        try {
            player.sendTitle(top, bottom, 5, 50, 10);
        } catch (Throwable legacy) {
            try {
                player.sendTitle(top, bottom);
            } catch (Throwable ignored) {
                // titles unsupported on this fork
            }
        }
    }

    /** Scales a base sound volume by the configured combat multiplier. */
    public static float volume(EnchantService service, float base) {
        float multiplier = (service != null && service.getCombatSettings() != null)
                ? service.getCombatSettings().soundVolume() : 1.0f;
        return base * multiplier;
    }

    /** Sets entity freeze ticks (powder-snow shiver) on 1.17+; no-op on older. */
    public static void setFreezeTicks(Entity entity, int ticks) {
        if (entity == null || ticks <= 0) {
            return;
        }
        try {
            entity.getClass().getMethod("setFreezeTicks", int.class).invoke(entity, ticks);
        } catch (Throwable ignored) {
            // 1.16 and older have no freeze mechanic
        }
    }

    private static final java.util.Set<String> NETHER_FOES = new java.util.HashSet<String>(java.util.Arrays.asList(
            "BLAZE", "WITHER_SKELETON", "GHAST", "MAGMA_CUBE", "PIGLIN", "PIGLIN_BRUTE", "HOGLIN", "ZOGLIN",
            "ZOMBIFIED_PIGLIN", "PIG_ZOMBIE", "STRIDER", "WITHER"));

    /** Heuristic undead check (name-based, so it works without the 1.16 EntityCategory API). */
    public static boolean isUndead(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        String name = entity.getType().name().toUpperCase(Locale.ENGLISH);
        return name.contains("ZOMBIE") || name.contains("SKELETON") || name.contains("WITHER")
                || name.equals("DROWNED") || name.equals("HUSK") || name.equals("STRAY")
                || name.equals("PHANTOM") || name.equals("ZOGLIN") || name.equals("ZOMBIFIED_PIGLIN");
    }

    /** True for mobs that originate in the Nether (so "non-Nether" bonuses can skip them). */
    public static boolean isNetherFoe(LivingEntity entity) {
        return entity != null && NETHER_FOES.contains(entity.getType().name().toUpperCase(Locale.ENGLISH));
    }

    /** Compact number for feedback (7.0 → "7", 4.5 → "4.5"). */
    public static String format(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    /** Damages one random worn armor piece by {@code amount} durability (Glasscutter shatter). */
    public static void shatterRandomArmor(LivingEntity victim, int amount) {
        try {
            EntityEquipment equipment = victim.getEquipment();
            if (equipment == null) {
                return;
            }
            ItemStack[] armor = equipment.getArmorContents();
            List<Integer> worn = new ArrayList<Integer>();
            for (int i = 0; i < armor.length; i++) {
                ItemStack piece = armor[i];
                if (piece != null && piece.getType() != Material.AIR && piece.getType().getMaxDurability() > 0) {
                    worn.add(i);
                }
            }
            if (worn.isEmpty()) {
                return;
            }
            int index = worn.get((int) (Math.random() * worn.size()));
            ItemStack piece = armor[index];
            short max = piece.getType().getMaxDurability();
            piece.setDurability((short) Math.min(max, piece.getDurability() + amount));
            armor[index] = piece;
            equipment.setArmorContents(armor);
        } catch (Throwable ignored) {
            // best effort — never break combat over durability
        }
    }

    /** Displayed main-hand attack damage of a weapon (base + ADD_NUMBER modifiers), or 0. */
    public static double attackDamage(ItemStack weapon) {
        if (weapon == null) {
            return 0.0;
        }
        try {
            double[] stats = dev.sergeantfuzzy.sfcore.enchant.storage.WeaponAttributes.mainHand(weapon.getType(), weapon.getItemMeta());
            return stats == null ? 0.0 : stats[0];
        } catch (Throwable ignored) {
            return 0.0;
        }
    }
}
