package dev.zcripted.obx.feature.economy.auction;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import dev.zcripted.obx.feature.economy.util.ItemCodec;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Player auction house ({@code /ah}): SQLite-backed listings with race-guarded
 * purchases and a returns ledger for everything that must flow back to a player
 * (cancelled/expired items, seller proceeds that didn't fit their wallet).
 *
 * <p><b>Concurrency model:</b> the listing row itself is the lock. Every
 * consuming path (buy, cancel, expiry sweep) first does a guarded
 * {@code DELETE WHERE id = ?} and only proceeds when it deleted the row — two
 * buyers racing for one listing can never both win, and a cancel racing a buy
 * loses cleanly. Money moves before goods, with refunds on the losing path.
 *
 * <p>Config ({@code economy.auction.*}): {@code enabled}, {@code max-listings},
 * {@code duration-hours}, {@code listing-fee}, {@code tax-percent}.
 */
public final class AuctionService {

    /** One active listing (item decoded on demand — see {@link #decodeItem}). */
    public static final class Listing {
        final int id;
        final UUID sellerUuid;
        final String sellerName;
        final String encodedItem;
        final double price;
        final double startingBid;
        final double buyout;
        final String category;
        final long expires;

        Listing(int id, UUID sellerUuid, String sellerName, String encodedItem,
                double price, double startingBid, double buyout, String category, long expires) {
            this.id = id;
            this.sellerUuid = sellerUuid;
            this.sellerName = sellerName;
            this.encodedItem = encodedItem;
            this.price = price;
            this.startingBid = startingBid;
            this.buyout = buyout;
            this.category = category;
            this.expires = expires;
        }

        public int id() { return id; }
        public UUID sellerUuid() { return sellerUuid; }
        public String sellerName() { return sellerName; }
        public double price() { return price; }
        public double startingBid() { return startingBid; }
        public double buyout() { return buyout; }
        public String category() { return category; }
        public long expires() { return expires; }
        public ItemStack decodeItem() { return ItemCodec.fromBase64(encodedItem); }

        /** True if this listing accepts bids (has a starting bid > 0). */
        public boolean isAuction() { return startingBid > 0.0; }

        /** True if this listing can be bought outright at its buyout price. */
        public boolean hasBuyout() { return buyout > 0.0; }
    }

    /** A bid placed on an auction-type listing. */
    public static final class Bid {
        public final int listingId;
        public final UUID bidderUuid;
        public final String bidderName;
        public final double amount;
        public final long timestamp;

        Bid(int listingId, UUID bidderUuid, String bidderName, double amount, long timestamp) {
            this.listingId = listingId;
            this.bidderUuid = bidderUuid;
            this.bidderName = bidderName;
            this.amount = amount;
            this.timestamp = timestamp;
        }
    }

    /** Pending buy confirmations (player -> listing id). */
    private final Map<UUID, Integer> pendingConfirm = new HashMap<>();

    /** Outcome of a listing/purchase/bid attempt (drives the message key). */
    public enum Result {
        OK, DISABLED, TOO_MANY, CANT_AFFORD, SOLD_OUT, OWN_LISTING, NOT_YOURS,
        FAILED, NEEDS_CONFIRM, BID_PLACED, OUTBID, BID_TOO_LOW, NO_BUYOUT
    }

    private static final String LISTINGS = "auction_listings";
    private static final String RETURNS = "auction_returns";
    private static final String BIDS = "auction_bids";

    private final ObxPlugin plugin;
    private final SqliteDataStore store;

    public AuctionService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            return;
        }
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + LISTINGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, seller_uuid TEXT NOT NULL,"
                + " seller_name TEXT, item TEXT NOT NULL, price REAL NOT NULL,"
                + " starting_bid REAL NOT NULL DEFAULT 0, buyout REAL NOT NULL DEFAULT 0,"
                + " category TEXT DEFAULT '',"
                + " created INTEGER NOT NULL, expires INTEGER NOT NULL)");
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + RETURNS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, owner_uuid TEXT NOT NULL,"
                + " item TEXT, money REAL NOT NULL DEFAULT 0, reason TEXT, created INTEGER NOT NULL)");
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + BIDS + " ("
                + "listing_id INTEGER NOT NULL, bidder_uuid TEXT NOT NULL,"
                + " bidder_name TEXT, amount REAL NOT NULL, ts INTEGER NOT NULL)");
        store.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bids_listing ON " + BIDS + " (listing_id)");
        // Add new columns for existing databases (skipped when already present)
        addColumnIfMissing(LISTINGS, "starting_bid", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(LISTINGS, "buyout", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(LISTINGS, "category", "TEXT DEFAULT ''");
    }

    /** Adds {@code column} to {@code table} if it isn't already present (SQLite-safe migration). */
    private void addColumnIfMissing(String table, String column, String definition) {
        boolean exists = store.queryFirst(
                "SELECT COUNT(*) AS c FROM pragma_table_info('" + table + "') WHERE name = ?",
                rs -> rs.getInt("c") > 0, column).orElse(false);
        if (!exists) {
            store.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("economy.auction.enabled", true) && store.isAvailable();
    }

    public int maxListings() {
        return Math.max(1, plugin.getConfig().getInt("economy.auction.max-listings", 5));
    }

    private long durationMillis() {
        return Math.max(1, plugin.getConfig().getInt("economy.auction.duration-hours", 48)) * 3_600_000L;
    }

    private double listingFee() {
        return Math.max(0.0, plugin.getConfig().getDouble("economy.auction.listing-fee", 0.0));
    }

    private double taxPercent() {
        return Math.max(0.0, Math.min(100.0, plugin.getConfig().getDouble("economy.auction.tax-percent", 0.0)));
    }

    // ── Listing ──────────────────────────────────────────────────────────────

    /**
     * Lists {@code item} for a fixed {@code price} (no auction/bidding).
     */
    public Result list(Player seller, ItemStack item, double price) {
        return list(seller, item, price, 0.0, 0.0, "");
    }

    /**
     * Lists {@code item} with optional starting bid, buyout, and category.
     * {@code price} is the fixed-price (used when {@code startingBid == 0}).
     * The item must already be REMOVED from the seller's inventory by the caller.
     */
    public Result list(Player seller, ItemStack item, double price,
                       double startingBid, double buyout, String category) {
        if (!isEnabled()) {
            return Result.DISABLED;
        }
        double cleanPrice = EconomyService.sanitize(price);
        double cleanStartingBid = EconomyService.sanitize(startingBid);
        double cleanBuyout = EconomyService.sanitize(buyout);
        if (item == null) {
            return Result.FAILED;
        }
        if (cleanPrice <= 0.0 && cleanStartingBid <= 0.0) {
            return Result.FAILED; // need either a fixed price or a starting bid
        }
        if (countBySeller(seller.getUniqueId()) >= maxListings()) {
            return Result.TOO_MANY;
        }
        String encoded = ItemCodec.toBase64(item);
        if (encoded == null) {
            return Result.FAILED;
        }
        EconomyService economy = plugin.getEconomyService();
        double fee = listingFee();
        if (fee > 0.0) {
            if (economy == null || !economy.withdraw(seller.getUniqueId(), seller.getName(), fee)) {
                return Result.CANT_AFFORD;
            }
        }
        long now = System.currentTimeMillis();
        boolean feePaid = fee > 0.0 && economy != null;
        int rows = store.executeUpdateRows(
                "INSERT INTO " + LISTINGS + " (seller_uuid, seller_name, item, price, starting_bid,"
                        + " buyout, category, created, expires)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                seller.getUniqueId(), seller.getName(), encoded, cleanPrice,
                cleanStartingBid, cleanBuyout, category, now, now + durationMillis());
        if (rows <= 0) {
            if (feePaid) {
                economy.deposit(seller.getUniqueId(), seller.getName(), fee);
            }
            return Result.FAILED;
        }
        if (feePaid) {
            // Listing fee → the visible server account (not burned).
            collectForServer(seller.getName(), "AH_LISTING_FEE", fee);
        }
        return Result.OK;
    }

    /** Routes sink revenue (listing fees, sales tax) to the visible server account. */
    private void collectForServer(String actor, String action, double amount) {
        dev.zcripted.obx.feature.economy.sink.ServerAccountService account = plugin.getServiceRegistry()
                .get(dev.zcripted.obx.feature.economy.sink.ServerAccountService.class);
        if (account != null) {
            account.deposit(actor, action, amount);
        }
    }

    // ── Bidding ──────────────────────────────────────────────────────────────

    /**
     * Places a bid on an auction listing. If the bid is the highest and >= starting
     * bid, the previous high bidder is refunded. A bid equal to or above the buyout
     * instantly wins (buyout path).
     *
     * <p>The withdraw → refund → insert sequence runs inside a single SQLite
     * transaction so a crash mid-way rolls back every change and no money is lost.
     */
    public Result bid(Player bidder, int listingId, double amount) {
        if (!isEnabled()) {
            return Result.DISABLED;
        }
        double cleanAmount = EconomyService.sanitize(amount);
        if (cleanAmount <= 0.0) {
            return Result.FAILED;
        }
        EconomyService economy = plugin.getEconomyService();
        if (economy == null) {
            return Result.FAILED;
        }
        Listing listing = store.queryFirst(
                "SELECT id, seller_uuid, seller_name, item, price, starting_bid, buyout, category, expires"
                        + " FROM " + LISTINGS + " WHERE id = ? AND expires > ?",
                this::mapListing, listingId, System.currentTimeMillis()).orElse(null);
        if (listing == null) {
            return Result.SOLD_OUT;
        }
        if (bidder.getUniqueId().equals(listing.sellerUuid())) {
            return Result.OWN_LISTING;
        }
        if (!listing.isAuction()) {
            return Result.FAILED; // fixed-price listing — use buy()
        }

        // Check the bid is at least the starting bid
        if (cleanAmount < listing.startingBid()) {
            return Result.BID_TOO_LOW;
        }

        // Pre-transaction read for the "bid too low" fast-path only.
        Bid preHigh = currentHighBid(listingId);
        if (preHigh != null && cleanAmount <= preHigh.amount) {
            return Result.BID_TOO_LOW;
        }

        // Buyout check: if amount >= buyout and buyout > 0, do instant buyout
        if (listing.hasBuyout() && cleanAmount >= listing.buyout()) {
            return buyoutListing(bidder, listing, economy);
        }

        // Atomic transaction: withdraw → refund previous high → insert new bid.
        // All three use the shared connection under autoCommit=false so any crash
        // rolls everything back and the bidder's money stays in their wallet.
        boolean committed = store.transaction(conn -> {
            // Reserve bid from wallet (part of the transaction — rolled back on failure)
            if (!economy.withdraw(bidder.getUniqueId(), bidder.getName(), cleanAmount)) {
                throw new java.sql.SQLException("insufficient funds");
            }

            // Refund the previous high bidder (re-read inside the lock for safety)
            Bid high = currentHighBid(listingId);
            if (high != null) {
                economy.deposit(high.bidderUuid, high.bidderName, high.amount);
            }

            // Insert the new bid record
            try (java.sql.PreparedStatement st = conn.prepareStatement(
                    "INSERT INTO " + BIDS
                            + " (listing_id, bidder_uuid, bidder_name, amount, ts) VALUES (?, ?, ?, ?, ?)")) {
                store.bind(st, listingId, bidder.getUniqueId(), bidder.getName(),
                        cleanAmount, System.currentTimeMillis());
                st.executeUpdate();
            }
        });

        if (!committed) {
            return Result.CANT_AFFORD;
        }

        // Log outside the transaction (async — non-critical).
        economy.logTransaction(bidder.getName(), bidder.getUniqueId(), bidder.getName(),
                "AH_BID", cleanAmount, economy.getBalance(bidder.getUniqueId()));

        return Result.OK;
    }

    /** Returns the current highest bid for a listing, or null. */
    public Bid currentHighBid(int listingId) {
        return store.queryFirst(
                "SELECT listing_id, bidder_uuid, bidder_name, amount, ts FROM " + BIDS
                        + " WHERE listing_id = ? ORDER BY amount DESC LIMIT 1",
                rs -> new Bid(rs.getInt("listing_id"),
                        UUID.fromString(rs.getString("bidder_uuid")),
                        rs.getString("bidder_name"),
                        rs.getDouble("amount"),
                        rs.getLong("ts")),
                listingId).orElse(null);
    }

    /** Buyout a listing at its buyout price. */
    private Result buyoutListing(Player buyer, Listing listing, EconomyService economy) {
        double price = listing.buyout();
        if (!economy.withdraw(buyer.getUniqueId(), buyer.getName(), price)) {
            return Result.CANT_AFFORD;
        }
        int rows = store.executeUpdateRows("DELETE FROM " + LISTINGS + " WHERE id = ?", listing.id());
        if (rows <= 0) {
            economy.deposit(buyer.getUniqueId(), buyer.getName(), price);
            return Result.SOLD_OUT;
        }
        // Refund any existing high bidder
        Bid currentHigh = currentHighBid(listing.id());
        if (currentHigh != null) {
            economy.deposit(currentHigh.bidderUuid, currentHigh.bidderName, currentHigh.amount);
        }
        // Clean up bids
        store.executeUpdate("DELETE FROM " + BIDS + " WHERE listing_id = ?", listing.id());

        double proceeds = EconomyService.sanitize(price * (1.0 - taxPercent() / 100.0));
        if (proceeds > 0.0 && listing.sellerUuid() != null) {
            if (economy.depositStrict(listing.sellerUuid(), listing.sellerName(), proceeds)) {
                economy.logTransaction(buyer.getName(), listing.sellerUuid(), listing.sellerName(),
                        "AH_SELL", proceeds, economy.getBalance(listing.sellerUuid()));
            } else {
                addReturn(listing.sellerUuid(), null, proceeds, "PROCEEDS");
            }
        }
        // Sales tax → the visible server account (not burned).
        collectForServer(buyer.getName(), "AH_TAX", price - proceeds);
        economy.logTransaction(buyer.getName(), buyer.getUniqueId(), buyer.getName(),
                "AH_BUYOUT", price, economy.getBalance(buyer.getUniqueId()));
        ItemStack goods = listing.decodeItem();
        if (goods != null) {
            deliver(buyer, goods);
        }
        return Result.OK;
    }

    // ── Buy confirm ──────────────────────────────────────────────────────────

    /** Registers a pending buy confirmation for a high-value purchase. */
    public void registerConfirm(Player player, int listingId) {
        pendingConfirm.put(player.getUniqueId(), listingId);
    }

    /** Checks and clears a pending confirmation. Returns true if confirmed. */
    public boolean checkConfirm(Player player, int listingId) {
        Integer pending = pendingConfirm.get(player.getUniqueId());
        if (pending == null || pending != listingId) {
            return false;
        }
        pendingConfirm.remove(player.getUniqueId());
        return true;
    }

    /** The listing id this player was asked to confirm, or {@code -1}. */
    public int pendingConfirmId(Player player) {
        Integer id = pendingConfirm.get(player.getUniqueId());
        return id == null ? -1 : id;
    }

    /** The amount above which a purchase requires /ah confirm. 0 = no confirmation needed. */
    public double confirmThreshold() {
        return Math.max(0.0, plugin.getConfig().getDouble("economy.auction.confirm-threshold", 0.0));
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public int countActive() {
        Integer count = store.queryFirst(
                "SELECT COUNT(*) AS c FROM " + LISTINGS + " WHERE expires > ?",
                rs -> rs.getInt("c"), System.currentTimeMillis()).orElse(null);
        return count == null ? 0 : count;
    }

    public int countBySeller(UUID seller) {
        Integer count = store.queryFirst(
                "SELECT COUNT(*) AS c FROM " + LISTINGS + " WHERE seller_uuid = ? AND expires > ?",
                rs -> rs.getInt("c"), seller, System.currentTimeMillis()).orElse(null);
        return count == null ? 0 : count;
    }

    /**
     * Browse all active listings with optional filters.
     *
     * @param offset     pagination offset
     * @param limit      page size
     * @param search     optional text search (matches item name/seller, case-insensitive)
     * @param category   optional category filter (empty = all)
     * @param sort       sort order: "price_asc", "price_desc", "newest" (default), "oldest"
     */
    public List<Listing> browse(int offset, int limit, String search, String category, String sort) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, seller_uuid, seller_name, item, price, starting_bid, buyout, category, expires"
                        + " FROM " + LISTINGS + " WHERE expires > ?");
        List<Object> params = new java.util.ArrayList<>();
        params.add(System.currentTimeMillis());

        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category);
        }

        if (search != null && !search.isEmpty()) {
            sql.append(" AND (LOWER(seller_name) LIKE ? OR LOWER(item) LIKE ?)");
            String like = "%" + search.toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }

        // Sort
        String order = "id DESC"; // default: newest first
        if ("price_asc".equals(sort)) order = "price ASC";
        else if ("price_desc".equals(sort)) order = "price DESC";
        else if ("oldest".equals(sort)) order = "id ASC";
        sql.append(" ORDER BY ").append(order);

        sql.append(" LIMIT ").append(Math.max(1, limit));
        sql.append(" OFFSET ").append(Math.max(0, offset));

        return store.queryAll(sql.toString(), this::mapListing, params.toArray());
    }

    /** Browse without filters (backward-compatible). */
    public List<Listing> browse(int offset, int limit) {
        return browse(offset, limit, null, null, "newest");
    }

    public List<Listing> bySeller(UUID seller, int offset, int limit) {
        return store.queryAll(
                "SELECT id, seller_uuid, seller_name, item, price, starting_bid, buyout, category, expires"
                        + " FROM " + LISTINGS
                        + " WHERE seller_uuid = ? AND expires > ? ORDER BY id DESC LIMIT " + Math.max(1, limit)
                        + " OFFSET " + Math.max(0, offset),
                this::mapListing, seller, System.currentTimeMillis());
    }

    public List<String> knownCategories() {
        return store.queryAll(
                "SELECT DISTINCT category FROM " + LISTINGS
                        + " WHERE category != '' AND expires > ? ORDER BY category ASC",
                rs -> rs.getString("category"), System.currentTimeMillis());
    }

    private Listing mapListing(java.sql.ResultSet rs) throws java.sql.SQLException {
        UUID seller = null;
        try {
            seller = UUID.fromString(rs.getString("seller_uuid"));
        } catch (IllegalArgumentException ignored) {
        }
        String cat = "";
        try { cat = rs.getString("category"); } catch (java.sql.SQLException ignored) {}
        return new Listing(rs.getInt("id"), seller, rs.getString("seller_name"),
                rs.getString("item"), rs.getDouble("price"),
                rs.getDouble("starting_bid"), rs.getDouble("buyout"),
                cat == null ? "" : cat, rs.getLong("expires"));
    }

    // ── Purchase / cancel / expiry ───────────────────────────────────────────

    /** Buys listing {@code id} for {@code buyer}. See class doc for the race model. */
    public Result buy(Player buyer, int id) {
        if (!isEnabled()) {
            return Result.DISABLED;
        }
        EconomyService economy = plugin.getEconomyService();
        if (economy == null) {
            return Result.FAILED;
        }
        Listing listing = store.queryFirst(
                "SELECT id, seller_uuid, seller_name, item, price, starting_bid, buyout, category, expires"
                        + " FROM " + LISTINGS + " WHERE id = ? AND expires > ?",
                this::mapListing, id, System.currentTimeMillis()).orElse(null);
        if (listing == null) {
            return Result.SOLD_OUT;
        }
        if (buyer.getUniqueId().equals(listing.sellerUuid())) {
            return Result.OWN_LISTING;
        }

        // Auction-type listing — redirect to bid
        if (listing.isAuction()) {
            return bid(buyer, id, listing.buyout() > 0 ? listing.buyout() : listing.startingBid());
        }

        // Decode BEFORE any money moves.
        ItemStack goods = listing.decodeItem();
        if (goods == null) {
            return Result.FAILED;
        }

        // Confirm threshold check
        double threshold = confirmThreshold();
        if (threshold > 0.0 && listing.price() >= threshold
                && !buyer.hasPermission("obx.ah.confirm.bypass")) {
            if (!checkConfirm(buyer, id)) {
                registerConfirm(buyer, id);
                return Result.NEEDS_CONFIRM;
            }
        }

        // Money first (guarded withdraw), then claim the row, then goods.
        if (!economy.withdraw(buyer.getUniqueId(), buyer.getName(), listing.price())) {
            return Result.CANT_AFFORD;
        }
        int rows = store.executeUpdateRows("DELETE FROM " + LISTINGS + " WHERE id = ?", id);
        if (rows <= 0) {
            economy.deposit(buyer.getUniqueId(), buyer.getName(), listing.price());
            return Result.SOLD_OUT;
        }
        double proceeds = EconomyService.sanitize(listing.price() * (1.0 - taxPercent() / 100.0));
        if (proceeds > 0.0 && listing.sellerUuid() != null) {
            if (economy.depositStrict(listing.sellerUuid(), listing.sellerName(), proceeds)) {
                economy.logTransaction(buyer.getName(), listing.sellerUuid(), listing.sellerName(),
                        "AH_SELL", proceeds, economy.getBalance(listing.sellerUuid()));
            } else {
                addReturn(listing.sellerUuid(), null, proceeds, "PROCEEDS");
            }
        }
        // Sales tax → the visible server account (not burned).
        collectForServer(buyer.getName(), "AH_TAX", listing.price() - proceeds);
        economy.logTransaction(buyer.getName(), buyer.getUniqueId(), buyer.getName(),
                "AH_BUY", listing.price(), economy.getBalance(buyer.getUniqueId()));
        deliver(buyer, goods);
        return Result.OK;
    }

    /** Cancels listing {@code id} (must belong to {@code player}); item → returns. */
    public Result cancel(Player player, int id) {
        Listing listing = store.queryFirst(
                "SELECT id, seller_uuid, seller_name, item, price, expires FROM " + LISTINGS + " WHERE id = ?",
                this::mapListing, id).orElse(null);
        if (listing == null) {
            return Result.SOLD_OUT;
        }
        if (!player.getUniqueId().equals(listing.sellerUuid()) && !player.hasPermission("obx.ah.admin")) {
            return Result.NOT_YOURS;
        }
        int rows = store.executeUpdateRows("DELETE FROM " + LISTINGS + " WHERE id = ?", id);
        if (rows <= 0) {
            return Result.SOLD_OUT; // bought in the same instant
        }
        // Refund any existing high bidder and clean up bids
        refundBids(listing, listing.encodedItem);
        addReturn(listing.sellerUuid(), listing.encodedItem, 0.0, "CANCELLED");
        return Result.OK;
    }

    /** Refunds the highest bidder and removes all bids for a listing. */
    private void refundBids(Listing listing, String encodedItem) {
        Bid highBid = currentHighBid(listing.id());
        EconomyService economy = plugin.getEconomyService();
        if (highBid != null && economy != null) {
            economy.deposit(highBid.bidderUuid, highBid.bidderName, highBid.amount);
            economy.logTransaction("CANCEL", highBid.bidderUuid, highBid.bidderName,
                    "AH_BID_REFUND", highBid.amount,
                    economy.getBalance(highBid.bidderUuid));
        }
        store.executeUpdate("DELETE FROM " + BIDS + " WHERE listing_id = ?", listing.id());
    }

    /** Moves expired listings into their sellers' returns ledgers (5-min task). */
    public void sweepExpired() {
        if (!store.isAvailable()) {
            return;
        }
        List<Listing> expired = store.queryAll(
                "SELECT id, seller_uuid, seller_name, item, price, expires FROM " + LISTINGS
                        + " WHERE expires <= ? LIMIT 100",
                this::mapListing, System.currentTimeMillis());
        for (Listing listing : expired) {
            int rows = store.executeUpdateRows("DELETE FROM " + LISTINGS + " WHERE id = ?", listing.id());
            if (rows > 0) {
                refundBids(listing, listing.encodedItem);
                addReturn(listing.sellerUuid(), listing.encodedItem, 0.0, "EXPIRED");
            }
        }
    }

    // ── Returns ledger ───────────────────────────────────────────────────────

    public int pendingReturns(UUID owner) {
        Integer count = store.queryFirst(
                "SELECT COUNT(*) AS c FROM " + RETURNS + " WHERE owner_uuid = ?",
                rs -> rs.getInt("c"), owner).orElse(null);
        return count == null ? 0 : count;
    }

    /**
     * Delivers {@code player}'s pending returns: money via strict deposit (rows kept
     * when the wallet is full), items only while inventory space remains. Each row
     * is claimed by guarded DELETE before delivery so a double-click can't dupe.
     *
     * @return {@code [itemsDelivered, moneyPaidCount]}
     */
    public int[] claim(Player player) {
        EconomyService economy = plugin.getEconomyService();
        List<Object[]> rows = store.queryAll(
                "SELECT id, item, money FROM " + RETURNS + " WHERE owner_uuid = ? ORDER BY id ASC LIMIT 50",
                rs -> new Object[]{rs.getInt("id"), rs.getString("item"), rs.getDouble("money")},
                player.getUniqueId());
        int items = 0;
        int payouts = 0;
        boolean inventoryFull = false;
        for (Object[] row : rows) {
            int id = (Integer) row[0];
            String encoded = (String) row[1];
            double money = (Double) row[2];
            if (money > 0.0) {
                if (economy == null) {
                    continue;
                }
                // Claim the row FIRST (guarded), then pay — paying before claiming
                // would let two parallel claims double-deposit the same row.
                if (store.executeUpdateRows("DELETE FROM " + RETURNS + " WHERE id = ?", id) <= 0) {
                    continue; // raced double-claim
                }
                if (!economy.depositStrict(player.getUniqueId(), player.getName(), money)) {
                    addReturn(player.getUniqueId(), null, money, "PROCEEDS"); // wallet full — requeue
                    continue;
                }
                payouts++;
                continue;
            }
            // Item rows need a free slot. Decode FIRST so a corrupt blob can't make us
            // delete the row and lose the item (delete-then-null-decode = silent loss).
            ItemStack item = ItemCodec.fromBase64(encoded);
            if (item == null) {
                // Unrecoverable encoding — drop the dead row so it can't wedge claiming.
                store.executeUpdateRows("DELETE FROM " + RETURNS + " WHERE id = ?", id);
                continue;
            }
            if (player.getInventory().firstEmpty() < 0) {
                inventoryFull = true;
                continue; // no space for THIS item — keep scanning so later money rows still pay
            }
            if (store.executeUpdateRows("DELETE FROM " + RETURNS + " WHERE id = ?", id) <= 0) {
                continue; // raced double-claim
            }
            deliver(player, item);
            items++;
        }
        // Negative items sentinel = "had item returns but inventory was full" so the
        // caller can show a distinct message instead of a misleading "nothing to claim".
        return new int[]{inventoryFull && items == 0 ? -1 : items, payouts};
    }

    private void addReturn(UUID owner, String encodedItem, double money, String reason) {
        if (owner == null) {
            return;
        }
        store.executeUpdate(
                "INSERT INTO " + RETURNS + " (owner_uuid, item, money, reason, created) VALUES (?, ?, ?, ?, ?)",
                owner, encodedItem, money, reason, System.currentTimeMillis());
    }

    private static void deliver(Player player, ItemStack item) {
        if (item == null) {
            return;
        }
        java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
}