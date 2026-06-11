package dev.zcripted.obx.feature.enchant.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Resolves {@link PotionEffectType}s by trying candidate names. Several effect
 * types were renamed across versions (e.g. {@code SLOW} → {@code SLOWNESS},
 * {@code SLOW_DIGGING} → {@code MINING_FATIGUE}, {@code DAMAGE_RESISTANCE} →
 * {@code RESISTANCE}, {@code INCREASE_DAMAGE} → {@code STRENGTH}); looking them
 * up by name keeps the single jar correct on 1.8.8 → 1.21.x.
 */
public final class Potions {

    private Potions() {
    }

    public static final String[] SLOWNESS = {"SLOWNESS", "SLOW"};
    public static final String[] MINING_FATIGUE = {"MINING_FATIGUE", "SLOW_DIGGING"};
    public static final String[] SPEED = {"SPEED"};
    public static final String[] STRENGTH = {"STRENGTH", "INCREASE_DAMAGE"};
    public static final String[] HUNGER = {"HUNGER"};
    public static final String[] REGENERATION = {"REGENERATION", "REGEN"};
    public static final String[] RESISTANCE = {"RESISTANCE", "DAMAGE_RESISTANCE"};
    public static final String[] ABSORPTION = {"ABSORPTION"};
    public static final String[] INVISIBILITY = {"INVISIBILITY"};
    public static final String[] NIGHT_VISION = {"NIGHT_VISION"};
    public static final String[] WATER_BREATHING = {"WATER_BREATHING"};
    public static final String[] WEAKNESS = {"WEAKNESS"};
    public static final String[] NAUSEA = {"NAUSEA", "CONFUSION"};
    public static final String[] BLINDNESS = {"BLINDNESS"};

    public static PotionEffectType type(String[] candidates) {
        for (String name : candidates) {
            PotionEffectType type = PotionEffectType.getByName(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    /**
     * Applies an effect. {@code amplifier} is zero-based (Slowness I = 0). No-op
     * if the effect type is unavailable on the running version.
     */
    public static void apply(LivingEntity entity, String[] candidates, int durationTicks, int amplifier) {
        if (entity == null) {
            return;
        }
        PotionEffectType type = type(candidates);
        if (type == null) {
            return;
        }
        try {
            entity.addPotionEffect(new PotionEffect(type, Math.max(1, durationTicks), Math.max(0, amplifier), true, false));
        } catch (Throwable ignored) {
            // never let an effect break gameplay
        }
    }

    /** Applies an effect using a displayed level (Slowness I → amplifier 0). */
    public static void applyLevel(LivingEntity entity, String[] candidates, int durationTicks, int displayLevel) {
        apply(entity, candidates, durationTicks, Math.max(0, displayLevel - 1));
    }
}