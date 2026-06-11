package dev.zcripted.obx.feature.hub.item;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hub.service.HubService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Central registry for the hub hotbar items. Each item is tagged with a
 * stable identifier so the right-click listener can dispatch by ID rather
 * than by display name (which would break under user-customised lores or
 * translations).
 *
 * <p>Identification strategy:
 * <ol>
 *   <li>Modern (1.14+): {@link org.bukkit.persistence.PersistentDataContainer}
 *       via reflection — keyed under {@code obx:hub_item_id}.</li>
 *   <li>Legacy (1.8 – 1.13): an invisible color-coded sentinel embedded at
 *       the end of the last lore line. The marker uses
 *       {@link ChatColor#COLOR_CHAR}-prefixed letters that render as a
 *       zero-width run, so the visible tooltip is unaffected.</li>
 * </ol>
 *
 * <p>Both paths are checked on read, so an item created on a 1.14+ server
 * still resolves on a downgrade and vice versa.
 */
public final class HubItems {

    public static final String ID_SERVER_SELECTOR = "server-selector";
    public static final String ID_JUMP_ROD = "jump-rod";
    public static final String ID_VANISH_ALL = "vanish-all";
    public static final String ID_LAUNCHPAD = "launchpad";

    /** Sub-state markers stored alongside the ID for two-state items. */
    public static final String STATE_VISIBLE = "visible";
    public static final String STATE_HIDDEN = "hidden";

    private static final String LEGACY_SENTINEL_PREFIX = ChatColor.RESET.toString() + ChatColor.BLACK + ChatColor.BLACK;

    // Reflection probes — resolved once, cached. Both null on 1.8 – 1.13.
    private static final Method GET_PERSISTENT_DATA_CONTAINER;
    private static final Method PDC_SET;
    private static final Method PDC_GET;
    private static final Method PDC_HAS;
    private static final Object PDC_STRING_TYPE;

    static {
        Method getContainer = null;
        Method set = null;
        Method get = null;
        Method has = null;
        Object stringType = null;
        try {
            getContainer = ItemMeta.class.getMethod("getPersistentDataContainer");
            Class<?> container = Class.forName("org.bukkit.persistence.PersistentDataContainer");
            Class<?> dataType = Class.forName("org.bukkit.persistence.PersistentDataType");
            stringType = dataType.getField("STRING").get(null);
            set = container.getMethod("set", NamespacedKey.class, dataType, Object.class);
            get = container.getMethod("get", NamespacedKey.class, dataType);
            has = container.getMethod("has", NamespacedKey.class, dataType);
        } catch (Throwable ignored) {
            // Pre-1.14 server — fall through to legacy lore sentinel.
            getContainer = null;
            set = null;
            get = null;
            has = null;
            stringType = null;
        }
        GET_PERSISTENT_DATA_CONTAINER = getContainer;
        PDC_SET = set;
        PDC_GET = get;
        PDC_HAS = has;
        PDC_STRING_TYPE = stringType;
    }

    private HubItems() {
    }

    private static NamespacedKey idKey(ObxPlugin plugin) {
        return new NamespacedKey(plugin, "hub_item_id");
    }

    private static NamespacedKey stateKey(ObxPlugin plugin) {
        return new NamespacedKey(plugin, "hub_item_state");
    }

    /**
     * Returns the hub item ID stored on {@code stack}, or {@code null} if the
     * stack isn't a tracked hub item.
     */
    public static String getId(ObxPlugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        String pdcId = readFromPdc(meta, idKey(plugin));
        if (pdcId != null) {
            return pdcId;
        }
        return readFromLegacySentinel(meta, "id");
    }

    /** Returns the optional sub-state (e.g. visible/hidden) for two-state items. */
    public static String getState(ObxPlugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        String pdcState = readFromPdc(meta, stateKey(plugin));
        if (pdcState != null) {
            return pdcState;
        }
        return readFromLegacySentinel(meta, "state");
    }

    public static boolean isHubItem(ObxPlugin plugin, ItemStack stack) {
        return getId(plugin, stack) != null;
    }

    // ── Builders ───────────────────────────────────────────────────────

    public static ItemStack buildServerSelector(ObxPlugin plugin, HubService hub) {
        Material material = hub.itemMaterial("items.server-selector", Material.matchMaterial("COMPASS") != null
                ? Material.matchMaterial("COMPASS") : Material.STONE);
        return buildHubItem(plugin, material,
                hub.itemName("items.server-selector", "&5&lServer Selector"),
                hub.itemLore("items.server-selector"),
                ID_SERVER_SELECTOR, null);
    }

    public static ItemStack buildJumpRod(ObxPlugin plugin, HubService hub) {
        Material material = hub.itemMaterial("items.jump-rod", Material.matchMaterial("FISHING_ROD") != null
                ? Material.matchMaterial("FISHING_ROD") : Material.STONE);
        return buildHubItem(plugin, material,
                hub.itemName("items.jump-rod", "&d&lJump-To Rod"),
                hub.itemLore("items.jump-rod"),
                ID_JUMP_ROD, null);
    }

    public static ItemStack buildVanishAll(ObxPlugin plugin, HubService hub, boolean playersVisible) {
        String stateKey = playersVisible ? STATE_VISIBLE : STATE_HIDDEN;
        String pathPrefix = "items.vanish-all." + stateKey;
        Material fallback = Material.matchMaterial(playersVisible ? "LIME_DYE" : "GRAY_DYE");
        if (fallback == null) {
            fallback = Material.matchMaterial("INK_SACK");
        }
        if (fallback == null) {
            fallback = Material.STONE;
        }
        Material material = hub.resolveMaterial(pathPrefix, fallback);
        return buildHubItem(plugin, material,
                hub.itemName(pathPrefix, playersVisible ? "&a&lPlayers: Visible" : "&7&lPlayers: Hidden"),
                hub.itemLore(pathPrefix),
                ID_VANISH_ALL, stateKey);
    }

    public static ItemStack buildLaunchpad(ObxPlugin plugin, HubService hub) {
        Material fallback = Material.matchMaterial("FIREWORK_ROCKET");
        if (fallback == null) {
            fallback = Material.matchMaterial("FIREWORK");
        }
        if (fallback == null) {
            fallback = Material.matchMaterial("FEATHER");
        }
        if (fallback == null) {
            fallback = Material.STONE;
        }
        Material material = hub.itemMaterial("items.launchpad", fallback);
        return buildHubItem(plugin, material,
                hub.itemName("items.launchpad", "&e&lLaunchpad"),
                hub.itemLore("items.launchpad"),
                ID_LAUNCHPAD, null);
    }

    // ── Core builder ───────────────────────────────────────────────────

    private static ItemStack buildHubItem(ObxPlugin plugin, Material material, String name, List<String> lore,
                                          String id, String state) {
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(translate(name));
        List<String> processed = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) {
                processed.add(translate(line));
            }
        }
        boolean wrotePdc = writeToPdc(meta, idKey(plugin), id);
        if (state != null) {
            writeToPdc(meta, stateKey(plugin), state);
        }
        if (!wrotePdc) {
            processed.add(buildLegacySentinel(id, state));
        }
        meta.setLore(processed);
        try {
            meta.addItemFlags(ItemFlag.values());
        } catch (Throwable ignored) {
            // ItemFlag enum constants vary by version; ignore unsupported ones.
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static String translate(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    // ── PDC plumbing ───────────────────────────────────────────────────

    private static boolean writeToPdc(ItemMeta meta, NamespacedKey key, String value) {
        if (GET_PERSISTENT_DATA_CONTAINER == null || PDC_SET == null || PDC_STRING_TYPE == null || value == null) {
            return false;
        }
        try {
            Object container = GET_PERSISTENT_DATA_CONTAINER.invoke(meta);
            PDC_SET.invoke(container, key, PDC_STRING_TYPE, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String readFromPdc(ItemMeta meta, NamespacedKey key) {
        if (GET_PERSISTENT_DATA_CONTAINER == null || PDC_GET == null || PDC_HAS == null || PDC_STRING_TYPE == null) {
            return null;
        }
        try {
            Object container = GET_PERSISTENT_DATA_CONTAINER.invoke(meta);
            Object has = PDC_HAS.invoke(container, key, PDC_STRING_TYPE);
            if (!(has instanceof Boolean) || !(Boolean) has) {
                return null;
            }
            Object value = PDC_GET.invoke(container, key, PDC_STRING_TYPE);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ── Legacy sentinel (1.8 – 1.13 fallback) ─────────────────────────

    private static String buildLegacySentinel(String id, String state) {
        StringBuilder sb = new StringBuilder(LEGACY_SENTINEL_PREFIX);
        appendInvisible(sb, "id=" + (id == null ? "" : id));
        if (state != null) {
            appendInvisible(sb, "|state=" + state);
        }
        return sb.toString();
    }

    /** Encodes ASCII payload as alternating COLOR_CHAR+char so the run is invisible. */
    private static void appendInvisible(StringBuilder sb, String payload) {
        for (int i = 0; i < payload.length(); i++) {
            char c = payload.charAt(i);
            sb.append(ChatColor.COLOR_CHAR).append(c);
        }
    }

    private static String readFromLegacySentinel(ItemMeta meta, String field) {
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return null;
        }
        String last = lore.get(lore.size() - 1);
        if (last == null || !last.startsWith(LEGACY_SENTINEL_PREFIX)) {
            return null;
        }
        String payload = decodeInvisible(last.substring(LEGACY_SENTINEL_PREFIX.length()));
        for (String token : payload.split("\\|")) {
            int eq = token.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = token.substring(0, eq);
            String value = token.substring(eq + 1);
            if (key.equals(field)) {
                return value;
            }
        }
        return null;
    }

    private static String decodeInvisible(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ChatColor.COLOR_CHAR) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}