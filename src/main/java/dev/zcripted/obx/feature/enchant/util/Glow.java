package dev.zcripted.obx.feature.enchant.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;

/**
 * Adds the enchantment "glint" (glow) to an item across all supported versions.
 *
 * <p>On 1.20.5+ it uses {@code ItemMeta#setEnchantmentGlintOverride(Boolean)}
 * (reflectively) — a clean, gameplay-free glint that is also removable. On older
 * servers it falls back to the long-standing trick of adding a hidden vanilla
 * enchantment ({@code Unbreaking} + {@link ItemFlag#HIDE_ENCHANTS}); the only
 * side effect there is a single hidden level of Unbreaking, which is left in
 * place if the custom enchants are later removed.
 */
public final class Glow {

    private Glow() {
    }

    /** Adds glow unconditionally (used for scrolls/books). */
    public static void apply(ItemMeta meta) {
        if (meta == null) {
            return;
        }
        if (trySetGlint(meta, Boolean.TRUE)) {
            return;
        }
        try {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } catch (Throwable ignored) {
            // glint is purely cosmetic — never break item creation over it
        }
    }

    /** Adds glow only if the item isn't already glowing from a real enchantment. */
    public static void ensure(ItemMeta meta) {
        if (meta == null || meta.hasEnchants()) {
            return;
        }
        apply(meta);
    }

    /** Clears the glint override (1.20.5+); the legacy fallback enchant is left untouched. */
    public static void clear(ItemMeta meta) {
        if (meta == null) {
            return;
        }
        trySetGlint(meta, null);
    }

    private static boolean trySetGlint(ItemMeta meta, Boolean value) {
        try {
            Method method = meta.getClass().getMethod("setEnchantmentGlintOverride", Boolean.class);
            // CraftMetaItem is an internal class; without setAccessible the reflective
            // invoke can fail (IllegalAccessException / module restriction) even though
            // the method is public. When that happens we'd silently fall back to the
            // hidden-Unbreaking glow hack — which then shows up as a real "Unbreaking"
            // enchantment in the tooltip. Forcing accessibility keeps the clean glint
            // override path working on 1.20.5+.
            method.setAccessible(true);
            method.invoke(meta, value);
            return true;
        } catch (Throwable notSupported) {
            return false;
        }
    }
}
