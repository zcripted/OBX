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
 * {@code /ah} (aliases: auction, auctionhouse) — the auction house.
 *
 * <ul>
 *   <li>{@code /ah} — browse GUI; {@code /ah mine} — own listings; {@code /ah claim} — returns.</li>
 *   <li>{@code /ah sell <price> [category]} — fixed-price listing of the held item.</li>
 *   <li>{@code /ah auction <startingBid> [buyout] [category]} — bidding listing of the held item.</li>
 *   <li>{@code /ah bid <id> <amount>} — bid on an auction listing (≥ buyout wins instantly).</li>
 *   <li>{@code /ah search <text…>} — browse GUI filtered by item/seller text.</li>
 *   <li>{@code /ah confirm [id]} — completes a high-value purchase that asked for confirmation.</li>
 * </ul>
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
                return sellHeld(player, auction, args, false);
            case "auction":
                return sellHeld(player, auction, args, true);
            case "bid":
                return bid(player, auction, args);
            case "search": {
                if (args.length < 2) {
                    languages.send(player, "economy.ah.usage");
                    return true;
                }
                String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                        .trim().toLowerCase(Locale.ENGLISH);
                AuctionMenu.open(plugin, player, AuctionMenu.ViewType.BROWSE, 0, "", "newest", query);
                return true;
            }
            case "confirm":
                return confirm(player, auction, args);
            default:
                languages.send(player, "economy.ah.usage");
                return true;
        }
    }

    /** {@code /ah confirm [id]} — retries the purchase the confirm gate paused. */
    private boolean confirm(Player player, AuctionService auction, String[] args) {
        int id = auction.pendingConfirmId(player);
        if (args.length >= 2) {
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                languages.send(player, "economy.ah.bad-id", Placeholders.with("input", args[1]));
                return true;
            }
        }
        if (id < 0) {
            languages.send(player, "economy.ah.confirm-none");
            return true;
        }
        // buy() consumes the registered confirmation and completes the purchase.
        sendBuyResult(player, auction.buy(player, id), id);
        return true;
    }

    /** {@code /ah bid <id> <amount>} — places a bid (≥ buyout buys instantly). */
    private boolean bid(Player player, AuctionService auction, String[] args) {
        if (args.length < 3) {
            languages.send(player, "economy.ah.usage");
            return true;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ignored) {
            languages.send(player, "economy.ah.bad-id", Placeholders.with("input", args[1]));
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException ignored) {
            languages.send(player, "economy.invalid-amount", Placeholders.with("input", args[2]));
            return true;
        }
        if (!Double.isFinite(amount) || amount <= 0.0) {
            languages.send(player, "economy.amount-positive");
            return true;
        }
        switch (auction.bid(player, id, amount)) {
            case OK:
                languages.send(player, "economy.ah.bid-placed");
                break;
            case BID_TOO_LOW:
                languages.send(player, "economy.ah.bid-too-low");
                break;
            case OWN_LISTING:
                languages.send(player, "economy.ah.own-listing");
                break;
            case CANT_AFFORD:
                languages.send(player, "economy.ah.cant-afford");
                break;
            case SOLD_OUT:
                languages.send(player, "economy.ah.sold-out");
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

    private void sendBuyResult(Player player, AuctionService.Result result, int id) {
        switch (result) {
            case OK:
                languages.send(player, "economy.ah.bought");
                break;
            case NEEDS_CONFIRM:
                languages.send(player, "economy.ah.confirm-needed", Placeholders.with("id", id));
                break;
            case OWN_LISTING:
                languages.send(player, "economy.ah.own-listing");
                break;
            case CANT_AFFORD:
                languages.send(player, "economy.ah.cant-afford");
                break;
            case SOLD_OUT:
                languages.send(player, "economy.ah.sold-out");
                break;
            case DISABLED:
                languages.send(player, "economy.ah.disabled");
                break;
            default:
                languages.send(player, "economy.ah.failed");
                break;
        }
    }

    /**
     * Lists the held item — fixed-price ({@code /ah sell <price> [category]}) or
     * auction ({@code /ah auction <startingBid> [buyout] [category]}).
     */
    private boolean sellHeld(Player player, AuctionService auction, String[] args, boolean asAuction) {
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
        double buyout = 0.0;
        String category;
        if (asAuction) {
            // /ah auction <startingBid> [buyout] [category] — a non-numeric second
            // argument is treated as the category (no buyout).
            int next = 2;
            if (args.length > next) {
                try {
                    buyout = Double.parseDouble(args[next]);
                    if (!Double.isFinite(buyout) || buyout < 0.0) {
                        languages.send(player, "economy.amount-positive");
                        return true;
                    }
                    if (buyout > 0.0 && buyout <= price) {
                        languages.send(player, "economy.ah.buyout-too-low");
                        return true;
                    }
                    next++;
                } catch (NumberFormatException isCategory) {
                    // fall through — args[next] is the category
                }
            }
            category = args.length > next ? args[next].toLowerCase(Locale.ENGLISH) : "";
        } else {
            category = args.length > 2 ? args[2].toLowerCase(Locale.ENGLISH) : "";
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
        AuctionService.Result result = asAuction
                ? auction.list(player, toList, 0.0, price, buyout, category)
                : auction.list(player, toList, price, 0.0, 0.0, category);
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
        String formatted = economy == null ? args[1] : economy.format(EconomyService.sanitize(price));
        if (asAuction) {
            java.util.Map<String, String> info = new java.util.HashMap<>();
            info.put("bid", formatted);
            info.put("buyout", buyout > 0.0
                    ? (economy == null ? String.valueOf(buyout) : economy.format(EconomyService.sanitize(buyout)))
                    : languages.get(player, "economy.ah.no-buyout-label"));
            info.put("count", String.valueOf(auction.countBySeller(player.getUniqueId())));
            info.put("max", String.valueOf(auction.maxListings()));
            languages.send(player, "economy.ah.auction-listed", info);
        } else {
            languages.send(player, "economy.ah.listed", Placeholders.with(
                    "price", formatted,
                    "count", String.valueOf(auction.countBySeller(player.getUniqueId())),
                    "max", String.valueOf(auction.maxListings())));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            for (String option : Arrays.asList("sell", "auction", "bid", "search", "confirm", "mine", "claim")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ENGLISH))) {
                    options.add(option);
                }
            }
            return options;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("auction"))) {
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