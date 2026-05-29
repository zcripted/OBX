package dev.sergeantfuzzy.sfcore.command.economy;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.economy.EconomyService;
import dev.sergeantfuzzy.sfcore.economy.WorthService;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorthCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final EconomyService economy;
    private final WorthService worth;

    public WorthCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
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
        if (!player.hasPermission("sfcore.worth")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType() == Material.AIR) {
            languages.send(player, "economy.worth.empty-hand");
            return true;
        }
        int amount = inHand.getAmount();
        if (args.length >= 1) {
            try {
                int requested = Integer.parseInt(args[0]);
                amount = Math.max(1, requested);
            } catch (NumberFormatException ignored) {
                languages.send(player, "economy.invalid-amount", Placeholders.with("input", args[0]));
                return true;
            }
        }
        double price = worth.getPrice(inHand.getType());
        if (price <= 0.0) {
            languages.send(player, "economy.worth.no-price",
                    Placeholders.with("material", inHand.getType().name()));
            return true;
        }
        double total = price * amount;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("material", inHand.getType().name());
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("each", economy.format(price));
        placeholders.put("total", economy.format(total));
        languages.send(player, "economy.worth.result", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> matches = new java.util.ArrayList<>();
        for (String amount : new String[]{"1", "8", "16", "32", "64"}) {
            if (amount.startsWith(prefix)) matches.add(amount);
        }
        return matches;
    }
}
