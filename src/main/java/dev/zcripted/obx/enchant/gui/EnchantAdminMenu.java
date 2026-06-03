package dev.zcripted.obx.enchant.gui;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.enchant.item.EnchantItems;
import dev.zcripted.obx.enchant.item.ScrollKind;
import dev.zcripted.obx.enchant.model.CustomEnchant;
import dev.zcripted.obx.enchant.model.EnchantCategory;
import dev.zcripted.obx.enchant.model.ItemTag;
import dev.zcripted.obx.enchant.service.ApplyResult;
import dev.zcripted.obx.enchant.service.EnchantFeedback;
import dev.zcripted.obx.enchant.service.EnchantService;
import dev.zcripted.obx.enchant.util.Sounds;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Arcanum admin GUI (and its read-only player browse twin). Three screens:
 * the main console (category icons + scroll shortcuts), a paginated category
 * list, and a per-level selector. The level selector mirrors the design's click
 * matrix: left-click applies to the held item, shift-left gives a scroll,
 * right-click gives an enchanted book, shift-right gives a stack of scrolls —
 * each routed through {@link EnchantFeedback} for action-bar + chat + sound
 * validation feedback. In browse mode all give/apply actions are suppressed.
 */
public final class EnchantAdminMenu {

    private static final int SIZE = 54;
    private static final String DIVIDER = ChatColor.DARK_GRAY + "─────────────────";
    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int SLOT_PROTECTION = 29;
    private static final int SLOT_SUCCESS = 31;
    private static final int SLOT_EXTRACTION = 33;
    private static final int SLOT_HELD_INFO = 49;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREV = 48;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_CLOSE = 53;

    private final OBX plugin;
    private final EnchantService service;
    private final EnchantItems items;
    private final EnchantFeedback feedback;

    public EnchantAdminMenu(OBX plugin) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.items = plugin.getEnchantItems();
        this.feedback = plugin.getEnchantFeedback();
    }

    // ── Screen builders ───────────────────────────────────────────────────────

    public void open(Player player, boolean browse) {
        EnchantMenuHolder holder = new EnchantMenuHolder(EnchantMenuHolder.Screen.MAIN, browse);
        String title = ChatColor.DARK_PURPLE + "Arcanum " + ChatColor.DARK_GRAY + "— "
                + (browse ? ChatColor.GRAY + "Browse" : ChatColor.LIGHT_PURPLE + "Admin Console");
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);
        fill(inv);

        for (int i = 0; i < CATEGORY_SLOTS.length && i < EnchantCategory.values().length; i++) {
            EnchantCategory category = EnchantCategory.values()[i];
            int slot = CATEGORY_SLOTS[i];
            int count = service.getRegistry().byCategory(category).size();
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Enchantments: " + ChatColor.WHITE + count);
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click " + ChatColor.GRAY + "to browse this category.");
            inv.setItem(slot, icon(category.getIconMaterial(), "BOOK",
                    category.getColor() + "" + ChatColor.BOLD + category.getDisplayName(), lore));
            holder.bindCategory(slot, category);
        }

        if (!browse) {
            inv.setItem(SLOT_PROTECTION, scrollShortcut(ScrollKind.PROTECTION, ChatColor.AQUA));
            holder.bindAction(SLOT_PROTECTION, EnchantMenuHolder.Action.GIVE_PROTECTION);
            inv.setItem(SLOT_SUCCESS, scrollShortcut(ScrollKind.SUCCESS, ChatColor.GREEN));
            holder.bindAction(SLOT_SUCCESS, EnchantMenuHolder.Action.GIVE_SUCCESS);
            inv.setItem(SLOT_EXTRACTION, scrollShortcut(ScrollKind.EXTRACTION, ChatColor.GOLD));
            holder.bindAction(SLOT_EXTRACTION, EnchantMenuHolder.Action.GIVE_EXTRACTION);
        }

        inv.setItem(SLOT_HELD_INFO, heldInfo(player));
        inv.setItem(SLOT_CLOSE, closeButton());
        holder.bindAction(SLOT_CLOSE, EnchantMenuHolder.Action.CLOSE);
        player.openInventory(inv);
        Sounds.click(player);
    }

    public void openCategory(Player player, EnchantCategory category, int page, boolean browse) {
        List<CustomEnchant> all = service.getRegistry().byCategory(category);
        int pageSize = CONTENT_SLOTS.length;
        int maxPage = Math.max(1, (int) Math.ceil(all.size() / (double) pageSize));
        page = Math.max(1, Math.min(page, maxPage));

        EnchantMenuHolder holder = new EnchantMenuHolder(EnchantMenuHolder.Screen.CATEGORY, browse);
        holder.setCategory(category);
        holder.setPage(page);
        String title = category.getColor() + category.getDisplayName() + ChatColor.DARK_GRAY + " — "
                + ChatColor.GRAY + "Page " + page + "/" + maxPage;
        Inventory inv = Bukkit.createInventory(holder, SIZE, trim(title));
        holder.setInventory(inv);
        fill(inv);

        int start = (page - 1) * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int index = start + i;
            if (index >= all.size()) {
                break;
            }
            CustomEnchant enchant = all.get(index);
            int slot = CONTENT_SLOTS[i];
            inv.setItem(slot, enchantIcon(enchant, browse));
            holder.bindEnchant(slot, enchant.getId());
        }

        navAndFooter(player, inv, holder, page, maxPage);
        player.openInventory(inv);
        Sounds.click(player);
    }

    public void openLevels(Player player, CustomEnchant enchant, boolean browse) {
        EnchantMenuHolder holder = new EnchantMenuHolder(EnchantMenuHolder.Screen.LEVELS, browse);
        holder.setCategory(enchant.getCategory());
        holder.setEnchantId(enchant.getId());
        String title = ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName());
        Inventory inv = Bukkit.createInventory(holder, SIZE, trim(title));
        holder.setInventory(inv);
        fill(inv);

        int max = enchant.getMaxLevel();
        int firstSlot = 10 + Math.max(0, (7 - max) / 2);
        for (int level = 1; level <= max; level++) {
            int slot = firstSlot + (level - 1);
            if (slot > 16) {
                slot = 19 + (slot - 17); // wrap into the next inner row if many levels
            }
            inv.setItem(slot, levelIcon(enchant, level, browse));
            holder.bindLevel(slot, level);
        }

        // Back to the category list.
        inv.setItem(SLOT_BACK, namedIcon("ARROW", "STICK", ChatColor.YELLOW + "Back",
                line(ChatColor.GRAY + "Return to " + enchant.getCategory().getColor() + enchant.getCategory().getDisplayName())));
        holder.bindAction(SLOT_BACK, EnchantMenuHolder.Action.BACK);
        inv.setItem(SLOT_HELD_INFO, heldInfo(player));
        inv.setItem(SLOT_CLOSE, closeButton());
        holder.bindAction(SLOT_CLOSE, EnchantMenuHolder.Action.CLOSE);
        player.openInventory(inv);
        Sounds.click(player);
    }

    // ── Click routing (called by EnchantMenuListener) ─────────────────────────

    public void handleClick(Player player, EnchantMenuHolder holder, int rawSlot, ClickType click) {
        if (rawSlot < 0 || rawSlot >= SIZE) {
            return;
        }
        EnchantMenuHolder.Action action = holder.actionAt(rawSlot);
        switch (action) {
            case CLOSE:
                player.closeInventory();
                return;
            case BACK:
                if (holder.getScreen() == EnchantMenuHolder.Screen.LEVELS && holder.getCategory() != null) {
                    openCategory(player, holder.getCategory(), 1, holder.isBrowse());
                } else {
                    open(player, holder.isBrowse());
                }
                return;
            case PREV_PAGE:
                openCategory(player, holder.getCategory(), holder.getPage() - 1, holder.isBrowse());
                return;
            case NEXT_PAGE:
                openCategory(player, holder.getCategory(), holder.getPage() + 1, holder.isBrowse());
                return;
            case OPEN_CATEGORY:
                EnchantCategory category = holder.categoryAt(rawSlot);
                if (category != null) {
                    openCategory(player, category, 1, holder.isBrowse());
                }
                return;
            case OPEN_ENCHANT:
                CustomEnchant enchant = service.getRegistry().get(holder.enchantAt(rawSlot));
                if (enchant != null) {
                    openLevels(player, enchant, holder.isBrowse());
                }
                return;
            case GIVE_PROTECTION:
                giveUtility(player, holder, ScrollKind.PROTECTION);
                return;
            case GIVE_SUCCESS:
                giveUtility(player, holder, ScrollKind.SUCCESS);
                return;
            case GIVE_EXTRACTION:
                giveUtility(player, holder, ScrollKind.EXTRACTION);
                return;
            case APPLY_LEVEL:
                handleLevelClick(player, holder, rawSlot, click);
                return;
            default:
                Sounds.click(player);
        }
    }

    private void handleLevelClick(Player player, EnchantMenuHolder holder, int rawSlot, ClickType click) {
        if (holder.isBrowse()) {
            Sounds.click(player);
            return;
        }
        CustomEnchant enchant = service.getRegistry().get(holder.getEnchantId());
        int level = holder.levelAt(rawSlot);
        if (enchant == null || level <= 0) {
            return;
        }
        boolean shift = click.isShiftClick();
        boolean right = click.isRightClick();
        if (!shift && !right) {
            // Left-click → apply directly to held item.
            ItemStack inHand = mainHand(player);
            ApplyResult result = service.apply(inHand, enchant, level);
            feedback.send(player, result, inHand);
            if (result.isSuccess()) {
                setMainHand(player, inHand);
                holder.getInventory().setItem(SLOT_HELD_INFO, heldInfo(player));
            }
        } else if (shift && !right) {
            // Shift-left → give one scroll.
            giveOrWarn(player, items.scroll(enchant, level, 1));
        } else if (!shift) {
            // Right-click → give a traditional enchanted book.
            giveOrWarn(player, items.book(enchant, level, 1));
        } else {
            // Shift-right → give a stack of scrolls.
            giveOrWarn(player, items.scroll(enchant, level, 64));
        }
    }

    private void giveUtility(Player player, EnchantMenuHolder holder, ScrollKind kind) {
        if (holder.isBrowse()) {
            Sounds.click(player);
            return;
        }
        giveOrWarn(player, items.utility(kind, 1));
    }

    private void giveOrWarn(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            ComponentMessenger.sendActionBar(player, ChatColor.RED + "✖ Inventory full");
            plugin.getLanguageManager().send(player, "enchant.inventory-full");
            Sounds.error(player);
            return;
        }
        player.getInventory().addItem(item);
        player.updateInventory();
        Sounds.confirm(player);
    }

    // ── Footer / nav ──────────────────────────────────────────────────────────

    private void navAndFooter(Player player, Inventory inv, EnchantMenuHolder holder, int page, int maxPage) {
        inv.setItem(SLOT_BACK, namedIcon("ARROW", "STICK", ChatColor.YELLOW + "Back",
                line(ChatColor.GRAY + "Return to the main console.")));
        holder.bindAction(SLOT_BACK, EnchantMenuHolder.Action.BACK);
        if (page > 1) {
            inv.setItem(SLOT_PREV, namedIcon("PAPER", "PAPER", ChatColor.YELLOW + "Previous Page",
                    line(ChatColor.GRAY + "Go to page " + (page - 1))));
            holder.bindAction(SLOT_PREV, EnchantMenuHolder.Action.PREV_PAGE);
        }
        if (page < maxPage) {
            inv.setItem(SLOT_NEXT, namedIcon("PAPER", "PAPER", ChatColor.YELLOW + "Next Page",
                    line(ChatColor.GRAY + "Go to page " + (page + 1))));
            holder.bindAction(SLOT_NEXT, EnchantMenuHolder.Action.NEXT_PAGE);
        }
        inv.setItem(SLOT_HELD_INFO, heldInfo(player));
        inv.setItem(SLOT_CLOSE, closeButton());
        holder.bindAction(SLOT_CLOSE, EnchantMenuHolder.Action.CLOSE);
    }

    // ── Icon builders ───────────────────────────────────────────────────────

    private ItemStack enchantIcon(CustomEnchant enchant, boolean browse) {
        List<String> lore = new ArrayList<String>();
        lore.add(DIVIDER);
        for (String line : enchant.getDescription()) {
            lore.add(ChatColor.GRAY + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', line)));
        }
        lore.add(DIVIDER);
        lore.add(ChatColor.GRAY + "Rarity " + ChatColor.DARK_GRAY + "» " + enchant.getRarity().getColor() + enchant.getRarity().getDisplayName());
        lore.add(ChatColor.GRAY + "Max level " + ChatColor.DARK_GRAY + "» " + ChatColor.WHITE + "I–" + CustomEnchant.roman(enchant.getMaxLevel()));
        lore.add(ChatColor.GRAY + "Applies to " + ChatColor.DARK_GRAY + "» " + ChatColor.WHITE + ItemTag.describe(enchant.getTags()));
        lore.add(DIVIDER);
        lore.add(ChatColor.YELLOW + "Click " + ChatColor.GRAY + "to choose a level.");
        return icon("ENCHANTED_BOOK", "BOOK", ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()), lore);
    }

    private ItemStack levelIcon(CustomEnchant enchant, int level, boolean browse) {
        List<String> lore = new ArrayList<String>();
        lore.add(DIVIDER);
        org.bukkit.configuration.ConfigurationSection section = enchant.levelSection(level);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                lore.add(ChatColor.GRAY + key.replace('_', ' ') + " " + ChatColor.DARK_GRAY + "» " + ChatColor.WHITE + section.get(key));
            }
        }
        lore.add(DIVIDER);
        if (browse) {
            lore.add(ChatColor.DARK_GRAY + "View only.");
        } else {
            lore.add(ChatColor.YELLOW + "Left-click " + ChatColor.GRAY + "apply to held item");
            lore.add(ChatColor.YELLOW + "Shift-left " + ChatColor.GRAY + "get a scroll");
            lore.add(ChatColor.YELLOW + "Right-click " + ChatColor.GRAY + "get an enchanted book");
            lore.add(ChatColor.YELLOW + "Shift-right " + ChatColor.GRAY + "get 64 scrolls");
        }
        String name = ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()) + " " + ChatColor.GRAY + CustomEnchant.roman(level);
        return icon("PAPER", "PAPER", name, lore);
    }

    private ItemStack heldInfo(Player player) {
        ItemStack inHand = mainHand(player);
        List<String> lore = new ArrayList<String>();
        if (inHand == null || inHand.getType() == Material.AIR) {
            lore.add(ChatColor.GRAY + "You are not holding an item.");
        } else {
            lore.add(ChatColor.GRAY + "Item: " + ChatColor.WHITE + inHand.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' '));
            List<ItemTag> tags = new ArrayList<ItemTag>(ItemTag.tagsFor(inHand));
            lore.add(ChatColor.GRAY + "Matches: " + ChatColor.WHITE + (tags.isEmpty() ? "—" : ItemTag.describe(tags)));
            Map<String, Integer> current = service.getStorage().read(inHand);
            lore.add(ChatColor.GRAY + "Arcanum: " + ChatColor.WHITE + current.size()
                    + ChatColor.GRAY + " / " + ChatColor.WHITE + service.getMaxEnchantsPerItem());
        }
        return icon("ITEM_FRAME", "PAPER", ChatColor.AQUA + "Held Item", lore);
    }

    private ItemStack scrollShortcut(ScrollKind kind, ChatColor color) {
        return namedIcon("PAPER", "PAPER", color + "Give " + kind.getLabel(),
                line(ChatColor.GRAY + "Click to receive one " + kind.getLabel() + "."));
    }

    private ItemStack closeButton() {
        return namedIcon("BARRIER", "REDSTONE_BLOCK", ChatColor.RED + "" + ChatColor.BOLD + "Close",
                line(ChatColor.GRAY + "Close this menu."));
    }

    // ── Low-level helpers ───────────────────────────────────────────────────

    private void fill(Inventory inv) {
        ItemStack pane = namedIcon("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", " ", null);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane.clone());
        }
    }

    private ItemStack icon(String materialName, String fallback, String name, List<String> lore) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.matchMaterial(fallback);
        }
        if (material == null) {
            material = Material.BOOK;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            try {
                meta.addItemFlags(ItemFlag.values());
            } catch (Throwable ignored) {
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack namedIcon(String materialName, String fallback, String name, List<String> lore) {
        return icon(materialName, fallback, name, lore);
    }

    private static List<String> line(String text) {
        List<String> list = new ArrayList<String>();
        list.add(text);
        return list;
    }

    private static String trim(String title) {
        // Inventory titles are limited to 32 chars on legacy servers.
        return title.length() > 32 ? title.substring(0, 32) : title;
    }

    @SuppressWarnings("deprecation")
    private ItemStack mainHand(Player player) {
        try {
            return player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError legacy) {
            return player.getInventory().getItemInHand();
        }
    }

    @SuppressWarnings("deprecation")
    private void setMainHand(Player player, ItemStack item) {
        try {
            player.getInventory().setItemInMainHand(item);
        } catch (NoSuchMethodError legacy) {
            player.getInventory().setItemInHand(item);
        }
        player.updateInventory();
    }
}
