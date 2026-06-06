package dev.zcripted.obx.feature.economy.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.feature.economy.service.BanknoteService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Right-click redemption for banknote items. The token's guarded DELETE in
 * {@link BanknoteService#redeem} is the single point of truth — duplicated copies
 * (creative clone, etc.) all race for the same row and only one can ever win;
 * losers are recognised as void and removed.
 */
public final class BanknoteListener implements Listener {

    private final ObxPlugin plugin;
    private final BanknoteService notes;

    public BanknoteListener(ObxPlugin plugin, BanknoteService notes) {
        this.plugin = plugin;
        this.notes = notes;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = dev.zcripted.obx.util.compat.InventoryCompat.mainHand(player);
        String token = notes.itemToken(item);
        if (token == null) {
            return;
        }
        event.setCancelled(true);
        if (!player.hasPermission("obx.withdraw")) {
            plugin.getLanguageManager().send(player, "core.no-permission");
            return;
        }
        double value = notes.redeem(player, token);
        if (value == -2) {
            // Balance headroom refusal — the note stays valid in hand.
            plugin.getLanguageManager().send(player, "economy.note.full");
            return;
        }
        // Valid redeem OR void copy: the physical item is consumed either way
        // (a void note is worthless paper — clearing it kills clone clutter).
        consumeOne(player, item);
        EconomyService economy = plugin.getEconomyService();
        if (value < 0) {
            plugin.getLanguageManager().send(player, "economy.note.void");
            return;
        }
        plugin.getLanguageManager().send(player, "economy.note.redeemed", Placeholders.with(
                "amount", economy == null ? String.valueOf(value) : economy.format(value),
                "balance", economy == null ? "?" : economy.format(economy.getBalance(player.getUniqueId()))));
    }

    private static void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            dev.zcripted.obx.util.compat.InventoryCompat.setMainHand(player, item);
        } else {
            dev.zcripted.obx.util.compat.InventoryCompat.setMainHand(player, null);
        }
    }
}
