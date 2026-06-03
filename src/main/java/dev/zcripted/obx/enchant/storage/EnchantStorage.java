package dev.zcripted.obx.enchant.storage;

import dev.zcripted.obx.enchant.model.CustomEnchant;
import dev.zcripted.obx.enchant.registry.EnchantRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads, writes, and removes Arcanum enchantments on an {@link ItemStack} using
 * <strong>structured item lore</strong> as the backing store, and renders that
 * lore into themed, sectioned tooltips.
 *
 * <p>Why lore and not the PersistentDataContainer: the plugin compiles against
 * the 1.12.2 API (PDC is 1.14+) yet must run on 1.8.8 → 1.21.x. Lore is available
 * everywhere, survives client/server round-trips, and doubles as the visible
 * tooltip.
 *
 * <h2>Tooltip layout</h2>
 * The lore is rebuilt into up to three sections (custom first, vanilla last):
 * <pre>
 * ▬▬▬ Enchantments ▬▬▬     (custom, non-cursed; dots colored by rarity)
 * ▬▬▬ Curses ▬▬▬           (custom, cursed; dark-red)
 * ▬▬▬ Vanilla ▬▬▬          (Bukkit enchantments; white dots)
 * </pre>
 * Each entry is {@code <name>  <filled●><empty○> <ROMAN> [MAX]} where the filled
 * dot count is the level and the total is the max level. Within a section,
 * entries are sorted by level (highest first), then name.
 *
 * <h2>Parsing back (storage)</h2>
 * Only the <em>custom</em> entries are storage; they are recovered by stripping
 * color, dropping a trailing {@code MAX} token, the roman level, and a dot-bar
 * token, then matching the remaining name against
 * {@link EnchantRegistry#byDisplayName(String)}. This parses both the new dotted
 * format and the legacy {@code <name> <ROMAN>} format (migration is automatic on
 * the next write). Vanilla entries and section headers are regenerated from the
 * item's real {@link Enchantment}s each write, never parsed as custom.
 */
public final class EnchantStorage {

    /** Filled tier dot (U+25CF ●). Escaped so it survives any source encoding. */
    private static final char DOT_FILLED = '●';
    /** Empty tier dot (U+25CB ○). */
    private static final char DOT_EMPTY = '○';
    /** Header bar glyph (U+25AC ▬). */
    private static final char BAR = '▬';
    /** Visible column the dot bar is padded to (approximate — MC font is proportional). */
    private static final int NAME_COLUMN = 20;
    /** Hard cap on rendered dots so an oddly-high max level can't blow out the line. */
    private static final int MAX_DOTS = 10;

    private static final String HEADER_ENCHANTS = header(ChatColor.AQUA, "Enchantments");
    private static final String HEADER_CURSES = header(ChatColor.DARK_RED, "Curses");
    private static final String HEADER_VANILLA = header(ChatColor.GRAY, "Vanilla");
    private static final String HEADER_ATTRIBUTES = header(ChatColor.GOLD, "Attributes");

    /** Labels for the custom attribute lines; also used to recognize them on re-parse. */
    private static final String LABEL_ATTACK_DAMAGE = "Attack Damage";
    private static final String LABEL_ATTACK_SPEED = "Attack Speed";

    private final EnchantRegistry registry;

    /** When false the Vanilla section (and HIDE_ENCHANTS) is suppressed; set from config. */
    private volatile boolean styleVanilla = true;

    public EnchantStorage(EnchantRegistry registry) {
        this.registry = registry;
    }

    public void setStyleVanilla(boolean styleVanilla) {
        this.styleVanilla = styleVanilla;
    }

    // ── Read ────────────────────────────────────────────────────────────────────

    /** All Arcanum enchantments currently on the item, as {@code enchantId → level}. */
    public Map<String, Integer> read(ItemStack item) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return result;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return result;
        }
        for (String line : meta.getLore()) {
            parseLine(line, result);
        }
        return result;
    }

    public int level(ItemStack item, String enchantId) {
        Integer level = read(item).get(enchantId == null ? null : enchantId.toLowerCase(Locale.ENGLISH));
        return level == null ? 0 : level;
    }

    public boolean has(ItemStack item, String enchantId) {
        return level(item, enchantId) > 0;
    }

    public int count(ItemStack item) {
        return read(item).size();
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Applies (or replaces) the enchantment at the given level and rewrites the
     * lore. Returns the same item instance for chaining. Caller is responsible
     * for validation (tags, conflicts, caps) — this is the raw write.
     */
    public ItemStack apply(ItemStack item, CustomEnchant enchant, int level) {
        if (item == null || enchant == null || item.getType() == Material.AIR) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        Split split = split(meta);
        split.enchants.put(enchant.getId(), Math.max(1, Math.min(enchant.getMaxLevel(), level)));
        writeLore(meta, split, item.getType());
        // Give the item the enchantment glow if it isn't already glowing.
        dev.zcripted.obx.enchant.util.Glow.ensure(meta);
        item.setItemMeta(meta);
        return item;
    }

    /** Removes an enchantment if present; returns the item. */
    public ItemStack remove(ItemStack item, String enchantId) {
        if (item == null || enchantId == null || item.getType() == Material.AIR) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        Split split = split(meta);
        if (split.enchants.remove(enchantId.toLowerCase(Locale.ENGLISH)) == null) {
            return item;
        }
        writeLore(meta, split, item.getType());
        // If no custom enchants remain, drop the glint override we may have added.
        if (split.enchants.isEmpty()) {
            dev.zcripted.obx.enchant.util.Glow.clear(meta);
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Rebuilds the full sectioned tooltip from the item's current state (custom
     * enchants parsed from lore + the item's real vanilla enchantments). Used by
     * the lore-styling listener so vanilla enchanting/anvil operations restyle the
     * item too. No-op on air / meta-less items.
     */
    public void refresh(ItemStack item) {
        refresh(item, null);
    }

    /**
     * As {@link #refresh(ItemStack)}, but merges {@code extraVanilla} into the
     * vanilla set first — used by the enchanting-table hook, where the chosen
     * enchantments aren't on the item yet when the event fires.
     */
    public void refresh(ItemStack item, Map<Enchantment, Integer> extraVanilla) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        Split split = split(meta);
        writeLore(meta, split, extraVanilla, item.getType());
        item.setItemMeta(meta);
    }

    // ── Single-line renderer (also used by scroll/book payload encoding) ─────────

    /**
     * The compact one-enchant line used by scrolls/books: {@code <coloredName> <ROMAN>}.
     * Kept simple (no dot bar) so scroll items read cleanly; the parser accepts it.
     */
    public String renderLine(CustomEnchant enchant, int level) {
        String name = ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName());
        return name + " " + ChatColor.GRAY + CustomEnchant.roman(level);
    }

    // ── Sectioned lore building ──────────────────────────────────────────────────

    private void writeLore(ItemMeta meta, Split split, Material material) {
        writeLore(meta, split, null, material);
    }

    private void writeLore(ItemMeta meta, Split split, Map<Enchantment, Integer> extraVanilla, Material material) {
        List<CustomEnchant> nonCursed = new ArrayList<CustomEnchant>();
        List<CustomEnchant> cursed = new ArrayList<CustomEnchant>();
        for (String id : split.enchants.keySet()) {
            CustomEnchant enchant = registry.get(id);
            if (enchant == null) {
                continue;
            }
            (enchant.isCursed() ? cursed : nonCursed).add(enchant);
        }
        sortCustomByTier(nonCursed, split.enchants);
        sortCustomByTier(cursed, split.enchants);

        boolean hasCustom = !nonCursed.isEmpty() || !cursed.isEmpty();
        List<Map.Entry<Enchantment, Integer>> vanilla = styleVanilla
                ? readVanilla(meta, extraVanilla, hasCustom)
                : Collections.<Map.Entry<Enchantment, Integer>>emptyList();
        // Attack damage/speed are only restyled on Arcane gear ("when enchantments
        // are applied"); the values are read from the item's real modifiers so they
        // stay correct even if a future enchant changes them.
        List<String> attributes = hasCustom ? buildAttributeBlock(material, meta) : null;

        // Assemble each populated section as its own block, separated by a blank
        // line for clean, easy-to-read spacing.
        List<List<String>> blocks = new ArrayList<List<String>>();
        if (!nonCursed.isEmpty()) {
            blocks.add(customBlock(HEADER_ENCHANTS, nonCursed, split.enchants));
        }
        if (!cursed.isEmpty()) {
            blocks.add(customBlock(HEADER_CURSES, cursed, split.enchants));
        }
        if (!vanilla.isEmpty()) {
            List<String> block = new ArrayList<String>();
            block.add(HEADER_VANILLA);
            for (Map.Entry<Enchantment, Integer> entry : vanilla) {
                block.add(renderVanillaLine(entry.getKey(), entry.getValue()));
            }
            blocks.add(block);
        }
        if (attributes != null && !attributes.isEmpty()) {
            blocks.add(attributes);
        }

        List<String> lore = new ArrayList<String>();
        for (List<String> block : blocks) {
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.addAll(block);
        }
        if (!split.other.isEmpty()) {
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.addAll(split.other);
        }
        meta.setLore(lore.isEmpty() ? null : lore);

        // Hide the default (blue) enchantment lines whenever we manage the vanilla
        // display: when we render a Vanilla section (don't duplicate it) AND when the
        // only real enchant is the cosmetic glow marker we skipped (don't reveal it).
        // The glint stays. Checked against both the merged set and the item's real
        // enchants so the enchanting-table path (enchants not yet applied) also hides.
        if (styleVanilla && (!vanilla.isEmpty() || meta.hasEnchants())) {
            try {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } catch (Throwable ignored) {
                // flag missing on an exotic fork — non-fatal
            }
        }
        // Hide the default gray attribute lines only when we rendered our own clean
        // Attack Damage / Attack Speed section in their place.
        if (attributes != null && !attributes.isEmpty()) {
            try {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            } catch (Throwable ignored) {
                // flag missing on an exotic fork — non-fatal
            }
        }
    }

    private List<String> customBlock(String headerLine, List<CustomEnchant> enchants, Map<String, Integer> levels) {
        List<String> block = new ArrayList<String>();
        block.add(headerLine);
        for (CustomEnchant enchant : enchants) {
            block.add(renderCustomLine(enchant, levels.get(enchant.getId())));
        }
        return block;
    }

    /**
     * Builds the clean Attack Damage / Attack Speed block, or {@code null} when the
     * item isn't a weapon (no attack-damage attribute) or the values can't be read
     * on this server — in which case the vanilla display is left untouched.
     */
    private List<String> buildAttributeBlock(Material material, ItemMeta meta) {
        double[] stats = WeaponAttributes.mainHand(material, meta);
        if (stats == null) {
            return null;
        }
        List<String> block = new ArrayList<String>();
        block.add(HEADER_ATTRIBUTES);
        block.add(attributeLine(LABEL_ATTACK_DAMAGE, stats[0]));
        block.add(attributeLine(LABEL_ATTACK_SPEED, stats[1]));
        return block;
    }

    private static String attributeLine(String label, double value) {
        return ChatColor.GRAY + label + " " + ChatColor.DARK_GRAY + "» " + ChatColor.WHITE + formatStat(value);
    }

    /** Drops a trailing {@code .0} (7.0 → "7") but keeps real decimals (1.6 → "1.6"). */
    private static String formatStat(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return new java.text.DecimalFormat("0.##").format(value);
    }

    /** One custom-enchant line: category-colored name (italic for curses) + dot bar. */
    private String renderCustomLine(CustomEnchant enchant, int level) {
        int max = enchant.getMaxLevel();
        String name = enchant.plainName();
        ChatColor nameColor = enchant.getCategory() != null ? enchant.getCategory().getColor() : ChatColor.GRAY;
        StringBuilder line = new StringBuilder();
        line.append(nameColor);
        if (enchant.isCursed()) {
            line.append(ChatColor.ITALIC).append(pad(name));
            line.append(dots(level, max, ChatColor.DARK_RED));
        } else {
            line.append(pad(name));
            line.append(dots(level, max, enchant.getRarity().getColor()));
        }
        line.append(' ').append(ChatColor.GRAY).append(CustomEnchant.roman(level));
        appendMax(line, level, max);
        return line.toString();
    }

    /** One vanilla-enchant line: gray name + white dot bar. */
    private String renderVanillaLine(Enchantment enchantment, int level) {
        int max = Math.max(level, safeMaxLevel(enchantment));
        StringBuilder line = new StringBuilder();
        line.append(ChatColor.GRAY).append(pad(VanillaEnchantNames.friendly(enchantment)));
        line.append(dots(level, max, ChatColor.WHITE));
        line.append(' ').append(ChatColor.GRAY).append(CustomEnchant.roman(level));
        appendMax(line, level, max);
        return line.toString();
    }

    private static void appendMax(StringBuilder line, int level, int max) {
        if (level >= max) {
            line.append(' ').append(ChatColor.DARK_GRAY).append("MAX");
        }
    }

    /**
     * Public access to the tiered dot bar (●/○) so chat tooltips can mirror the
     * item-lore level design. Filled dots use {@code fill}, the rest dark gray.
     */
    public static String levelDots(int level, int max, ChatColor fill) {
        return dots(level, max, fill);
    }

    /** Builds the colored dot bar: filled dots in {@code fill}, remaining in dark gray. */
    private static String dots(int level, int max, ChatColor fill) {
        int total = Math.max(1, Math.min(max, MAX_DOTS));
        int filled = Math.max(0, Math.min(level, total));
        StringBuilder bar = new StringBuilder();
        bar.append(fill);
        for (int i = 0; i < filled; i++) {
            bar.append(DOT_FILLED);
        }
        if (filled < total) {
            bar.append(ChatColor.DARK_GRAY);
            for (int i = filled; i < total; i++) {
                bar.append(DOT_EMPTY);
            }
        }
        return bar.toString();
    }

    /** Pads a (color-free) name with trailing spaces toward the dot column. */
    private static String pad(String name) {
        int visible = name == null ? 0 : name.length();
        int spaces = Math.max(2, NAME_COLUMN - visible);
        StringBuilder builder = new StringBuilder(name == null ? "" : name);
        for (int i = 0; i < spaces; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private static String header(ChatColor titleColor, String title) {
        String bars = "" + BAR + BAR + BAR;
        return ChatColor.DARK_GRAY.toString() + ChatColor.BOLD + bars + ' '
                + titleColor + ChatColor.BOLD + title + ' '
                + ChatColor.DARK_GRAY + ChatColor.BOLD + bars;
    }

    private static int safeMaxLevel(Enchantment enchantment) {
        try {
            int max = enchantment.getMaxLevel();
            return max > 0 ? max : 1;
        } catch (Throwable ignored) {
            return 1;
        }
    }

    /** Custom enchants sorted by level (highest first), then rarity, then name. */
    private void sortCustomByTier(List<CustomEnchant> list, final Map<String, Integer> levels) {
        Collections.sort(list, new Comparator<CustomEnchant>() {
            @Override
            public int compare(CustomEnchant a, CustomEnchant b) {
                int byLevel = Integer.compare(level(b), level(a));
                if (byLevel != 0) {
                    return byLevel;
                }
                int byRarity = b.getRarity().ordinal() - a.getRarity().ordinal();
                if (byRarity != 0) {
                    return byRarity;
                }
                return a.plainName().compareToIgnoreCase(b.plainName());
            }

            private int level(CustomEnchant enchant) {
                Integer value = levels.get(enchant.getId());
                return value == null ? 0 : value;
            }
        });
    }

    /** Vanilla enchantments on the item (plus optional pending ones), sorted by level desc then name. */
    private List<Map.Entry<Enchantment, Integer>> readVanilla(ItemMeta meta, Map<Enchantment, Integer> extra, boolean hasCustom) {
        Map<Enchantment, Integer> merged = new LinkedHashMap<Enchantment, Integer>();
        if (meta.hasEnchants()) {
            merged.putAll(meta.getEnchants());
        }
        if (extra != null) {
            for (Map.Entry<Enchantment, Integer> entry : extra.entrySet()) {
                Integer present = merged.get(entry.getKey());
                merged.put(entry.getKey(), present == null ? entry.getValue() : Math.max(present, entry.getValue()));
            }
        }
        // The Glow util adds a hidden level-1 Unbreaking as a glow source on servers
        // that lack the glint-override API. It only ever lands on items that carry a
        // custom Arcanum enchant, so drop a lone Unbreaking I there — it's a cosmetic
        // marker, not an enchantment the player chose, and must not appear as a real
        // "Vanilla" entry.
        if (hasCustom) {
            Integer unbreaking = merged.get(Enchantment.DURABILITY);
            if (unbreaking != null && unbreaking.intValue() == 1) {
                merged.remove(Enchantment.DURABILITY);
            }
        }
        List<Map.Entry<Enchantment, Integer>> list = new ArrayList<Map.Entry<Enchantment, Integer>>(merged.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Enchantment, Integer>>() {
            @Override
            public int compare(Map.Entry<Enchantment, Integer> a, Map.Entry<Enchantment, Integer> b) {
                int byLevel = Integer.compare(b.getValue(), a.getValue());
                if (byLevel != 0) {
                    return byLevel;
                }
                return VanillaEnchantNames.friendly(a.getKey())
                        .compareToIgnoreCase(VanillaEnchantNames.friendly(b.getKey()));
            }
        });
        return list;
    }

    // ── Parsing ──────────────────────────────────────────────────────────────────

    /**
     * Recovers a custom (enchant, level) pair from one lore line, if present.
     * Tolerates the new dotted format, the legacy {@code <name> <ROMAN>} format,
     * a trailing {@code MAX} marker, and column padding. Section headers and
     * vanilla lines do not resolve to a custom enchant and are ignored.
     */
    private void parseLine(String line, Map<String, Integer> out) {
        if (line == null) {
            return;
        }
        String stripped = ChatColor.stripColor(line).trim();
        if (stripped.isEmpty() || stripped.indexOf(BAR) >= 0) {
            return;
        }
        String[] tokens = stripped.split("\\s+");
        int end = tokens.length;
        if (end > 0 && tokens[end - 1].equalsIgnoreCase("MAX")) {
            end--;
        }
        if (end <= 0) {
            return;
        }
        int level = CustomEnchant.parseRoman(tokens[end - 1]);
        if (level <= 0) {
            return;
        }
        end--;
        if (end > 0 && isDotBar(tokens[end - 1])) {
            end--;
        }
        if (end <= 0) {
            return;
        }
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < end; i++) {
            if (i > 0) {
                name.append(' ');
            }
            name.append(tokens[i]);
        }
        CustomEnchant enchant = registry.byDisplayName(name.toString());
        if (enchant != null && enchant.hasLevel(level)) {
            out.put(enchant.getId(), level);
        }
    }

    /** True if the token is a run of (only) filled/empty tier-dot glyphs. */
    private static boolean isDotBar(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c != DOT_FILLED && c != DOT_EMPTY) {
                return false;
            }
        }
        return true;
    }

    /**
     * A line we generated (header or any dotted enchant line) that must be dropped
     * and regenerated rather than preserved as foreign lore. Custom enchant lines
     * are already captured by {@link #parseLine}; this catches headers and the
     * vanilla section (whose names don't resolve to a custom enchant).
     */
    private static boolean isManagedDecoration(String stripped) {
        return stripped.indexOf(BAR) >= 0
                || stripped.indexOf(DOT_FILLED) >= 0
                || stripped.indexOf(DOT_EMPTY) >= 0
                || stripped.startsWith(LABEL_ATTACK_DAMAGE)
                || stripped.startsWith(LABEL_ATTACK_SPEED);
    }

    private Split split(ItemMeta meta) {
        Split split = new Split();
        if (!meta.hasLore()) {
            return split;
        }
        for (String line : meta.getLore()) {
            Map<String, Integer> single = new LinkedHashMap<String, Integer>();
            parseLine(line, single);
            if (!single.isEmpty()) {
                split.enchants.putAll(single);
                continue;
            }
            String stripped = ChatColor.stripColor(line).trim();
            if (stripped.isEmpty()) {
                split.other.add(line);
                continue;
            }
            if (isManagedDecoration(stripped)) {
                continue; // header / vanilla line — regenerated on write
            }
            split.other.add(line);
        }
        // Drop leading blank separators so repeated writes don't accumulate them.
        while (!split.other.isEmpty() && ChatColor.stripColor(split.other.get(0)).trim().isEmpty()) {
            split.other.remove(0);
        }
        return split;
    }

    private static final class Split {
        private final Map<String, Integer> enchants = new LinkedHashMap<String, Integer>();
        private final List<String> other = new ArrayList<String>();
    }
}
