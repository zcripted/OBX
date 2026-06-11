package dev.zcripted.obx.feature.backpack.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Per-player portable storage ("backpack"): every player gets exactly one 3-row
 * (27-slot) inventory whose contents live in SQLite — the database is the single
 * source of truth, never the item.
 *
 * <p><b>Virtual vs physical.</b> A backpack starts <em>virtual</em>: {@code /backpack}
 * opens it directly. {@code /backpack convert} flips it to <em>physical</em>: the player
 * receives a tagged item (material chosen per server version, see
 * {@link #resolveItemMaterial()}) that acts as the <em>key</em> — right-clicking it opens
 * the same stored inventory. Because contents stay in the database, losing / burning /
 * destroying the item never loses items.
 *
 * <p><b>Dupe-guard.</b> Each player row carries a random instance token, stamped into the
 * physical item's lore. Only an item whose token matches the database is usable.
 * {@code /backpack respawn} (and every convert/virtualize) <em>rotates</em> the token, so
 * any older copies — duplicated, stashed in a chest, picked up by someone else — become
 * permanently void. Void copies are deleted on sight by the listener. The item itself
 * never stores contents, so duplicating the item can never duplicate items.
 *
 * <p><b>Persistence.</b> Contents are serialized ({@link BukkitObjectOutputStream},
 * Base64) and written on inventory close, player quit, and module disable (where the
 * store's shutdown mode makes the write synchronous). Token/mode changes write
 * synchronously so the dupe-guard can't race a crash.
 */
public final class BackpackService {

    /** 3 rows of portable storage per player. */
    public static final int SIZE = 27;

    private static final String TABLE = "obx_backpack";
    /** Stable, language-independent lore tag prefixes parsed back out of physical items. */
    private static final String OWNER_TAG = "Owner: ";
    private static final String TOKEN_TAG = "ID: ";

    private final ObxPlugin plugin;
    private final Material itemMaterial;

    public BackpackService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.itemMaterial = resolveItemMaterial();
    }

    /** Creates the backing table. Call once from the module's enable. */
    public void load() {
        SqliteDataStore store = plugin.getDataStore();
        if (store == null || !store.isAvailable()) {
            return;
        }
        store.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "uuid TEXT PRIMARY KEY,"
                + "token TEXT NOT NULL,"
                + "physical INTEGER NOT NULL DEFAULT 0,"
                + "contents TEXT)");
    }

    /** Whether the SQLite store is usable; without it backpacks stay closed (no silent item loss). */
    public boolean isStorageAvailable() {
        SqliteDataStore store = plugin.getDataStore();
        return store != null && store.isAvailable();
    }

    // ── profile (token + mode) ───────────────────────────────────────────────

    /** Whether {@code uuid}'s backpack is in physical-item mode. */
    public boolean isPhysical(UUID uuid) {
        return plugin.getDataStore().queryFirst(
                "SELECT physical FROM " + TABLE + " WHERE uuid = ?",
                rs -> rs.getInt("physical") != 0, uuid).orElse(false);
    }

    /** The current (only valid) item token for {@code uuid}, creating the row if absent. */
    public String currentToken(UUID uuid) {
        ensureRow(uuid);
        return plugin.getDataStore().queryFirst(
                "SELECT token FROM " + TABLE + " WHERE uuid = ?",
                rs -> rs.getString("token"), uuid).orElse("");
    }

    /**
     * Rotates {@code uuid}'s instance token and returns the new one. Every previously
     * issued physical item becomes void the moment this commits — the dupe-guard core.
     * Synchronous on purpose: the new item is handed out right after, so the guard
     * must already be durable.
     */
    public String rotateToken(UUID uuid) {
        ensureRow(uuid);
        String token = newToken();
        plugin.getDataStore().executeUpdate(
                "UPDATE " + TABLE + " SET token = ? WHERE uuid = ?", token, uuid);
        return token;
    }

    /** Switches between virtual (false) and physical (true) mode. Synchronous like the token. */
    public void setPhysical(UUID uuid, boolean physical) {
        ensureRow(uuid);
        plugin.getDataStore().executeUpdate(
                "UPDATE " + TABLE + " SET physical = ? WHERE uuid = ?", physical, uuid);
    }

    private void ensureRow(UUID uuid) {
        plugin.getDataStore().executeUpdate(
                "INSERT OR IGNORE INTO " + TABLE + " (uuid, token, physical) VALUES (?, ?, 0)",
                uuid, newToken());
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                .toUpperCase(Locale.ENGLISH);
    }

    // ── open / save ──────────────────────────────────────────────────────────

    /** Opens {@code player}'s stored backpack inventory (loads contents from SQLite). */
    public void open(Player player) {
        ensureRow(player.getUniqueId());
        BackpackHolder holder = new BackpackHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, SIZE,
                plugin.getLanguageManager().get(player, "backpack.title"));
        holder.attach(inventory);
        ItemStack[] stored = loadContents(player.getUniqueId());
        if (stored != null) {
            for (int i = 0; i < Math.min(stored.length, SIZE); i++) {
                inventory.setItem(i, stored[i]);
            }
        }
        player.openInventory(inventory);
    }

    /** Persists {@code contents} for {@code uuid} (async; inline during shutdown). */
    public void saveContents(UUID uuid, ItemStack[] contents) {
        ensureRow(uuid);
        String encoded = toBase64(contents);
        if (encoded == null) {
            // Serialization failed (e.g. a plugin-provided ItemStack that won't
            // serialize). NEVER overwrite the previously-good row with emptiness —
            // skipping the save preserves the last known contents.
            plugin.getLogger().warning("Backpack save skipped for " + uuid
                    + " — contents failed to serialize; previous contents preserved.");
            return;
        }
        plugin.getDataStore().executeUpdateAsync(
                "UPDATE " + TABLE + " SET contents = ? WHERE uuid = ?", encoded, uuid);
    }

    /** Loads {@code uuid}'s stored contents, or {@code null} when empty/unreadable. */
    public ItemStack[] loadContents(UUID uuid) {
        String encoded = plugin.getDataStore().queryFirst(
                "SELECT contents FROM " + TABLE + " WHERE uuid = ?",
                rs -> rs.getString("contents"), uuid).orElse(null);
        return encoded == null || encoded.isEmpty() ? null : fromBase64(encoded);
    }

    /**
     * Saves and closes every open backpack view. Called on module disable so a
     * reload / stop never drops in-flight edits (the store writes inline then).
     */
    public void closeAndSaveAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            try {
                Inventory top = player.getOpenInventory().getTopInventory();
                if (top != null && top.getHolder() instanceof BackpackHolder) {
                    saveContents(((BackpackHolder) top.getHolder()).owner(), top.getContents());
                    player.closeInventory();
                }
            } catch (Throwable ignored) {
                // best-effort per player; never block disable
            }
        }
    }

    // ── physical item ────────────────────────────────────────────────────────

    /**
     * Backpack item material per server generation, newest first:
     * BUNDLE (1.17+) → SHULKER_BOX (1.13+) → PURPLE_SHULKER_BOX (1.11/1.12) →
     * CHEST (1.8.8+, always present). Resolved once at construction.
     */
    static Material resolveItemMaterial() {
        for (String name : new String[]{"BUNDLE", "SHULKER_BOX", "PURPLE_SHULKER_BOX", "CHEST"}) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                return material;
            }
        }
        return Material.CHEST;
    }

    public Material itemMaterial() {
        return itemMaterial;
    }

    /**
     * Builds the tagged physical backpack item for {@code owner} carrying {@code token}.
     * The decorative name/lore are localized; the machine-read Owner/ID tag lines are
     * appended in code so they stay parseable in every language.
     */
    public ItemStack createItem(Player owner, String token) {
        ItemStack item = new ItemStack(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(plugin.getLanguageManager().get(owner, "backpack.item.name",
                Placeholders.with("player", owner.getName())));
        List<String> lore = new ArrayList<>(plugin.getLanguageManager().list(owner,
                "backpack.item.lore", Placeholders.with("player", owner.getName())));
        lore.add(ChatColor.DARK_GRAY + OWNER_TAG + owner.getUniqueId());
        lore.add(ChatColor.DARK_GRAY + TOKEN_TAG + token);
        meta.setLore(lore);
        // Hide bundle-specific tooltip on modern versions (BUNDLE material).
        try {
            meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
        } catch (IllegalArgumentException ignored) {
            // Pre-1.17: flag doesn't exist, not needed.
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Whether {@code item} carries the backpack Owner/ID tag pair (any owner, any token). */
    public boolean isBackpackItem(ItemStack item) {
        return itemOwner(item) != null && itemToken(item) != null;
    }

    /** The owner UUID stamped on {@code item}, or {@code null} when untagged/malformed. */
    public UUID itemOwner(ItemStack item) {
        String value = loreValue(item, OWNER_TAG);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    /** The instance token stamped on {@code item}, or {@code null} when untagged. */
    public String itemToken(ItemStack item) {
        return loreValue(item, TOKEN_TAG);
    }

    /**
     * A physical item is usable only when its owner's backpack is in physical mode AND
     * its token matches the database — the single dupe-guard check.
     */
    public boolean isValidItem(ItemStack item) {
        UUID owner = itemOwner(item);
        String token = itemToken(item);
        return owner != null && token != null
                && isPhysical(owner) && token.equals(currentToken(owner));
    }

    /**
     * Removes every VOID backpack copy tagged with {@code player}'s UUID from their
     * inventory (stale token, or any copy while in virtual mode). The currently valid
     * item is never touched. Returns the number of stacks removed.
     */
    public int removeStaleItems(Player player) {
        int removed = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !player.getUniqueId().equals(itemOwner(item))) {
                continue;
            }
            if (!isValidItem(item)) {
                inventory.setItem(slot, null);
                removed++;
            }
        }
        return removed;
    }

    /**
     * Collapses the player's valid key to exactly ONE item of amount 1. Creative
     * middle-click can clone the key (same owner + same token, even a full stack) —
     * the contents stay single-copy in the database, but surplus keys are clutter,
     * so on every use the used copy is normalized and the extras removed.
     */
    public void normalizeKeyItem(Player player) {
        String token = currentToken(player.getUniqueId());
        boolean keptOne = false;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !player.getUniqueId().equals(itemOwner(item))
                    || !token.equals(itemToken(item))) {
                continue;
            }
            if (!keptOne) {
                keptOne = true;
                if (item.getAmount() > 1) {
                    item.setAmount(1);
                    inventory.setItem(slot, item);
                }
            } else {
                inventory.setItem(slot, null);
            }
        }
    }

    /** Whether {@code player} currently carries their valid (token-matching) backpack item. */
    public boolean hasValidItem(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && player.getUniqueId().equals(itemOwner(item)) && isValidItem(item)) {
                return true;
            }
        }
        return false;
    }

    private static String loreValue(ItemStack item, String tag) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) {
            return null;
        }
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped != null && stripped.startsWith(tag)) {
                return stripped.substring(tag.length()).trim();
            }
        }
        return null;
    }

    // ── serialization ────────────────────────────────────────────────────────

    /**
     * Serializes an inventory snapshot to Base64 via Bukkit's object streams (1.8-safe).
     * Returns {@code null} on failure — callers must SKIP the save (writing "" here used
     * to silently wipe the player's previously-good contents).
     */
    static String toBase64(ItemStack[] contents) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeInt(contents.length);
            for (ItemStack item : contents) {
                out.writeObject(item);
            }
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (Exception failure) {
            return null;
        }
    }

    /** Deserializes {@link #toBase64} output; {@code null} on any corruption (empty backpack). */
    static ItemStack[] fromBase64(String encoded) {
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
            int length = in.readInt();
            ItemStack[] contents = new ItemStack[Math.min(Math.max(length, 0), SIZE)];
            for (int i = 0; i < length; i++) {
                Object item = in.readObject();
                if (i < contents.length) {
                    contents[i] = (ItemStack) item;
                }
            }
            return contents;
        } catch (Exception failure) {
            return null;
        }
    }

    /** Identifies open backpack views and carries the owning player's UUID. */
    public static final class BackpackHolder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        BackpackHolder(UUID owner) {
            this.owner = owner;
        }

        void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        public UUID owner() {
            return owner;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}