package dev.zcripted.obx.enchant.item;

import dev.zcripted.obx.enchant.model.CustomEnchant;
import dev.zcripted.obx.enchant.model.EnchantCategory;
import dev.zcripted.obx.enchant.service.EnchantService;
import dev.zcripted.obx.enchant.util.EnchantHover;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a stylized, interactive <strong>written book</strong> ("Codex") for a single
 * enchantment category. The book lists every enchant in that category alphabetically;
 * each name carries a hover tooltip (name, category, rarity, max level, applicable
 * items, description) and a click to print full details in chat, and each level is a
 * click-to-apply button.
 *
 * <p>Interactivity is built with the Spigot/BungeeCord chat-component API
 * ({@code net.md_5.bungee.api.chat.*}), which exists on every supported server
 * (1.8.8 → 1.21.x); the component path is wrapped so it degrades to plain-text pages
 * on any fork where book components are unavailable. Clicks run
 * {@code /obxench bookinfo <id>} and {@code /obxench bookapply <id> <level>} as the
 * reader, both gated by {@code obx.enchants.book}.
 */
public final class EnchantGuideBook {

    /** Enchants per content page (name + level row + spacing fits comfortably). */
    private static final int PER_PAGE = 3;

    private EnchantGuideBook() {
    }

    public static ItemStack create(EnchantService service, EnchantCategory category) {
        Material material = matchOr("WRITTEN_BOOK", Material.BOOK);
        ItemStack item = new ItemStack(material, 1);
        ItemMeta baseMeta = item.getItemMeta();
        if (!(baseMeta instanceof BookMeta)) {
            // Fork without a writable book meta — return a labelled fallback item.
            if (baseMeta != null) {
                baseMeta.setDisplayName(category.getColor() + "✦ " + category.getDisplayName() + " Codex");
                item.setItemMeta(baseMeta);
            }
            return item;
        }
        BookMeta meta = (BookMeta) baseMeta;

        List<CustomEnchant> enchants = sortedByName(service.getRegistry().byCategory(category));

        meta.setTitle(ChatColor.stripColor(category.getDisplayName()) + " Codex");
        meta.setAuthor("Arcanum");
        trySetGeneration(meta);
        meta.setDisplayName(category.getColor() + "✦ " + ChatColor.BOLD + category.getDisplayName() + " Codex");
        meta.setLore(coverLore(category, enchants.size()));
        dev.zcripted.obx.enchant.util.Glow.ensure(meta);

        boolean rich = writeComponentPages(meta, category, enchants);
        if (!rich) {
            meta.setPages(plainPages(category, enchants));
        }
        item.setItemMeta(meta);
        return item;
    }

    // ── Cover (inventory) lore ───────────────────────────────────────────────

    private static List<String> coverLore(EnchantCategory category, int count) {
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.DARK_GRAY + "─────────────────");
        lore.add(ChatColor.GRAY + "Category " + ChatColor.DARK_GRAY + "» " + category.getColor() + category.getDisplayName());
        lore.add(ChatColor.GRAY + "Entries " + ChatColor.DARK_GRAY + "» " + ChatColor.WHITE + count);
        lore.add(ChatColor.DARK_GRAY + "─────────────────");
        lore.add(ChatColor.GRAY + "Right-click to open the codex.");
        lore.add(ChatColor.DARK_GRAY + "Hover, click to learn, click a level to apply.");
        return lore;
    }

    // ── Interactive (component) pages ─────────────────────────────────────────

    private static boolean writeComponentPages(BookMeta meta, EnchantCategory category, List<CustomEnchant> enchants) {
        try {
            BookMeta.Spigot spigot = meta.spigot();
            spigot.addPage(coverPage(category, enchants.size()));
            List<BaseComponent> page = new ArrayList<BaseComponent>();
            int onPage = 0;
            for (CustomEnchant enchant : enchants) {
                appendEnchant(page, enchant);
                if (++onPage >= PER_PAGE) {
                    spigot.addPage(page.toArray(new BaseComponent[0]));
                    page = new ArrayList<BaseComponent>();
                    onPage = 0;
                }
            }
            if (!page.isEmpty()) {
                spigot.addPage(page.toArray(new BaseComponent[0]));
            }
            return true;
        } catch (Throwable componentsUnavailable) {
            return false;
        }
    }

    private static BaseComponent[] coverPage(EnchantCategory category, int count) {
        List<BaseComponent> page = new ArrayList<BaseComponent>();
        plain(page, category.getColor().toString() + ChatColor.BOLD + ChatColor.stripColor(category.getDisplayName()) + "\n");
        plain(page, ChatColor.DARK_GRAY + "Arcanum Codex\n\n");
        plain(page, ChatColor.GRAY + "" + count + " enchantments,\nlisted A–Z.\n\n");
        plain(page, ChatColor.DARK_GRAY + "How to use:\n");
        plain(page, ChatColor.GRAY + "• Hover a name for\n  its details.\n");
        plain(page, ChatColor.GRAY + "• Click a name to\n  read more in chat.\n");
        plain(page, ChatColor.GRAY + "• Click a level to\n  apply it to your\n  held item.");
        return page.toArray(new BaseComponent[0]);
    }

    private static void appendEnchant(List<BaseComponent> page, CustomEnchant enchant) {
        ChatColor color = enchant.getCategory().getColor();
        String name = ChatColor.stripColor(enchant.plainName());
        // Clickable, hoverable name → prints full info in chat.
        interactive(page, color.toString() + ChatColor.BOLD + "❖ " + ChatColor.RESET + color + name,
                EnchantHover.tooltip(enchant, 0, null),
                "/obxench bookinfo " + enchant.getId());
        plain(page, "\n" + ChatColor.GRAY + "Apply: ");
        // One click-to-apply button per level.
        for (int level = 1; level <= enchant.getMaxLevel(); level++) {
            List<String> hover = new ArrayList<String>();
            hover.add(ChatColor.WHITE + "Apply " + color + name + ChatColor.GRAY + " " + CustomEnchant.roman(level));
            hover.add(ChatColor.GRAY + "to the item in your hand.");
            hover.add(ChatColor.DARK_GRAY + "Only upgrades to a higher level.");
            String params = describeLevel(enchant, level);
            if (!params.isEmpty()) {
                hover.add(ChatColor.DARK_GRAY + "────────────");
                hover.add(ChatColor.GRAY + params);
            }
            interactive(page, ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + CustomEnchant.roman(level) + ChatColor.DARK_GRAY + "]",
                    hover, "/obxench bookapply " + enchant.getId() + " " + level);
            plain(page, " ");
        }
        plain(page, "\n\n");
    }

    private static String describeLevel(CustomEnchant enchant, int level) {
        ConfigurationSection section = enchant.levelSection(level);
        if (section == null) {
            return "";
        }
        List<String> parts = new ArrayList<String>();
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("scroll")) {
                continue;
            }
            parts.add(key.replace('_', ' ') + " " + ChatColor.WHITE + section.get(key) + ChatColor.GRAY);
        }
        return parts.isEmpty() ? "" : String.join(ChatColor.DARK_GRAY + ", " + ChatColor.GRAY, parts);
    }

    // ── Plain-text fallback pages (no interactivity) ──────────────────────────

    private static List<String> plainPages(EnchantCategory category, List<CustomEnchant> enchants) {
        List<String> pages = new ArrayList<String>();
        StringBuilder cover = new StringBuilder();
        cover.append(category.getColor()).append(ChatColor.BOLD).append(ChatColor.stripColor(category.getDisplayName())).append("\n");
        cover.append(ChatColor.DARK_GRAY).append("Arcanum Codex\n\n");
        cover.append(ChatColor.GRAY).append(enchants.size()).append(" enchantments,\nlisted A–Z.\n\n");
        cover.append(ChatColor.GRAY).append("Use /obxench info\n<id> for details,\nor /obxench apply\n<id> <level>.");
        pages.add(cover.toString());

        StringBuilder page = new StringBuilder();
        int onPage = 0;
        for (CustomEnchant enchant : enchants) {
            page.append(enchant.getCategory().getColor()).append(ChatColor.BOLD)
                    .append("❖ ").append(ChatColor.RESET).append(enchant.getCategory().getColor())
                    .append(ChatColor.stripColor(enchant.plainName())).append("\n");
            page.append(ChatColor.GRAY).append("I–").append(CustomEnchant.roman(enchant.getMaxLevel()))
                    .append(ChatColor.DARK_GRAY).append(" • ").append(enchant.getRarity().getColor())
                    .append(enchant.getRarity().getDisplayName()).append("\n\n");
            if (++onPage >= PER_PAGE) {
                pages.add(page.toString());
                page = new StringBuilder();
                onPage = 0;
            }
        }
        if (page.length() > 0) {
            pages.add(page.toString());
        }
        return pages;
    }

    // ── Component helpers ──────────────────────────────────────────────────────

    private static void plain(List<BaseComponent> page, String legacyText) {
        Collections.addAll(page, TextComponent.fromLegacyText(legacyText));
    }

    private static void interactive(List<BaseComponent> page, String legacyText, List<String> hoverLines, String command) {
        BaseComponent[] parts = TextComponent.fromLegacyText(legacyText);
        HoverEvent hover = makeHover(hoverLines);
        ClickEvent click = command == null ? null : new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
        for (BaseComponent part : parts) {
            if (hover != null) {
                part.setHoverEvent(hover);
            }
            if (click != null) {
                part.setClickEvent(click);
            }
            page.add(part);
        }
    }

    @SuppressWarnings("deprecation")
    private static HoverEvent makeHover(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        BaseComponent[] hoverComponents = TextComponent.fromLegacyText(String.join("\n", lines));
        HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents);
        try {
            event.getClass().getMethod("setLegacy", boolean.class).invoke(event, true);
        } catch (Throwable ignored) {
            // setLegacy only exists on newer BungeeCord chat APIs; harmless when absent.
        }
        return event;
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    private static List<CustomEnchant> sortedByName(List<CustomEnchant> source) {
        List<CustomEnchant> list = new ArrayList<CustomEnchant>(source);
        Collections.sort(list, new Comparator<CustomEnchant>() {
            @Override
            public int compare(CustomEnchant a, CustomEnchant b) {
                return ChatColor.stripColor(a.plainName()).compareToIgnoreCase(ChatColor.stripColor(b.plainName()));
            }
        });
        return list;
    }

    private static Material matchOr(String name, Material fallback) {
        Material material = Material.matchMaterial(name);
        return material != null ? material : fallback;
    }

    private static void trySetGeneration(BookMeta meta) {
        try {
            Class<?> generation = Class.forName("org.bukkit.inventory.meta.BookMeta$Generation");
            Object original = Enum.valueOf(generation.asSubclass(Enum.class), "ORIGINAL");
            meta.getClass().getMethod("setGeneration", generation).invoke(meta, original);
        } catch (Throwable ignored) {
            // BookMeta.Generation is 1.10+; safe to skip on older servers.
        }
    }
}
