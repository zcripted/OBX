package dev.zcripted.obx.feature.economy.auction;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * Click dispatch for the auction GUIs. Both views are read-only (all movement
 * cancelled); the actual buy/cancel safety lives in {@link AuctionService}'s
 * row-guarded operations — a double-click or stale page can lose the race but
 * never duplicate money or items.
 */
public final class AuctionListener implements Listener {

    private final ObxPlugin plugin;

    public AuctionListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof AuctionMenu.Holder)) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        AuctionMenu.Holder holder = (AuctionMenu.Holder) top.getHolder();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return; // own-inventory clicks — already cancelled above
        }
        switch (slot) {
            case AuctionMenu.NAV_CLOSE:
                player.closeInventory();
                return;
            case AuctionMenu.NAV_TOGGLE:
                AuctionMenu.open(plugin, player,
                        holder.view() == AuctionMenu.ViewType.MINE
                                ? AuctionMenu.ViewType.BROWSE : AuctionMenu.ViewType.MINE, 0,
                        holder.category(), holder.sort(), holder.search());
                return;
            case AuctionMenu.NAV_CLAIM:
                claim(player, holder);
                return;
            case AuctionMenu.NAV_SORT:
                if (holder.view() == AuctionMenu.ViewType.BROWSE) {
                    String currentSort = holder.sort();
                    String nextSort = "newest";
                    for (int i = 0; i < AuctionMenu.SORT_OPTIONS.length; i++) {
                        if (AuctionMenu.SORT_OPTIONS[i].equals(currentSort)) {
                            nextSort = AuctionMenu.SORT_OPTIONS[(i + 1) % AuctionMenu.SORT_OPTIONS.length];
                            break;
                        }
                    }
                    AuctionMenu.open(plugin, player, holder.view(), 0,
                            holder.category(), nextSort, holder.search());
                }
                return;
            case AuctionMenu.NAV_CATEGORY:
                if (holder.view() == AuctionMenu.ViewType.BROWSE) {
                    cycleCategory(player, holder);
                }
                return;
            case AuctionMenu.NAV_SEARCH:
                if (holder.view() == AuctionMenu.ViewType.BROWSE && !holder.search().isEmpty()) {
                    AuctionMenu.open(plugin, player, holder.view(), 0,
                            holder.category(), holder.sort(), "");
                }
                return;
            case AuctionMenu.NAV_PREV:
                AuctionMenu.open(plugin, player, holder.view(), holder.page() - 1,
                        holder.category(), holder.sort(), holder.search());
                return;
            case AuctionMenu.NAV_NEXT:
                AuctionMenu.open(plugin, player, holder.view(), holder.page() + 1,
                        holder.category(), holder.sort(), holder.search());
                return;
            default:
                break;
        }
        if (slot >= AuctionMenu.PAGE_SIZE) {
            return; // filler / balance card
        }
        int listingId = holder.listingIdAt(slot);
        if (listingId < 0) {
            return;
        }
        AuctionService auction = plugin.getServiceRegistry().get(AuctionService.class);
        if (auction == null) {
            return;
        }
        if (holder.view() == AuctionMenu.ViewType.MINE) {
            cancelListing(player, holder, auction, listingId);
        } else if (event.isShiftClick() && player.hasPermission("obx.ah.admin")) {
            // Staff moderation: shift-click in browse pulls a listing (item → seller's
            // returns) instead of buying it. cancel() enforces obx.ah.admin server-side.
            cancelListing(player, holder, auction, listingId);
        } else {
            buyListing(player, holder, auction, listingId);
        }
    }

    private void buyListing(Player player, AuctionMenu.Holder holder, AuctionService auction, int listingId) {
        if (!player.hasPermission("obx.ah")) {
            plugin.getLanguageManager().send(player, "core.no-permission");
            return;
        }
        AuctionService.Result result = auction.buy(player, listingId);
        switch (result) {
            case OK:
                plugin.getLanguageManager().send(player, "economy.ah.bought");
                break;
            case OWN_LISTING:
                plugin.getLanguageManager().send(player, "economy.ah.own-listing");
                break;
            case CANT_AFFORD:
                plugin.getLanguageManager().send(player, "economy.ah.cant-afford");
                break;
            case SOLD_OUT:
                plugin.getLanguageManager().send(player, "economy.ah.sold-out");
                break;
            case NEEDS_CONFIRM:
                plugin.getLanguageManager().send(player, "economy.ah.confirm-needed",
                        dev.zcripted.obx.util.text.Placeholders.with("id", listingId));
                return; // don't reopen — player must type /ah confirm
            case BID_PLACED:
                plugin.getLanguageManager().send(player, "economy.ah.bid-placed");
                break;
            case BID_TOO_LOW:
                plugin.getLanguageManager().send(player, "economy.ah.bid-too-low");
                break;
            default:
                plugin.getLanguageManager().send(player, "economy.ah.failed");
                break;
        }
        reopen(plugin, player, holder);
    }

    private void cancelListing(Player player, AuctionMenu.Holder holder, AuctionService auction, int listingId) {
        AuctionService.Result result = auction.cancel(player, listingId);
        if (result == AuctionService.Result.OK) {
            plugin.getLanguageManager().send(player, "economy.ah.cancelled");
        } else if (result == AuctionService.Result.NOT_YOURS) {
            plugin.getLanguageManager().send(player, "economy.ah.not-yours");
        } else {
            plugin.getLanguageManager().send(player, "economy.ah.sold-out");
        }
        reopen(plugin, player, holder);
    }

    private void claim(Player player, AuctionMenu.Holder holder) {
        AuctionService auction = plugin.getServiceRegistry().get(AuctionService.class);
        if (auction == null) {
            return;
        }
        int[] delivered = auction.claim(player);
        if (delivered[0] < 0) {
            plugin.getLanguageManager().send(player, "economy.ah.claim-full");
        } else if (delivered[0] == 0 && delivered[1] <= 0) {
            plugin.getLanguageManager().send(player, "economy.ah.claim-empty");
        } else {
        plugin.getLanguageManager().send(player, "economy.ah.claimed", Placeholders.with(
                "items", Math.max(0, delivered[0]), "payouts", delivered[1]));
        }
        reopen(plugin, player, holder);
    }

    private static void reopen(ObxPlugin plugin, Player player, AuctionMenu.Holder holder) {
        AuctionMenu.open(plugin, player, holder.view(), holder.page(),
                holder.category(), holder.sort(), holder.search());
    }

    private void cycleCategory(Player player, AuctionMenu.Holder holder) {
        AuctionService auction = plugin.getServiceRegistry().get(AuctionService.class);
        if (auction == null) return;
        List<String> cats = new java.util.ArrayList<>(auction.knownCategories());
        cats.add(0, ""); // "All" option
        String current = holder.category();
        int idx = cats.indexOf(current);
        String next = cats.get((idx + 1) % cats.size());
        AuctionMenu.open(plugin, player, holder.view(), 0,
                next, holder.sort(), holder.search());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof AuctionMenu.Holder) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }
}