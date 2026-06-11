package dev.zcripted.obx.feature.enchant.storage;

import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Computes the main-hand <strong>Attack Damage</strong> and <strong>Attack
 * Speed</strong> a client would display for an item, so OBX can render those
 * vanilla attributes in its own clean style (and hide the default gray lines).
 *
 * <p>The math mirrors vanilla: the displayed value is the player's base
 * ({@code 1.0} damage, {@code 4.0} speed) plus the item's {@code ADD_NUMBER}
 * attribute modifiers for the hand slot. The source of those modifiers is, in
 * order: the item's <em>explicit</em> modifiers if it has any (they fully replace
 * the material defaults, exactly as vanilla treats them), otherwise the
 * material's default modifiers.
 *
 * <p>Everything is reflective: {@code ItemMeta#getAttributeModifiers(EquipmentSlot)}
 * is 1.13+/1.14+ and {@code Material#getDefaultAttributeModifiers(EquipmentSlot)}
 * is 1.16+, neither exists in the 1.12.2 compile target. Attribute identity is
 * matched by key name ({@code …attack_damage} / {@code …attack_speed}) rather than
 * a hard {@code Attribute} constant, since those constants were reorganized in
 * 1.21. When nothing can be resolved (older server, or the item isn't a weapon)
 * {@link #mainHand} returns {@code null} and the caller leaves the vanilla display
 * untouched — better than showing a wrong number.
 */
public final class WeaponAttributes {

    public static final double BASE_DAMAGE = 1.0;
    public static final double BASE_SPEED = 4.0;

    private WeaponAttributes() {
    }

    /**
     * @return {@code [attackDamage, attackSpeed]} as the client would show them,
     *         or {@code null} if the item has no attack-damage attribute (not a
     *         weapon) or the modifiers can't be read on this server.
     */
    public static double[] mainHand(Material material, ItemMeta meta) {
        if (material == null) {
            return null;
        }
        Object source = explicitModifiers(meta);
        if (source == null || multimapIsEmpty(source)) {
            source = defaultModifiers(material);
        }
        if (source == null) {
            return null;
        }
        Double damageSum = null;
        Double speedSum = null;
        try {
            Object asMap = source.getClass().getMethod("asMap").invoke(source);
            if (!(asMap instanceof Map)) {
                return null;
            }
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) asMap).entrySet()) {
                String id = attributeId(entry.getKey());
                boolean isDamage = id.contains("attack_damage");
                boolean isSpeed = id.contains("attack_speed");
                if (!isDamage && !isSpeed) {
                    continue;
                }
                double sum = sumAddNumber(entry.getValue());
                if (isDamage) {
                    damageSum = sum;
                } else {
                    speedSum = sum;
                }
            }
        } catch (Throwable unreadable) {
            return null;
        }
        if (damageSum == null) {
            return null; // no attack damage → not a weapon we describe
        }
        double speed = speedSum == null ? BASE_SPEED : BASE_SPEED + speedSum;
        return new double[]{BASE_DAMAGE + damageSum, speed};
    }

    private static Object explicitModifiers(ItemMeta meta) {
        if (meta == null) {
            return null;
        }
        try {
            Method method = meta.getClass().getMethod("getAttributeModifiers", EquipmentSlot.class);
            method.setAccessible(true);
            return method.invoke(meta, EquipmentSlot.HAND);
        } catch (Throwable notSupported) {
            return null;
        }
    }

    private static Object defaultModifiers(Material material) {
        try {
            Method method = Material.class.getMethod("getDefaultAttributeModifiers", EquipmentSlot.class);
            method.setAccessible(true);
            return method.invoke(material, EquipmentSlot.HAND);
        } catch (Throwable notSupported) {
            return null;
        }
    }

    private static boolean multimapIsEmpty(Object multimap) {
        try {
            Object result = multimap.getClass().getMethod("isEmpty").invoke(multimap);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable ignored) {
            return true;
        }
    }

    /** Sums the {@code ADD_NUMBER} modifier amounts in a collection of AttributeModifiers. */
    private static double sumAddNumber(Object collection) {
        if (!(collection instanceof Collection)) {
            return 0.0;
        }
        double sum = 0.0;
        for (Object element : (Collection<?>) collection) {
            if (!(element instanceof AttributeModifier)) {
                continue;
            }
            AttributeModifier modifier = (AttributeModifier) element;
            if (isAddNumber(modifier)) {
                sum += modifier.getAmount();
            }
        }
        return sum;
    }

    private static boolean isAddNumber(AttributeModifier modifier) {
        try {
            // Compare by name so a future Operation rename can't throw; the additive
            // base operation is ADD_NUMBER (some mappings expose ADD_VALUE).
            String operation = modifier.getOperation().name();
            return "ADD_NUMBER".equals(operation) || "ADD_VALUE".equals(operation);
        } catch (Throwable ignored) {
            return true; // assume additive if the operation can't be read
        }
    }

    /** Lowercased identity of an Attribute key, across the enum (≤1.20.6) and registry (1.21+) forms. */
    private static String attributeId(Object attribute) {
        if (attribute == null) {
            return "";
        }
        // Namespaced key path (works on both forms via getKey().getKey()).
        try {
            Object key = attribute.getClass().getMethod("getKey").invoke(attribute);
            if (key != null) {
                Object path = key.getClass().getMethod("getKey").invoke(key);
                if (path != null) {
                    return path.toString().toLowerCase(java.util.Locale.ENGLISH);
                }
            }
        } catch (Throwable ignored) {
            // not a Keyed attribute — fall through
        }
        if (attribute instanceof Enum) {
            return ((Enum<?>) attribute).name().toLowerCase(java.util.Locale.ENGLISH);
        }
        return String.valueOf(attribute).toLowerCase(java.util.Locale.ENGLISH);
    }
}