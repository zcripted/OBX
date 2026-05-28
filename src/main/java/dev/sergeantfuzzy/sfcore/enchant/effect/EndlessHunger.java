package dev.sergeantfuzzy.sfcore.enchant.effect;

import dev.sergeantfuzzy.sfcore.enchant.model.CustomEnchant;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Persistent kill counter for the <b>Endless Hunger</b> (#43) Mythic enchant.
 *
 * <p>The count is stored directly on the item as a single visible lore line, which
 * is the only cross-version persistence available here (PDC is 1.14+ and the plugin
 * compiles against 1.12.2). The line is intentionally human-readable progress text;
 * {@code EnchantStorage} treats it as foreign "other" lore and preserves it through
 * its tooltip rewrites, so anvil/enchant-table operations don't wipe the count. The
 * raw kill total is the source of truth (parsed back from the parenthetical); the
 * displayed percent and the {@code [Hunger ×N]} name suffix are derived from it.
 *
 * <p>"Unique enemies killed" is implemented as the total kill count: each kill is a
 * distinct entity, and persisting a per-victim UUID set in lore is infeasible.
 */
public final class EndlessHunger {

    private EndlessHunger() {
    }

    /** Strip-colored sentinel that identifies our lore line. */
    private static final String LABEL = "Endless Hunger";
    private static final Pattern KILLS = Pattern.compile("\\((\\d+)\\s*kills\\)");
    private static final Pattern NAME_SUFFIX = Pattern.compile("\\s*\\[Hunger [x×]\\d+\\]$");

    /** Raw kills recorded on the item (0 if none / unreadable). */
    public static int kills(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return 0;
        }
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped.contains(LABEL)) {
                Matcher m = KILLS.matcher(stripped);
                if (m.find()) {
                    try {
                        return Integer.parseInt(m.group(1));
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    /** Stacks earned so far: one per {@code per} kills, capped to the level's cap. */
    public static int stacks(int kills, CustomEnchant e, int level) {
        int per = Math.max(1, e.levelInt(level, "kills_per_stack", 10));
        int capPercent = (int) Math.round(e.levelDouble(level, "cap", 0.15) * 100.0);
        return Math.min(capPercent, kills / per);
    }

    /** Outgoing-damage multiplier-add from the current stacks (e.g. 0.12 for +12%). */
    public static double bonus(ItemStack item, CustomEnchant e, int level) {
        return stacks(kills(item), e, level) / 100.0;
    }

    /**
     * Records one kill on {@code weapon}, persists the new count + display, and writes
     * the item back to the killer's main hand. Returns {@code true} when this kill
     * crossed a new stack milestone (so the caller can play milestone FX).
     */
    public static boolean record(Player killer, ItemStack weapon, CustomEnchant e, int level) {
        if (killer == null || weapon == null) {
            return false;
        }
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) {
            return false;
        }
        int oldKills = kills(weapon);
        int oldStacks = stacks(oldKills, e, level);
        int newKills = oldKills + 1;
        int newStacks = stacks(newKills, e, level);

        writeLine(meta, newKills, newStacks);
        writeNameSuffix(meta, newStacks);
        weapon.setItemMeta(meta);
        // Persist by writing the mutated item back into the held slot (cross-version).
        try {
            killer.getInventory().setItem(killer.getInventory().getHeldItemSlot(), weapon);
        } catch (Throwable ignored) {
            // odd fork — the meta change on the in-hand reference may already suffice
        }
        return newStacks > oldStacks;
    }

    private static void writeLine(ItemMeta meta, int kills, int stacks) {
        String marker = ChatColor.DARK_GRAY + "✦ " + ChatColor.GRAY + LABEL + " "
                + ChatColor.DARK_GRAY + "» " + ChatColor.RED + "+" + stacks + "%"
                + ChatColor.DARK_GRAY + " (" + kills + " kills)";
        List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
        int index = -1;
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).contains(LABEL)) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            lore.set(index, marker);
        } else {
            lore.add(marker);
        }
        meta.setLore(lore);
    }

    private static void writeNameSuffix(ItemMeta meta, int stacks) {
        try {
            String base = meta.hasDisplayName() ? meta.getDisplayName() : "";
            base = NAME_SUFFIX.matcher(base).replaceAll("");
            if (stacks > 0) {
                String suffix = " " + ChatColor.GOLD + "[Hunger ×" + stacks + "]";
                meta.setDisplayName(base.isEmpty() ? null : base + suffix);
                if (base.isEmpty()) {
                    // No base name to hang the suffix on — use the suffix alone (trimmed).
                    meta.setDisplayName(ChatColor.GOLD + "[Hunger ×" + stacks + "]");
                }
            } else if (!base.isEmpty()) {
                meta.setDisplayName(base);
            }
        } catch (Throwable ignored) {
            // never let a cosmetic rename break kill processing
        }
    }
}
