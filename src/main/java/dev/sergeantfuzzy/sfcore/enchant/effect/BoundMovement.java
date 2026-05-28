package dev.sergeantfuzzy.sfcore.enchant.effect;

import dev.sergeantfuzzy.sfcore.enchant.storage.EnchantStorage;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces Curse of the Bound's "cannot sprint" downside.
 *
 * <p>On modern Minecraft (e.g. 1.21) sprinting is client-authoritative:
 * cancelling {@code PlayerToggleSprintEvent} and {@code setSprinting(false)} do
 * not stop the client, and in creative the hunger gate is bypassed by fly
 * ability. The one lever the client always honors is the abilities walk/fly speed
 * (sent in the Player Abilities packet). So while a bound player is sprinting we
 * scale their walk and fly speed down so sprinting yields no speed advantage, and
 * restore their original values the moment they stop. A non-sprinting bound player
 * keeps normal walking speed — only the sprint benefit is removed.
 *
 * <p>Original speeds are captured per player and restored exactly, so other
 * plugins' speed values aren't clobbered. State is cleared on stop/quit/disable.
 */
public final class BoundMovement {

    // Counter the sprint speed multipliers: walk-sprint ≈ ×1.3, creative fly-sprint ≈ ×2.
    private static final float WALK_FACTOR = 0.77f;
    private static final float FLY_FACTOR = 0.5f;

    private final EnchantStorage storage;
    /** uuid → [originalWalkSpeed, originalFlySpeed] captured when we first throttle. */
    private final Map<UUID, float[]> saved = new ConcurrentHashMap<UUID, float[]>();

    public BoundMovement(EnchantStorage storage) {
        this.storage = storage;
    }

    /** Called from the sprint-toggle event with the NEW sprint state. */
    public void update(Player player, boolean sprinting) {
        boolean bound = storage.level(player.getInventory().getBoots(), "curse_of_the_bound") > 0;
        if (bound && sprinting) {
            throttle(player);
        } else {
            restore(player);
        }
    }

    /** Called from the tick task as a safety net (fly-state change, unequip, etc.). */
    public void enforce(Player player) {
        update(player, player.isSprinting());
    }

    private void throttle(Player player) {
        UUID id = player.getUniqueId();
        float[] original = saved.get(id);
        if (original == null) {
            original = new float[]{player.getWalkSpeed(), player.getFlySpeed()};
            saved.put(id, original);
        }
        applyIfChanged(player, clamp(original[0] * WALK_FACTOR), clamp(original[1] * FLY_FACTOR));
    }

    /** Restores the player's captured speeds (no-op if we never throttled them). */
    public void restore(Player player) {
        float[] original = saved.remove(player.getUniqueId());
        if (original == null) {
            return;
        }
        applyIfChanged(player, clamp(original[0]), clamp(original[1]));
    }

    private void applyIfChanged(Player player, float walk, float fly) {
        try {
            if (player.getWalkSpeed() != walk) {
                player.setWalkSpeed(walk);
            }
            if (player.getFlySpeed() != fly) {
                player.setFlySpeed(fly);
            }
        } catch (Throwable ignored) {
            // setWalkSpeed/setFlySpeed only reject out-of-range values, which clamp prevents.
        }
    }

    private static float clamp(float value) {
        if (value < -1f) {
            return -1f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }
}
