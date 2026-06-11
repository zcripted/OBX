package dev.zcripted.obx.feature.economy.shop;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import org.bukkit.Material;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EcoShop-style dynamic shop pricing: every material accumulates a signed trade
 * volume (buys add, sells subtract) that drifts its price multiplier:
 *
 * <pre>m = 1 + volume × sensitivity, clamped to ±max-drift</pre>
 *
 * <p><b>Anti-exploit asymmetry:</b> the BUY multiplier may drift both ways, but the
 * SELL multiplier is additionally capped at {@code 1.0} — sell payouts can sag below
 * base but never rise above it. Without this, a player could pump an item's volume
 * by mass-buying and then sell back at the inflated price ("self-pump" arbitrage);
 * with it, every round trip still loses at least the base buy/sell margin.
 *
 * <p>Volume decays toward neutral by {@code daily-decay} per day (lazily, on read),
 * so prices recover on their own. State persists in {@code shop_dynamic} and the
 * whole system is inert unless {@code dynamic-pricing.enabled} is set in shop.yml.
 */
public final class ShopPricing {

    private static final String TABLE = "shop_dynamic";

    private static final class Entry {
        volatile double volume;
        volatile long updated;

        Entry(double volume, long updated) {
            this.volume = volume;
            this.updated = updated;
        }
    }

    private final ObxPlugin plugin;
    private final SqliteDataStore store;
    private final Map<Material, Entry> volumes = new ConcurrentHashMap<>();

    public ShopPricing(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        volumes.clear();
        if (!store.isAvailable()) {
            return;
        }
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "material TEXT PRIMARY KEY, volume REAL NOT NULL, updated INTEGER NOT NULL)");
        for (Object[] row : store.queryAll(
                "SELECT material, volume, updated FROM " + TABLE,
                rs -> new Object[]{rs.getString("material"), rs.getDouble("volume"), rs.getLong("updated")})) {
            Material material = Material.matchMaterial((String) row[0]);
            if (material != null) {
                volumes.put(material, new Entry((Double) row[1], (Long) row[2]));
            }
        }
    }

    /** Multiplier applied to BUY prices (drifts within ±max-drift). */
    public double buyMultiplier(Material material) {
        ShopService shop = shopService();
        if (shop == null || !shop.dynamicPricingEnabled()) {
            return 1.0;
        }
        double drift = shop.dynamicMaxDrift();
        return clamp(rawMultiplier(shop, material), 1.0 - drift, 1.0 + drift);
    }

    /** Multiplier applied to SELL prices (sags within −max-drift but NEVER above 1.0). */
    public double sellMultiplier(Material material) {
        ShopService shop = shopService();
        if (shop == null || !shop.dynamicPricingEnabled()) {
            return 1.0;
        }
        return clamp(rawMultiplier(shop, material), 1.0 - shop.dynamicMaxDrift(), 1.0);
    }

    /** Records {@code units} bought — pushes the material's price upward. */
    public void recordBuy(Material material, int units) {
        record(material, units);
    }

    /** Records {@code units} sold — pushes the material's price downward. */
    public void recordSell(Material material, int units) {
        record(material, -units);
    }

    private void record(Material material, int signedUnits) {
        ShopService shop = shopService();
        if (shop == null || !shop.dynamicPricingEnabled() || material == null || signedUnits == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Entry entry = volumes.computeIfAbsent(material, key -> new Entry(0.0, now));
        synchronized (entry) {
            entry.volume = decayed(shop, entry, now) + signedUnits;
            entry.updated = now;
        }
        if (store.isAvailable()) {
            store.executeUpdateAsync(
                    "INSERT INTO " + TABLE + " (material, volume, updated) VALUES (?, ?, ?)"
                            + " ON CONFLICT(material) DO UPDATE SET volume = ?, updated = ?",
                    material.name(), entry.volume, now, entry.volume, now);
        }
    }

    private double rawMultiplier(ShopService shop, Material material) {
        Entry entry = volumes.get(material);
        if (entry == null) {
            return 1.0;
        }
        return 1.0 + decayed(shop, entry, System.currentTimeMillis()) * shop.dynamicSensitivity();
    }

    /** The entry's volume after lazily applying {@code daily-decay} per elapsed day. */
    private static double decayed(ShopService shop, Entry entry, long now) {
        double decay = shop.dynamicDailyDecay();
        if (decay <= 0.0) {
            return entry.volume;
        }
        double days = Math.max(0.0, (now - entry.updated) / 86_400_000.0);
        return entry.volume * Math.pow(1.0 - decay, days);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private ShopService shopService() {
        return plugin.getServiceRegistry().get(ShopService.class);
    }
}