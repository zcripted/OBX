package dev.zcripted.obx.feature.enchant.scroll;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.enchant.item.EnchantItems;
import dev.zcripted.obx.feature.enchant.model.CustomEnchant;
import dev.zcripted.obx.feature.enchant.model.EnchantCategory;
import dev.zcripted.obx.feature.enchant.service.ApplyResult;
import dev.zcripted.obx.feature.enchant.service.EnchantFeedback;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.enchant.service.ScrollSettings;
import dev.zcripted.obx.feature.enchant.util.Sounds;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The brains of the scroll system: applying enchant scrolls/books to items
 * (success roll, protection, destroy-on-failure, XP cost), imbuing protection /
 * success scrolls onto enchant scrolls, and the extraction / transmutation
 * utility actions. The owning listener handles the physical item consumption
 * based on the returned {@link ScrollResult}.
 */
public final class ScrollApplyService {

    private final OBX plugin;
    private final EnchantService service;
    private final EnchantItems items;
    private final EnchantFeedback feedback;
    private final LanguageManager languages;

    public ScrollApplyService(OBX plugin) {
        this.plugin = plugin;
        this.service = plugin.getEnchantService();
        this.items = plugin.getEnchantItems();
        this.feedback = plugin.getEnchantFeedback();
        this.languages = plugin.getLanguageManager();
    }

    /**
     * Attempts to apply an enchant scroll or book ({@code scroll}) to
     * {@code target}. Mutates {@code target} in place on success. The XP cost
     * differs for the anvil vs. the convenience drag-and-drop path.
     */
    public ScrollResult applyScroll(Player player, ItemStack target, ItemStack scroll, boolean anvil) {
        CustomEnchant enchant = items.payloadEnchant(scroll);
        int level = items.payloadLevel(scroll);
        if (enchant == null || level <= 0) {
            return ScrollResult.UTILITY_FAILED;
        }
        // Validate against a clone so a rejection doesn't half-modify the item.
        ApplyResult probe = service.apply(target.clone(), enchant, level);
        if (!probe.isSuccess()) {
            feedback.send(player, probe, target);
            return ScrollResult.REJECTED;
        }

        ScrollSettings settings = service.getScrollSettings();
        int cost = anvil ? settings.anvilXpCost(enchant.getRarity()) : settings.dragDropXpCost(enchant.getRarity());
        if (!hasXp(player, cost)) {
            languages.send(player, "enchant.scroll.no-xp", one("cost", Integer.toString(cost)));
            ComponentMessenger.sendActionBar(player, ChatColor.RED + "✖ Need " + cost + " XP levels");
            Sounds.error(player);
            return ScrollResult.NO_XP;
        }

        double rate = settings.successRate(enchant.getRarity());
        if (items.isBoosted(scroll)) {
            rate += settings.getSuccessBoost();
        }
        chargeXp(player, cost);

        boolean success = Math.random() < Math.min(1.0, rate);
        if (success) {
            ApplyResult result = service.apply(target, enchant, level);
            feedback.send(player, result, target);
            return result.getStatus() == dev.zcripted.obx.feature.enchant.service.ApplyStatus.UPGRADED
                    ? ScrollResult.UPGRADED : ScrollResult.APPLIED;
        }

        // Failure handling.
        Map<String, String> ph = enchantPlaceholders(enchant, level);
        if (items.isProtected(scroll) && Math.random() < settings.getProtectSaveChance()) {
            languages.send(player, "enchant.scroll.failed-protected", ph);
            ComponentMessenger.sendActionBar(player, ChatColor.YELLOW + "✖ Failed — item protected");
            Sounds.error(player);
            return ScrollResult.FAILED_PROTECTED;
        }
        if (settings.isDestroyOnFailure()) {
            languages.send(player, "enchant.scroll.failed-destroyed", ph);
            ComponentMessenger.sendActionBar(player, ChatColor.RED + "✖ Failed — item destroyed");
            Sounds.error(player);
            return ScrollResult.FAILED_DESTROYED;
        }
        languages.send(player, "enchant.scroll.failed-kept", ph);
        ComponentMessenger.sendActionBar(player, ChatColor.RED + "✖ Application failed");
        Sounds.error(player);
        return ScrollResult.FAILED_KEPT;
    }

    /** Merges a Protection or Success scroll onto an enchant scroll (drag-drop). */
    public ScrollResult imbue(Player player, ItemStack enchantScroll, boolean protection) {
        if (items.payloadEnchant(enchantScroll) == null) {
            return ScrollResult.UTILITY_FAILED;
        }
        if (protection) {
            if (items.isProtected(enchantScroll)) {
                return ScrollResult.UTILITY_FAILED;
            }
            items.markProtected(enchantScroll);
            languages.send(player, "enchant.scroll.imbued-protect");
        } else {
            if (items.isBoosted(enchantScroll)) {
                return ScrollResult.UTILITY_FAILED;
            }
            items.markBoosted(enchantScroll);
            languages.send(player, "enchant.scroll.imbued-boost");
        }
        Sounds.confirm(player);
        return ScrollResult.UTILITY_OK;
    }

    /**
     * Extraction: removes one enchantment from {@code target} and gives the player
     * a scroll for it at a reduced level. Mutates {@code target}.
     */
    public ScrollResult extract(Player player, ItemStack target) {
        Map<String, Integer> present = service.getStorage().read(target);
        CustomEnchant removable = firstRemovable(present);
        if (removable == null) {
            languages.send(player, "enchant.scroll.no-enchants");
            Sounds.error(player);
            return ScrollResult.UTILITY_FAILED;
        }
        int level = present.get(removable.getId());
        service.getStorage().remove(target, removable.getId());
        int newLevel = Math.max(1, level - service.getScrollSettings().getExtractionLevelPenalty());
        giveOrDrop(player, items.scroll(removable, newLevel, 1));
        Map<String, String> ph = enchantPlaceholders(removable, newLevel);
        languages.send(player, "enchant.scroll.extracted", ph);
        Sounds.confirm(player);
        return ScrollResult.UTILITY_OK;
    }

    /**
     * Transmutation: re-rolls one enchantment on {@code target} into a different
     * enchantment of the same category that fits the item. Mutates {@code target}.
     */
    public ScrollResult transmute(Player player, ItemStack target) {
        Map<String, Integer> present = service.getStorage().read(target);
        if (present.isEmpty()) {
            languages.send(player, "enchant.scroll.no-enchants");
            Sounds.error(player);
            return ScrollResult.UTILITY_FAILED;
        }
        List<String> ids = new ArrayList<String>(present.keySet());
        String chosenId = ids.get((int) (Math.random() * ids.size()));
        CustomEnchant current = service.getRegistry().get(chosenId);
        if (current == null) {
            return ScrollResult.UTILITY_FAILED;
        }
        int level = present.get(chosenId);
        EnchantCategory category = current.getCategory();

        List<CustomEnchant> candidates = new ArrayList<CustomEnchant>();
        for (CustomEnchant candidate : service.getRegistry().byCategory(category)) {
            if (candidate.getId().equals(chosenId)) {
                continue;
            }
            if (!dev.zcripted.obx.feature.enchant.model.ItemTag.matchesAny(target, candidate.getTags())) {
                continue;
            }
            if (service.findConflict(target, candidate) != null) {
                continue;
            }
            candidates.add(candidate);
        }
        if (candidates.isEmpty()) {
            languages.send(player, "enchant.scroll.transmute-failed");
            Sounds.error(player);
            return ScrollResult.UTILITY_FAILED;
        }
        CustomEnchant replacement = candidates.get((int) (Math.random() * candidates.size()));
        int newLevel = Math.min(replacement.getMaxLevel(), Math.max(1, level));
        service.getStorage().remove(target, chosenId);
        service.getStorage().apply(target, replacement, newLevel);

        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("old", ChatColor.translateAlternateColorCodes('&', current.getDisplayName()));
        ph.put("enchant", ChatColor.translateAlternateColorCodes('&', replacement.getDisplayName()));
        ph.put("roman", CustomEnchant.roman(newLevel));
        languages.send(player, "enchant.scroll.transmuted", ph);
        Sounds.success(player);
        return ScrollResult.UTILITY_OK;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CustomEnchant firstRemovable(Map<String, Integer> present) {
        for (Map.Entry<String, Integer> entry : present.entrySet()) {
            CustomEnchant enchant = service.getRegistry().get(entry.getKey());
            if (enchant == null) {
                continue;
            }
            if (enchant.levelBoolean(entry.getValue(), "no_remove", false)) {
                continue; // Curse of the Bound and similar can't be extracted.
            }
            return enchant;
        }
        return null;
    }

    private boolean hasXp(Player player, int cost) {
        return player.getGameMode() == GameMode.CREATIVE || player.getLevel() >= cost;
    }

    private void chargeXp(Player player, int cost) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        player.giveExpLevels(-cost);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.updateInventory();
    }

    private Map<String, String> enchantPlaceholders(CustomEnchant enchant, int level) {
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("enchant", ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));
        ph.put("roman", CustomEnchant.roman(level));
        return ph;
    }

    private static Map<String, String> one(String key, String value) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(key, value);
        return map;
    }
}
