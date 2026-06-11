package dev.zcripted.obx.feature.economy.service;

import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Banknotes ({@code /withdraw <amount>}): converts wallet money into a signed,
 * single-use PAPER item that anyone may redeem by right-clicking.
 *
 * <p><b>Forgery/dupe proofing (same model as backpack keys):</b> the item's lore
 * carries only a random token; the VALUE lives in SQLite ({@code economy_notes}).
 * Redemption is a guarded {@code DELETE WHERE token = ?} — exactly ONE redeem can
 * ever win, so creative-cloned or otherwise duplicated notes become worthless paper
 * the moment any copy is cashed, and edited lore can't mint money (no row = no value).
 */
public final class BanknoteService {

    private static final String TABLE = "economy_notes";
    /** Lore line prefixes used to recognise + parse a note (colour codes stripped first). */
    public static final String VALUE_TAG = "Value: ";
    public static final String TOKEN_TAG = "Note: ";

    private final ObxPlugin plugin;
    private final SqliteDataStore store;

    public BanknoteService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    public void load() {
        if (!store.isAvailable()) {
            return;
        }
        store.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "token TEXT PRIMARY KEY, value REAL NOT NULL, issuer TEXT, ts INTEGER NOT NULL)");
    }

    /**
     * Withdraws {@code amount} from {@code player} and hands them a banknote item.
     * Order matters: money out first (guarded), THEN the token row, THEN the item —
     * a failure at any step leaves no orphaned value.
     *
     * @return the redeemable token, or {@code null} when the withdraw was refused.
     */
    public String issue(Player player, double amount) {
        double value = EconomyService.sanitize(amount);
        if (value <= 0.0 || !store.isAvailable()) {
            return null;
        }
        EconomyService economy = plugin.getEconomyService();
        if (economy == null || !economy.withdraw(player.getUniqueId(), player.getName(), value)) {
            return null;
        }
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        store.executeUpdate("INSERT INTO " + TABLE + " (token, value, issuer, ts) VALUES (?, ?, ?, ?)",
                token, value, player.getName(), System.currentTimeMillis());
        economy.logTransaction(player.getName(), player.getUniqueId(), player.getName(),
                "WITHDRAW", value, economy.getBalance(player.getUniqueId()));
        ItemStack note = createNoteItem(player, value, token);
        java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(note);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        return token;
    }

    /**
     * Redeems {@code token} for {@code player}. The guarded DELETE is the atomic
     * "spend" — 0 rows means the note was already cashed (or forged).
     *
     * @return the redeemed value, {@code -1} for invalid/used notes, {@code -2} when
     *         the player's balance lacks headroom (note stays valid).
     */
    public double redeem(Player player, String token) {
        if (token == null || !store.isAvailable()) {
            return -1;
        }
        EconomyService economy = plugin.getEconomyService();
        if (economy == null) {
            return -1;
        }
        Double value = store.queryFirst("SELECT value FROM " + TABLE + " WHERE token = ?",
                rs -> rs.getDouble("value"), token).orElse(null);
        if (value == null || value <= 0.0) {
            return -1;
        }
        // Headroom first: a cap refusal must leave the note intact.
        if (economy.getBalance(player.getUniqueId()) + value > EconomyService.MAX_BALANCE) {
            return -2;
        }
        int rows = store.executeUpdateRows("DELETE FROM " + TABLE + " WHERE token = ?", token);
        if (rows <= 0) {
            return -1; // raced: someone else cashed a copy first
        }
        if (!economy.depositStrict(player.getUniqueId(), player.getName(), value)) {
            // Headroom vanished between check and pay — restore the note row.
            store.executeUpdate("INSERT INTO " + TABLE + " (token, value, issuer, ts) VALUES (?, ?, ?, ?)",
                    token, value, player.getName(), System.currentTimeMillis());
            return -2;
        }
        economy.logTransaction(player.getName(), player.getUniqueId(), player.getName(),
                "REDEEM", value, economy.getBalance(player.getUniqueId()));
        return value;
    }

    /** Whether {@code item} looks like a banknote (material + token lore line). */
    public boolean isNoteItem(ItemStack item) {
        return itemToken(item) != null;
    }

    /** The note token on {@code item}, or {@code null} when it isn't a banknote. */
    public String itemToken(ItemStack item) {
        if (item == null || item.getType() != noteMaterial() || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) {
            return null;
        }
        for (String line : meta.getLore()) {
            String plain = ChatColor.stripColor(line);
            if (plain != null && plain.startsWith(TOKEN_TAG)) {
                String token = plain.substring(TOKEN_TAG.length()).trim();
                return token.isEmpty() ? null : token;
            }
        }
        return null;
    }

    private ItemStack createNoteItem(Player player, double value, String token) {
        EconomyService economy = plugin.getEconomyService();
        ItemStack note = new ItemStack(noteMaterial(), 1);
        ItemMeta meta = note.getItemMeta();
        if (meta != null) {
            String formatted = economy == null ? String.valueOf(value) : economy.format(value);
            meta.setDisplayName(plugin.getLanguageManager().get(player, "economy.note.item.name",
                    java.util.Collections.singletonMap("amount", formatted)));
            List<String> lore = new java.util.ArrayList<>(plugin.getLanguageManager().list(
                    player, "economy.note.item.lore",
                    dev.zcripted.obx.util.text.Placeholders.with(
                            "amount", formatted, "issuer", player.getName())));
            lore.add(ChatColor.DARK_GRAY + VALUE_TAG + formatted);
            lore.add(ChatColor.DARK_GRAY + TOKEN_TAG + token);
            meta.setLore(lore);
            note.setItemMeta(meta);
        }
        return note;
    }

    private static Material noteMaterial() {
        Material paper = Material.matchMaterial("PAPER");
        return paper == null ? Material.STONE : paper;
    }

    /**
     * Returns configured banknote denominations from
     * {@code economy.banknote.denominations} (a list of numbers).
     * When empty or not configured, any amount can be withdrawn as a single note.
     */
    public List<Double> denominations() {
        List<?> raw = plugin.getConfig().getList("economy.banknote.denominations");
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<Double> result = new ArrayList<>();
        for (Object val : raw) {
            if (val instanceof Number) {
                double d = ((Number) val).doubleValue();
                if (d > 0) result.add(d);
            }
        }
        return result;
    }

    /**
     * Whether banknotes placed alone in a crafting grid can be cashed in
     * ({@code economy.banknote.craft-redeem}). The redemption itself lives in
     * {@code BanknoteListener}'s craft handlers; each note still goes through
     * the guarded {@link #redeem} so craft redemption can't dupe either.
     */
    public boolean craftRedeemEnabled() {
        return plugin.getConfig().getBoolean("economy.banknote.craft-redeem", true);
    }

    /** The stored value behind {@code token}, or {@code 0} for void/forged notes. */
    public double tokenValue(String token) {
        if (token == null || !store.isAvailable()) {
            return 0.0;
        }
        Double value = store.queryFirst("SELECT value FROM " + TABLE + " WHERE token = ?",
                rs -> rs.getDouble("value"), token).orElse(null);
        return value == null || value <= 0.0 ? 0.0 : value;
    }
}