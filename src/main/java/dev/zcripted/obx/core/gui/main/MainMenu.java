package dev.zcripted.obx.core.gui.main;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.gui.WarpMenuStyling;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class MainMenu {

    public static final int CORE_ITEM_SLOT = 4;
    public static final int WARP_ITEM_SLOT = 19;
    private static final int[] INFORMATION_SLOTS = {19, 21, 23, 25};

    private MainMenu() {
    }

    public static void open(ObxPlugin plugin, Player player) {
        MainMenuHolder holder = new MainMenuHolder();
        String title = ChatColor.DARK_PURPLE.toString() + ChatColor.BOLD + "OBX "
                + ChatColor.YELLOW.toString() + ChatColor.BOLD + "Menu";
        Inventory inventory = Bukkit.createInventory(holder, 36, title);
        holder.setInventory(inventory);

        ItemStack filler = createPane(" ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler.clone());
        }

        ItemStack coreItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta coreMeta = coreItem.getItemMeta();
        if (coreMeta != null) {
            coreMeta.setDisplayName(ChatColor.DARK_PURPLE + "OBX");
            coreMeta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "Developer: " + ChatColor.WHITE + "zcripted",
                    ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion()
            ));
            coreItem.setItemMeta(coreMeta);
        }
        inventory.setItem(CORE_ITEM_SLOT, coreItem);

        ItemStack warpsItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta warpsMeta = warpsItem.getItemMeta();
        if (warpsMeta != null) {
            warpsMeta.setDisplayName(WarpMenuStyling.gradientTitle("Warps"));
            warpsMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Browse and teleport to warps.",
                    "",
                    ChatColor.YELLOW + "Click:" + ChatColor.GRAY + " Open warp menu"
            ));
            warpsItem.setItemMeta(warpsMeta);
        }
        inventory.setItem(WARP_ITEM_SLOT, warpsItem);

        // Neutral decorative filler around the warp item — no customer-facing
        // "Coming Soon" tiles on a shipped build.
        ItemStack reservedPane = createPane(" ", java.util.Collections.<String>emptyList());
        for (int slot : INFORMATION_SLOTS) {
            if (slot == WARP_ITEM_SLOT) {
                continue;
            }
            inventory.setItem(slot, reservedPane.clone());
        }

        player.openInventory(inventory);
    }

    private static ItemStack createPane(String name, java.util.List<String> lore) {
        Material material = Material.matchMaterial("GRAY_STAINED_GLASS_PANE");
        if (material == null) {
            material = Material.matchMaterial("STAINED_GLASS_PANE");
        }
        boolean legacy = material != null && material == Material.matchMaterial("STAINED_GLASS_PANE");
        if (material == null) {
            material = Material.matchMaterial("GLASS_PANE");
        }
        if (material == null) {
            material = Material.matchMaterial("THIN_GLASS");
        }
        if (material == null) {
            material = Material.AIR;
        }
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
}
