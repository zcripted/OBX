package dev.sergeantfuzzy.sfcore.enchant.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Item-type targeting tags. An enchantment declares which tags it is valid on;
 * the service checks a held item against those tags before applying.
 *
 * <p>Classification is done by the material's <em>name</em> (e.g. ends with
 * {@code _SWORD}) rather than by referencing {@link Material} constants. This is
 * deliberate: the jar compiles against the 1.12.2 API but must run on 1.8.8 →
 * 1.21.x, where many materials (TRIDENT, MACE, NETHERITE_*, …) and even renamed
 * ones (SPADE → SHOVEL, CARROT → CARROTS) do not exist as compile-time enums.
 * Name matching keeps a single jar correct everywhere.
 *
 * <p>Composite tags ({@link #WEAPON}, {@link #ARMOR}, …) expand to a set of base
 * tags; {@link #ANY} matches everything.
 */
public enum ItemTag {

    // ── Base tags ───────────────────────────────────────────────────────────
    SWORD,
    AXE,
    PICKAXE,
    SHOVEL,
    HOE,
    BOW,
    CROSSBOW,
    TRIDENT,
    MACE,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    SHIELD,
    ELYTRA,
    FISHING_ROD,
    SHEARS,
    FLINT_AND_STEEL,
    FOOD,
    CROP_SEED,
    BLOCK,

    // ── Composite tags ──────────────────────────────────────────────────────
    WEAPON,
    RANGED,
    ARMOR,
    TOOL,
    ANY;

    private static final Set<String> SEED_NAMES = buildSeedNames();

    /** Base tags this tag expands to (a base tag expands to just itself). */
    public Set<ItemTag> expand() {
        switch (this) {
            case WEAPON:
                return EnumSet.of(SWORD, AXE, TRIDENT, MACE);
            case RANGED:
                return EnumSet.of(BOW, CROSSBOW, TRIDENT);
            case ARMOR:
                return EnumSet.of(HELMET, CHESTPLATE, LEGGINGS, BOOTS);
            case TOOL:
                return EnumSet.of(PICKAXE, AXE, SHOVEL, HOE, SHEARS);
            case ANY:
                return EnumSet.allOf(ItemTag.class);
            default:
                return EnumSet.of(this);
        }
    }

    /** Resolves a (possibly composite) list of tags into the set of base tags. */
    public static Set<ItemTag> expandAll(Iterable<ItemTag> tags) {
        Set<ItemTag> result = EnumSet.noneOf(ItemTag.class);
        if (tags != null) {
            for (ItemTag tag : tags) {
                if (tag != null) {
                    result.addAll(tag.expand());
                }
            }
        }
        return result;
    }

    /** Tolerant lookup by name; returns {@code null} if unknown. */
    public static ItemTag fromId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.trim().toUpperCase(Locale.ENGLISH);
        for (ItemTag tag : values()) {
            if (tag.name().equals(normalized)) {
                return tag;
            }
        }
        return null;
    }

    /**
     * Returns every base tag the given item matches. An item can match several
     * (a diamond sword is {@code SWORD}; a carrot is both {@code FOOD} and
     * {@code CROP_SEED}).
     */
    public static Set<ItemTag> tagsFor(ItemStack item) {
        Set<ItemTag> result = EnumSet.noneOf(ItemTag.class);
        if (item == null || item.getType() == Material.AIR) {
            return result;
        }
        Material material = item.getType();
        String name = material.name().toUpperCase(Locale.ENGLISH);

        if (name.endsWith("_SWORD")) {
            result.add(SWORD);
        }
        if (name.endsWith("_PICKAXE")) {
            result.add(PICKAXE);
        }
        if (name.endsWith("_AXE") && !name.endsWith("_PICKAXE")) {
            result.add(AXE);
        }
        if (name.endsWith("_SHOVEL") || name.endsWith("_SPADE")) {
            result.add(SHOVEL);
        }
        if (name.endsWith("_HOE")) {
            result.add(HOE);
        }
        if (name.equals("BOW")) {
            result.add(BOW);
        }
        if (name.equals("CROSSBOW")) {
            result.add(CROSSBOW);
        }
        if (name.equals("TRIDENT")) {
            result.add(TRIDENT);
        }
        if (name.equals("MACE")) {
            result.add(MACE);
        }
        if (name.endsWith("_HELMET") || name.equals("TURTLE_HELMET") || name.endsWith("_SKULL")
                || name.endsWith("_HEAD") || name.equals("CARVED_PUMPKIN")) {
            result.add(HELMET);
        }
        if (name.endsWith("_CHESTPLATE")) {
            result.add(CHESTPLATE);
        }
        if (name.endsWith("_LEGGINGS")) {
            result.add(LEGGINGS);
        }
        if (name.endsWith("_BOOTS")) {
            result.add(BOOTS);
        }
        if (name.equals("SHIELD")) {
            result.add(SHIELD);
        }
        if (name.equals("ELYTRA")) {
            result.add(ELYTRA);
        }
        if (name.equals("FISHING_ROD")) {
            result.add(FISHING_ROD);
        }
        if (name.equals("SHEARS")) {
            result.add(SHEARS);
        }
        if (name.equals("FLINT_AND_STEEL")) {
            result.add(FLINT_AND_STEEL);
        }
        if (SEED_NAMES.contains(name)) {
            result.add(CROP_SEED);
        }
        try {
            if (material.isEdible()) {
                result.add(FOOD);
            }
        } catch (Throwable ignored) {
            // isEdible exists on all supported versions; guard defensively anyway.
        }
        if (material.isBlock()) {
            result.add(BLOCK);
        }
        return result;
    }

    /**
     * True if the item matches at least one of the (possibly composite) declared
     * tags. {@code ANY} always matches a non-air item.
     */
    public static boolean matchesAny(ItemStack item, Iterable<ItemTag> declared) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        Set<ItemTag> wanted = expandAll(declared);
        if (wanted.contains(ANY)) {
            return true;
        }
        Set<ItemTag> have = tagsFor(item);
        for (ItemTag tag : have) {
            if (wanted.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    /** Human-readable tag list for error messages, e.g. {@code "Swords, Axes"}. */
    public static String describe(List<ItemTag> declared) {
        if (declared == null || declared.isEmpty()) {
            return "any item";
        }
        Set<String> labels = new LinkedHashSet<>();
        for (ItemTag tag : declared) {
            if (tag == ANY) {
                return "any item";
            }
            labels.add(prettify(tag));
        }
        return String.join(", ", labels);
    }

    private static String prettify(ItemTag tag) {
        String lower = tag.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : lower.toCharArray()) {
            if (cap && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(c);
            }
            if (c == ' ') {
                cap = true;
            }
        }
        return sb.toString();
    }

    private static Set<String> buildSeedNames() {
        Set<String> set = new HashSet<>(Arrays.asList(
                "WHEAT_SEEDS", "BEETROOT_SEEDS", "MELON_SEEDS", "PUMPKIN_SEEDS",
                "CARROT", "CARROTS", "POTATO", "POTATOES", "NETHER_WART",
                "TORCHFLOWER_SEEDS", "PITCHER_POD", "COCOA_BEANS", "SWEET_BERRIES"
        ));
        return Collections.unmodifiableSet(set);
    }
}
