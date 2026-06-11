package dev.zcripted.obx.feature.enchant.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cross-version particle helper. The {@link Particle} enum exists from 1.9, but
 * several constants were renamed over time ({@code ENCHANTMENT_TABLE} →
 * {@code ENCHANT}, {@code VILLAGER_HAPPY} → {@code HAPPY_VILLAGER}, …) and the
 * {@code spawnParticle} method itself is absent on 1.8. Constants are resolved by
 * name with fallbacks and every spawn is guarded, so particle polish simply
 * no-ops on servers that can't render it.
 */
public final class Particles {

    private Particles() {
    }

    public static final String[] CHILL = {"SNOWFLAKE", "ITEM_SNOWBALL", "SNOWBALL", "CLOUD"};
    public static final String[] CRIT = {"CRIT", "CRIT_MAGIC"};
    public static final String[] SPARK = {"ELECTRIC_SPARK", "CRIT", "FIREWORKS_SPARK"};
    public static final String[] SOUL_FIRE = {"SOUL_FIRE_FLAME", "FLAME"};
    public static final String[] PORTAL = {"PORTAL", "REVERSE_PORTAL"};
    public static final String[] MAGIC = {"ENCHANT", "ENCHANTMENT_TABLE"};
    public static final String[] HAPPY = {"HAPPY_VILLAGER", "VILLAGER_HAPPY"};
    public static final String[] TOTEM = {"TOTEM", "TOTEM_OF_UNDYING", "FLAME"};
    public static final String[] FLAME = {"FLAME"};
    public static final String[] SMOKE = {"SMOKE_NORMAL", "SMOKE", "CAMPFIRE_COSY_SMOKE"};
    public static final String[] CLOUD = {"CLOUD"};
    public static final String[] HEART = {"HEART"};

    private static final ConcurrentHashMap<String, Particle> CACHE = new ConcurrentHashMap<String, Particle>();
    private static final Particle MISSING = pickAny();

    private static Particle pickAny() {
        Particle[] values = Particle.values();
        return values.length > 0 ? values[0] : null;
    }

    private static Particle resolve(String[] names) {
        String key = names.length == 0 ? "" : names[0];
        Particle cached = CACHE.get(key);
        if (cached != null) {
            return cached == MISSING ? null : cached;
        }
        for (String name : names) {
            try {
                Particle particle = Particle.valueOf(name);
                CACHE.put(key, particle);
                return particle;
            } catch (IllegalArgumentException ignored) {
                // try next candidate
            }
        }
        if (MISSING != null) {
            CACHE.put(key, MISSING);
        }
        return null;
    }

    public static void at(Location location, String[] candidates, int count, double spread) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        Particle particle = resolve(candidates);
        if (particle == null) {
            return;
        }
        try {
            location.getWorld().spawnParticle(particle, location, count, spread, spread, spread, 0.0);
        } catch (Throwable ignored) {
            // spawnParticle is unavailable on 1.8 — particles are purely cosmetic, so ignore.
        }
    }

    public static void burst(Location location, String[] candidates) {
        at(location, candidates, 12, 0.4);
    }

    /**
     * Spawns particles visible to a single {@code viewer} only (the player-scoped
     * {@code Player#spawnParticle}, 1.9+). Used for the Bloodletter gravestone, which
     * marks a victim's death spot for that victim alone.
     */
    public static void atPlayer(Player viewer, Location location, String[] candidates, int count, double spread) {
        if (viewer == null || location == null || location.getWorld() == null) {
            return;
        }
        Particle particle = resolve(candidates);
        if (particle == null) {
            return;
        }
        try {
            viewer.spawnParticle(particle, location, count, spread, spread, spread, 0.0);
        } catch (Throwable ignored) {
            // spawnParticle(Player overload) unavailable (1.8) — cosmetic, ignore.
        }
    }

    /**
     * Spawns block-fragment particles tinted by a block material — used for the
     * blood effect ({@code REDSTONE_BLOCK} → red shards that fall like blood). The
     * particle enum was renamed ({@code BLOCK_CRACK} → {@code BLOCK}) and the data
     * type differs by version ({@code BlockData} 1.13+ vs {@code MaterialData}),
     * so both the constant and the data are resolved tolerantly. Falls back to an
     * untinted spawn (then no-op) if the typed overload isn't available.
     */
    public static void block(Location location, String materialName, int count, double spread) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        Particle particle = resolve(new String[]{"BLOCK_CRACK", "BLOCK", "BLOCK_DUST"});
        if (particle == null) {
            return;
        }
        Material material = Material.matchMaterial(materialName);
        Object data = material == null ? null : blockData(material);
        if (data != null) {
            try {
                location.getWorld().spawnParticle(particle, location, count, spread, spread, spread, 0.0, data);
                return;
            } catch (Throwable wrongDataType) {
                // data type mismatch on this fork — fall through to an untinted spawn
            }
        }
        try {
            location.getWorld().spawnParticle(particle, location, count, spread, spread, spread, 0.0);
        } catch (Throwable ignored) {
            // spawnParticle unavailable (1.8) — purely cosmetic, ignore
        }
    }

    /** {@code Material.createBlockData()} (1.13+) or a legacy {@code MaterialData} (1.12-), or null. */
    private static Object blockData(Material material) {
        try {
            Method createBlockData = Material.class.getMethod("createBlockData");
            return createBlockData.invoke(material);
        } catch (Throwable notModern) {
            // pre-1.13 — try the legacy MaterialData expected by BLOCK_CRACK there
        }
        try {
            return new org.bukkit.material.MaterialData(material);
        } catch (Throwable ignored) {
            return null;
        }
    }
}