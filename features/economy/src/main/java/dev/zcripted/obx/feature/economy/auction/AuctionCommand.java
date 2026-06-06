package dev.zcripted.obx.feature.economy.auction;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.command.AbstractObxCommand;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /ah [sell <price>|mine|claim]} (aliases: auction, auctionhouse).
 * No args opens the browse GUI; {@code sell} lists the held item.
 */
public class AuctionCommand extends AbstractObxCommand implements TabCompleter {

    public AuctionCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.ah")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        AuctionService auction = plugin.getServiceRegistry().get(AuctionService.class);
        if (auction == null || !auction.isEnabled()) {
            languages.send(player, "economy.ah.disabled");
            return true;
        }
        if (args.length == 0) {
            AuctionMenu.open(plugin, player, AuctionMenu.ViewType.BROWSE, 0);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "mine":
                AuctionMenu.open(plugin, player, AuctionMenu.ViewType.MINE, 0);
                return true;
            case "claim": {
                int[] delivered = auction.claim(player);
                if (delivered[0] < 0) {
                    // Inventory was full and nothing else paid out — items stay queued.
                    languages.send(player, "economy.ah.claim-full");
                } else if (delivered[0] == 0 && delivered[1] <= 0) {
                    languages.send(player, "economy.ah.claim-empty");
                } else {
                    languages.send(player, "economy.ah.claimed", Placeholders.with(
                            "items", Math.max(0, delivered[0]), "payouts", delivered[1]));
                }
                return true;
            }
            case "sell":
                return sellHeld(player, auction, args);
            default:
                languages.send(player, "economy.ah.usage");
                return true;
        }
    }

    private boolean sellHeld(Player player, AuctionService auction, String[] args) {
        if (!player.hasPermission("obx.ah.sell")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 2) {
            languages.send(player, "economy.ah.usage");
            return true;
        }
        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException ignored) {
            languages.send(player, "economy.invalid-amount", Placeholders.with("input", args[1]));
            return true;
        }
        if (!Double.isFinite(price) || price <= 0.0) {
            languages.send(player, "economy.amount-positive");
            return true;
        }
        ItemStack held = dev.zcripted.obx.util.compat.InventoryCompat.mainHand(player);
        if (held == null || held.getType() == Material.AIR) {
            languages.send(player, "economy.ah.no-item");
            return true;
        }
        // Remove the item BEFORE listing (no listing while it's still usable); any
        // failure result puts it straight back in hand.
        ItemStack toList = held.clone();
        dev.zcripted.obx.util.compat.InventoryCompat.setMainHand(player, null);
        AuctionService.Result result = auction.list(player, toList, price);
        if (result != AuctionService.Result.OK) {
            dev.zcripted.obx.util.compat.InventoryCompat.setMainHand(player, toList);
            switch (result) {
                case TOO_MANY:
                    languages.send(player, "economy.ah.too-many", Placeholders.with(
                            "max", String.valueOf(auction.maxListings())));
                    break;
                case CANT_AFFORD:
                    languages.send(player, "economy.ah.fee-unpaid");
                    break;
                case DISABLED:
                    languages.send(player, "economy.ah.disabled");
                    break;
                default:
                    languages.send(player, "economy.ah.failed");
                    break;
            }
            return true;
        }
        EconomyService economy = plugin.getEconomyService();
        languages.send(player, "economy.ah.listed", Placeholders.with(
                "price", economy == null ? args[1] : economy.format(EconomyService.sanitize(price)),
                "count", String.valueOf(auction.countBySeller(player.getUniqueId())),
                "max", String.valueOf(auction.maxListings())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            for (String option : Arrays.asList("sell", "mine", "claim")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ENGLISH))) {
                    options.add(option);
                }
            }
            return options;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            List<String> amounts = new ArrayList<>();
            for (String amount : new String[]{"100", "500", "1000"}) {
                if (amount.startsWith(args[1])) {
                    amounts.add(amount);
                }
            }
            return amounts;
        }
        return Collections.emptyList();
    }
}
