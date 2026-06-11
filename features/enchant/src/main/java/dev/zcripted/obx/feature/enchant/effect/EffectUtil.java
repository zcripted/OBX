package dev.zcripted.obx.feature.enchant.effect;

import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Small shared helpers used by the Arcanum effect listeners — reading enchant
 * levels off armor / the held item, healing, version-tolerant health and
 * main-hand access, and Curse of the Bound's armor-toughness attribute.
 */
public final class EffectUtil {

    /** Stable id for the Curse of the Bound armor-toughness modifier. */
    private static final UUID BOUND_TOUGHNESS_ID = UUID.fromString("5f43d000-b0c0-4d00-8a00-5f636f726562");
    private static volatile Attribute toughnessAttribute;
    private static volatile boolean toughnessResolved;

    private EffectUtil() {
    }

    /**
     * Applies (amount &gt; 0) or clears (amount == 0) the real
     * {@code GENERIC_ARMOR_TOUGHNESS} attribute for Curse of the Bound. Idempotent
     * — always removes our previous modifier first, so it's safe to call every
     * tick. No-ops on versions without the attribute (e.g. 1.8).
     */
    public static void setBoundToughness(Player player, double amount) {
        Attribute attribute = toughnessAttribute();
        if (attribute == null) {
            return;
        }
        AttributeInstance instance;
        try {
            instance = player.getAttribute(attribute);
        } catch (Throwable ignored) {
            return;
        }
        if (instance == null) {
            return;
        }
        for (AttributeModifier modifier : new ArrayList<AttributeModifier>(instance.getModifiers())) {
            if (BOUND_TOUGHNESS_ID.equals(modifier.getUniqueId())) {
                try {
                    instance.removeModifier(modifier);
                } catch (Throwable ignored) {
                }
            }
        }
        if (amount > 0) {
            try {
                instance.addModifier(new AttributeModifier(BOUND_TOUGHNESS_ID, "obx.bound.toughness",
                        amount, AttributeModifier.Operation.ADD_NUMBER));
            } catch (Throwable ignored) {
            }
        }
    }

    private static Attribute toughnessAttribute() {
        if (toughnessResolved) {
            return toughnessAttribute;
        }
        for (String name : new String[]{"GENERIC_ARMOR_TOUGHNESS", "ARMOR_TOUGHNESS"}) {
            try {
                toughnessAttribute = Attribute.valueOf(name);
                break;
            } catch (IllegalArgumentException ignored) {
                // try next name
            }
        }
        toughnessResolved = true;
        return toughnessAttribute;
    }

    /** Highest level of {@code enchantId} found across the player's worn armor. */
    public static int armorLevel(EnchantStorage storage, Player player, String enchantId) {
        int best = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            int level = storage.level(piece, enchantId);
            if (level > best) {
                best = level;
            }
        }
        return best;
    }

    public static int heldLevel(EnchantStorage storage, Player player, String enchantId) {
        return storage.level(mainHand(player), enchantId);
    }

    public static ItemStack armorPieceWith(EnchantStorage storage, Player player, String enchantId) {
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (storage.level(piece, enchantId) > 0) {
                return piece;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public static double maxHealth(LivingEntity entity) {
        return entity.getMaxHealth();
    }

    public static double healthFraction(LivingEntity entity) {
        double max = maxHealth(entity);
        return max <= 0 ? 1.0 : entity.getHealth() / max;
    }

    @SuppressWarnings("deprecation")
    public static void heal(Player player, double amount) {
        if (amount <= 0) {
            return;
        }
        double max = maxHealth(player);
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }

    @SuppressWarnings("deprecation")
    public static ItemStack mainHand(Player player) {
        try {
            return player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError legacy) {
            return player.getInventory().getItemInHand();
        }
    }
}