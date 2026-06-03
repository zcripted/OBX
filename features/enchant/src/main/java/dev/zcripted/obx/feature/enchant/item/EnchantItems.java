package dev.zcripted.obx.feature.enchant.item;

import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.model.EnchantRarity;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds and identifies the physical Arcanum items — enchantment scrolls, the
 * traditional enchanted-book version, and the utility scrolls (protection,
 * success, extraction, transmutation).
 *
 * <p>Like the rest of the module these items are identified without NBT/PDC:
 * a final italic dark-gray marker line in the lore names the {@link ScrollKind},
 * and the carried enchantment (for scrolls/books) is parsed from the same lore
 * line {@link EnchantStorage} uses on gear. Glow and custom-model-data are
 * applied reflectively so the single jar stays correct on 1.8.8 → 1.21.x.
 */
public final class EnchantItems {

    private static final String MARKER = ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "Arcanum ";
    private static final String PROTECTED_LINE = ChatColor.AQUA + "✦ Protected";
    private static final String BOOSTED_LINE = ChatColor.GREEN + "✦ Boosted";
    private static final String DIVIDER = ChatColor.DARK_GRAY + "─────────────────";

    private final EnchantService service;

    public EnchantItems(EnchantService service) {
        this.service = service;
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    public ItemStack scroll(CustomEnchant enchant, int level, int amount) {
        EnchantRarity rarity = EnchantRarity.forLevel(level);
        boolean book = rarity.ordinal() >= EnchantRarity.RARE.ordinal();
        Material material = book ? matchOr("ENCHANTED_BOOK", Material.BOOK) : matchOr("PAPER", Material.PAPER);
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(rarity.getColor() + "✦ Enchantment Scroll");
        List<String> lore = new ArrayList<String>();
        lore.add(DIVIDER);
        lore.add(service.getStorage().renderLine(enchant, level));
        lore.add(ChatColor.GRAY + "Rarity " + ChatColor.DARK_GRAY + "» " + rarity.getColor() + rarity.getDisplayName());
        lore.add(DIVIDER);
        for (String line : enchant.getDescription()) {
            lore.add(ChatColor.GRAY + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', line)));
        }
        lore.add(DIVIDER);
        lore.add(ChatColor.GRAY + "Anvil " + ChatColor.DARK_GRAY + "» " + ChatColor.WHITE + "combine with an item");
        lore.add(ChatColor.GRAY + "Drag " + ChatColor.DARK_GRAY + "» " + ChatColor.WHITE + "drop onto a matching item");
        lore.add(DIVIDER);
        lore.add(MARKER + ScrollKind.ENCHANT_SCROLL.getLabel());
        meta.setLore(lore);
        if (rarity.ordinal() >= EnchantRarity.EPIC.ordinal() || enchant.isGlow()) {
            addGlow(meta);
        }
        if (enchant.getCustomModelData() > 0) {
            trySetCustomModelData(meta, enchant.getCustomModelData());
        }
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack book(CustomEnchant enchant, int level, int amount) {
        Material material = matchOr("ENCHANTED_BOOK", Material.BOOK);
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        String coloredName = ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName());
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Enchanted Book " + ChatColor.GRAY + "» " + coloredName + " " + ChatColor.GRAY + CustomEnchant.roman(level));
        List<String> lore = new ArrayList<String>();
        lore.add(DIVIDER);
        lore.add(service.getStorage().renderLine(enchant, level));
        lore.add(DIVIDER);
        for (String line : enchant.getDescription()) {
            lore.add(ChatColor.GRAY + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', line)));
        }
        lore.add(DIVIDER);
        lore.add(ChatColor.GRAY + "Combine in an anvil to apply.");
        lore.add(DIVIDER);
        lore.add(MARKER + ScrollKind.BOOK.getLabel());
        meta.setLore(lore);
        addGlow(meta);
        if (enchant.getCustomModelData() > 0) {
            trySetCustomModelData(meta, enchant.getCustomModelData());
        }
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack utility(ScrollKind kind, int amount) {
        ItemStack item = new ItemStack(matchOr("PAPER", Material.PAPER), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        ChatColor color;
        List<String> body = new ArrayList<String>();
        switch (kind) {
            case PROTECTION:
                color = ChatColor.AQUA;
                body.add(ChatColor.GRAY + "Place beside an enchant scroll in an anvil.");
                body.add(ChatColor.GRAY + "Saves the item if the application fails.");
                break;
            case SUCCESS:
                color = ChatColor.GREEN;
                body.add(ChatColor.GRAY + "Place beside an enchant scroll in an anvil.");
                body.add(ChatColor.GRAY + "Boosts the next application's success rate.");
                break;
            case EXTRACTION:
                color = ChatColor.GOLD;
                body.add(ChatColor.GRAY + "Right-click a held item to pull one");
                body.add(ChatColor.GRAY + "enchantment back out as a scroll.");
                break;
            case TRANSMUTATION:
                color = ChatColor.LIGHT_PURPLE;
                body.add(ChatColor.GRAY + "Right-click a held item to re-roll one");
                body.add(ChatColor.GRAY + "enchantment within its category.");
                break;
            default:
                color = ChatColor.WHITE;
                break;
        }
        meta.setDisplayName(color + "✦ " + kind.getLabel());
        List<String> lore = new ArrayList<String>();
        lore.add(DIVIDER);
        lore.addAll(body);
        lore.add(DIVIDER);
        lore.add(MARKER + kind.getLabel());
        meta.setLore(lore);
        addGlow(meta);
        item.setItemMeta(meta);
        return item;
    }

    // ── Identification ────────────────────────────────────────────────────────

    public ScrollKind kindOf(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line).trim();
            if (stripped.startsWith("Arcanum ")) {
                ScrollKind kind = ScrollKind.fromLabel(stripped.substring("Arcanum ".length()).trim());
                if (kind != null) {
                    return kind;
                }
            }
        }
        return null;
    }

    public boolean isArcanumItem(ItemStack item) {
        return kindOf(item) != null;
    }

    // ── Protect / Success imbue (a Protection or Success scroll merged onto an enchant scroll) ──

    public boolean isProtected(ItemStack item) {
        return hasStatusLine(item, "Protected");
    }

    public boolean isBoosted(ItemStack item) {
        return hasStatusLine(item, "Boosted");
    }

    public void markProtected(ItemStack item) {
        addStatusLine(item, PROTECTED_LINE, "Protected");
    }

    public void markBoosted(ItemStack item) {
        addStatusLine(item, BOOSTED_LINE, "Boosted");
    }

    private boolean hasStatusLine(ItemStack item, String keyword) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return false;
        }
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line).trim();
            if (stripped.equals("✦ " + keyword)) {
                return true;
            }
        }
        return false;
    }

    private void addStatusLine(ItemStack item, String line, String keyword) {
        if (item == null || hasStatusLine(item, keyword)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
        // Insert just after the enchant line (index 1) when possible, else at the top.
        int index = lore.isEmpty() ? 0 : 1;
        lore.add(Math.min(index, lore.size()), line);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /** The enchantment carried by a scroll or book (parsed from its lore), or {@code null}. */
    public CustomEnchant payloadEnchant(ItemStack item) {
        ScrollKind kind = kindOf(item);
        if (kind != ScrollKind.ENCHANT_SCROLL && kind != ScrollKind.BOOK) {
            return null;
        }
        Map<String, Integer> read = service.getStorage().read(item);
        for (String id : read.keySet()) {
            return service.getRegistry().get(id);
        }
        return null;
    }

    public int payloadLevel(ItemStack item) {
        ScrollKind kind = kindOf(item);
        if (kind != ScrollKind.ENCHANT_SCROLL && kind != ScrollKind.BOOK) {
            return 0;
        }
        Map<String, Integer> read = service.getStorage().read(item);
        for (Integer level : read.values()) {
            return level;
        }
        return 0;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static Material matchOr(String name, Material fallback) {
        Material material = Material.matchMaterial(name);
        return material != null ? material : fallback;
    }

    private static void addGlow(ItemMeta meta) {
        dev.zcripted.obx.feature.enchant.util.Glow.apply(meta);
    }

    private static void trySetCustomModelData(ItemMeta meta, int value) {
        try {
            Method method = meta.getClass().getMethod("setCustomModelData", Integer.class);
            method.invoke(meta, Integer.valueOf(value));
        } catch (Throwable ignored) {
            // setCustomModelData is 1.14+; silently skip on older servers.
        }
    }
}
