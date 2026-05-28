package dev.sergeantfuzzy.sfcore.enchant.listener;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.enchant.item.EnchantItems;
import dev.sergeantfuzzy.sfcore.enchant.item.ScrollKind;
import dev.sergeantfuzzy.sfcore.enchant.service.EnchantService;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Right-clicking a custom enchantment <b>scroll</b> or <b>book</b> in hand does nothing
 * (it's applied to an item via an anvil or drag-and-drop). This shows a brief action-bar
 * hint so players don't think the item is broken. The action bar fades on its own after
 * ~3 seconds, so no clear task is needed.
 *
 * <p>Only the enchant-carrying forms ({@link ScrollKind#BOOK} / {@link ScrollKind#ENCHANT_SCROLL})
 * are hinted — the Codex guide books (which are meant to be right-clicked to open) and the
 * utility scrolls (protection/success/extraction/transmutation) are left alone.
 */
public final class EnchantBookUseListener implements Listener {

    private final EnchantService service;
    private final EnchantItems items;
    private final LanguageManager languages;

    public EnchantBookUseListener(Main plugin) {
        this.service = plugin.getEnchantService();
        this.items = plugin.getEnchantItems();
        this.languages = plugin.getLanguageManager();
    }

    @EventHandler(ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        if (!service.isEnabled()) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        ScrollKind kind = items.kindOf(item);
        if (kind != ScrollKind.BOOK && kind != ScrollKind.ENCHANT_SCROLL) {
            return;
        }
        ComponentMessenger.sendActionBar(event.getPlayer(), languages.get(event.getPlayer(), "enchant.book.use-hint"));
    }
}
