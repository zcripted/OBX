package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.warp.gui.WarpMenuStyling;
import dev.zcripted.obx.core.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Player-action sub-menu for the {@code /staff} GUI. Three rows / 27 slots:
 * the middle row carries five placeholder action buttons (warn / mute / kick
 * / tempban / ban) centered across slots 11 – 15; the bottom row reserves
 * slots for the back-to-main and red-X close buttons. Action click handlers
 * are intentionally placeholders for this drop — the buttons render the
 * correct labels and report which player is selected, but no moderation
 * action fires yet.
 */
public final class StaffActionMenu {

    public static final int INVENTORY_SIZE = 27;
    public static final int SLOT_WARN = 11;
    public static final int SLOT_MUTE = 12;
    public static final int SLOT_KICK = 13;
    public static final int SLOT_TEMPBAN = 14;
    public static final int SLOT_BAN = 15;
    public static final int SLOT_BACK = 18;
    public static final int SLOT_CLOSE = 26;

    private StaffActionMenu() {
    }

    public static void open(OBX plugin, Player viewer, UUID targetUuid, String fallbackName) {
        LanguageManager languages = plugin.getLanguageManager();
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = target.getName() != null ? target.getName() : fallbackName;

        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("player", targetName == null ? "?" : targetName);

        StaffActionMenuHolder holder = new StaffActionMenuHolder(targetUuid, targetName);
        String title = languages.get(viewer, "admin.staff.action.title", replacements);
        if (title == null || title.isEmpty()) {
            title = WarpMenuStyling.gradientTitle("Actions: " + (targetName == null ? "?" : targetName));
        }
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inventory);

        ItemStack filler = WarpMenuStyling.createFiller();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, filler.clone());
        }

        inventory.setItem(SLOT_WARN, buildAction(viewer, languages, "warn",
                resolveMaterial("PAPER"), replacements));
        inventory.setItem(SLOT_MUTE, buildAction(viewer, languages, "mute",
                resolveMaterial("BOOK"), replacements));
        inventory.setItem(SLOT_KICK, buildAction(viewer, languages, "kick",
                resolveMaterial("LEATHER_BOOTS", "IRON_BOOTS"), replacements));
        inventory.setItem(SLOT_TEMPBAN, buildAction(viewer, languages, "tempban",
                resolveMaterial("CLOCK", "WATCH"), replacements));
        inventory.setItem(SLOT_BAN, buildAction(viewer, languages, "ban",
                resolveMaterial("REDSTONE_BLOCK", "TNT"), replacements));

        ItemStack backItem = buildBack(viewer, languages);
        inventory.setItem(SLOT_BACK, backItem);

        ItemStack closeItem = StaffMenu.buildCloseHead(viewer, languages);
        inventory.setItem(SLOT_CLOSE, closeItem);

        viewer.openInventory(inventory);
    }

    private static ItemStack buildAction(Player viewer, LanguageManager languages, String action,
                                         Material material, Map<String, String> replacements) {
        String name = languages.get(viewer, "admin.staff.action." + action + ".name", replacements);
        if (name == null || name.isEmpty()) {
            name = ChatColor.YELLOW + action.substring(0, 1).toUpperCase() + action.substring(1);
        }
        List<String> lore = languages.list(viewer, "admin.staff.action." + action + ".lore", replacements);
        return WarpMenuStyling.item(material, colorize(name), colorizeLines(lore));
    }

    private static ItemStack buildBack(Player viewer, LanguageManager languages) {
        Material material = resolveMaterial("ARROW", "FEATHER");
        String name = languages.get(viewer, "admin.staff.back.name");
        if (name == null || name.isEmpty()) {
            name = ChatColor.YELLOW + "Back";
        }
        List<String> lore = languages.list(viewer, "admin.staff.back.lore", null);
        return WarpMenuStyling.item(material, colorize(name), colorizeLines(lore));
    }

    private static Material resolveMaterial(String preferred, String... fallbacks) {
        return WarpMenuStyling.resolveMaterial(preferred, fallbacks);
    }

    private static String colorize(String line) {
        if (line == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', line);
    }

    private static List<String> colorizeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.List<String> colored = new java.util.ArrayList<>();
        for (String line : lines) {
            colored.add(colorize(line));
        }
        return colored;
    }
}
