package dev.zcripted.obx.gui.admin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
 * dev.zcripted.obx.gui.admin.AdminMenuRender.*}, so existing call sites are
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
            item.setItemMeta(meta);
        }
        return item;
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

    static ItemStack createBackItem() {
        ItemStack back = new ItemStack(resolveMaterial(new String[]{"ARROW", "SPECTRAL_ARROW"}));
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.DARK_PURPLE + "Back to Admin Panel");
            backMeta.setLore(Arrays.asList(ChatColor.GRAY + "Return to the main Admin menu."));
            back.setItemMeta(backMeta);
        }
        return back;
    }

    static ItemStack createBackItemToServerControl() {
        ItemStack back = new ItemStack(resolveMaterial(new String[]{"ARROW", "SPECTRAL_ARROW"}));
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.DARK_PURPLE + "Back to Server Control");
            backMeta.setLore(Arrays.asList(ChatColor.GRAY + "Return to server control options."));
            back.setItemMeta(backMeta);
        }
        return back;
    }

    static ItemStack createBackItemToWorldControls() {
        ItemStack back = new ItemStack(resolveMaterial(new String[]{"ARROW", "SPECTRAL_ARROW"}));
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.DARK_PURPLE + "Back to World Controls");
            backMeta.setLore(Arrays.asList(ChatColor.GRAY + "Return to world controls."));
            back.setItemMeta(backMeta);
        }
        return back;
    }

    static ItemStack createCloseItem() {
        ItemStack close = new ItemStack(resolveMaterial(new String[]{"BARRIER"}));
        ItemMeta meta = close.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Close");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to close this menu"));
            close.setItemMeta(meta);
        }
        return close;
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
