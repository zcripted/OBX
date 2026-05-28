package dev.sergeantfuzzy.sfcore.gui.player;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.hub.HubService;
import dev.sergeantfuzzy.sfcore.hub.messaging.BungeeMessenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Player-facing server selector inventory. Layout is driven by
 * {@code systems/hub.yml} → {@code selector.servers}. Live online counts are
 * requested via {@link BungeeMessenger} a few ticks before the inventory
 * opens so the values are reasonably fresh — subsequent re-opens within the
 * cache TTL re-use the cached values without re-querying.
 */
public final class ServerSelectorMenu {

    private static final int AUTO_FIRST_SLOT = 10;

    private ServerSelectorMenu() {
    }

    /**
     * Opens the selector inventory immediately and (if a proxy messenger is
     * available) fires off a PlayerCount request in the background — the live
     * counts populate from the cache on the next open. Opening synchronously on
     * the interact tick keeps the menu instant with no delayed window between the
     * click and the inventory appearing.
     */
    public static void open(Main plugin, Player player) {
        HubService hub = plugin.getHubService();
        BungeeMessenger messenger = plugin.getBungeeMessenger();
        if (hub == null) {
            return;
        }

        List<HubService.ServerEntry> entries = hub.getSelectorServers();
        if (messenger != null) {
            List<String> ids = new ArrayList<>(entries.size());
            for (HubService.ServerEntry entry : entries) {
                ids.add(entry.getId());
            }
            messenger.requestCounts(player, ids);
        }
        renderAndOpen(plugin, player, hub, messenger, entries);
    }

    private static void renderAndOpen(Main plugin, Player player, HubService hub, BungeeMessenger messenger,
                                      List<HubService.ServerEntry> entries) {
        if (!player.isOnline()) {
            return;
        }

        int rows = hub.selectorRows();
        int size = Math.max(9, rows * 9);
        String title = translate(hub.selectorTitle());
        ServerSelectorHolder holder = new ServerSelectorHolder();
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        if (hub.selectorOutlineEnabled()) {
            ItemStack outline = createOutline(hub);
            for (int slot = 0; slot < size; slot++) {
                if (isOutlineSlot(slot, size)) {
                    inventory.setItem(slot, outline.clone());
                }
            }
        }

        Set<Integer> usedSlots = new HashSet<>();
        int autoSlot = AUTO_FIRST_SLOT;
        for (HubService.ServerEntry entry : entries) {
            int slot = entry.getSlot();
            if (slot < 0 || slot >= size) {
                while (autoSlot < size && (usedSlots.contains(autoSlot) || isOutlineSlot(autoSlot, size))) {
                    autoSlot++;
                }
                if (autoSlot >= size) {
                    break;
                }
                slot = autoSlot;
                autoSlot++;
            }
            usedSlots.add(slot);
            inventory.setItem(slot, buildServerIcon(plugin, hub, messenger, entry));
            holder.bindSlot(slot, entry.getId());
        }

        if (hub.selectorCloseEnabled()) {
            int closeSlot = hub.selectorCloseSlot();
            if (closeSlot < 0 || closeSlot >= size) {
                closeSlot = size - 5; // bottom-center
            }
            // Never clobber a server icon — only place the close button on a free/outline slot.
            if (!usedSlots.contains(closeSlot)) {
                inventory.setItem(closeSlot, buildCloseButton(plugin, player, hub));
                holder.setCloseSlot(closeSlot);
            }
        }

        player.openInventory(inventory);
    }

    private static ItemStack buildCloseButton(Main plugin, Player player, HubService hub) {
        Material material = hub.selectorCloseMaterial();
        if (material == null || material == Material.AIR) {
            material = Material.STONE;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        // Name/lore are language-driven (hub.selector.close.*) and already color-translated by the LanguageManager.
        meta.setDisplayName(plugin.getLanguageManager().get(player, "hub.selector.close.name"));
        meta.setLore(plugin.getLanguageManager().list(player, "hub.selector.close.lore", Collections.<String, String>emptyMap()));
        try {
            meta.addItemFlags(ItemFlag.values());
        } catch (Throwable ignored) {
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack buildServerIcon(Main plugin, HubService hub, BungeeMessenger messenger,
                                             HubService.ServerEntry entry) {
        Material material = entry.getMaterial();
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(translate(entry.getDisplayName()));

        List<String> lore = new ArrayList<>();
        for (String line : entry.getLore()) {
            lore.add(translate(line));
        }
        lore.add("");
        int online = messenger == null ? -1 : messenger.cachedOnline(entry.getId());
        if (online < 0) {
            lore.add(ChatColor.GRAY + "Online: " + ChatColor.YELLOW + "?");
        } else {
            lore.add(ChatColor.GRAY + "Online: " + ChatColor.GREEN + online);
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "› " + ChatColor.WHITE + "Click to connect");
        meta.setLore(lore);

        try {
            meta.addItemFlags(ItemFlag.values());
        } catch (Throwable ignored) {
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack createOutline(HubService hub) {
        Material material = hub.selectorOutlineMaterial();
        if (material == null || material == Material.AIR) {
            return new ItemStack(Material.AIR);
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            try {
                meta.addItemFlags(ItemFlag.values());
            } catch (Throwable ignored) {
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static boolean isOutlineSlot(int slot, int size) {
        int rows = size / 9;
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

    private static String translate(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
