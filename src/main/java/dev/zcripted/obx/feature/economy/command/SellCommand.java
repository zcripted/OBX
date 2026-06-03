package dev.zcripted.obx.feature.economy.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
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

    public SellCommand(OBX plugin) {
        super(plugin);
        this.economy = plugin.getEconomyService();
        this.worth = plugin.getWorthService();
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

    private boolean sellHand(Player player) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
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
        double total = price * amount;
        player.getInventory().setItemInMainHand(null);
        economy.deposit(player.getUniqueId(), player.getName(), total);
        report(player, inHand.getType(), amount, total);
        return true;
    }

    private boolean sellMaterial(Player player, Material material) {
        double price = worth.getPrice(material);
        if (price <= 0.0) {
            languages.send(player, "economy.worth.no-price", Placeholders.with("material", material.name()));
            return true;
        }
        PlayerInventory inventory = player.getInventory();
        int sold = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() != material) continue;
            sold += stack.getAmount();
            inventory.setItem(slot, null);
        }
        if (sold == 0) {
            languages.send(player, "economy.sell.none", Placeholders.with("material", material.name()));
            return true;
        }
        double total = price * sold;
        economy.deposit(player.getUniqueId(), player.getName(), total);
        report(player, material, sold, total);
        return true;
    }

    private boolean sellAll(Player player) {
        PlayerInventory inventory = player.getInventory();
        Map<Material, Integer> tallies = new HashMap<>();
        double total = 0.0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;
            double price = worth.getPrice(stack.getType());
            if (price <= 0.0) continue;
            int amount = stack.getAmount();
            tallies.merge(stack.getType(), amount, Integer::sum);
            total += price * amount;
            inventory.setItem(slot, null);
        }
        if (tallies.isEmpty()) {
            languages.send(player, "economy.sell.nothing");
            return true;
        }
        economy.deposit(player.getUniqueId(), player.getName(), total);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("total", economy.format(total));
        placeholders.put("balance", economy.format(economy.getBalance(player.getUniqueId())));
        languages.send(player, "economy.sell.all", placeholders);
        return true;
    }

    private void report(Player player, Material material, int amount, double total) {
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
