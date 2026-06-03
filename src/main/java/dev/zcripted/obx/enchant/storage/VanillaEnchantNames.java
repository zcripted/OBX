package dev.zcripted.obx.enchant.storage;

import org.bukkit.enchantments.Enchantment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cross-version friendly display names for vanilla {@link Enchantment}s, used by
 * the styled "Vanilla" lore section.
 *
 * <p>The plugin compiles against the 1.12.2 API (no {@code Enchantment#getKey}),
 * so the namespaced key is read <strong>reflectively</strong> when present (1.13+)
 * and prettified ({@code fire_protection → Fire Protection}). On 1.8–1.12 the
 * reflective key is absent, so the deprecated {@link Enchantment#getName()} NMS
 * name is mapped through {@link #LEGACY_NAMES} (e.g. {@code DAMAGE_ALL → Sharpness}).
 */
public final class VanillaEnchantNames {

    private VanillaEnchantNames() {
    }

    /** Small connector words kept lowercase mid-name (e.g. "Luck of the Sea"). */
    private static final Set<String> SMALL_WORDS = new HashSet<String>(Arrays.asList("of", "the", "and"));

    /** Namespaced-key overrides where a literal prettify isn't the in-game wording. */
    private static final Map<String, String> KEY_OVERRIDES = new HashMap<String, String>();

    /** Legacy NMS {@code getName()} values → friendly names, for 1.8–1.12 runtimes. */
    private static final Map<String, String> LEGACY_NAMES = new HashMap<String, String>();

    static {
        KEY_OVERRIDES.put("binding_curse", "Curse of Binding");
        KEY_OVERRIDES.put("vanishing_curse", "Curse of Vanishing");
        KEY_OVERRIDES.put("sweeping", "Sweeping Edge");
        KEY_OVERRIDES.put("sweeping_edge", "Sweeping Edge");

        LEGACY_NAMES.put("PROTECTION_ENVIRONMENTAL", "Protection");
        LEGACY_NAMES.put("PROTECTION_FIRE", "Fire Protection");
        LEGACY_NAMES.put("PROTECTION_FALL", "Feather Falling");
        LEGACY_NAMES.put("PROTECTION_EXPLOSIONS", "Blast Protection");
        LEGACY_NAMES.put("PROTECTION_PROJECTILE", "Projectile Protection");
        LEGACY_NAMES.put("OXYGEN", "Respiration");
        LEGACY_NAMES.put("WATER_WORKER", "Aqua Affinity");
        LEGACY_NAMES.put("THORNS", "Thorns");
        LEGACY_NAMES.put("DEPTH_STRIDER", "Depth Strider");
        LEGACY_NAMES.put("FROST_WALKER", "Frost Walker");
        LEGACY_NAMES.put("BINDING_CURSE", "Curse of Binding");
        LEGACY_NAMES.put("DAMAGE_ALL", "Sharpness");
        LEGACY_NAMES.put("DAMAGE_UNDEAD", "Smite");
        LEGACY_NAMES.put("DAMAGE_ARTHROPODS", "Bane of Arthropods");
        LEGACY_NAMES.put("KNOCKBACK", "Knockback");
        LEGACY_NAMES.put("FIRE_ASPECT", "Fire Aspect");
        LEGACY_NAMES.put("LOOT_BONUS_MOBS", "Looting");
        LEGACY_NAMES.put("SWEEPING_EDGE", "Sweeping Edge");
        LEGACY_NAMES.put("DIG_SPEED", "Efficiency");
        LEGACY_NAMES.put("SILK_TOUCH", "Silk Touch");
        LEGACY_NAMES.put("DURABILITY", "Unbreaking");
        LEGACY_NAMES.put("LOOT_BONUS_BLOCKS", "Fortune");
        LEGACY_NAMES.put("ARROW_DAMAGE", "Power");
        LEGACY_NAMES.put("ARROW_KNOCKBACK", "Punch");
        LEGACY_NAMES.put("ARROW_FIRE", "Flame");
        LEGACY_NAMES.put("ARROW_INFINITE", "Infinity");
        LEGACY_NAMES.put("LUCK", "Luck of the Sea");
        LEGACY_NAMES.put("LURE", "Lure");
        LEGACY_NAMES.put("MENDING", "Mending");
        LEGACY_NAMES.put("VANISHING_CURSE", "Curse of Vanishing");
    }

    /** Friendly, title-cased display name for a vanilla enchantment. */
    public static String friendly(Enchantment enchantment) {
        if (enchantment == null) {
            return "Enchantment";
        }
        String key = keyName(enchantment);
        if (key != null && !key.isEmpty()) {
            String override = KEY_OVERRIDES.get(key.toLowerCase(Locale.ENGLISH));
            return override != null ? override : prettify(key);
        }
        try {
            String legacy = enchantment.getName();
            if (legacy != null && !legacy.isEmpty()) {
                String mapped = LEGACY_NAMES.get(legacy.toUpperCase(Locale.ENGLISH));
                return mapped != null ? mapped : prettify(legacy);
            }
        } catch (Throwable ignored) {
            // getName() is deprecated/removed on some forks — fall through
        }
        return "Enchantment";
    }

    /** Reads the namespaced key path reflectively (1.13+), or {@code null} when absent. */
    private static String keyName(Enchantment enchantment) {
        try {
            Object namespacedKey = Enchantment.class.getMethod("getKey").invoke(enchantment);
            if (namespacedKey == null) {
                return null;
            }
            Object path = namespacedKey.getClass().getMethod("getKey").invoke(namespacedKey);
            return path == null ? null : path.toString();
        } catch (Throwable notSupported) {
            return null;
        }
    }

    /** {@code fire_protection} / {@code FIRE_PROTECTION} → {@code Fire Protection}. */
    private static String prettify(String raw) {
        String[] parts = raw.toLowerCase(Locale.ENGLISH).split("[_\\s]+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            if (i > 0 && SMALL_WORDS.contains(part)) {
                builder.append(part);
            } else {
                builder.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    builder.append(part.substring(1));
                }
            }
        }
        return builder.length() == 0 ? "Enchantment" : builder.toString();
    }
}
