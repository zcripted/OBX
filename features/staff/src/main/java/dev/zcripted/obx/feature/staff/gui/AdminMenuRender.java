package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Generic, state-free rendering helpers for the admin menu family.
 *
 * <p>These used to live inside {@link AdminSubMenu}, which had grown past 1,400
 * lines. They have no dependency on any submenu's state — they only build items,
 * resolve cross-version materials, and place items into inventories — so they
 * are extracted here as a small, reusable toolkit. {@link AdminSubMenu} (and any
 * future admin menu) pulls them in via {@code import static
 * dev.zcripted.obx.feature.staff.gui.AdminMenuRender.*}, so existing call sites are
 * unchanged. Keeping them in one place also means the version-fallback material
 * lists (e.g. {@code GRAY_STAINED_GLASS_PANE} → {@code STAINED_GLASS_PANE}) have
 * a single home.</p>
 */
final class AdminMenuRender {

    private AdminMenuRender() {
    }

    static void fillWithFiller(Inventory inventory) {
        ItemStack filler = createPane(" ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler.clone());
        }
    }

    static ItemStack createPane(String name, List<String> lore) {
        Material material = resolveMaterial(new String[]{"GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "GLASS_PANE", "THIN_GLASS", "AIR"});
        boolean legacy = material != null && material.name().equalsIgnoreCase("STAINED_GLASS_PANE");
        ItemStack pane = legacy ? new ItemStack(material, 1, (short) 7) : new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            pane.setItemMeta(meta);
        }
        return pane;
    }

    static ItemStack createMenuItem(String[] materials, String name, List<String> lore) {
        ItemStack item = new ItemStack(resolveMaterial(materials));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            hideAttributes(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Suppresses vanilla attribute modifier lines (e.g. the attack damage/speed a
     * sword, trident, or axe icon would otherwise show) so menu icons stay clean.
     * Wrapped defensively because {@code HIDE_ATTRIBUTES} is absent on very old API
     * levels.
     */
    static void hideAttributes(ItemMeta meta) {
        if (meta == null) {
            return;
        }
        try {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        } catch (Throwable ignored) {
            // ItemFlag.HIDE_ATTRIBUTES unavailable on this server — leave as-is.
        }
    }

    static void place(Inventory inventory, int slot, ItemStack item) {
        if (inventory == null || item == null) {
            return;
        }
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
    }

    /** Resolves the OBX language manager from the providing plugin (this is a feature module). */
    private static LanguageManager languages() {
        return ((ObxPlugin) JavaPlugin.getProvidingPlugin(AdminMenuRender.class)).getLanguageManager();
    }

    private static ItemStack navItem(Player player, String[] materials, String nameKey, String loreKey) {
        LanguageManager lang = languages();
        ItemStack item = new ItemStack(resolveMaterial(materials));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang.get(player, nameKey));
            meta.setLore(lang.list(player, loreKey, java.util.Collections.<String, String>emptyMap()));
            hideAttributes(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack createBackItem(Player player) {
        return navItem(player, new String[]{"ARROW", "SPECTRAL_ARROW"}, "admin.gui.back.admin.name", "admin.gui.back.admin.lore");
    }

    static ItemStack createBackItemToServerControl(Player player) {
        return navItem(player, new String[]{"ARROW", "SPECTRAL_ARROW"}, "admin.gui.back.server-control.name", "admin.gui.back.server-control.lore");
    }

    static ItemStack createBackItemToWorldControls(Player player) {
        return navItem(player, new String[]{"ARROW", "SPECTRAL_ARROW"}, "admin.gui.back.world-controls.name", "admin.gui.back.world-controls.lore");
    }

    static ItemStack createCloseItem(Player player) {
        return navItem(player, new String[]{"BARRIER"}, "admin.gui.close.name", "admin.gui.close.lore");
    }

    static Material resolveMaterial(String[] names) {
        if (names == null) {
            return Material.AIR;
        }
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                return material;
            }
        }
        return Material.AIR;
    }

    static List<String> loreLines(String... lines) {
        return Arrays.asList(lines);
    }

    static String statusLine(String label, boolean enabled) {
        return ChatColor.GRAY + label + ": " + (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED");
    }

    static String valueLine(String label, Object value) {
        return ChatColor.GRAY + label + ": " + ChatColor.YELLOW + String.valueOf(value);
    }
}
