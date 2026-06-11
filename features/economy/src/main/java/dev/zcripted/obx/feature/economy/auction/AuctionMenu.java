package dev.zcripted.obx.feature.economy.auction;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.gui.MenuHolder;
import dev.zcripted.obx.feature.economy.auction.AuctionService.Listing;
import dev.zcripted.obx.feature.economy.shop.ShopMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AuctionMenu {

    public enum ViewType { BROWSE, MINE }

    public static final int PAGE_SIZE = 45;
    public static final int NAV_TOGGLE = 45;
    public static final int NAV_CLAIM = 46;
    public static final int NAV_PREV = 47;
    public static final int NAV_SORT = 48;
    public static final int NAV_CATEGORY = 49;
    public static final int NAV_BALANCE = 50;
    public static final int NAV_SEARCH = 51;
    public static final int NAV_NEXT = 52;
    public static final int NAV_CLOSE = 53;

    /** Sort modes. */
    public static final String[] SORT_OPTIONS = {"newest", "oldest", "price_asc", "price_desc"};
    public static final String[] SORT_LABELS = {"Newest", "Oldest", "Price \u2191", "Price \u2193"};

    private AuctionMenu() {
    }

    public static final class Holder extends MenuHolder {
        private final ViewType view;
        private final int page;
        private final String category;
        private final String sort;
        private final String search;
        private final List<Integer> listingIds;

        Holder(ViewType view, int page, String category, String sort, String search, List<Integer> listingIds) {
            this.view = view;
            this.page = page;
            this.category = category;
            this.sort = sort;
            this.search = search;
            this.listingIds = listingIds;
        }

        public ViewType view() { return view; }
        public int page() { return page; }
        public String category() { return category; }
        public String sort() { return sort; }
        public String search() { return search; }
        public int listingIdAt(int slot) {
            return slot >= 0 && slot < listingIds.size() ? listingIds.get(slot) : -1;
        }
    }

    public static void open(ObxPlugin plugin, Player player, ViewType view, int page) {
        open(plugin, player, view, page, "", "newest", "");
    }

    public static void open(ObxPlugin plugin, Player player, ViewType view, int page,
                            String category, String sort, String search) {
        AuctionService auction = plugin.getServiceRegistry().get(AuctionService.class);
        if (auction == null || !auction.isEnabled()) {
            plugin.getLanguageManager().send(player, "economy.ah.disabled");
            return;
        }
        int current = Math.max(0, page);
        List<Listing> listings = view == ViewType.MINE
                ? auction.bySeller(player.getUniqueId(), current * PAGE_SIZE, PAGE_SIZE)
                : auction.browse(current * PAGE_SIZE, PAGE_SIZE, search, category, sort);
        if (listings.isEmpty() && current > 0) {
            open(plugin, player, view, 0, category, sort, search);
            return;
        }
        List<Integer> ids = new ArrayList<>(listings.size());
        String titleKey = view == ViewType.MINE ? "economy.ah.gui.title-mine" : "economy.ah.gui.title";
        Holder holder = new Holder(view, current, category, sort, search, ids);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                plugin.getLanguageManager().get(player, titleKey,
                        Collections.singletonMap("page", String.valueOf(current + 1))));
        holder.setInventory(inventory);

        for (int i = 0; i < listings.size() && i < PAGE_SIZE; i++) {
            Listing listing = listings.get(i);
            ids.add(listing.id());
            inventory.setItem(i, listingTile(plugin, player, listing, view));
        }

        for (int slot = PAGE_SIZE; slot < 54; slot++) {
            inventory.setItem(slot, ShopMenu.icon(new String[]{"GRAY_STAINED_GLASS_PANE",
                    "STAINED_GLASS_PANE", "THIN_GLASS", "STONE"}, " ", Collections.<String>emptyList()));
        }

        String toggleKey = view == ViewType.MINE ? "economy.ah.gui.browse" : "economy.ah.gui.mine";
        inventory.setItem(NAV_TOGGLE, ShopMenu.icon(new String[]{"BOOK"},
                plugin.getLanguageManager().get(player, toggleKey + ".name"),
                plugin.getLanguageManager().list(player, toggleKey + ".lore",
                        Collections.singletonMap("count",
                                String.valueOf(auction.countBySeller(player.getUniqueId()))))));

        int pending = auction.pendingReturns(player.getUniqueId());
        inventory.setItem(NAV_CLAIM, ShopMenu.icon(new String[]{"CHEST"},
                plugin.getLanguageManager().get(player, "economy.ah.gui.claim.name"),
                plugin.getLanguageManager().list(player, "economy.ah.gui.claim.lore",
                        Collections.singletonMap("count", String.valueOf(pending)))));

        if (current > 0) {
            inventory.setItem(NAV_PREV, ShopMenu.icon(new String[]{"PAPER"},
                    plugin.getLanguageManager().get(player, "shop.gui.prev.name",
                            Collections.singletonMap("page", String.valueOf(current))),
                    Collections.<String>emptyList()));
        }

        // Sort button (browse only)
        if (view == ViewType.BROWSE) {
            int sortIndex = 0;
            for (int i = 0; i < SORT_OPTIONS.length; i++) {
                if (SORT_OPTIONS[i].equals(sort)) { sortIndex = i; break; }
            }
            String nextSort = SORT_OPTIONS[(sortIndex + 1) % SORT_OPTIONS.length];
            String nextLabel = SORT_LABELS[(sortIndex + 1) % SORT_OPTIONS.length];
            inventory.setItem(NAV_SORT, ShopMenu.icon(new String[]{"HOPPER"},
                    plugin.getLanguageManager().get(player, "economy.ah.gui.sort.name",
                            Collections.singletonMap("current", SORT_LABELS[sortIndex])),
                    plugin.getLanguageManager().list(player, "economy.ah.gui.sort.lore",
                            Collections.singletonMap("next", nextLabel))));
        }

        // Category filter (browse only)
        if (view == ViewType.BROWSE) {
            List<String> cats = auction.knownCategories();
            String catLabel = category.isEmpty() ? "All" : category;
            inventory.setItem(NAV_CATEGORY, ShopMenu.icon(new String[]{"COMPARATOR"},
                    plugin.getLanguageManager().get(player, "economy.ah.gui.category.name",
                            Collections.singletonMap("current", catLabel)),
                    plugin.getLanguageManager().list(player, "economy.ah.gui.category.lore",
                            Collections.singletonMap("count", String.valueOf(cats.size())))));

            // Search info
            if (!search.isEmpty()) {
                inventory.setItem(NAV_SEARCH, ShopMenu.icon(new String[]{"SIGN"},
                        plugin.getLanguageManager().get(player, "economy.ah.gui.search.name"),
                        plugin.getLanguageManager().list(player, "economy.ah.gui.search.lore",
                                Collections.singletonMap("query", search))));
            }
        }

        EconomyService economy = plugin.getEconomyService();
        inventory.setItem(NAV_BALANCE, ShopMenu.icon(new String[]{"GOLD_INGOT"},
                plugin.getLanguageManager().get(player, "shop.gui.balance.name"),
                plugin.getLanguageManager().list(player, "shop.gui.balance.lore",
                        Collections.singletonMap("balance", economy == null ? "?"
                                : economy.format(economy.getBalance(player.getUniqueId()))))));

        if (listings.size() == PAGE_SIZE) {
            inventory.setItem(NAV_NEXT, ShopMenu.icon(new String[]{"PAPER"},
                    plugin.getLanguageManager().get(player, "shop.gui.next.name",
                            pageInfo(current, listings.size())),
                    Collections.<String>emptyList()));
        }

        inventory.setItem(NAV_CLOSE, ShopMenu.icon(new String[]{"BARRIER"},
                plugin.getLanguageManager().get(player, "shop.gui.close.name"),
                plugin.getLanguageManager().list(player, "shop.gui.close.lore",
                        Collections.<String, String>emptyMap())));

        player.openInventory(inventory);
    }

    private static Map<String, String> pageInfo(int current, int count) {
        Map<String, String> info = new HashMap<>();
        info.put("page", String.valueOf(current + 1));
        info.put("pages", count == PAGE_SIZE ? (current + 2) + "+" : String.valueOf(current + 1));
        return info;
    }

    private static ItemStack listingTile(ObxPlugin plugin, Player player, Listing listing, ViewType view) {
        ItemStack decoded = listing.decodeItem();
        if (decoded == null) {
            return ShopMenu.icon(new String[]{"BARRIER"},
                    plugin.getLanguageManager().get(player, "economy.ah.gui.corrupt.name"),
                    Collections.<String>emptyList());
        }
        ItemStack display = decoded.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            EconomyService economy = plugin.getEconomyService();
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            Map<String, String> info = new HashMap<>();
            String displayPrice;
            if (listing.isAuction()) {
                displayPrice = "Starting bid: " + (economy == null ? String.valueOf(listing.startingBid())
                        : economy.format(listing.startingBid()));
                if (listing.hasBuyout()) {
                    displayPrice += " / Buyout: " + (economy == null ? String.valueOf(listing.buyout())
                            : economy.format(listing.buyout()));
                }
            } else {
                displayPrice = economy == null ? String.valueOf(listing.price()) : economy.format(listing.price());
            }
            info.put("price", displayPrice);
            info.put("seller", listing.sellerName() == null ? "?" : listing.sellerName());
            info.put("left", timeLeft(listing.expires()));
            if (listing.category() != null && !listing.category().isEmpty()) {
                info.put("category", listing.category());
            } else {
                info.put("category", "");
            }
            String footerKey = view == ViewType.MINE ? "economy.ah.gui.entry-mine" : "economy.ah.gui.entry";
            lore.addAll(plugin.getLanguageManager().list(player, footerKey, info));
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private static String timeLeft(long expires) {
        long remaining = Math.max(0L, expires - System.currentTimeMillis());
        long minutes = remaining / 60_000L;
        long days = minutes / 1_440L;
        long hours = (minutes % 1_440L) / 60L;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + (minutes % 60L) + "m";
        }
        return Math.max(1, minutes) + "m";
    }
}