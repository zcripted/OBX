package dev.zcripted.obx.feature.economy.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.feature.economy.service.WorthService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellCommand extends AbstractObxCommand implements TabCompleter {

    private final EconomyService economy;
    private final WorthService worth;

    public SellCommand(ObxPlugin plugin) {
        super(plugin);
        this.economy = plugin.getEconomyService();
        this.worth = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.service.WorthService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.sell")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "economy.sell.usage");
            return true;
        }
        String mode = args[0].toLowerCase();
        if (mode.equals("hand")) {
            return sellHand(player);
        }
        if (mode.equals("all")) {
            return sellAll(player);
        }
        Material material;
        try {
            material = Material.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            languages.send(player, "economy.sell.unknown-material", Placeholders.with("material", mode));
            return true;
        }
        return sellMaterial(player, material);
    }

    /**
     * Pre-pay guards shared by every sell path, checked BEFORE items are removed so a
     * refusal never costs the player anything: (1) the per-player daily sell cap
     * ({@code economy.sell.daily-cap}), (2) MAX_BALANCE headroom — refusing beats the
     * old behaviour of silently clamping the overflow away.
     */
    private boolean blockedBySellGuards(Player player, double total) {
        dev.zcripted.obx.feature.economy.service.SellLimitTracker limits =
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.service.SellLimitTracker.class);
        if (limits != null && limits.dailyCap() > 0.0) {
            double remaining = limits.remaining(player.getUniqueId());
            if (total > remaining) {
                languages.send(player, "economy.sell.daily-cap", Placeholders.with(
                        "remaining", economy.format(Math.max(0.0, remaining)),
                        "cap", economy.format(limits.dailyCap())));
                return true;
            }
        }
        if (economy.getBalance(player.getUniqueId()) + total > EconomyService.MAX_BALANCE) {
            languages.send(player, "economy.sell.balance-full");
            return true;
        }
        return false;
    }

    /** Counts {@code total} against the player's daily sell cap (no-op when disabled). */
    private void recordSold(Player player, double total) {
        dev.zcripted.obx.feature.economy.service.SellLimitTracker limits =
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.service.SellLimitTracker.class);
        if (limits != null) {
            limits.record(player.getUniqueId(), total);
        }
    }

    private boolean sellHand(Player player) {
        ItemStack inHand = dev.zcripted.obx.util.compat.InventoryCompat.mainHand(player);
        if (inHand == null || inHand.getType() == Material.AIR) {
            languages.send(player, "economy.worth.empty-hand");
            return true;
        }
        double price = worth.getPrice(inHand.getType());
        if (price <= 0.0) {
            languages.send(player, "economy.worth.no-price",
                    Placeholders.with("material", inHand.getType().name()));
            return true;
        }
        int amount = inHand.getAmount();
        double total = EconomyService.sanitize(price * amount
                * dev.zcripted.obx.feature.economy.service.SellBoost.multiplier(player));
        Material type = inHand.getType();
        if (blockedBySellGuards(player, total)) {
            return true;
        }
        // Remove first, then pay: this ordering means a player can never end up holding
        // both the items and the money. Verify the removal actually took effect before
        // crediting, so a no-op setItem can't mint currency out of nothing.
        dev.zcripted.obx.util.compat.InventoryCompat.setMainHand(player, null);
        ItemStack afterHand = dev.zcripted.obx.util.compat.InventoryCompat.mainHand(player);
        if (afterHand != null && afterHand.getType() != Material.AIR) {
            languages.send(player, "economy.sell.failed");
            return true;
        }
        economy.deposit(player.getUniqueId(), player.getName(), total);
        recordSold(player, total);
        report(player, type, amount, total);
        return true;
    }

    private boolean sellMaterial(Player player, Material material) {
        double price = worth.getPrice(material);
        if (price <= 0.0) {
            languages.send(player, "economy.worth.no-price", Placeholders.with("material", material.name()));
            return true;
        }
        double multiplier = dev.zcripted.obx.feature.economy.service.SellBoost.multiplier(player);
        PlayerInventory inventory = player.getInventory();
        // Guard pass first (no removal): estimate the payout so a cap refusal costs nothing.
        int carrying = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.getType() == material) carrying += stack.getAmount();
        }
        if (carrying == 0) {
            languages.send(player, "economy.sell.none", Placeholders.with("material", material.name()));
            return true;
        }
        if (blockedBySellGuards(player, EconomyService.sanitize(price * carrying * multiplier))) {
            return true;
        }
        // Remove first, then pay. Count only what we actually clear, and re-verify the
        // slot is empty afterwards so a no-op setItem can't credit money for items that
        // are still in the inventory (dupe guard).
        int sold = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() != material) continue;
            int amount = stack.getAmount();
            inventory.setItem(slot, null);
            ItemStack after = inventory.getItem(slot);
            if (after != null && after.getType() == material) {
                continue; // removal didn't take effect — don't pay for this slot
            }
            sold += amount;
        }
        if (sold == 0) {
            languages.send(player, "economy.sell.none", Placeholders.with("material", material.name()));
            return true;
        }
        double total = EconomyService.sanitize(price * sold * multiplier);
        economy.deposit(player.getUniqueId(), player.getName(), total);
        recordSold(player, total);
        report(player, material, sold, total);
        return true;
    }

    private boolean sellAll(Player player) {
        double multiplier = dev.zcripted.obx.feature.economy.service.SellBoost.multiplier(player);
        PlayerInventory inventory = player.getInventory();
        // Guard pass first (no removal): estimate the payout so a cap refusal costs nothing.
        double estimate = 0.0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;
            double price = worth.getPrice(stack.getType());
            if (price > 0.0) estimate += price * stack.getAmount();
        }
        if (estimate > 0.0 && blockedBySellGuards(player, EconomyService.sanitize(estimate * multiplier))) {
            return true;
        }
        Map<Material, Integer> tallies = new HashMap<>();
        double total = 0.0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;
            double price = worth.getPrice(stack.getType());
            if (price <= 0.0) continue;
            Material type = stack.getType();
            int amount = stack.getAmount();
            // Remove first, then tally — verify the slot cleared so we never credit
            // money for items that are still present (dupe guard).
            inventory.setItem(slot, null);
            ItemStack after = inventory.getItem(slot);
            if (after != null && after.getType() == type) {
                continue; // removal didn't take effect — skip this slot
            }
            tallies.merge(type, amount, Integer::sum);
            total += price * amount;
        }
        if (tallies.isEmpty()) {
            languages.send(player, "economy.sell.nothing");
            return true;
        }
        total = EconomyService.sanitize(total * multiplier);
        economy.deposit(player.getUniqueId(), player.getName(), total);
        recordSold(player, total);
        economy.logTransaction(player.getName(), player.getUniqueId(), player.getName(),
                "SELL", total, economy.getBalance(player.getUniqueId()));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("total", economy.format(total));
        placeholders.put("balance", economy.format(economy.getBalance(player.getUniqueId())));
        languages.send(player, "economy.sell.all", placeholders);
        return true;
    }

    private void report(Player player, Material material, int amount, double total) {
        // Audit trail: sales are money creation — always logged.
        economy.logTransaction(player.getName(), player.getUniqueId(), player.getName(),
                "SELL", total, economy.getBalance(player.getUniqueId()));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("material", material.name());
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("total", economy.format(total));
        placeholders.put("balance", economy.format(economy.getBalance(player.getUniqueId())));
        languages.send(player, "economy.sell.result", placeholders);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new java.util.ArrayList<>(Arrays.asList("hand", "all"));
            return options;
        }
        return Collections.emptyList();
    }
}
