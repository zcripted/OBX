package dev.zcripted.obx.feature.mail.pm.gui;

import dev.zcripted.obx.feature.mail.pm.InboxMessage;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The inbox GUI: a 54-slot menu with a glass-pane border and message-preview items in
 * the inner 4×7 area (newest first). Unread and read messages use different materials;
 * bookmarked messages glow and carry a star. Left-click reads (and marks read),
 * right-click deletes, and shift-click toggles a bookmark. The bottom row carries a
 * "Clear" button that removes every non-bookmarked message.
 */
public final class InboxMenu {

    private static final int SIZE = 54;
    private static final int PREVIEW_CHARS = 28;
    /** Bottom-row slot holding the Clear Inbox button. */
    public static final int CLEAR_SLOT = 49;
    private static final int[] CONTENT = contentSlots();

    private InboxMenu() {
    }

    public static void open(ObxPlugin plugin, Player player) {
        LanguageManager languages = plugin.getLanguageManager();
        List<InboxMessage> messages = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.pm.PrivateMessageService.class).inbox(player.getUniqueId());

        InboxMenuHolder holder = new InboxMenuHolder();
        String title = ChatColor.translateAlternateColorCodes('&',
                languages.get(player, "inbox.title", Collections.singletonMap("count", Integer.toString(messages.size()))));
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        Inventory inventory = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inventory);

        ItemStack filler = pane();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }
        if (messages.isEmpty()) {
            inventory.setItem(22, emptyItem(languages, player));
        } else {
            for (int i = 0; i < messages.size() && i < CONTENT.length; i++) {
                InboxMessage message = messages.get(messages.size() - 1 - i); // newest first
                int slot = CONTENT[i];
                inventory.setItem(slot, previewItem(languages, player, message));
                holder.map(slot, message);
            }
        }
        inventory.setItem(CLEAR_SLOT, clearButton(languages, player));
        player.openInventory(inventory);
    }

    private static ItemStack previewItem(LanguageManager languages, Player viewer, InboxMessage message) {
        boolean read = message.isRead();
        boolean bookmarked = message.isBookmarked();

        Material material = read
                ? matchOr("BOOK", Material.PAPER)
                : matchOr("WRITABLE_BOOK", matchOr("BOOK_AND_QUILL", Material.PAPER));
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String envelope = read ? "&7✉" : "&e✉";       // ✉ open vs sealed
        String star = bookmarked ? "&5★ " : "";              // ★ bookmark marker
        String nameColor = read ? "&7" : "&f";
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                star + envelope + " " + nameColor + message.getSenderName()));

        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8" + message.dateLabel() + "  ·  " + message.timeLabel()));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8──────────────"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7" + preview(message.getText())));
        lore.add("");
        String statusKey = bookmarked ? "inbox.entry.bookmarked" : (read ? "inbox.entry.read" : "inbox.entry.unread");
        lore.add(languages.get(viewer, statusKey));
        lore.add("");
        lore.add(languages.get(viewer, "inbox.entry.click-read"));
        lore.add(languages.get(viewer, "inbox.entry.click-delete"));
        lore.add(languages.get(viewer, bookmarked ? "inbox.entry.click-unbookmark" : "inbox.entry.click-bookmark"));
        meta.setLore(lore);
        if (bookmarked) {
            applyGlow(meta);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack clearButton(LanguageManager languages, Player viewer) {
        ItemStack item = new ItemStack(matchOr("LAVA_BUCKET", matchOr("BARRIER", Material.BUCKET)), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(languages.get(viewer, "inbox.clear-name"));
            List<String> lore = new ArrayList<String>();
            lore.add(languages.get(viewer, "inbox.clear-lore1"));
            lore.add(languages.get(viewer, "inbox.clear-lore2"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void applyGlow(ItemMeta meta) {
        try {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } catch (Throwable ignored) {
            // Glow is cosmetic — older/odd forks just show the item without a glint.
        }
    }

    private static String preview(String text) {
        String clean = ChatColor.stripColor(text == null ? "" : text);
        if (clean.length() <= PREVIEW_CHARS) {
            return clean;
        }
        return clean.substring(0, PREVIEW_CHARS) + "…";
    }

    private static ItemStack emptyItem(LanguageManager languages, Player viewer) {
        ItemStack item = new ItemStack(matchOr("BOOK", Material.BOOK), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(languages.get(viewer, "inbox.empty"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack pane() {
        ItemStack pane = new ItemStack(matchOr("GRAY_STAINED_GLASS_PANE", matchOr("STAINED_GLASS_PANE", Material.GLASS)), 1);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static Material matchOr(String name, Material fallback) {
        Material material = Material.matchMaterial(name);
        return material != null ? material : fallback;
    }

    /** The inner 4×7 content slots of a 54-slot inventory (rows 1–4, columns 1–7). */
    private static int[] contentSlots() {
        int[] slots = new int[28];
        int index = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[index++] = row * 9 + col;
            }
        }
        return slots;
    }
}