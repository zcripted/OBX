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
 * Banknote redemption — right-click, or via the crafting grid
 * ({@code economy.banknote.craft-redeem}): a grid holding ONLY banknotes shows
 * a gold-nugget "Redeem" result; clicking it cashes every note in one go.
 * Either way the token's guarded DELETE in {@link BanknoteService#redeem} is
 * the single point of truth — duplicated copies (creative clone, etc.) all
 * race for the same row and only one can ever win; losers are recognised as
 * void and removed.
 */
public final class BanknoteListener implements Listener {

    /** Hidden lore marker identifying the synthetic craft-redeem result item. */
    private static final String REDEEM_TAG = org.bukkit.ChatColor.DARK_GRAY + "obx:redeem";

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

    // ── Craft-grid redemption ────────────────────────────────────────────────

    /**
     * A grid holding ONLY banknotes previews a "Redeem {total}" result. Any
     * non-note item in the matrix means the craft isn't ours — leave vanilla
     * (and other plugins') recipes alone.
     */
    @EventHandler
    public void onPrepareCraft(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        if (!notes.craftRedeemEnabled()) {
            return;
        }
        org.bukkit.inventory.CraftingInventory inventory = event.getInventory();
        double total = 0.0;
        int count = 0;
        for (ItemStack item : inventory.getMatrix()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            String token = notes.itemToken(item);
            if (token == null) {
                return; // mixed grid — not a redemption
            }
            total += notes.tokenValue(token);
            count++;
        }
        if (count == 0) {
            return;
        }
        inventory.setResult(redeemIndicator(event.getView().getPlayer(), total, count));
    }

    /**
     * Clicking the redeem result cashes every note in the grid. Always
     * cancelled — money is paid by {@link BanknoteService#redeem}'s guarded
     * path, never by vanilla item movement; a wallet-headroom refusal stops
     * mid-way and leaves the remaining notes in the grid.
     */
    @EventHandler
    public void onCraft(org.bukkit.event.inventory.CraftItemEvent event) {
        if (!notes.craftRedeemEnabled() || !isRedeemIndicator(event.getInventory().getResult())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("obx.withdraw")) {
            plugin.getLanguageManager().send(player, "core.no-permission");
            return;
        }
        org.bukkit.inventory.CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        double total = 0.0;
        int redeemed = 0;
        boolean walletFull = false;
        for (int slot = 0; slot < matrix.length; slot++) {
            String token = notes.itemToken(matrix[slot]);
            if (token == null) {
                continue;
            }
            double value = notes.redeem(player, token);
            if (value == -2) {
                walletFull = true; // headroom refusal — this and later notes stay valid
                break;
            }
            matrix[slot] = null; // redeemed OR void copy — consumed either way
            if (value > 0) {
                total += value;
                redeemed++;
            }
        }
        inventory.setMatrix(matrix);
        inventory.setResult(null);
        EconomyService economy = plugin.getEconomyService();
        if (redeemed > 0) {
            plugin.getLanguageManager().send(player, "economy.note.craft-redeemed", Placeholders.with(
                    "count", redeemed,
                    "amount", economy == null ? String.valueOf(total) : economy.format(total),
                    "balance", economy == null ? "?"
                            : economy.format(economy.getBalance(player.getUniqueId()))));
        } else if (walletFull) {
            plugin.getLanguageManager().send(player, "economy.note.full");
        } else {
            plugin.getLanguageManager().send(player, "economy.note.void");
        }
    }

    /** Builds the synthetic gold-nugget result shown for a pure-banknote grid. */
    private ItemStack redeemIndicator(org.bukkit.entity.HumanEntity viewer, double total, int count) {
        Material nugget = Material.matchMaterial("GOLD_NUGGET");
        if (nugget == null) {
            nugget = Material.matchMaterial("GOLD_INGOT");
        }
        ItemStack result = new ItemStack(nugget == null ? Material.PAPER : nugget, 1);
        org.bukkit.inventory.meta.ItemMeta meta = result.getItemMeta();
        if (meta != null && viewer instanceof Player) {
            Player player = (Player) viewer;
            EconomyService economy = plugin.getEconomyService();
            String formatted = economy == null ? String.valueOf(total) : economy.format(total);
            meta.setDisplayName(plugin.getLanguageManager().get(player, "economy.note.craft-result.name",
                    java.util.Collections.singletonMap("amount", formatted)));
            java.util.List<String> lore = new java.util.ArrayList<>(plugin.getLanguageManager().list(
                    player, "economy.note.craft-result.lore",
                    Placeholders.with("amount", formatted, "count", count)));
            lore.add(REDEEM_TAG);
            meta.setLore(lore);
            result.setItemMeta(meta);
        }
        return result;
    }

    /** Whether {@code item} is the synthetic redeem result (carries {@link #REDEEM_TAG}). */
    private static boolean isRedeemIndicator(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getLore() != null && meta.getLore().contains(REDEEM_TAG);
    }
}