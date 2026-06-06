package dev.zcripted.obx.feature.economy.auction;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import dev.zcripted.obx.feature.economy.util.ItemCodec;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
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
        final long expires;

        Listing(int id, UUID sellerUuid, String sellerName, String encodedItem, double price, long expires) {
            this.id = id;
            this.sellerUuid = sellerUuid;
            this.sellerName = sellerName;
            this.encodedItem = encodedItem;
            this.price = price;
            this.expires = expires;
        }

        public int id() { return id; }
        public UUID sellerUuid() { return sellerUuid; }
        public String sellerName() { return sellerName; }
        public double price() { return price; }
        public long expires() { return expires; }
        public ItemStack decodeItem() { return ItemCodec.fromBase64(encodedItem); }
    }

    /** Outcome of a listing/purchase attempt (drives the message key). */
    public enum Result { OK, DISABLED, TOO_MANY, CANT_AFFORD, SOLD_OUT, OWN_LISTING, NOT_YOURS, FAILED }

    private static final String LISTINGS = "auction_listings";
    private static final String RETURNS = "auction_returns";

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
                + " created INTEGER NOT NULL, expires INTEGER NOT NULL)");
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + RETURNS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, owner_uuid TEXT NOT NULL,"
                + " item TEXT, money REAL NOT NULL DEFAULT 0, reason TEXT, created INTEGER NOT NULL)");
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
     * Lists {@code item} for {@code price}. The item must already be REMOVED from
     * the seller's inventory by the caller (hand-clear precedes this call); on any
     * failure result the caller restores it.
     */
    public Result list(Player seller, ItemStack item, double price) {
        if (!isEnabled()) {
            return Result.DISABLED;
        }
        double cleanPrice = EconomyService.sanitize(price);
        if (cleanPrice <= 0.0 || item == null) {
            return Result.FAILED;
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
        int rows = store.executeUpdateRows(
                "INSERT INTO " + LISTINGS + " (seller_uuid, seller_name, item, price, created, expires)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                seller.getUniqueId(), seller.getName(), encoded, cleanPrice, now, now + durationMillis());
        if (rows <= 0) {
            // The INSERT failed (DB locked / disk full). Refund any listing fee and fail —
            // the caller restores the held item, so nothing is destroyed by a write hiccup.
            if (fee > 0.0 && economy != null) {
                economy.deposit(seller.getUniqueId(), seller.getName(), fee);
            }
            return Result.FAILED;
        }
        return Result.OK;
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

    public List<Listing> browse(int offset, int limit) {
        return store.queryAll(
                "SELECT id, seller_uuid, seller_name, item, price, expires FROM " + LISTINGS
                        + " WHERE expires > ? ORDER BY id DESC LIMIT " + Math.max(1, limit)
                        + " OFFSET " + Math.max(0, offset),
                this::mapListing, System.currentTimeMillis());
    }

    public List<Listing> bySeller(UUID seller, int offset, int limit) {
        return store.queryAll(
                "SELECT id, seller_uuid, seller_name, item, price, expires FROM " + LISTINGS
                        + " WHERE seller_uuid = ? AND expires > ? ORDER BY id DESC LIMIT " + Math.max(1, limit)
                        + " OFFSET " + Math.max(0, offset),
                this::mapListing, seller, System.currentTimeMillis());
    }

    private Listing mapListing(java.sql.ResultSet rs) throws java.sql.SQLException {
        UUID seller = null;
        try {
            seller = UUID.fromString(rs.getString("seller_uuid"));
        } catch (IllegalArgumentException ignored) {
            // corrupt row — listing still renders, just can't match a seller
        }
        return new Listing(rs.getInt("id"), seller, rs.getString("seller_name"),
                rs.getString("item"), rs.getDouble("price"), rs.getLong("expires"));
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
                "SELECT id, seller_uuid, seller_name, item, price, expires FROM " + LISTINGS
                        + " WHERE id = ? AND expires > ?",
                this::mapListing, id, System.currentTimeMillis()).orElse(null);
        if (listing == null) {
            return Result.SOLD_OUT;
        }
        if (buyer.getUniqueId().equals(listing.sellerUuid())) {
            return Result.OWN_LISTING; // own listings are cancelled, not bought
        }
        // Decode BEFORE any money moves: a listing whose item no longer deserializes
        // (e.g. serialized on a newer server version) must fail the purchase outright,
        // not charge the buyer and deliver nothing.
        ItemStack goods = listing.decodeItem();
        if (goods == null) {
            return Result.FAILED;
        }
        // Money first (guarded withdraw), then claim the row, then goods.
        if (!economy.withdraw(buyer.getUniqueId(), buyer.getName(), listing.price())) {
            return Result.CANT_AFFORD;
        }
        int rows = store.executeUpdateRows("DELETE FROM " + LISTINGS + " WHERE id = ?", id);
        if (rows <= 0) {
            // Lost the race (another buyer / a cancel) — full refund.
            economy.deposit(buyer.getUniqueId(), buyer.getName(), listing.price());
            return Result.SOLD_OUT;
        }
        // Seller proceeds (minus auction tax). If their wallet lacks headroom the
        // money goes to their returns ledger instead of evaporating.
        double proceeds = EconomyService.sanitize(listing.price() * (1.0 - taxPercent() / 100.0));
        if (proceeds > 0.0 && listing.sellerUuid() != null) {
            if (economy.depositStrict(listing.sellerUuid(), listing.sellerName(), proceeds)) {
                economy.logTransaction(buyer.getName(), listing.sellerUuid(), listing.sellerName(),
                        "AH_SELL", proceeds, economy.getBalance(listing.sellerUuid()));
            } else {
                addReturn(listing.sellerUuid(), null, proceeds, "PROCEEDS");
            }
        }
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
        addReturn(listing.sellerUuid(), listing.encodedItem, 0.0, "CANCELLED");
        return Result.OK;
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
