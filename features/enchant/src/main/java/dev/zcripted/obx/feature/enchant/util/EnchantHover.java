package dev.zcripted.obx.feature.enchant.util;

import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.model.ItemTag;
import dev.zcripted.obx.feature.enchant.storage.EnchantStorage;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the chat hover tooltip shown when a custom enchantment's name appears in
 * a message, and sends a line with that name made hoverable.
 *
 * <p>The tooltip lists: name, category, rarity, level (the applied level when
 * known, else the max level), the item it was applied to (when known), the valid
 * item types, and the description. Sending splits the resolved line on the colored
 * enchant name so only the name carries the hover; if the name can't be located
 * (e.g. a message that doesn't contain it) it falls back to a whole-line hover,
 * and {@link ComponentMessenger} downgrades to plain text for console senders.
 */
public final class EnchantHover {

    private EnchantHover() {
    }

    /**
     * @param level    applied level, or {@code <= 0} to show "Max Level" instead
     * @param itemName name of the item it was applied to, or {@code null} to omit
     */
    public static List<String> tooltip(CustomEnchant enchant, int level, String itemName) {
        List<String> lines = new ArrayList<String>();
        ChatColor categoryColor = enchant.getCategory().getColor();
        lines.add(categoryColor + ChatColor.stripColor(enchant.plainName()));
        lines.add(ChatColor.GRAY + "Category: " + categoryColor + enchant.getCategory().getDisplayName());
        lines.add(ChatColor.GRAY + "Rarity: " + enchant.getRarity().getColor() + enchant.getRarity().getDisplayName());
        ChatColor rarityColor = enchant.getRarity().getColor();
        int maxLevel = enchant.getMaxLevel();
        if (level > 0) {
            // Mirror the item-lore tier bar (●/○ in rarity color) and add an RPG "Lvl N" label.
            lines.add(ChatColor.GRAY + "Level " + ChatColor.DARK_GRAY + "» "
                    + EnchantStorage.levelDots(level, maxLevel, rarityColor)
                    + " " + ChatColor.WHITE + "Lvl " + level + ChatColor.GRAY + "/" + maxLevel);
        } else {
            lines.add(ChatColor.GRAY + "Max Level " + ChatColor.DARK_GRAY + "» "
                    + EnchantStorage.levelDots(maxLevel, maxLevel, rarityColor)
                    + " " + ChatColor.WHITE + "Lvl " + maxLevel);
        }
        if (itemName != null && !itemName.isEmpty()) {
            lines.add(ChatColor.GRAY + "Applied to: " + ChatColor.WHITE + ChatColor.stripColor(itemName));
        }
        lines.add(ChatColor.GRAY + "Applies to: " + ChatColor.WHITE + ItemTag.describe(enchant.getTags()));
        if (!enchant.getDescription().isEmpty()) {
            lines.add(ChatColor.DARK_GRAY + "────────────");
            for (String line : enchant.getDescription()) {
                lines.add(ChatColor.GRAY + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', line)));
            }
        }
        return lines;
    }

    /**
     * Sends {@code resolvedLine} with the (already color-translated) {@code enchantName}
     * portion carrying the given hover tooltip.
     */
    public static void send(CommandSender sender, String resolvedLine, String enchantName, List<String> hover) {
        if (sender == null || resolvedLine == null) {
            return;
        }
        int index = (enchantName == null || enchantName.isEmpty()) ? -1 : resolvedLine.indexOf(enchantName);
        if (index < 0) {
            // Name not found in the line — hover the whole line instead.
            ComponentMessenger.sendHoverMessage(sender, resolvedLine, hover, null);
            return;
        }
        String before = resolvedLine.substring(0, index);
        String after = resolvedLine.substring(index + enchantName.length());
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<ComponentMessenger.InteractiveMessagePart>();
        if (!before.isEmpty()) {
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(before));
        }
        parts.add(ComponentMessenger.InteractiveMessagePart.interactive(enchantName, hover, null, false));
        if (!after.isEmpty()) {
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(after));
        }
        ComponentMessenger.sendJoinedHoverMessages(sender, parts);
    }

    /** Convenience: build the tooltip and send the line in one call. */
    public static void send(CommandSender sender, String resolvedLine, CustomEnchant enchant, int level, String itemName) {
        String name = ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName());
        send(sender, resolvedLine, name, tooltip(enchant, level, itemName));
    }
}
