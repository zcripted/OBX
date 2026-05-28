package dev.sergeantfuzzy.sfcore.enchant.service;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.enchant.model.CustomEnchant;
import dev.sergeantfuzzy.sfcore.enchant.model.ItemTag;
import dev.sergeantfuzzy.sfcore.enchant.util.EnchantHover;
import dev.sergeantfuzzy.sfcore.enchant.util.Sounds;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Turns an {@link ApplyResult} into the two-channel feedback the design
 * mandates (§7.4): a transient action-bar notification <em>and</em> a full chat
 * explanation, plus a success/error sound. Every message routes through the
 * existing {@link LanguageManager} (EN + DE) under the {@code enchant.*} keys.
 */
public final class EnchantFeedback {

    private final LanguageManager languages;

    public EnchantFeedback(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    public void send(Player player, ApplyResult result, ItemStack item) {
        Map<String, String> ph = placeholders(result, item);
        String suffix = suffix(result.getStatus());
        String actionBar = languages.get(player, "enchant.actionbar." + suffix, ph);
        ComponentMessenger.sendActionBar(player, actionBar);

        // Chat line: make the enchant name hoverable with full details.
        String chatLine = languages.get(player, "enchant.apply." + suffix, ph);
        CustomEnchant enchant = result.getEnchant();
        if (enchant != null) {
            String itemName = result.isSuccess() ? itemName(item) : null;
            EnchantHover.send(player, chatLine, enchant, result.getLevel(), itemName);
        } else {
            player.sendMessage(chatLine);
        }
        if (result.getStatus() == ApplyStatus.UPGRADED) {
            Sounds.confirm(player);
            dev.sergeantfuzzy.sfcore.enchant.util.Particles.burst(player.getEyeLocation(), dev.sergeantfuzzy.sfcore.enchant.util.Particles.MAGIC);
        } else if (result.isSuccess()) {
            Sounds.success(player);
            dev.sergeantfuzzy.sfcore.enchant.util.Particles.burst(player.getEyeLocation(), dev.sergeantfuzzy.sfcore.enchant.util.Particles.MAGIC);
        } else {
            Sounds.error(player);
        }
    }

    private Map<String, String> placeholders(ApplyResult result, ItemStack item) {
        Map<String, String> ph = new LinkedHashMap<String, String>();
        CustomEnchant enchant = result.getEnchant();
        if (enchant != null) {
            ph.put("enchant", ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));
            ph.put("tags", ItemTag.describe(enchant.getTags()));
            ph.put("maxlevel", Integer.toString(enchant.getMaxLevel()));
        } else {
            ph.put("enchant", "?");
            ph.put("tags", "?");
            ph.put("maxlevel", "?");
        }
        ph.put("level", Integer.toString(result.getLevel()));
        ph.put("roman", CustomEnchant.roman(result.getLevel()));
        ph.put("oldlevel", Integer.toString(result.getPreviousLevel()));
        ph.put("oldroman", CustomEnchant.roman(result.getPreviousLevel()));
        ph.put("cap", Integer.toString(result.getCap()));
        ph.put("item", itemName(item));
        CustomEnchant conflict = result.getConflicting();
        ph.put("conflict", conflict == null ? "?" : ChatColor.translateAlternateColorCodes('&', conflict.getDisplayName()));
        return ph;
    }

    private static String itemName(ItemStack item) {
        if (item == null) {
            return "nothing";
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
        }
        String lower = item.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return lower;
    }

    private static String suffix(ApplyStatus status) {
        switch (status) {
            case APPLIED:
                return "applied";
            case UPGRADED:
                return "upgraded";
            case WRONG_TYPE:
                return "wrong-type";
            case EMPTY_HAND:
                return "empty-hand";
            case INVALID_LEVEL:
                return "invalid-level";
            case ALREADY_APPLIED:
                return "already-applied";
            case CONFLICT:
                return "conflict";
            case SLOT_CAP_REACHED:
                return "cap";
            case DISABLED:
            default:
                return "disabled";
        }
    }
}
