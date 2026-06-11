package dev.zcripted.obx.feature.economy.bank;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.gui.MenuHolder;
import dev.zcripted.obx.feature.economy.service.BankService;
import dev.zcripted.obx.feature.economy.shop.ShopMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BankMenu {

    public static final int PAGE_SIZE = 36;
    public static final int NAV_DEPOSIT = 45;
    public static final int NAV_WITHDRAW = 46;
    public static final int NAV_PREV = 48;
    public static final int NAV_BALANCE = 49;
    public static final int NAV_NEXT = 50;
    public static final int NAV_CLOSE = 53;

    private BankMenu() {
    }

    public static final class Holder extends MenuHolder {
        private final int page;

        Holder(int page) {
            this.page = page;
        }

        public int page() {
            return page;
        }
    }

    public static void open(ObxPlugin plugin, Player player, int page) {
        BankService bank = plugin.getServiceRegistry().get(BankService.class);
        if (bank == null || !bank.isEnabled()) {
            plugin.getLanguageManager().send(player, "economy.bank.disabled");
            return;
        }
        int current = Math.max(0, page);
        List<BankService.BankTransaction> history = bank.history(player.getUniqueId(), current * PAGE_SIZE, PAGE_SIZE);

        Holder holder = new Holder(current);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                plugin.getLanguageManager().get(player, "economy.bank.gui.title",
                        Collections.singletonMap("page", String.valueOf(current + 1))));
        holder.setInventory(inventory);

        EconomyService economy = plugin.getEconomyService();
        double bankBalance = bank.balance(player.getUniqueId(), player.getName());
        double walletBalance = economy == null ? 0 : economy.getBalance(player.getUniqueId());
        // The player's EFFECTIVE rate — balance tiers + rank tiers, not the flat default.
        double rate = bank.effectiveRate(bankBalance, player.getUniqueId());
        double projectedDaily = EconomyService.sanitize(bankBalance * rate / 100.0);

        for (int i = 0; i < PAGE_SIZE && i < history.size(); i++) {
            BankService.BankTransaction tx = history.get(i);
            inventory.setItem(i, transactionTile(plugin, player, tx, economy));
        }

        for (int slot = PAGE_SIZE; slot < 54; slot++) {
            inventory.setItem(slot, ShopMenu.icon(new String[]{"GRAY_STAINED_GLASS_PANE",
                    "STAINED_GLASS_PANE", "THIN_GLASS", "STONE"}, " ", Collections.<String>emptyList()));
        }

        inventory.setItem(NAV_DEPOSIT, ShopMenu.icon(new String[]{"CHEST"},
                plugin.getLanguageManager().get(player, "economy.bank.gui.deposit.name"),
                plugin.getLanguageManager().list(player, "economy.bank.gui.deposit.lore", Collections.<String, String>emptyMap())));

        inventory.setItem(NAV_WITHDRAW, ShopMenu.icon(new String[]{"ENDER_CHEST"},
                plugin.getLanguageManager().get(player, "economy.bank.gui.withdraw.name"),
                plugin.getLanguageManager().list(player, "economy.bank.gui.withdraw.lore", Collections.<String, String>emptyMap())));

        if (current > 0) {
            inventory.setItem(NAV_PREV, ShopMenu.icon(new String[]{"PAPER"},
                    plugin.getLanguageManager().get(player, "shop.gui.prev.name", pageInfo(current)),
                    Collections.<String>emptyList()));
        }

        java.util.Map<String, String> balanceInfo = new java.util.HashMap<>();
        balanceInfo.put("wallet", economy == null ? "?" : economy.format(walletBalance));
        balanceInfo.put("banked", economy == null ? "?" : economy.format(bankBalance));
        balanceInfo.put("rate", String.valueOf(rate));
        balanceInfo.put("projected", economy == null ? "?" : economy.format(projectedDaily));
        inventory.setItem(NAV_BALANCE, ShopMenu.icon(new String[]{"GOLD_INGOT"},
                plugin.getLanguageManager().get(player, "economy.bank.gui.balance.name"),
                plugin.getLanguageManager().list(player, "economy.bank.gui.balance.lore", balanceInfo)));

        if (history.size() == PAGE_SIZE) {
            inventory.setItem(NAV_NEXT, ShopMenu.icon(new String[]{"PAPER"},
                    plugin.getLanguageManager().get(player, "shop.gui.next.name", pageInfo(current)),
                    Collections.<String>emptyList()));
        }

        inventory.setItem(NAV_CLOSE, ShopMenu.icon(new String[]{"BARRIER"},
                plugin.getLanguageManager().get(player, "shop.gui.close.name"),
                plugin.getLanguageManager().list(player, "shop.gui.close.lore",
                        Collections.<String, String>emptyMap())));

        player.openInventory(inventory);
    }

    private static java.util.Map<String, String> pageInfo(int current) {
        java.util.Map<String, String> info = new java.util.HashMap<>();
        info.put("page", String.valueOf(current + 1));
        info.put("pages", String.valueOf(current + 1));
        return info;
    }

    private static ItemStack transactionTile(ObxPlugin plugin, Player player, BankService.BankTransaction tx, EconomyService economy) {
        String material;
        if ("BANK_DEPOSIT".equals(tx.type)) {
            material = "GREEN_STAINED_GLASS_PANE";
        } else if ("BANK_WITHDRAW".equals(tx.type)) {
            material = "RED_STAINED_GLASS_PANE";
        } else if ("BANK_INTEREST".equals(tx.type)) {
            material = "LIME_STAINED_GLASS_PANE";
        } else {
            material = "GRAY_STAINED_GLASS_PANE";
        }
            java.util.Map<String, String> txInfo = new java.util.HashMap<>();
            String typeLabel = tx.type != null ? tx.type.replace("BANK_", "") : "?";
            String amt = economy == null ? String.valueOf(tx.amount) : economy.format(tx.amount);
            String bal = economy == null ? "?" : economy.format(tx.balanceAfter);
            txInfo.put("type", typeLabel);
            txInfo.put("amount", amt);
            txInfo.put("balance", bal);
            txInfo.put("date", tx.timestamp == null ? "?" : tx.timestamp);
            return ShopMenu.icon(new String[]{material, "STAINED_GLASS_PANE", "THIN_GLASS", "STONE"},
                    plugin.getLanguageManager().get(player, "economy.bank.gui.history.name", txInfo),
                    plugin.getLanguageManager().list(player, "economy.bank.gui.history.lore", txInfo));
    }
}