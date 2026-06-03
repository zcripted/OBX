package dev.zcripted.obx.enchant.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Named combat sound palette. Like {@link Sounds}, each entry is a list of
 * candidate enum names tried with {@link Sound#valueOf} so a single jar resolves
 * the right constant across 1.8.8 → 1.21.x (sound names were reorganized several
 * times). Listeners reference {@code SoundPalette.CRIT_GOLD} rather than raw
 * strings, so server owners can swap them in one place.
 */
public final class SoundPalette {

    private SoundPalette() {
    }

    public static final String[] CRIT_GOLD = {"BLOCK_AMETHYST_BLOCK_CHIME", "ENTITY_PLAYER_ATTACK_CRIT", "ENTITY_ARROW_HIT_PLAYER", "ORB_PICKUP"};
    public static final String[] LIFESTEAL = {"ENTITY_GENERIC_DRINK", "ENTITY_WITCH_DRINK", "DRINK"};
    public static final String[] RAGE = {"ENTITY_RAVAGER_ROAR", "ENTITY_ENDER_DRAGON_GROWL", "ENTITY_WOLF_GROWL"};
    public static final String[] STREAK = {"ENTITY_PLAYER_LEVELUP", "PLAYER_LEVELUP", "LEVEL_UP"};
    public static final String[] SWEEP = {"ENTITY_PLAYER_ATTACK_SWEEP", "ENTITY_PLAYER_ATTACK_STRONG"};
    public static final String[] WHOOSH = {"ITEM_TRIDENT_RIPTIDE_1", "ENTITY_PLAYER_ATTACK_SWEEP", "ENTITY_PLAYER_ATTACK_KNOCKBACK"};
    public static final String[] BLEED = {"ENTITY_PLAYER_HURT", "HURT_FLESH"};
    public static final String[] FROST = {"BLOCK_GLASS_BREAK", "GLASS", "DIG_GLASS"};
    public static final String[] PHASE = {"ENTITY_PHANTOM_BITE", "ENTITY_ENDERMAN_TELEPORT", "ENDERMAN_TELEPORT"};
    public static final String[] STUN = {"ITEM_SHIELD_BREAK", "ITEM_SHIELD_BLOCK"};
    public static final String[] BONE = {"BLOCK_BONE_BLOCK_BREAK", "DIG_STONE"};

    private static Sound resolve(String[] candidates) {
        for (String name : candidates) {
            try {
                return Sound.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // try next candidate
            }
        }
        return null;
    }

    /** Plays a palette entry at the player. {@code volume} should already include the config multiplier. */
    public static void play(Player player, String[] palette, float volume, float pitch) {
        if (player == null || volume <= 0.0f) {
            return;
        }
        playAt(player.getLocation(), palette, volume, pitch);
    }

    /** Plays a palette entry at a world location (heard by everyone nearby). */
    public static void playAt(Location location, String[] palette, float volume, float pitch) {
        if (location == null || location.getWorld() == null || volume <= 0.0f) {
            return;
        }
        Sound sound = resolve(palette);
        if (sound == null) {
            return;
        }
        try {
            location.getWorld().playSound(location, sound, volume, pitch);
        } catch (Throwable ignored) {
            // never let a cosmetic sound break gameplay
        }
    }
}
