package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.gui.MenuHolder;
import dev.zcripted.obx.core.language.LanguageManager;

import static dev.zcripted.obx.feature.staff.gui.AdminMenuRender.createBackItem;
import static dev.zcripted.obx.feature.staff.gui.AdminMenuRender.createBackItemToServerControl;
import static dev.zcripted.obx.feature.staff.gui.AdminMenuRender.createBackItemToWorldControls;
import static dev.zcripted.obx.feature.staff.gui.AdminMenuRender.createCloseItem;
import static dev.zcripted.obx.feature.staff.gui.AdminMenuRender.createMenuItem;
import static dev.zcripted.obx.feature.staff.gui.AdminMenuRender.fillWithFiller;
import static dev.zcripted.obx.feature.staff.gui.AdminMenuRender.hideAttributes;
import static dev.zcripted.obx.feature.staff.gui.AdminMenuRender.place;
import static dev.zcripted.obx.feature.staff.gui.AdminMenuRender.resolveMaterial;
import dev.zcripted.obx.feature.world.service.DaylightCycleFallback;
import dev.zcripted.obx.util.text.Placeholders;
import dev.zcripted.obx.feature.world.service.ServerControlActions;
import dev.zcripted.obx.feature.world.service.ServerControlState;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AdminSubMenu {

    public static final int BACK_SLOT = 22;
    private static final int SERVER_BACK_SLOT = 31;
    // Economy Control nav lives on its own bottom row (36-slot menu): back + close
    // centered, away from the action tiles — slot 22 hosts the Shop tile there.
    public static final int ECONOMY_BACK_SLOT = 30;
    public static final int ECONOMY_CLOSE_SLOT = 32;
    private static final int CLOSE_SLOT = AdminMenu.CLOSE_SLOT;
    // Dedicated nav slots for the 54-slot Game Rule Editor — kept in the bottom row,
    // away from the gamerule grid (slots 0–44) so close/back never mix with rules.
    public static final int GAMERULE_BACK_SLOT = 45;
    public static final int GAMERULE_CLOSE_SLOT = 53;

    private static final Set<UUID> stopConfirmations = new HashSet<>();

    // Padlock glyphs prepended to the Lock / Unlock Server buttons. Built from
    // code points (ASCII source) so the file stays encoding-safe regardless of
    // how it is saved — same reasoning as the math-bold escapes in MessageDefaults.
    private static final String LOCK_ICON = new String(Character.toChars(0x1F512));   // closed padlock
    private static final String UNLOCK_ICON = new String(Character.toChars(0x1F513)); // open padlock

    public enum SubMenuType {
        GENERIC,
        SERVER_CONTROL,
        SERVER_STATE,
        PLAYER_ACCESS,
        PERFORMANCE_HEALTH,
        WORLD_CONTROLS,
        WEATHER,
        TIME,
        GAMERULES,
        PLUGIN_SYSTEMS,
        HUB,
        JAIL_CENTER,
        MOB_TOOLS,
        WORLD_BORDER,
        MODULES,
        ECONOMY,
        CONFIRM
    }

    private AdminSubMenu() {
    }

    // ── Localization helpers ─────────────────────────────────────────────────
    // ObxPlugin is an interface implemented by the bootstrap in the plugin module,
    // so a feature module resolves it via the providing plugin instead of a hard
    // reference. Every menu/item below is rendered per-player in EN/DE/ES.

    private static LanguageManager languages() {
        return ((ObxPlugin) JavaPlugin.getProvidingPlugin(AdminSubMenu.class)).getLanguageManager();
    }

    private static String t(Player player, String key) {
        return languages().get(player, key);
    }

    private static String t(Player player, String key, Map<String, String> placeholders) {
        return languages().get(player, key, placeholders);
    }

    private static List<String> tl(Player player, String key) {
        return languages().list(player, key, Collections.<String, String>emptyMap());
    }

    private static List<String> tl(Player player, String key, Map<String, String> placeholders) {
        return languages().list(player, key, placeholders);
    }

    /** Localized, colored ENABLED / DISABLED word for status lines. */
    private static String onOff(Player player, boolean enabled) {
        return t(player, enabled ? "admin.gui.common.enabled" : "admin.gui.common.disabled");
    }

    /** Localized gradient title for an admin submenu. */
    private static String title(Player player, String key) {
        return AdminMenu.gradientTitle(ChatColor.stripColor(t(player, key)));
    }

    private static Map<String, String> map(String... kv) {
        Map<String, String> out = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out.put(kv[i], kv[i + 1]);
        }
        return out;
    }

    public static void open(ObxPlugin plugin, Player player, AdminMenu.PlaceholderView placeholder) {
        // Route on the stable localization key (not the display name), so navigation
        // works identically for EN/DE/ES players.
        String key = placeholder.key();
        if ("server-control".equals(key)) {
            openServerControlMenu(player, placeholder);
        } else if ("moderation".equals(key)) {
            openJailCenterMenu(plugin, player, placeholder);
        } else if ("fun-utilities".equals(key)) {
            openMobToolsMenu(plugin, player, placeholder);
        } else if ("economy".equals(key)) {
            openEconomyMenu(plugin, player, placeholder);
        } else {
            openGenericMenu(player, placeholder);
        }
    }

    // ── Economy Control Panel (reached from the AdminMenu Economy tile) ────────

    /**
     * Economy Control Panel: every item carries LIVE data pulled on open/refresh —
     * the top balances, total supply / account count / average, the worth.yml price
     * count, the latest audit-log movement, and the shop category count. Click
     * actions are dispatched from {@link #handleEconomyMenuClick}.
     */
    public static void openEconomyMenu(ObxPlugin plugin, Player player, AdminMenu.PlaceholderView placeholder) {
        Holder holder = new Holder(placeholder, SubMenuType.ECONOMY);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.economy"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);

        dev.zcripted.obx.api.economy.EconomyService economy = plugin.getEconomyService();

        // Row 1: currency card · Row 2: action tiles centered at 11–15 · Row 3: shop
        // centered · Row 4 (nav-only): back + close.
        place(inventory, 4, economyCurrencyItem(player, economy));
        place(inventory, 11, economyTopItem(player, economy));
        place(inventory, 12, economyOverviewItem(player, economy));
        place(inventory, 13, createMenuItem(new String[]{"PLAYER_HEAD", "SKULL_ITEM"},
                t(player, "admin.gui.eco.manage.name"), tl(player, "admin.gui.eco.manage.lore")));
        place(inventory, 14, economyWorthItem(plugin, player));
        place(inventory, 15, economyTransactionsItem(player, economy));
        place(inventory, 22, economyShopItem(plugin, player));

        place(inventory, ECONOMY_BACK_SLOT, createBackItem(player));
        place(inventory, ECONOMY_CLOSE_SLOT, createCloseItem(player));
        player.openInventory(inventory);
    }

    private static dev.zcripted.obx.api.economy.EconomyService economyService(ObxPlugin plugin) {
        return plugin.getEconomyService();
    }

    private static ItemStack economyCurrencyItem(Player player, dev.zcripted.obx.api.economy.EconomyService economy) {
        boolean up = economy != null;
        return createMenuItem(new String[]{"SUNFLOWER", "DOUBLE_PLANT", "GOLD_NUGGET"},
                t(player, "admin.gui.eco.currency.name"),
                tl(player, "admin.gui.eco.currency.lore", map(
                        "symbol", up ? economy.getCurrencySymbol() : "?",
                        "name", up ? economy.getCurrencyName() : "?",
                        "plural", up ? economy.getCurrencyNamePlural() : "?",
                        "starting", up ? economy.format(economy.getStartingBalance()) : "?")));
    }

    private static ItemStack economyTopItem(Player player, dev.zcripted.obx.api.economy.EconomyService economy) {
        String[] rows = {"—", "—", "—"};
        int accounts = -1;
        if (economy != null) {
            // 3s-cached snapshot — the 0.5s refresh cadence must not hit SQLite per render.
            EconomyStats.Snapshot stats = EconomyStats.get(economy);
            for (int i = 0; i < stats.top.size() && i < 3; i++) {
                rows[i] = stats.top.get(i).getName() + " §8— §a" + economy.format(stats.top.get(i).getBalance());
            }
            accounts = stats.accounts;
        }
        return createMenuItem(new String[]{"GOLD_BLOCK"}, t(player, "admin.gui.eco.top.name"),
                tl(player, "admin.gui.eco.top.lore", map(
                        "one", rows[0], "two", rows[1], "three", rows[2],
                        "accounts", accounts < 0 ? "?" : String.valueOf(accounts))));
    }

    private static ItemStack economyOverviewItem(Player player, dev.zcripted.obx.api.economy.EconomyService economy) {
        String supply = "?";
        String accounts = "?";
        String average = "?";
        String starting = "?";
        if (economy != null) {
            EconomyStats.Snapshot stats = EconomyStats.get(economy);
            supply = stats.supply < 0 ? "?" : economy.format(stats.supply);
            accounts = stats.accounts < 0 ? "?" : String.valueOf(stats.accounts);
            average = (stats.supply < 0 || stats.accounts <= 0) ? "?" : economy.format(stats.supply / stats.accounts);
            starting = economy.format(economy.getStartingBalance());
        }
        return createMenuItem(new String[]{"EMERALD"}, t(player, "admin.gui.eco.overview.name"),
                tl(player, "admin.gui.eco.overview.lore", map(
                        "supply", supply, "accounts", accounts, "average", average, "starting", starting)));
    }

    private static ItemStack economyWorthItem(ObxPlugin plugin, Player player) {
        dev.zcripted.obx.feature.economy.service.WorthService worth =
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.service.WorthService.class);
        int priced = worth == null ? -1 : worth.pricedCount();
        return createMenuItem(new String[]{"HOPPER"}, t(player, "admin.gui.eco.worth.name"),
                tl(player, "admin.gui.eco.worth.lore", map("count", priced < 0 ? "?" : String.valueOf(priced))));
    }

    private static ItemStack economyTransactionsItem(Player player, dev.zcripted.obx.api.economy.EconomyService economy) {
        String[] rows = {t(player, "admin.gui.eco.tx.none"), "", ""};
        if (economy != null) {
            java.util.List<dev.zcripted.obx.api.economy.EconomyService.TransactionEntry> latest =
                    EconomyStats.get(economy).recent;
            java.text.SimpleDateFormat time = new java.text.SimpleDateFormat("MM-dd HH:mm");
            for (int i = 0; i < latest.size() && i < 3; i++) {
                dev.zcripted.obx.api.economy.EconomyService.TransactionEntry entry = latest.get(i);
                rows[i] = t(player, "admin.gui.eco.tx.row", map(
                        "time", time.format(new java.util.Date(entry.getTime())),
                        "action", entry.getAction(),
                        "amount", economy.format(entry.getAmount()),
                        "target", entry.getTargetName() == null ? "?" : entry.getTargetName()));
            }
        }
        return createMenuItem(new String[]{"WRITABLE_BOOK", "BOOK_AND_QUILL", "BOOK"},
                t(player, "admin.gui.eco.tx.name"),
                tl(player, "admin.gui.eco.tx.lore", map("one", rows[0], "two", rows[1], "three", rows[2])));
    }

    private static ItemStack economyShopItem(ObxPlugin plugin, Player player) {
        dev.zcripted.obx.feature.economy.shop.ShopService shop =
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.shop.ShopService.class);
        int categories = shop == null ? -1 : shop.getCategories().size();
        return createMenuItem(new String[]{"EMERALD_BLOCK"}, t(player, "admin.gui.eco.shop.name"),
                tl(player, "admin.gui.eco.shop.lore", map("count", categories < 0 ? "?" : String.valueOf(categories))));
    }

    /** Click dispatch for the Economy Control Panel. */
    public static void handleEconomyMenuClick(ObxPlugin plugin, Player player, int slot, ClickType click,
                                              AdminMenu.PlaceholderView placeholder) {
        switch (slot) {
            case 11:
                player.closeInventory();
                player.performCommand("baltop");
                return;
            case 12:
                openEconomyMenu(plugin, player, placeholder); // refresh the live stats
                return;
            case 13:
                player.closeInventory();
                sendManageBalancesPanel(plugin, player);
                return;
            case 14: {
                final dev.zcripted.obx.feature.economy.service.WorthService worth =
                        plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.service.WorthService.class);
                if (worth == null) {
                    return;
                }
                openConfirmMenu(player, placeholder, new String[]{"HOPPER"},
                        t(player, "admin.gui.eco.worth.name"),
                        tl(player, "admin.gui.eco.confirm.worth"),
                        clicker -> {
                            worth.reload();
                            languages().send(clicker, "admin.gui.eco.worth-reloaded",
                                    Collections.singletonMap("count", String.valueOf(worth.pricedCount())));
                            openEconomyMenu(plugin, clicker, placeholder);
                        },
                        clicker -> openEconomyMenu(plugin, clicker, placeholder));
                return;
            }
            case 15:
                player.closeInventory();
                player.performCommand("eco log");
                return;
            case 22: {
                final dev.zcripted.obx.feature.economy.shop.ShopService shop =
                        plugin.getServiceRegistry().get(dev.zcripted.obx.feature.economy.shop.ShopService.class);
                if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                    if (shop == null) {
                        return;
                    }
                    openConfirmMenu(player, placeholder, new String[]{"EMERALD_BLOCK"},
                            t(player, "admin.gui.eco.shop.name"),
                            tl(player, "admin.gui.eco.confirm.shop"),
                            clicker -> {
                                shop.reload();
                                languages().send(clicker, "admin.gui.eco.shop-reloaded",
                                        Collections.singletonMap("count", String.valueOf(shop.getCategories().size())));
                                openEconomyMenu(plugin, clicker, placeholder);
                            },
                            clicker -> openEconomyMenu(plugin, clicker, placeholder));
                } else {
                    player.closeInventory();
                    player.performCommand("shop");
                }
                return;
            }
            default:
                // informational tiles (4) and filler — ignore
        }
    }

    /**
     * Chat action panel for "Manage Balances": one row of click-to-suggest buttons
     * that prefill the matching {@code /eco} command (works for offline players too).
     */
    private static void sendManageBalancesPanel(ObxPlugin plugin, Player player) {
        languages().send(player, "admin.gui.eco.manage.chat.header");
        java.util.List<ComponentMessenger.InteractiveMessagePart> parts = new java.util.ArrayList<>();
        parts.add(ComponentMessenger.InteractiveMessagePart.plain("  "));
        String[][] actions = {
                {"admin.gui.eco.manage.chat.give", "/eco give "},
                {"admin.gui.eco.manage.chat.take", "/eco take "},
                {"admin.gui.eco.manage.chat.set", "/eco set "},
                {"admin.gui.eco.manage.chat.reset", "/eco reset "},
                {"admin.gui.eco.manage.chat.log", "/eco log "}
        };
        for (int i = 0; i < actions.length; i++) {
            if (i > 0) {
                parts.add(ComponentMessenger.InteractiveMessagePart.plain("  "));
            }
            parts.add(ComponentMessenger.InteractiveMessagePart.interactive(
                    t(player, actions[i][0]),
                    tl(player, "admin.gui.eco.manage.chat.hover", map("command", actions[i][1].trim())),
                    actions[i][1], false));
        }
        ComponentMessenger.sendJoinedHoverMessages(player, parts);
        player.sendMessage(" ");
    }

    // ── Reusable confirmation step for destructive admin actions ──────────────

    /**
     * Generic confirmation menu: an info card flanked by green Confirm / red Cancel.
     * The actions are carried on the holder and dispatched by
     * {@link #handleConfirmMenuClick}. Used by the Economy panel's reload actions;
     * open one from any submenu by passing the appropriate reopen-cancel runnable.
     */
    public static void openConfirmMenu(Player player, AdminMenu.PlaceholderView placeholder, String[] materials,
                                       String subjectName, List<String> infoLore,
                                       java.util.function.Consumer<Player> onConfirm,
                                       java.util.function.Consumer<Player> onCancel) {
        Holder holder = new Holder(placeholder, SubMenuType.CONFIRM);
        holder.setConfirmActions(onConfirm, onCancel);
        Inventory inventory = Bukkit.createInventory(holder, 27, title(player, "admin.gui.title.confirm"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        place(inventory, 11, createMenuItem(new String[]{"LIME_WOOL", "EMERALD_BLOCK"},
                t(player, "admin.gui.confirm.confirm.name"), tl(player, "admin.gui.confirm.confirm.lore")));
        place(inventory, 13, createMenuItem(materials, subjectName, infoLore));
        place(inventory, 15, createMenuItem(new String[]{"RED_WOOL", "REDSTONE_BLOCK"},
                t(player, "admin.gui.confirm.cancel.name"), tl(player, "admin.gui.confirm.cancel.lore")));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        player.openInventory(inventory);
    }

    /**
     * Confirm-menu dispatch: 11 = confirm, 15/back = cancel. The action receives the
     * CLICKING player (not a reference captured when the menu opened), so a relog
     * between open and confirm can never execute against a stale entity.
     */
    public static void handleConfirmMenuClick(Player player, Holder holder, int slot) {
        if (slot == 11) {
            java.util.function.Consumer<Player> confirm = holder.confirmAction();
            if (confirm != null) {
                confirm.accept(player);
            }
        } else if (slot == 15 || slot == BACK_SLOT) {
            java.util.function.Consumer<Player> cancel = holder.cancelAction();
            if (cancel != null) {
                cancel.accept(player);
            } else {
                player.closeInventory();
            }
        }
    }

    public static void openJailCenterMenu(ObxPlugin plugin, Player player, AdminMenu.PlaceholderView placeholder) {
        Holder holder = new Holder(placeholder, SubMenuType.JAIL_CENTER);
        Inventory inventory = Bukkit.createInventory(holder, 27, title(player, "admin.gui.title.jail-center"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);

        int jailCount = plugin.getServiceRegistry().get(dev.zcripted.obx.api.jail.JailApi.class) == null ? 0 : plugin.getServiceRegistry().get(dev.zcripted.obx.api.jail.JailApi.class).getJails().size();
        StringBuilder jailList = new StringBuilder();
        if (plugin.getServiceRegistry().get(dev.zcripted.obx.api.jail.JailApi.class) != null) {
            int i = 0;
            for (dev.zcripted.obx.api.jail.Jail jail : plugin.getServiceRegistry().get(dev.zcripted.obx.api.jail.JailApi.class).getJails()) {
                if (i++ > 0) jailList.append(", ");
                jailList.append(jail.getName());
            }
        }
        String jailListLine = jailList.length() == 0 ? t(player, "admin.gui.jail.none") : ChatColor.GRAY + jailList.toString();

        place(inventory, 10, createMenuItem(new String[]{"IRON_BARS"}, t(player, "admin.gui.jail.anchors.name"),
                tl(player, "admin.gui.jail.anchors.lore", map("count", String.valueOf(jailCount), "list", jailListLine))));
        place(inventory, 12, createMenuItem(new String[]{"COMPASS"}, t(player, "admin.gui.jail.set.name"),
                tl(player, "admin.gui.jail.set.lore")));
        place(inventory, 14, createMenuItem(new String[]{"REDSTONE_BLOCK"}, t(player, "admin.gui.jail.delete.name"),
                tl(player, "admin.gui.jail.delete.lore")));
        place(inventory, 16, createMenuItem(new String[]{"CLOCK", "WATCH"}, t(player, "admin.gui.jail.time.name"),
                tl(player, "admin.gui.jail.time.lore")));
        place(inventory, BACK_SLOT, createBackItem(player));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        player.openInventory(inventory);
    }

    public static void openMobToolsMenu(ObxPlugin plugin, Player player, AdminMenu.PlaceholderView placeholder) {
        Holder holder = new Holder(placeholder, SubMenuType.MOB_TOOLS);
        Inventory inventory = Bukkit.createInventory(holder, 27, title(player, "admin.gui.title.mob-tools"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);

        place(inventory, 10, createMenuItem(new String[]{"DIAMOND_SWORD"}, t(player, "admin.gui.mob.butcher.name"),
                tl(player, "admin.gui.mob.butcher.lore")));
        place(inventory, 12, createMenuItem(new String[]{"BLAZE_POWDER"}, t(player, "admin.gui.mob.spawn.name"),
                tl(player, "admin.gui.mob.spawn.lore")));
        place(inventory, 14, createMenuItem(new String[]{"LIGHTNING_ROD", "TRIDENT"}, t(player, "admin.gui.mob.smite.name"),
                tl(player, "admin.gui.mob.smite.lore")));
        place(inventory, 16, createMenuItem(new String[]{"OAK_SAPLING", "SAPLING"}, t(player, "admin.gui.mob.grow-tree.name"),
                tl(player, "admin.gui.mob.grow-tree.lore")));
        place(inventory, BACK_SLOT, createBackItem(player));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        player.openInventory(inventory);
    }

    private static void openGenericMenu(Player player, AdminMenu.PlaceholderView placeholder) {
        Holder holder = new Holder(placeholder, SubMenuType.GENERIC);
        Inventory inventory = Bukkit.createInventory(holder, 27, AdminMenu.gradientTitle(ChatColor.stripColor(placeholder.name())));
        holder.setInventory(inventory);
        fillWithFiller(inventory);

        ItemStack info = new ItemStack(resolveMaterial(placeholder.materials()));
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(placeholder.name());
            meta.setLore(placeholder.lore());
            info.setItemMeta(meta);
        }
        inventory.setItem(13, info);

        place(inventory, BACK_SLOT, createBackItem(player));
        place(inventory, CLOSE_SLOT, createCloseItem(player));

        player.openInventory(inventory);
    }

    private static void openServerControlMenu(Player player, AdminMenu.PlaceholderView placeholder) {
        Holder holder = new Holder(placeholder, SubMenuType.SERVER_CONTROL);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.server-control"));
        holder.setInventory(inventory);

        fillWithFiller(inventory);

        place(inventory, 10, serverStateOverviewItem(player));
        place(inventory, 12, playerAccessOverviewItem(player));
        place(inventory, 14, performanceOverviewItem(player));
        place(inventory, 16, worldControlsOverviewItem(player));

        place(inventory, 19, createMenuItem(new String[]{"REDSTONE_COMPARATOR", "COMPARATOR"},
                t(player, "admin.gui.sc.plugin-systems.name"), tl(player, "admin.gui.sc.plugin-systems.lore")));

        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createBackItem(player));

        player.openInventory(inventory);
    }

    public static void openServerStateMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.SERVER_STATE);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.server-state"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        place(inventory, 10, createMenuItem(new String[]{"BARRIER"}, t(player, "admin.gui.ss.stop.name"),
                tl(player, "admin.gui.ss.stop.lore")));
        place(inventory, 12, createMenuItem(new String[]{"REDSTONE_BLOCK"}, t(player, "admin.gui.ss.restart.name"),
                tl(player, "admin.gui.ss.restart.lore")));
        place(inventory, 14, createMenuItem(new String[]{"TOTEM_OF_UNDYING"}, t(player, "admin.gui.ss.safe-restart.name"),
                tl(player, "admin.gui.ss.safe-restart.lore")));
        place(inventory, 16, lockServerItem(player));
        place(inventory, 19, unlockServerItem(player));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createBackItemToServerControl(player));
        player.openInventory(inventory);
    }

    public static void openPlayerAccessMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.PLAYER_ACCESS);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.player-access"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        place(inventory, 10, whitelistToggleItem(player));
        place(inventory, 12, joinLockToggleItem(player));
        place(inventory, 14, maxPlayersItem(player));
        place(inventory, 16, createMenuItem(new String[]{"BARRIER"}, t(player, "admin.gui.pa.kick-nonops.name"),
                tl(player, "admin.gui.pa.kick-nonops.lore")));
        place(inventory, 19, createMenuItem(new String[]{"ENDER_EYE"}, t(player, "admin.gui.pa.spectator.name"),
                tl(player, "admin.gui.pa.spectator.lore")));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createBackItemToServerControl(player));
        player.openInventory(inventory);
    }

    public static void openPerformanceMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.PERFORMANCE_HEALTH);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.performance"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        place(inventory, 10, createMenuItem(new String[]{"CLOCK", "WATCH"}, t(player, "admin.gui.perf.tps.name"),
                tl(player, "admin.gui.perf.tps.lore")));
        place(inventory, 12, createMenuItem(new String[]{"HOPPER"}, t(player, "admin.gui.perf.clear.name"),
                tl(player, "admin.gui.perf.clear.lore")));
        place(inventory, 14, redstoneToggleItem(player));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createBackItemToServerControl(player));
        player.openInventory(inventory);
    }

    public static void openWorldControlsMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.WORLD_CONTROLS);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.world-controls"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        boolean autoSave = detectAutoSave();
        boolean daylight = detectDaylightCycle();
        place(inventory, 10, createMenuItem(new String[]{"BOOK"}, t(player, "admin.gui.wc.save.name"),
                tl(player, "admin.gui.wc.save.lore")));
        place(inventory, 12, createMenuItem(new String[]{"REDSTONE_COMPARATOR", "COMPARATOR"}, t(player, "admin.gui.wc.autosave.name"),
                tl(player, "admin.gui.wc.autosave.lore", map("autosave", onOff(player, autoSave)))));
        place(inventory, 14, createMenuItem(new String[]{"MAP"}, t(player, "admin.gui.wc.border.name"),
                worldBorderLore(player)));
        place(inventory, 16, createMenuItem(new String[]{"WATER_BUCKET"}, t(player, "admin.gui.wc.weather.name"),
                weatherControlLore(player)));
        place(inventory, 19, createMenuItem(new String[]{"CLOCK", "WATCH"}, t(player, "admin.gui.wc.time.name"),
                tl(player, "admin.gui.wc.time.lore", map("daylight", onOff(player, daylight)))));
        place(inventory, 21, createMenuItem(new String[]{"WRITABLE_BOOK", "BOOK_AND_QUILL"}, t(player, "admin.gui.wc.gamerule.name"),
                gameruleEditorLore(player)));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createBackItemToServerControl(player));
        player.openInventory(inventory);
    }

    public static void openWeatherMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.WEATHER);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.weather"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        Map<String, String> current = map("current", weatherStateText(player));
        place(inventory, 11, createMenuItem(new String[]{"SUNFLOWER", "GLOWSTONE_DUST"}, t(player, "admin.gui.weather-menu.clear.name"),
                tl(player, "admin.gui.weather-menu.clear.lore", current)));
        place(inventory, 13, createMenuItem(new String[]{"WATER_BUCKET"}, t(player, "admin.gui.weather-menu.rain.name"),
                tl(player, "admin.gui.weather-menu.rain.lore", current)));
        place(inventory, 15, createMenuItem(new String[]{"TRIDENT", "NETHER_STAR"}, t(player, "admin.gui.weather-menu.thunder.name"),
                tl(player, "admin.gui.weather-menu.thunder.lore", current)));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createBackItemToWorldControls(player));
        player.openInventory(inventory);
    }

    public static void openTimeMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.TIME);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.time"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        long time = detectTime();
        boolean daylight = detectDaylightCycle();
        String timeStr = String.valueOf(time);
        String daylightOn = onOff(player, true);
        place(inventory, 11, createMenuItem(new String[]{"HONEYCOMB", "TORCH"}, t(player, "admin.gui.time.morning.name"),
                tl(player, "admin.gui.time.morning.lore", map("daylight", daylightOn, "time", timeStr))));
        place(inventory, 13, createMenuItem(new String[]{"CLOCK", "WATCH"}, t(player, "admin.gui.time.noon.name"),
                tl(player, "admin.gui.time.noon.lore", map("daylight", daylightOn, "time", timeStr))));
        place(inventory, 15, createMenuItem(new String[]{"ENDER_PEARL"}, t(player, "admin.gui.time.night.name"),
                tl(player, "admin.gui.time.night.lore", map("daylight", daylightOn, "time", timeStr))));
        place(inventory, 20, createMenuItem(new String[]{"PACKED_ICE", "ICE"}, t(player, "admin.gui.time.freeze.name"),
                tl(player, "admin.gui.time.freeze.lore", map("daylight", onOff(player, daylight)))));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createBackItemToWorldControls(player));
        player.openInventory(inventory);
    }

    public static void openGameruleMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.GAMERULES);
        Inventory inventory = Bukkit.createInventory(holder, 54, title(player, "admin.gui.title.gamerule"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        // Lay out supported rules A–Z first, then the unsupported ones (firework
        // stars) on their own separated row — computed against the running version.
        GameruleEntry.rebuildLayout(world);
        for (GameruleEntry entry : GameruleEntry.values()) {
            int slot = entry.slot();
            if (slot >= 0) {
                place(inventory, slot, createGameruleItem(player, world, entry));
            }
        }
        // Nav lives in the dedicated bottom row, never mixed with the rules.
        place(inventory, GAMERULE_BACK_SLOT, createBackItemToWorldControls(player));
        place(inventory, GAMERULE_CLOSE_SLOT, createCloseItem(player));
        player.openInventory(inventory);
    }

    public static void openPluginSystemsMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.PLUGIN_SYSTEMS);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.plugin-systems"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        place(inventory, 12, createMenuItem(new String[]{"BOOK"}, t(player, "admin.gui.ps.reload.name"),
                tl(player, "admin.gui.ps.reload.lore")));
        place(inventory, 14, createMenuItem(new String[]{"REPEATER", "REDSTONE_REPEATER"}, t(player, "admin.gui.ps.modules.name"),
                tl(player, "admin.gui.ps.modules.lore")));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createBackItemToServerControl(player));
        player.openInventory(inventory);
    }

    /**
     * Hub / Lobby Controls — reached from the AdminMenu Hub slot (43) and
     * from {@code /hub menu}. Renders the current hub-mode state, kit
     * options, per-world list, and an enable toggle.
     *
     * <p>Click actions are wired in
     * {@link dev.zcripted.obx.core.gui.main.MainMenuListener}.
     */
    public static void openHubMenu(ObxPlugin plugin, Player player) {
        Holder holder = new Holder(null, SubMenuType.HUB);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.hub"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);

        dev.zcripted.obx.api.hub.HubApi hub = plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class);
        boolean hubEnabled = hub != null && hub.isEnabled();
        List<String> worlds = hub == null ? java.util.Collections.<String>emptyList() : hub.getHubWorlds();
        int worldCount = worlds.size();
        String worldSummary = worldCount == 0 ? "—" : String.join(", ", worlds);
        int cdSeconds = hub == null ? 3 : hub.launchpadCooldownSeconds();
        boolean jumpRodEnabled = hub != null && hub.isJumpRodEnabled();
        boolean vanishAllEnabled = hub != null && hub.isVanishAllEnabled();

        place(inventory, 10, createMenuItem(
                new String[]{hubEnabled ? "LIME_DYE" : "GRAY_DYE", "INK_SACK", "DYE"},
                t(player, hubEnabled ? "admin.gui.hub.mode.name-enabled" : "admin.gui.hub.mode.name-disabled"),
                tl(player, "admin.gui.hub.mode.lore", map("state", onOff(player, hubEnabled)))));

        place(inventory, 12, createMenuItem(new String[]{"GRASS_BLOCK", "GRASS"}, t(player, "admin.gui.hub.worlds.name"),
                tl(player, "admin.gui.hub.worlds.lore", map("count", String.valueOf(worldCount), "worlds", worldSummary))));

        place(inventory, 14, createMenuItem(new String[]{"COMPASS"}, t(player, "admin.gui.hub.selector.name"),
                tl(player, "admin.gui.hub.selector.lore")));

        place(inventory, 16, createMenuItem(new String[]{"BOOK"}, t(player, "admin.gui.hub.reload.name"),
                tl(player, "admin.gui.hub.reload.lore")));

        place(inventory, 19, createMenuItem(new String[]{"CHEST"}, t(player, "admin.gui.hub.kit.name"),
                tl(player, "admin.gui.hub.kit.lore")));

        place(inventory, 21, createMenuItem(new String[]{"FIREWORK_ROCKET", "FIREWORK", "FEATHER"},
                t(player, "admin.gui.hub.launchpad.name"),
                tl(player, "admin.gui.hub.launchpad.lore", map("cooldown", cdSeconds + "s"))));

        place(inventory, 23, createMenuItem(new String[]{"FISHING_ROD"}, t(player, "admin.gui.hub.jumprod.name"),
                tl(player, "admin.gui.hub.jumprod.lore", map("state", onOff(player, jumpRodEnabled)))));

        place(inventory, 25, createMenuItem(new String[]{"LIME_DYE", "INK_SACK", "DYE"},
                t(player, "admin.gui.hub.visibility.name"),
                tl(player, "admin.gui.hub.visibility.lore", map("state", onOff(player, vanishAllEnabled)))));

        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createBackItem(player));
        player.openInventory(inventory);
    }

    /**
     * Handles click dispatch for the HUB sub-menu. Called from
     * {@link dev.zcripted.obx.core.gui.main.MainMenuListener}
     * when the holder's type is {@link SubMenuType#HUB}.
     */
    public static void handleHubMenuClick(ObxPlugin plugin, Player player, int slot,
                                          org.bukkit.event.inventory.ClickType click) {
        if (plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class) == null) {
            return;
        }
        switch (slot) {
            case 10: {
                boolean next = plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class).toggleEnabled();
                if (next) {
                    if (plugin.getHubKitApplier() != null) {
                        plugin.getHubKitApplier().applyToAllInHubWorlds();
                    }
                } else {
                    // Clean up flight + vanish state so disabling can't leave
                    // players free-flying or others permanently hidden.
                    if (plugin.getHubKitApplier() != null) {
                        plugin.getHubKitApplier().revokeFlightInHubWorlds();
                    }
                    if (plugin.getHubItemUseListener() != null) {
                        plugin.getHubItemUseListener().resetVisibilityForAll();
                    }
                }
                plugin.getLanguageManager().send(player,
                        next ? "hub.admin.enabled" : "hub.admin.disabled");
                openHubMenu(plugin, player);
                return;
            }
            case 12: {
                String worldName = player.getWorld().getName();
                java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                placeholders.put("world", worldName);
                if (click == org.bukkit.event.inventory.ClickType.RIGHT
                        || click == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
                    boolean removed = plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class).removeHubWorld(worldName);
                    plugin.getLanguageManager().send(player,
                            removed ? "hub.admin.world.removed" : "hub.admin.world.not-listed",
                            placeholders);
                } else {
                    boolean added = plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class).addHubWorld(worldName);
                    plugin.getLanguageManager().send(player,
                            added ? "hub.admin.world.added" : "hub.admin.world.already-listed",
                            placeholders);
                }
                openHubMenu(plugin, player);
                return;
            }
            case 14:
                player.closeInventory();
                plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class).openServerSelector(player);
                return;
            case 16:
                plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class).reload();
                plugin.getLanguageManager().send(player, "hub.admin.reloaded");
                openHubMenu(plugin, player);
                return;
            case 19:
                if (plugin.getHubKitApplier() != null) {
                    plugin.getHubKitApplier().applyToAllInHubWorlds();
                }
                openHubMenu(plugin, player);
                return;
            default:
                // Other slots are informational placeholders — ignore.
        }
    }

    private static ItemStack createGameruleItem(Player player, World world, GameruleEntry entry) {
        Boolean enabled = getBooleanGameRule(world, entry.rule());
        String state;
        List<String> lore;
        String[] materials;
        if (enabled == null) {
            // Unsupported on this server/version — shown as a firework star, separated.
            state = t(player, "admin.gui.gamerule.item.na");
            lore = tl(player, "admin.gui.gamerule.item.lore-na", map("rule", entry.rule()));
            materials = new String[]{"FIREWORK_STAR", "FIREWORK_CHARGE"};
        } else {
            state = t(player, enabled ? "admin.gui.gamerule.on" : "admin.gui.gamerule.off");
            lore = tl(player, "admin.gui.gamerule.item.lore", map("state", state, "rule", entry.rule()));
            materials = entry.materials();
        }
        ItemStack item = new ItemStack(resolveMaterial(materials));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(t(player, "admin.gui.gamerule.item.name", map("name", t(player, entry.nameKey()), "state", state)));
            meta.setLore(lore);
            hideAttributes(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Live status-item builders ────────────────────────────────────────────
    // Rebuilt on every menu open and by AdminMenuRefreshTask so the displayed
    // whitelist / join-lock / redstone / max-player state always matches the
    // server's actual state — even when another admin changes it (or a reload
    // happens) while the menu is open.

    private static ItemStack serverStateOverviewItem(Player player) {
        return createMenuItem(new String[]{"LEVER"}, t(player, "admin.gui.sc.server-state.name"),
                tl(player, "admin.gui.sc.server-state.lore", map(
                        "whitelist", onOff(player, Bukkit.hasWhitelist()),
                        "joinlock", onOff(player, ServerControlState.isJoinLocked()),
                        "redstone", onOff(player, !ServerControlState.isRedstoneFrozen()),
                        "max", String.valueOf(Bukkit.getMaxPlayers()))));
    }

    private static ItemStack playerAccessOverviewItem(Player player) {
        return createMenuItem(new String[]{"PLAYER_HEAD", "SKULL_ITEM"}, t(player, "admin.gui.sc.player-access.name"),
                tl(player, "admin.gui.sc.player-access.lore", map(
                        "whitelist", onOff(player, Bukkit.hasWhitelist()),
                        "joinlock", onOff(player, ServerControlState.isJoinLocked()),
                        "max", String.valueOf(Bukkit.getMaxPlayers()))));
    }

    private static ItemStack performanceOverviewItem(Player player) {
        return createMenuItem(new String[]{"CLOCK", "WATCH"}, t(player, "admin.gui.sc.performance.name"),
                tl(player, "admin.gui.sc.performance.lore", map(
                        "redstone", onOff(player, !ServerControlState.isRedstoneFrozen()))));
    }

    private static ItemStack worldControlsOverviewItem(Player player) {
        return createMenuItem(new String[]{"GRASS_BLOCK", "GRASS"}, t(player, "admin.gui.sc.world-controls.name"),
                tl(player, "admin.gui.sc.world-controls.lore", map(
                        "autosave", onOff(player, detectAutoSave()))));
    }

    private static ItemStack lockServerItem(Player player) {
        return createMenuItem(new String[]{"IRON_DOOR"}, t(player, "admin.gui.ss.lock.name", map("icon", LOCK_ICON)),
                tl(player, "admin.gui.ss.lock.lore", map(
                        "whitelist", onOff(player, Bukkit.hasWhitelist()),
                        "joinlock", onOff(player, ServerControlState.isJoinLocked()))));
    }

    private static ItemStack unlockServerItem(Player player) {
        return createMenuItem(new String[]{"OAK_DOOR"}, t(player, "admin.gui.ss.unlock.name", map("icon", UNLOCK_ICON)),
                tl(player, "admin.gui.ss.unlock.lore", map(
                        "whitelist", onOff(player, Bukkit.hasWhitelist()),
                        "joinlock", onOff(player, ServerControlState.isJoinLocked()))));
    }

    private static ItemStack whitelistToggleItem(Player player) {
        return createMenuItem(new String[]{"PAPER"}, t(player, "admin.gui.pa.whitelist.name"),
                tl(player, "admin.gui.pa.whitelist.lore", map("current", onOff(player, Bukkit.hasWhitelist()))));
    }

    private static ItemStack joinLockToggleItem(Player player) {
        return createMenuItem(new String[]{"IRON_BARS"}, t(player, "admin.gui.pa.joinlock.name"),
                tl(player, "admin.gui.pa.joinlock.lore", map("current", onOff(player, ServerControlState.isJoinLocked()))));
    }

    private static ItemStack maxPlayersItem(Player player) {
        return createMenuItem(new String[]{"PLAYER_HEAD", "SKULL_ITEM"}, t(player, "admin.gui.pa.maxplayers.name"),
                tl(player, "admin.gui.pa.maxplayers.lore", map("max", String.valueOf(Bukkit.getMaxPlayers()))));
    }

    private static ItemStack redstoneToggleItem(Player player) {
        return createMenuItem(new String[]{"REDSTONE_TORCH"}, t(player, "admin.gui.perf.redstone.name"),
                tl(player, "admin.gui.perf.redstone.lore", map("redstone", onOff(player, !ServerControlState.isRedstoneFrozen()))));
    }

    /**
     * Re-renders the live status items of an open admin submenu so the displayed
     * whitelist / join-lock / redstone / max-player state matches the server.
     * Driven by {@link dev.zcripted.obx.feature.staff.gui.AdminMenuRefreshTask}
     * and called immediately after a click that changes server state.
     */
    public static void refresh(Holder holder, Player player) {
        if (holder == null || holder.getType() == null || player == null) {
            return;
        }
        Inventory inventory = holder.getInventory();
        if (inventory == null) {
            return;
        }
        switch (holder.getType()) {
            case SERVER_CONTROL:
                place(inventory, 10, serverStateOverviewItem(player));
                place(inventory, 12, playerAccessOverviewItem(player));
                place(inventory, 14, performanceOverviewItem(player));
                place(inventory, 16, worldControlsOverviewItem(player));
                break;
            case SERVER_STATE:
                place(inventory, 16, lockServerItem(player));
                place(inventory, 19, unlockServerItem(player));
                break;
            case PLAYER_ACCESS:
                place(inventory, 10, whitelistToggleItem(player));
                place(inventory, 12, joinLockToggleItem(player));
                place(inventory, 14, maxPlayersItem(player));
                break;
            case PERFORMANCE_HEALTH:
                place(inventory, 14, redstoneToggleItem(player));
                break;
            case ECONOMY: {
                ObxPlugin plugin = (ObxPlugin) JavaPlugin.getProvidingPlugin(AdminSubMenu.class);
                dev.zcripted.obx.api.economy.EconomyService economy = plugin.getEconomyService();
                place(inventory, 4, economyCurrencyItem(player, economy));
                place(inventory, 11, economyTopItem(player, economy));
                place(inventory, 12, economyOverviewItem(player, economy));
                place(inventory, 14, economyWorthItem(plugin, player));
                place(inventory, 15, economyTransactionsItem(player, economy));
                place(inventory, 22, economyShopItem(plugin, player));
                break;
            }
            default:
                break;
        }
    }

    private static Boolean getBooleanGameRule(World world, String ruleName) {
        if (world == null || ruleName == null) {
            return null;
        }
        boolean daylightRule = "doDaylightCycle".equalsIgnoreCase(ruleName);
        Object gameRule = resolveGameRule(ruleName);
        if (gameRule != null) {
            try {
                java.lang.reflect.Method method = world.getClass().getMethod("getGameRuleValue", gameRule.getClass());
                Object value = method.invoke(world, gameRule);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
            } catch (Throwable ignored) {
                // Fall through to legacy string access.
            }
        }
        try {
            String value = world.getGameRuleValue(ruleName);
            if (value == null) {
                return daylightRule ? !DaylightCycleFallback.isFrozen(world) : null;
            }
            return Boolean.parseBoolean(value);
        } catch (IllegalArgumentException ignored) {
            return daylightRule ? !DaylightCycleFallback.isFrozen(world) : null;
        } catch (Throwable ignored) {
            return daylightRule ? !DaylightCycleFallback.isFrozen(world) : null;
        }
    }

    private static boolean setBooleanGameRule(ObxPlugin plugin, World world, String ruleName, boolean value) {
        if (plugin == null || world == null || ruleName == null) {
            return false;
        }
        boolean daylightRule = "doDaylightCycle".equalsIgnoreCase(ruleName);
        Object gameRule = resolveGameRule(ruleName);
        if (gameRule != null) {
            try {
                java.lang.reflect.Method method = findSetGameRuleMethod(world.getClass(), gameRule.getClass());
                if (method != null) {
                    method.invoke(world, gameRule, value);
                    return true;
                }
            } catch (Throwable ignored) {
                // Fall through to legacy string access.
            }
        }
        try {
            world.setGameRuleValue(ruleName, Boolean.toString(value));
            return true;
        } catch (IllegalArgumentException ignored) {
            if (daylightRule) {
                DaylightCycleFallback.setFrozen(plugin, world, !value);
                return true;
            }
            return false;
        } catch (Throwable ignored) {
            if (daylightRule) {
                DaylightCycleFallback.setFrozen(plugin, world, !value);
                return true;
            }
            return false;
        }
    }

    private static Object resolveGameRule(String ruleName) {
        try {
            Class<?> gameRuleClass = Class.forName("org.bukkit.GameRule");
            java.lang.reflect.Method getByName = gameRuleClass.getMethod("getByName", String.class);
            return getByName.invoke(null, ruleName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static java.lang.reflect.Method findSetGameRuleMethod(Class<?> worldClass, Class<?> gameRuleClass) {
        for (java.lang.reflect.Method method : worldClass.getMethods()) {
            if (!"setGameRule".equals(method.getName())) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 2 && params[0].isAssignableFrom(gameRuleClass)) {
                return method;
            }
        }
        return null;
    }

    private static boolean detectAutoSave() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        return world != null && world.isAutoSave();
    }

    private static boolean detectDaylightCycle() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        Boolean val = getBooleanGameRule(world, "doDaylightCycle");
        return val == null || val;
    }

    private static String weatherStateText(Player player) {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) {
            return t(player, "admin.gui.weather.unknown");
        }
        if (world.isThundering()) {
            return t(player, "admin.gui.weather.thunder");
        }
        if (world.hasStorm()) {
            return t(player, "admin.gui.weather.rain");
        }
        return t(player, "admin.gui.weather.clear");
    }

    private static long detectTime() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        return world == null ? -1L : world.getTime();
    }

    // ── World Controls overview lore (live, clean-styled) ─────────────────────

    /** Live world-border settings for the player's current world. */
    private static List<String> worldBorderLore(Player player) {
        java.util.Locale en = java.util.Locale.ENGLISH;
        try {
            org.bukkit.WorldBorder b = player.getWorld().getWorldBorder();
            org.bukkit.Location c = b.getCenter();
            String blocks = t(player, "admin.gui.unit.blocks");
            String buffer = t(player, "admin.gui.unit.buffer");
            String block = t(player, "admin.gui.unit.block");
            String diameter = String.format(en, "%,.0f", b.getSize()) + " " + blocks;
            String center = String.format(en, "%,.1f, %,.1f", c.getX(), c.getZ());
            String warning = b.getWarningDistance() + " " + blocks + " · " + b.getWarningTime() + "s";
            String damage = String.format(en, "%.1f", b.getDamageBuffer()) + " " + buffer
                    + " · " + String.format(en, "%.2f", b.getDamageAmount()) + "/" + block;
            return tl(player, "admin.gui.wc.border.lore", map(
                    "world", player.getWorld().getName(),
                    "diameter", diameter,
                    "center", center,
                    "warning", warning,
                    "damage", damage));
        } catch (Throwable ignored) {
            return tl(player, "admin.gui.wc.border.lore-unavailable");
        }
    }

    /** Live weather state + available options for the Weather Control item. */
    private static List<String> weatherControlLore(Player player) {
        return tl(player, "admin.gui.wc.weather.lore", map("current", weatherStateText(player)));
    }

    /** Preview of the first 10 alphabetical enabled (ON) gamerules. */
    private static List<String> gameruleEditorLore(Player player) {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        java.util.List<String> on = new java.util.ArrayList<>();
        for (GameruleEntry entry : GameruleEntry.values()) {
            Boolean value = getBooleanGameRule(world, entry.rule());
            if (value != null && value) {
                on.add(t(player, entry.nameKey()));
            }
        }
        java.util.Collections.sort(on, String.CASE_INSENSITIVE_ORDER);
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(t(player, "admin.gui.wc.gamerule.lore.header"));
        lore.add("");
        lore.add(t(player, "admin.gui.wc.gamerule.lore.count", map("count", String.valueOf(on.size()))));
        if (on.isEmpty()) {
            lore.add(t(player, "admin.gui.wc.gamerule.lore.none"));
        }
        int shown = Math.min(10, on.size());
        for (int i = 0; i < shown; i++) {
            lore.add(t(player, "admin.gui.wc.gamerule.lore.entry", map("name", on.get(i))));
        }
        if (on.size() > shown) {
            lore.add(t(player, "admin.gui.wc.gamerule.lore.more", map("count", String.valueOf(on.size() - shown))));
        }
        lore.add("");
        lore.add(t(player, "admin.gui.wc.gamerule.lore.footer"));
        return lore;
    }

    // Action handlers
    public static void handleAction(ObxPlugin plugin, Player player, Holder holder, int slot, ClickType click) {
        if (holder == null || holder.getType() == null) {
            return;
        }
        switch (holder.getType()) {
            case SERVER_CONTROL:
                handleServerControlClick(plugin, player, slot);
                break;
            case SERVER_STATE:
                handleServerStateClick(plugin, player, slot);
                break;
            case PLAYER_ACCESS:
                handlePlayerAccessClick(plugin, player, slot, click);
                break;
            case PERFORMANCE_HEALTH:
                handlePerformanceClick(plugin, player, slot, click);
                break;
            case WORLD_CONTROLS:
                handleWorldControlsClick(plugin, player, slot, click);
                break;
            case WEATHER:
                handleWeatherClick(plugin, player, slot);
                break;
            case TIME:
                handleTimeClick(plugin, player, slot);
                break;
            case GAMERULES:
                handleGameruleClick(plugin, player, slot);
                break;
            case WORLD_BORDER:
                handleWorldBorderClick(plugin, player, slot);
                break;
            case MODULES:
                handleModulesClick(plugin, player, slot);
                break;
            case PLUGIN_SYSTEMS:
                handlePluginSystemsClick(plugin, player, slot);
                break;
            case ECONOMY:
                handleEconomyMenuClick(plugin, player, slot, click, holder.getPlaceholder());
                break;
            case CONFIRM:
                handleConfirmMenuClick(player, holder, slot);
                break;
            default:
                break;
        }
        // Reflect any state change immediately for the clicking admin (the global
        // AdminMenuRefreshTask covers other admins viewing the same menu).
        refresh(holder, player);
        try {
            player.updateInventory();
        } catch (Throwable ignored) {
            // updateInventory is deprecated/absent on some forks — non-fatal.
        }
    }

    private static void handleServerControlClick(ObxPlugin plugin, Player player, int slot) {
        if (slot == 10) {
            openServerStateMenu(player);
        } else if (slot == 12) {
            openPlayerAccessMenu(player);
        } else if (slot == 14) {
            openPerformanceMenu(player);
        } else if (slot == 16) {
            openWorldControlsMenu(player);
        } else if (slot == 19) {
            openPluginSystemsMenu(player);
        }
    }

    private static void handleServerStateClick(ObxPlugin plugin, Player player, int slot) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 10) {
            if (stopConfirmations.remove(player.getUniqueId())) {
                languages.send(player, "admin.confirm.stop");
                Bukkit.shutdown();
            } else {
                stopConfirmations.add(player.getUniqueId());
                languages.send(player, "admin.confirm.stop-prompt");
            }
        } else if (slot == 12) {
            startRestartCountdown(plugin, player, 5, "admin.restart.label.quick");
        } else if (slot == 14) {
            startRestartCountdown(plugin, player, 5, "admin.restart.label.safe");
        } else if (slot == 16) {
            // Lock Server: whitelist on + join lock on (join lock kicks non-ops).
            ServerControlActions.setWhitelist(plugin, player, true);
            ServerControlActions.setJoinLock(plugin, player, true);
        } else if (slot == 19) {
            // Unlock Server: whitelist off + join lock off.
            ServerControlActions.setWhitelist(plugin, player, false);
            ServerControlActions.setJoinLock(plugin, player, false);
        }
    }

    private static void handlePlayerAccessClick(ObxPlugin plugin, Player player, int slot, ClickType click) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 10) {
            ServerControlActions.toggleWhitelist(plugin, player);
        } else if (slot == 12) {
            ServerControlActions.toggleJoinLock(plugin, player);
        } else if (slot == 14) {
            int delta = 0;
            if (click.isLeftClick() && !click.isRightClick()) {
                delta = -1;
            } else if (click.isRightClick() && !click.isLeftClick()) {
                delta = 1;
            }
            if (delta != 0) {
                adjustMaxPlayers(delta);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("delta", String.valueOf(delta));
                placeholders.put("max", String.valueOf(Bukkit.getMaxPlayers()));
                languages.send(player, "admin.max-players.updated", placeholders);
            }
        } else if (slot == 16) {
            ServerControlActions.kickNonOps(plugin, player);
        } else if (slot == 19) {
            ServerControlActions.forceSpectator(plugin, player);
        }
    }

    private static void handlePerformanceClick(ObxPlugin plugin, Player player, int slot, ClickType click) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 10) {
            // Identical styled report to the /tps command.
            dev.zcripted.obx.core.diagnostics.TpsCommand.sendReport(plugin, player);
            player.closeInventory();
        } else if (slot == 12) {
            ServerControlActions.ClearMode mode = ServerControlActions.ClearMode.ALL;
            if (click.isRightClick() && !click.isLeftClick()) {
                mode = ServerControlActions.ClearMode.MOBS_ONLY;
            }
            if (click.isShiftClick() && click.isLeftClick()) {
                mode = ServerControlActions.ClearMode.ITEMS_ONLY;
            }
            ServerControlActions.clearEntities(plugin, player, mode);
        } else if (slot == 14) {
            toggleRedstone(plugin, player);
        }
    }

    private static void handleWorldControlsClick(ObxPlugin plugin, Player player, int slot, ClickType click) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 10) {
            for (World world : Bukkit.getWorlds()) {
                world.save();
            }
            ServerControlActions.saveWorldsMessage(plugin, player, Bukkit.getWorlds());
        } else if (slot == 12) {
            ServerControlActions.toggleAutoSave(plugin, player);
            openWorldControlsMenu(player); // re-render so the Autosave item lore reflects the new state
        } else if (slot == 14) {
            openWorldBorderMenu(player);
        } else if (slot == 16) {
            openWeatherMenu(player);
        } else if (slot == 19) {
            openTimeMenu(player);
        } else if (slot == 21) {
            openGameruleMenu(player);
        }
    }

    private static void handlePluginSystemsClick(ObxPlugin plugin, Player player, int slot) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 12) {
            plugin.reloadPlugin();
            languages.send(player, "admin.plugin.reloaded");
            String message = languages.formatConsole("admin.plugin.reloaded-console", Placeholders.with("player", player.getName()));
            Bukkit.getServer().getConsoleSender().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        } else if (slot == 14) {
            openModulesMenu(plugin, player);
        }
    }

    private static void handleWeatherClick(ObxPlugin plugin, Player player, int slot) {
        String changed = null;
        if (slot == 11) {
            for (World world : Bukkit.getWorlds()) {
                world.setStorm(false);
                world.setThundering(false);
            }
            changed = "clear";
        } else if (slot == 13) {
            for (World world : Bukkit.getWorlds()) {
                world.setStorm(true);
                world.setThundering(false);
            }
            changed = "rain";
        } else if (slot == 15) {
            for (World world : Bukkit.getWorlds()) {
                world.setStorm(true);
                world.setThundering(true);
            }
            changed = "thunder";
        }
        if (changed != null) {
            // Boxed message + per-event button row + console mirror (shared with /weather).
            ServerControlActions.weatherMessage(plugin, player, changed);
        }
    }

    private static void handleTimeClick(ObxPlugin plugin, Player player, int slot) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 11) {
            for (World world : Bukkit.getWorlds()) {
                world.setTime(0);
                setBooleanGameRule(plugin, world, "doDaylightCycle", true);
            }
            languages.send(player, "admin.time.morning");
        } else if (slot == 13) {
            for (World world : Bukkit.getWorlds()) {
                world.setTime(6000);
                setBooleanGameRule(plugin, world, "doDaylightCycle", true);
            }
            languages.send(player, "admin.time.noon");
        } else if (slot == 15) {
            for (World world : Bukkit.getWorlds()) {
                world.setTime(13000);
                setBooleanGameRule(plugin, world, "doDaylightCycle", true);
            }
            languages.send(player, "admin.time.night");
        } else if (slot == 20) {
            World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (world != null) {
                Boolean current = getBooleanGameRule(world, "doDaylightCycle");
                if (current == null) {
                    return;
                }
                boolean newValue = !current;
                boolean updated = false;
                for (World w : Bukkit.getWorlds()) {
                    if (setBooleanGameRule(plugin, w, "doDaylightCycle", newValue)) {
                        updated = true;
                    }
                }
                if (!updated) {
                    return;
                }
                String state = languages.get(player, newValue ? "admin.time.daylight.state.enabled" : "admin.time.daylight.state.frozen");
                languages.send(player, "admin.time.daylight.status", Placeholders.with("state", state));
            }
        } else {
            return;
        }
        // Re-render the Time menu so the freeze toggle + "Current Time" lore reflect the new state
        // immediately (the menu isn't otherwise on the live-refresh task).
        openTimeMenu(player);
    }

    private static void handleGameruleClick(ObxPlugin plugin, Player player, int slot) {
        GameruleEntry entry = GameruleEntry.forSlot(slot);
        if (entry == null) {
            return;
        }
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) {
            return;
        }
        Boolean current = getBooleanGameRule(world, entry.rule());
        if (current == null) {
            return;
        }
        boolean now = !current;
        boolean updated = false;
        for (World w : Bukkit.getWorlds()) {
            if (setBooleanGameRule(plugin, w, entry.rule(), now)) {
                updated = true;
            }
        }
        if (!updated) {
            return;
        }
        LanguageManager languages = plugin.getLanguageManager();
        String state = languages.get(player, now ? "admin.gamerule.state.on" : "admin.gamerule.state.off");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("rule", languages.get(player, entry.nameKey()));
        placeholders.put("state", state);
        languages.send(player, "admin.gamerule.toggled", placeholders);
        openGameruleMenu(player);
    }

    private static void startRestartCountdown(ObxPlugin plugin, Player initiator, int seconds, String labelKey) {
        final BossBar bar = Bukkit.createBossBar(plugin.getLanguageManager().get(initiator, "admin.restart.title", Collections.singletonMap("label", plugin.getLanguageManager().get(initiator, labelKey))), BarColor.RED, BarStyle.SOLID);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("label", plugin.getLanguageManager().get(initiator, labelKey));
        placeholders.put("seconds", String.valueOf(seconds));
        plugin.getLanguageManager().broadcast("admin.restart.countdown-start", placeholders);

        final int[] remaining = {seconds};
        final dev.zcripted.obx.core.platform.scheduler.CancellableTask[] taskRef = new dev.zcripted.obx.core.platform.scheduler.CancellableTask[1];
        Runnable tick = () -> {
            if (remaining[0] <= 0) {
                placeholders.put("seconds", "0");
                bar.removeAll();
                if (taskRef[0] != null) {
                    taskRef[0].cancel();
                }
                plugin.getLanguageManager().broadcast("admin.restart.countdown-now", placeholders);
                Bukkit.shutdown();
                return;
            }
            double progress = Math.max(0.0, Math.min(1.0, remaining[0] / (double) seconds));
            bar.setTitle(plugin.getLanguageManager().get(initiator, "admin.restart.title", Collections.singletonMap("label", plugin.getLanguageManager().get(initiator, labelKey))));
            bar.setProgress(progress);
            placeholders.put("seconds", String.valueOf(remaining[0]));
            for (Player player : Bukkit.getOnlinePlayers()) {
                String labelText = plugin.getLanguageManager().get(player, labelKey);
                Map<String, String> playerPlaceholders = new HashMap<>(placeholders);
                playerPlaceholders.put("label", labelText);
                ComponentMessenger.sendActionBar(player, plugin.getLanguageManager().get(player, "admin.restart.actionbar", playerPlaceholders));
                player.sendTitle(plugin.getLanguageManager().get(player, "admin.restart.title", playerPlaceholders),
                        plugin.getLanguageManager().get(player, "admin.restart.subtitle", playerPlaceholders), 0, 20, 10);
            }
            remaining[0]--;
        };
        taskRef[0] = plugin.getSchedulerAdapter().runRepeating(tick, 1L, 20L);
    }

    private static void toggleRedstone(ObxPlugin plugin, Player player) {
        boolean frozen = ServerControlState.toggleRedstoneFrozen();
        ServerControlActions.redstoneMessage(plugin, player, frozen);
    }

    private static void adjustMaxPlayers(int delta) {
        try {
            Object craftServer = Bukkit.getServer();
            Field maxPlayersField = craftServer.getClass().getDeclaredField("maxPlayers");
            maxPlayersField.setAccessible(true);
            int current = (int) maxPlayersField.get(craftServer);
            int updated = Math.max(1, current + delta);
            maxPlayersField.set(craftServer, updated);
        } catch (Exception ignored) {
            // ignore failures on unsupported platforms
        }
    }


    public static final class Holder extends MenuHolder {
        private final AdminMenu.PlaceholderView placeholder;
        private final SubMenuType type;
        // CONFIRM menus carry their pending action + the "reopen previous menu" cancel.
        // Consumers receive the CLICKING player so no stale Player reference is held.
        private java.util.function.Consumer<Player> confirmAction;
        private java.util.function.Consumer<Player> cancelAction;

        public Holder(AdminMenu.PlaceholderView placeholder, SubMenuType type) {
            this.placeholder = placeholder;
            this.type = type;
        }

        public AdminMenu.PlaceholderView getPlaceholder() {
            return placeholder;
        }

        public SubMenuType getType() {
            return type;
        }

        void setConfirmActions(java.util.function.Consumer<Player> confirm,
                               java.util.function.Consumer<Player> cancel) {
            this.confirmAction = confirm;
            this.cancelAction = cancel;
        }

        java.util.function.Consumer<Player> confirmAction() {
            return confirmAction;
        }

        java.util.function.Consumer<Player> cancelAction() {
            return cancelAction;
        }
    }

    /**
     * Every boolean gamerule across the supported MC range (1.8 → latest). Slots
     * are auto-assigned across the 45-slot editor, skipping the reserved nav
     * slots (close = {@link AdminMenu#CLOSE_SLOT}, back = {@link #SERVER_BACK_SLOT}).
     * Rules the running server doesn't recognise render as "N/A" (handled in
     * {@code createGameruleItem}/{@code handleGameruleClick}), so a single list
     * works on every version. Each material array ends in a universally-available
     * fallback so nothing renders as AIR.
     */
    // ── World Border (reached from World Controls slot 14) ────────────────────

    public static void openWorldBorderMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.WORLD_BORDER);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.world-border"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        double size = borderSize(player);
        String world = player.getWorld() == null ? "?" : player.getWorld().getName();
        String diameter = String.format(java.util.Locale.ENGLISH, "%.0f", size) + " " + t(player, "admin.gui.unit.blocks");
        place(inventory, 10, createMenuItem(new String[]{"RED_CONCRETE", "REDSTONE_BLOCK"}, t(player, "admin.gui.wb.minus-1000.name"),
                tl(player, "admin.gui.wb.minus-1000.lore")));
        place(inventory, 11, createMenuItem(new String[]{"PINK_CONCRETE", "REDSTONE"}, t(player, "admin.gui.wb.minus-100.name"),
                tl(player, "admin.gui.wb.minus-100.lore")));
        place(inventory, 13, createMenuItem(new String[]{"MAP"}, t(player, "admin.gui.wb.info.name"),
                tl(player, "admin.gui.wb.info.lore", map("world", world, "diameter", diameter))));
        place(inventory, 15, createMenuItem(new String[]{"LIME_CONCRETE", "EMERALD_BLOCK"}, t(player, "admin.gui.wb.plus-100.name"),
                tl(player, "admin.gui.wb.plus-100.lore")));
        place(inventory, 16, createMenuItem(new String[]{"GREEN_CONCRETE", "EMERALD"}, t(player, "admin.gui.wb.plus-1000.name"),
                tl(player, "admin.gui.wb.plus-1000.lore")));
        place(inventory, 20, createMenuItem(new String[]{"COMPASS"}, t(player, "admin.gui.wb.center.name"),
                tl(player, "admin.gui.wb.center.lore")));
        place(inventory, 24, createMenuItem(new String[]{"BARRIER"}, t(player, "admin.gui.wb.reset.name"),
                tl(player, "admin.gui.wb.reset.lore")));
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createBackItemToWorldControls(player));
        player.openInventory(inventory);
    }

    private static double borderSize(Player player) {
        try {
            return player.getWorld().getWorldBorder().getSize();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static void handleWorldBorderClick(ObxPlugin plugin, Player player, int slot) {
        org.bukkit.WorldBorder border;
        try {
            border = player.getWorld().getWorldBorder();
        } catch (Throwable ignored) {
            return;
        }
        if (border == null) {
            return;
        }
        LanguageManager languages = plugin.getLanguageManager();
        String world = player.getWorld().getName();
        double size = border.getSize();
        Double newSize = null;
        if (slot == 10) {
            newSize = Math.max(1.0, size - 1000.0);
        } else if (slot == 11) {
            newSize = Math.max(1.0, size - 100.0);
        } else if (slot == 15) {
            newSize = size + 100.0;
        } else if (slot == 16) {
            newSize = size + 1000.0;
        } else if (slot == 20) {
            border.setCenter(player.getLocation().getX(), player.getLocation().getZ());
            languages.send(player, "admin.world.border.centered", Placeholders.with("world", world));
            openWorldBorderMenu(player);
            return;
        } else if (slot == 24) {
            border.reset();
            languages.send(player, "admin.world.border.reset", Placeholders.with("world", world));
            openWorldBorderMenu(player);
            return;
        }
        if (newSize != null) {
            border.setSize(newSize);
            languages.send(player, "admin.world.border.size", Placeholders.with(
                    "world", world, "size", String.format(java.util.Locale.ENGLISH, "%.0f", newSize)));
            openWorldBorderMenu(player);
        }
    }

    // ── Module toggles (reached from Plugin + Systems slot 14) ─────────────────

    public static void openModulesMenu(ObxPlugin plugin, Player player) {
        Holder holder = new Holder(null, SubMenuType.MODULES);
        Inventory inventory = Bukkit.createInventory(holder, 36, title(player, "admin.gui.title.modules"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        for (ModuleEntry module : ModuleEntry.values()) {
            boolean on = module.isEnabled(plugin);
            String name = (on ? ChatColor.GREEN : ChatColor.RED) + t(player, module.nameKey());
            String action = t(player, on ? "admin.gui.modules.action.disable" : "admin.gui.modules.action.enable");
            place(inventory, module.slot(), createMenuItem(module.materials(), name,
                    tl(player, "admin.gui.modules.item.lore", map("state", onOff(player, on), "action", action))));
        }
        place(inventory, CLOSE_SLOT, createCloseItem(player));
        place(inventory, SERVER_BACK_SLOT, createMenuItem(new String[]{"ARROW", "SPECTRAL_ARROW"},
                t(player, "admin.gui.back.plugin-systems.name"), tl(player, "admin.gui.back.plugin-systems.lore")));
        player.openInventory(inventory);
    }

    private static void handleModulesClick(ObxPlugin plugin, Player player, int slot) {
        ModuleEntry module = ModuleEntry.forSlot(slot);
        if (module == null) {
            return;
        }
        boolean now = !module.isEnabled(plugin);
        module.setEnabled(plugin, now);
        LanguageManager languages = plugin.getLanguageManager();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("module", languages.get(player, module.nameKey()));
        placeholders.put("state", languages.get(player, now ? "admin.module.state.enabled" : "admin.module.state.disabled"));
        languages.send(player, "admin.module.toggled", placeholders);
        dev.zcripted.obx.util.message.ConsoleLog.info(plugin,
                "Module '" + module.display() + "' " + (now ? "enabled" : "disabled") + " by " + player.getName());
        openModulesMenu(plugin, player);
    }

    /**
     * The plugin systems exposed in the Module Toggles menu. Each persists its
     * {@code enabled} flag (via the service's own setter or a direct config
     * write) and re-applies live. Always-on services (economy, moderation) and
     * the entity-lifecycle hologram module are intentionally excluded — those
     * are managed elsewhere.
     */
    private enum ModuleEntry {
        CHAT("Chat Formatting", "admin.gui.module.chat", 10, new String[]{"WRITABLE_BOOK", "BOOK_AND_QUILL", "BOOK"}),
        SCOREBOARD("Scoreboard", "admin.gui.module.scoreboard", 11, new String[]{"OAK_SIGN", "SIGN"}),
        TABLIST("Tablist", "admin.gui.module.tablist", 12, new String[]{"PLAYER_HEAD", "SKULL_ITEM", "PAPER"}),
        JOIN_LEAVE("Join / Leave Broadcasts", "admin.gui.module.join-leave", 13, new String[]{"OAK_DOOR", "WOODEN_DOOR", "WOOD_DOOR"}),
        JOIN_MOTD("Welcome MOTD", "admin.gui.module.join-motd", 14, new String[]{"PAINTING"}),
        HUB("Hub / Lobby", "admin.gui.module.hub", 15, new String[]{"COMPASS"}),
        AFK("AFK System", "admin.gui.module.afk", 16, new String[]{"FEATHER"}),
        DEATHDROP("Death Grouping", "admin.gui.module.deathdrop", 17, new String[]{"CHEST"}),
        WEBHOOK_WARNINGS("Webhook Warnings", "admin.gui.module.webhook-warn", 19, new String[]{"BELL", "NOTE_BLOCK", "NOTEBLOCK"});

        private final String display;
        private final String nameKey;
        private final int slot;
        private final String[] materials;

        ModuleEntry(String display, String nameKey, int slot, String[] materials) {
            this.display = display;
            this.nameKey = nameKey;
            this.slot = slot;
            this.materials = materials;
        }

        String display() {
            return display;
        }

        String nameKey() {
            return nameKey;
        }

        int slot() {
            return slot;
        }

        String[] materials() {
            return materials;
        }

        static ModuleEntry forSlot(int slot) {
            for (ModuleEntry module : values()) {
                if (module.slot == slot) {
                    return module;
                }
            }
            return null;
        }

        boolean isEnabled(ObxPlugin plugin) {
            switch (this) {
                case CHAT:
                    return plugin.getChatService() != null && plugin.getChatService().isEnabled();
                case SCOREBOARD:
                    return plugin.getScoreboardService() != null && plugin.getScoreboardService().isEnabled();
                case TABLIST:
                    return plugin.getTablistService() != null && plugin.getTablistService().isEnabled();
                case JOIN_LEAVE:
                    return plugin.getJoinLeaveService() != null && plugin.getJoinLeaveService().isJoinLeaveEnabled();
                case JOIN_MOTD:
                    return plugin.getJoinLeaveService() != null && plugin.getJoinLeaveService().isJoinMotdEnabled();
                case HUB:
                    return plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class) != null && plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class).isEnabled();
                case AFK:
                    return plugin.getAfkService() != null && plugin.getAfkService().isEnabled();
                case DEATHDROP:
                    return plugin.getModuleManager() != null && plugin.getModuleManager().isEnabled("deathdrop");
                case WEBHOOK_WARNINGS: {
                    dev.zcripted.obx.core.diagnostics.WebhookWarningService warnings = plugin.getServiceRegistry()
                            .get(dev.zcripted.obx.core.diagnostics.WebhookWarningService.class);
                    return warnings != null && warnings.isEnabled();
                }
                default:
                    return false;
            }
        }

        void setEnabled(ObxPlugin plugin, boolean value) {
            switch (this) {
                case CHAT:
                    writeModuleFlag(plugin, "systems/chat_management.yml", "enabled", value);
                    if (plugin.getChatService() != null) plugin.getChatService().reload();
                    break;
                case SCOREBOARD:
                    writeModuleFlag(plugin, "systems/scoreboard.yml", "enabled", value);
                    if (plugin.getScoreboardService() != null) plugin.getScoreboardService().reload();
                    break;
                case TABLIST:
                    writeModuleFlag(plugin, "systems/tablist.yml", "enabled", value);
                    if (plugin.getTablistService() != null) plugin.getTablistService().reload();
                    break;
                case JOIN_LEAVE:
                    if (plugin.getJoinLeaveService() != null) plugin.getJoinLeaveService().setJoinLeaveEnabled(value);
                    break;
                case JOIN_MOTD:
                    if (plugin.getJoinLeaveService() != null) plugin.getJoinLeaveService().setJoinMotdEnabled(value);
                    break;
                case HUB:
                    if (plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class) != null) plugin.getServiceRegistry().get(dev.zcripted.obx.api.hub.HubApi.class).setEnabled(value);
                    break;
                case AFK:
                    if (plugin.getAfkService() != null) plugin.getAfkService().setEnabled(value);
                    break;
                case DEATHDROP:
                    if (plugin.getModuleManager() != null) plugin.getModuleManager().setEnabled("deathdrop", value);
                    break;
                case WEBHOOK_WARNINGS: {
                    dev.zcripted.obx.core.diagnostics.WebhookWarningService warnings = plugin.getServiceRegistry()
                            .get(dev.zcripted.obx.core.diagnostics.WebhookWarningService.class);
                    if (warnings != null) warnings.setEnabled(value); // immediate + persisted to config.yml
                    break;
                }
                default:
                    break;
            }
        }
    }

    private static void writeModuleFlag(ObxPlugin plugin, String relFile, String key, boolean value) {
        java.io.File file = new java.io.File(plugin.getDataFolder(), relFile);
        org.bukkit.configuration.file.YamlConfiguration yaml =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        yaml.set(key, value);
        try {
            yaml.save(file);
        } catch (java.io.IOException ex) {
            plugin.getLogger().warning("Failed to write module flag to " + relFile + ": " + ex.getMessage());
        }
    }

    private enum GameruleEntry {
        ANNOUNCE_ADVANCEMENTS("announceAdvancements", "Announce Advancements", new String[]{"PAPER"}),
        BLOCK_EXPLOSION_DROP_DECAY("blockExplosionDropDecay", "Block Explosion Drop Decay", new String[]{"TNT", "PAPER"}),
        COMMAND_BLOCK_OUTPUT("commandBlockOutput", "Command Block Output", new String[]{"COMMAND_BLOCK", "COMMAND", "PAPER"}),
        DISABLE_ELYTRA_CHECK("disableElytraMovementCheck", "Disable Elytra Check", new String[]{"ELYTRA", "PAPER"}),
        DISABLE_RAIDS("disableRaids", "Disable Raids", new String[]{"CROSSBOW", "PAPER"}),
        DO_DAYLIGHT("doDaylightCycle", "Daylight Cycle", new String[]{"CLOCK", "WATCH"}),
        DO_ENTITY_DROPS("doEntityDrops", "Entity Drops", new String[]{"HOPPER"}),
        DO_FIRE_TICK("doFireTick", "Fire Spread", new String[]{"FLINT_AND_STEEL"}),
        DO_IMMEDIATE_RESPAWN("doImmediateRespawn", "Immediate Respawn", new String[]{"TOTEM_OF_UNDYING", "PAPER"}),
        DO_INSOMNIA("doInsomnia", "Phantoms (Insomnia)", new String[]{"PHANTOM_MEMBRANE", "PAPER"}),
        DO_LIMITED_CRAFTING("doLimitedCrafting", "Limited Crafting", new String[]{"CRAFTING_TABLE", "WORKBENCH"}),
        DO_MOB_LOOT("doMobLoot", "Mob Loot", new String[]{"ROTTEN_FLESH"}),
        DO_MOB_SPAWNING("doMobSpawning", "Mob Spawning", new String[]{"SPAWNER", "MOB_SPAWNER", "PAPER"}),
        DO_PATROL_SPAWNING("doPatrolSpawning", "Patrol Spawning", new String[]{"WHITE_BANNER", "BANNER", "PAPER"}),
        DO_TILE_DROPS("doTileDrops", "Block Drops", new String[]{"PISTON"}),
        DO_TRADER_SPAWNING("doTraderSpawning", "Trader Spawning", new String[]{"EMERALD"}),
        DO_VINES_SPREAD("doVinesSpread", "Vines Spread", new String[]{"VINE", "PAPER"}),
        DO_WARDEN_SPAWNING("doWardenSpawning", "Warden Spawning", new String[]{"SCULK_CATALYST", "PAPER"}),
        DO_WEATHER("doWeatherCycle", "Weather Cycle", new String[]{"WATER_BUCKET"}),
        DROWNING_DAMAGE("drowningDamage", "Drowning Damage", new String[]{"PUFFERFISH", "PAPER"}),
        ENDER_PEARLS_VANISH("enderPearlsVanishOnDeath", "Ender Pearls Vanish On Death", new String[]{"ENDER_PEARL"}),
        FALL_DAMAGE("fallDamage", "Fall Damage", new String[]{"FEATHER"}),
        FIRE_DAMAGE("fireDamage", "Fire Damage", new String[]{"BLAZE_POWDER", "PAPER"}),
        FORGIVE_DEAD_PLAYERS("forgiveDeadPlayers", "Forgive Dead Players", new String[]{"GOLDEN_APPLE"}),
        FREEZE_DAMAGE("freezeDamage", "Freeze Damage", new String[]{"ICE", "PAPER"}),
        GLOBAL_SOUND_EVENTS("globalSoundEvents", "Global Sound Events", new String[]{"NOTE_BLOCK", "NOTEBLOCK", "PAPER"}),
        KEEP_INVENTORY("keepInventory", "Keep Inventory", new String[]{"CHEST"}),
        LAVA_SOURCE_CONVERSION("lavaSourceConversion", "Lava Source Conversion", new String[]{"LAVA_BUCKET"}),
        LOG_ADMIN_COMMANDS("logAdminCommands", "Log Admin Commands", new String[]{"WRITABLE_BOOK", "BOOK_AND_QUILL", "BOOK"}),
        MOB_EXPLOSION_DROP_DECAY("mobExplosionDropDecay", "Mob Explosion Drop Decay", new String[]{"CREEPER_HEAD", "PAPER"}),
        MOB_GRIEFING("mobGriefing", "Mob Griefing", new String[]{"CREEPER_SPAWN_EGG", "MONSTER_EGG", "PAPER"}),
        NATURAL_REGEN("naturalRegeneration", "Natural Regeneration", new String[]{"APPLE"}),
        PROJECTILES_BREAK_BLOCKS("projectilesCanBreakBlocks", "Projectiles Break Blocks", new String[]{"ARROW", "PAPER"}),
        REDUCED_DEBUG_INFO("reducedDebugInfo", "Reduced Debug Info", new String[]{"COMPASS"}),
        SEND_COMMAND_FEEDBACK("sendCommandFeedback", "Command Feedback", new String[]{"REPEATER", "REDSTONE_REPEATER", "PAPER"}),
        SHOW_DEATH_MESSAGES("showDeathMessages", "Show Death Messages", new String[]{"SKELETON_SKULL", "SKULL_ITEM", "PAPER"}),
        SPECTATORS_GEN_CHUNKS("spectatorsGenerateChunks", "Spectators Generate Chunks", new String[]{"ENDER_EYE", "PAPER"}),
        TNT_EXPLOSION_DROP_DECAY("tntExplosionDropDecay", "TNT Explosion Drop Decay", new String[]{"TNT", "PAPER"}),
        UNIVERSAL_ANGER("universalAnger", "Universal Anger", new String[]{"IRON_SWORD"}),
        WATER_SOURCE_CONVERSION("waterSourceConversion", "Water Source Conversion", new String[]{"WATER_BUCKET"});

        private static final Map<Integer, GameruleEntry> BY_SLOT = new HashMap<>();
        private static final Map<GameruleEntry, Integer> SLOT_OF = new java.util.EnumMap<>(GameruleEntry.class);
        // Rules occupy slots 0..44 (rows 0-4); the bottom row (45..53) is reserved
        // for navigation, so nav never mixes with the rules.
        private static final int GR_AREA_END = 44;

        /**
         * Recomputes the editor layout against {@code world}: supported rules first,
         * sorted A–Z; then the unsupported ones (also A–Z) starting on the next row
         * for a clean visual break. Called on every menu open since support depends
         * on the running server version.
         */
        static void rebuildLayout(World world) {
            BY_SLOT.clear();
            SLOT_OF.clear();
            java.util.List<GameruleEntry> all = new java.util.ArrayList<>(java.util.Arrays.asList(values()));
            all.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.display, b.display));
            java.util.List<GameruleEntry> supported = new java.util.ArrayList<>();
            java.util.List<GameruleEntry> unsupported = new java.util.ArrayList<>();
            for (GameruleEntry entry : all) {
                if (getBooleanGameRule(world, entry.rule) != null) {
                    supported.add(entry);
                } else {
                    unsupported.add(entry);
                }
            }
            int slot = 0;
            for (GameruleEntry entry : supported) {
                if (slot > GR_AREA_END) {
                    break;
                }
                BY_SLOT.put(slot, entry);
                SLOT_OF.put(entry, slot);
                slot++;
            }
            if (!unsupported.isEmpty() && slot <= GR_AREA_END) {
                int sep = ((slot + 8) / 9) * 9; // round up to the next row for separation
                if (sep > GR_AREA_END) {
                    sep = Math.min(slot + 1, GR_AREA_END); // no room for a full-row gap — small gap
                }
                slot = sep;
                for (GameruleEntry entry : unsupported) {
                    if (slot > GR_AREA_END) {
                        break;
                    }
                    BY_SLOT.put(slot, entry);
                    SLOT_OF.put(entry, slot);
                    slot++;
                }
            }
        }

        private final String rule;
        private final String display;
        private final String[] materials;

        GameruleEntry(String rule, String display, String[] materials) {
            this.rule = rule;
            this.display = display;
            this.materials = materials;
        }

        public String rule() {
            return rule;
        }

        public String display() {
            return display;
        }

        /** Localization key for the per-player display name (DE/ES translated). */
        public String nameKey() {
            return "admin.gui.gamerule.name." + rule;
        }

        public int slot() {
            Integer slot = SLOT_OF.get(this);
            return slot == null ? -1 : slot;
        }

        public String[] materials() {
            return materials;
        }

        public static GameruleEntry forSlot(int slot) {
            return BY_SLOT.get(slot);
        }
    }
}