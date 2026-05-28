package dev.sergeantfuzzy.sfcore.enchant.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Resolves {@link Sound} constants by trying a list of candidate names with
 * {@code Sound.valueOf}. Sound enum names changed between versions (1.8 had
 * {@code VILLAGER_NO}; 1.9+ has {@code ENTITY_VILLAGER_NO}; 1.13+/1.21 renamed
 * several again), so a single jar must look them up by name rather than
 * referencing a constant that may not exist at runtime.
 */
public final class Sounds {

    private Sounds() {
    }

    public static final String[] SUCCESS = {"ENTITY_PLAYER_LEVELUP", "PLAYER_LEVELUP", "LEVEL_UP"};
    public static final String[] CONFIRM = {"ENTITY_EXPERIENCE_ORB_PICKUP", "EXPERIENCE_ORB_PICKUP", "ORB_PICKUP"};
    public static final String[] ERROR = {"ENTITY_VILLAGER_NO", "VILLAGER_NO", "UI_BUTTON_CLICK", "CLICK"};
    public static final String[] CLICK = {"UI_BUTTON_CLICK", "CLICK"};
    public static final String[] MAGIC = {"BLOCK_ENCHANTMENT_TABLE_USE", "ENCHANTMENT_TABLE_USE", "BLOCK_AMETHYST_BLOCK_CHIME"};
    public static final String[] BLINK = {"ENTITY_ENDERMAN_TELEPORT", "ENTITY_ENDERMEN_TELEPORT", "ENDERMAN_TELEPORT"};
    public static final String[] ZAP = {"ENTITY_LIGHTNING_BOLT_IMPACT", "ENTITY_LIGHTNING_IMPACT", "AMBIENCE_THUNDER"};

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

    public static void play(Player player, String[] candidates, float volume, float pitch) {
        if (player == null) {
            return;
        }
        Sound sound = resolve(candidates);
        if (sound != null) {
            try {
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Throwable ignored) {
                // never let a cosmetic sound break gameplay
            }
        }
    }

    public static void success(Player player) {
        play(player, SUCCESS, 0.7f, 1.2f);
    }

    public static void confirm(Player player) {
        play(player, CONFIRM, 0.7f, 1.4f);
    }

    public static void error(Player player) {
        play(player, ERROR, 0.6f, 0.7f);
    }

    public static void click(Player player) {
        play(player, CLICK, 0.5f, 1.0f);
    }
}
