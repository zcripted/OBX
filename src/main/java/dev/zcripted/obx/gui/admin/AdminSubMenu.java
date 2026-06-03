package dev.zcripted.obx.gui.admin;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.gui.MenuHolder;
import dev.zcripted.obx.language.LanguageManager;

import static dev.zcripted.obx.gui.admin.AdminMenuRender.createBackItem;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.createBackItemToServerControl;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.createBackItemToWorldControls;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.createCloseItem;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.createMenuItem;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.createPane;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.fillWithFiller;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.loreLines;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.place;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.resolveMaterial;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.statusLine;
import static dev.zcripted.obx.gui.admin.AdminMenuRender.valueLine;
import dev.zcripted.obx.util.control.DaylightCycleFallback;
import dev.zcripted.obx.util.text.Placeholders;
import dev.zcripted.obx.util.control.ServerControlActions;
import dev.zcripted.obx.util.control.ServerControlState;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.Arrays;
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
    private static final int CLOSE_SLOT = AdminMenu.CLOSE_SLOT;

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
        MODULES
    }

    private AdminSubMenu() {
    }

    public static void open(OBX plugin, Player player, AdminMenu.PlaceholderView placeholder) {
        String stripped = ChatColor.stripColor(placeholder.name()).toLowerCase();
        if ("server control".equals(stripped)) {
            openServerControlMenu(player, placeholder);
        } else if ("moderation".equals(stripped)) {
            openJailCenterMenu(plugin, player, placeholder);
        } else if ("fun utilities".equals(stripped)) {
            openMobToolsMenu(plugin, player, placeholder);
        } else {
            openGenericMenu(player, placeholder);
        }
    }

    public static void openJailCenterMenu(OBX plugin, Player player, AdminMenu.PlaceholderView placeholder) {
        Holder holder = new Holder(placeholder, SubMenuType.JAIL_CENTER);
        Inventory inventory = Bukkit.createInventory(holder, 27, AdminMenu.gradientTitle("Jail Center"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);

        int jailCount = plugin.getJailService() == null ? 0 : plugin.getJailService().getJails().size();
        StringBuilder jailList = new StringBuilder();
        if (plugin.getJailService() != null) {
            int i = 0;
            for (dev.zcripted.obx.jail.Jail jail : plugin.getJailService().getJails()) {
                if (i++ > 0) jailList.append(", ");
                jailList.append(jail.getName());
            }
        }

        place(inventory, 10, createMenuItem(new String[]{"IRON_BARS"}, ChatColor.DARK_PURPLE + "Jail Anchors",
                loreLines(
                        ChatColor.GRAY + "Configured jails: " + ChatColor.YELLOW + jailCount,
                        jailList.length() == 0 ? ChatColor.DARK_GRAY + "(none yet)" : ChatColor.GRAY + jailList.toString(),
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "run /jails"
                )));
        place(inventory, 12, createMenuItem(new String[]{"COMPASS"}, ChatColor.DARK_PURPLE + "Set Jail Here",
                loreLines(
                        ChatColor.GRAY + "Save this location as a jail anchor.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "suggests /setjail <name>"
                )));
        place(inventory, 14, createMenuItem(new String[]{"REDSTONE_BLOCK"}, ChatColor.RED + "Delete Jail",
                loreLines(
                        ChatColor.GRAY + "Remove a jail anchor by name.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "suggests /deljail <name>"
                )));
        place(inventory, 16, createMenuItem(new String[]{"CLOCK", "WATCH"}, ChatColor.LIGHT_PURPLE + "Check Jail Time",
                loreLines(
                        ChatColor.GRAY + "View remaining jail time for a player.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "suggests /jailtime <player>"
                )));
        place(inventory, BACK_SLOT, createBackItem());
        place(inventory, CLOSE_SLOT, createCloseItem());
        player.openInventory(inventory);
    }

    public static void openMobToolsMenu(OBX plugin, Player player, AdminMenu.PlaceholderView placeholder) {
        Holder holder = new Holder(placeholder, SubMenuType.MOB_TOOLS);
        Inventory inventory = Bukkit.createInventory(holder, 27, AdminMenu.gradientTitle("Mob Tools"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);

        place(inventory, 10, createMenuItem(new String[]{"DIAMOND_SWORD"}, ChatColor.RED + "Butcher Nearby",
                loreLines(
                        ChatColor.GRAY + "Kill mobs within 32 blocks.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "runs /butcher 32"
                )));
        place(inventory, 12, createMenuItem(new String[]{"BLAZE_POWDER"}, ChatColor.DARK_PURPLE + "Spawn Mob",
                loreLines(
                        ChatColor.GRAY + "Spawn a mob at your crosshair.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "suggests /spawnmob <type>"
                )));
        place(inventory, 14, createMenuItem(new String[]{"LIGHTNING_ROD", "TRIDENT"}, ChatColor.LIGHT_PURPLE + "Smite at Crosshair",
                loreLines(
                        ChatColor.GRAY + "Strike lightning at your crosshair.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "runs /smite"
                )));
        place(inventory, 16, createMenuItem(new String[]{"OAK_SAPLING", "SAPLING"}, ChatColor.GREEN + "Grow Tree",
                loreLines(
                        ChatColor.GRAY + "Generate a tree at your crosshair.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "runs /tree"
                )));
        place(inventory, BACK_SLOT, createBackItem());
        place(inventory, CLOSE_SLOT, createCloseItem());
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

        place(inventory, BACK_SLOT, createBackItem());
        place(inventory, CLOSE_SLOT, createCloseItem());

        player.openInventory(inventory);
    }

    private static void openServerControlMenu(Player player, AdminMenu.PlaceholderView placeholder) {
        Holder holder = new Holder(placeholder, SubMenuType.SERVER_CONTROL);
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("Server Control"));
        holder.setInventory(inventory);

        fillWithFiller(inventory);

        place(inventory, 10, serverStateOverviewItem());
        place(inventory, 12, playerAccessOverviewItem());
        place(inventory, 14, performanceOverviewItem());
        place(inventory, 16, worldControlsOverviewItem());

        place(inventory, 19, createMenuItem(new String[]{"REDSTONE_COMPARATOR", "COMPARATOR"},
                ChatColor.DARK_PURPLE + "Plugin + Systems",
                Arrays.asList(
                        ChatColor.GRAY + "Plugin/system tools:",
                        ChatColor.YELLOW + "- Reload OBX configs",
                        ChatColor.YELLOW + "- Toggle plugin modules"
                )));

        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItem());

        player.openInventory(inventory);
    }

    public static void openServerStateMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.SERVER_STATE);
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("Server State"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        place(inventory, 10, createMenuItem(new String[]{"BARRIER"}, ChatColor.RED + "Stop Server",
                loreLines(
                        ChatColor.GRAY + "Stops the server immediately.",
                        ChatColor.YELLOW + "Requires double-click confirmation."
                )));
        place(inventory, 12, createMenuItem(new String[]{"REDSTONE_BLOCK"}, ChatColor.DARK_PURPLE + "Restart (5s)",
                loreLines(
                        ChatColor.GRAY + "Broadcast 5s warning then shutdown.",
                        ChatColor.YELLOW + "Not a graceful restart."
                )));
        place(inventory, 14, createMenuItem(new String[]{"TOTEM_OF_UNDYING"}, ChatColor.DARK_PURPLE + "Safe Restart",
                loreLines(
                        ChatColor.GRAY + "Graceful restart with short delay.",
                        ChatColor.YELLOW + "Use when players need warning."
                )));
        place(inventory, 16, lockServerItem());
        place(inventory, 19, unlockServerItem());
        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItemToServerControl());
        player.openInventory(inventory);
    }

    public static void openPlayerAccessMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.PLAYER_ACCESS);
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("Player Access"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        place(inventory, 10, whitelistToggleItem());
        place(inventory, 12, joinLockToggleItem());
        place(inventory, 14, maxPlayersItem());
        place(inventory, 16, createMenuItem(new String[]{"BARRIER"}, ChatColor.RED + "Kick Non-Ops",
                loreLines(
                        ChatColor.GRAY + "Immediately kick all non-op players.",
                        ChatColor.YELLOW + "Use with join lock or whitelist."
                )));
        place(inventory, 19, createMenuItem(new String[]{"ENDER_EYE"}, ChatColor.LIGHT_PURPLE + "Spectator Only",
                loreLines(
                        ChatColor.GRAY + "Force non-ops into spectator mode.",
                        ChatColor.YELLOW + "Ops remain unchanged."
                )));
        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItemToServerControl());
        player.openInventory(inventory);
    }

    public static void openPerformanceMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.PERFORMANCE_HEALTH);
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("Performance + Health"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        place(inventory, 10, createMenuItem(new String[]{"CLOCK", "WATCH"}, ChatColor.DARK_PURPLE + "View TPS",
                loreLines(
                        ChatColor.GRAY + "Show current server TPS (1m sample).",
                        ChatColor.YELLOW + "Closes menu on click."
                )));
        place(inventory, 12, createMenuItem(new String[]{"HOPPER"}, ChatColor.YELLOW + "Clear Entities",
                loreLines(
                        ChatColor.GRAY + "Cleanup utilities:",
                        ChatColor.YELLOW + "Left-click: All entities",
                        ChatColor.YELLOW + "Right-click: Mobs only",
                        ChatColor.YELLOW + "Shift-left: Items only"
                )));
        place(inventory, 14, redstoneToggleItem());
        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItemToServerControl());
        player.openInventory(inventory);
    }

    public static void openWorldControlsMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.WORLD_CONTROLS);
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("World Controls"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        boolean autoSave = detectAutoSave();
        boolean daylight = detectDaylightCycle();
        place(inventory, 10, createMenuItem(new String[]{"BOOK"}, ChatColor.DARK_PURPLE + "Save Worlds",
                loreLines(
                        ChatColor.GRAY + "Flush chunks to disk for all worlds.",
                        ChatColor.YELLOW + "Use before restart or backups."
                )));
        place(inventory, 12, createMenuItem(new String[]{"REDSTONE_COMPARATOR", "COMPARATOR"}, ChatColor.DARK_PURPLE + "Autosave Toggle",
                loreLines(
                        statusLine("Autosave", autoSave),
                        ChatColor.GRAY + "Flip auto-save for every loaded world."
                )));
        place(inventory, 14, createMenuItem(new String[]{"MAP"}, ChatColor.YELLOW + "World Border",
                loreLines(
                        ChatColor.GRAY + "Resize / recenter / reset the",
                        ChatColor.GRAY + "current world's border.",
                        ChatColor.YELLOW + "Opens the World Border editor."
                )));
        place(inventory, 16, createMenuItem(new String[]{"WATER_BUCKET"}, ChatColor.LIGHT_PURPLE + "Weather Control",
                loreLines(
                        ChatColor.GRAY + "Clear / Rain / Thunder for all worlds.",
                        ChatColor.YELLOW + "Opens weather submenu."
                )));
        place(inventory, 19, createMenuItem(new String[]{"CLOCK", "WATCH"}, ChatColor.DARK_PURPLE + "Time Control",
                loreLines(
                        statusLine("Daylight Cycle", daylight),
                        ChatColor.YELLOW + "Set time or freeze cycle.",
                        ChatColor.YELLOW + "Opens time submenu."
                )));
        place(inventory, 21, createMenuItem(new String[]{"WRITABLE_BOOK", "BOOK_AND_QUILL"}, ChatColor.YELLOW + "Game Rule Editor",
                loreLines(
                        ChatColor.GRAY + "Toggle common gamerules.",
                        ChatColor.YELLOW + "Opens gamerule editor."
                )));
        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItemToServerControl());
        player.openInventory(inventory);
    }

    public static void openWeatherMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.WEATHER);
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("Weather Control"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        String current = detectWeatherState();
        place(inventory, 11, createMenuItem(new String[]{"SUNFLOWER", "GLOWSTONE_DUST"}, ChatColor.DARK_PURPLE + "Clear",
                loreLines(
                        ChatColor.GRAY + "Set all worlds to clear weather.",
                        valueLine("Current", current)
                )));
        place(inventory, 13, createMenuItem(new String[]{"WATER_BUCKET"}, ChatColor.LIGHT_PURPLE + "Rain",
                loreLines(
                        ChatColor.GRAY + "Set all worlds to rain.",
                        valueLine("Current", current)
                )));
        place(inventory, 15, createMenuItem(new String[]{"TRIDENT", "NETHER_STAR"}, ChatColor.DARK_PURPLE + "Thunder",
                loreLines(
                        ChatColor.GRAY + "Set all worlds to thunder & lightning.",
                        valueLine("Current", current)
                )));
        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItemToWorldControls());
        player.openInventory(inventory);
    }

    public static void openTimeMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.TIME);
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("Time Control"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        long time = detectTime();
        boolean daylight = detectDaylightCycle();
        place(inventory, 11, createMenuItem(new String[]{"HONEYCOMB", "TORCH"}, ChatColor.DARK_PURPLE + "Morning",
                loreLines(
                        ChatColor.GRAY + "Set time to morning (0).",
                        statusLine("Daylight Cycle", true),
                        valueLine("Current Time", time)
                )));
        place(inventory, 13, createMenuItem(new String[]{"CLOCK", "WATCH"}, ChatColor.YELLOW + "Noon",
                loreLines(
                        ChatColor.GRAY + "Set time to noon (6000).",
                        statusLine("Daylight Cycle", true),
                        valueLine("Current Time", time)
                )));
        place(inventory, 15, createMenuItem(new String[]{"ENDER_PEARL"}, ChatColor.BLUE + "Night",
                loreLines(
                        ChatColor.GRAY + "Set time to night (13000).",
                        statusLine("Daylight Cycle", true),
                        valueLine("Current Time", time)
                )));
        place(inventory, 20, createMenuItem(new String[]{"PACKED_ICE", "ICE"}, ChatColor.LIGHT_PURPLE + "Freeze/Unfreeze",
                loreLines(
                        ChatColor.GRAY + "Toggle doDaylightCycle for all worlds.",
                        statusLine("Currently", daylight)
                )));
        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItemToWorldControls());
        player.openInventory(inventory);
    }

    public static void openGameruleMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.GAMERULES);
        Inventory inventory = Bukkit.createInventory(holder, 45, AdminMenu.gradientTitle("Game Rule Editor"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        for (GameruleEntry entry : GameruleEntry.values()) {
            place(inventory, entry.slot(), createGameruleItem(world, entry));
        }
        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItemToWorldControls());
        player.openInventory(inventory);
    }

    public static void openPluginSystemsMenu(Player player) {
        Holder holder = new Holder(null, SubMenuType.PLUGIN_SYSTEMS);
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("Plugin + Systems"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        place(inventory, 12, createMenuItem(new String[]{"BOOK"}, ChatColor.DARK_PURPLE + "Reload Configs",
                loreLines(
                        ChatColor.GRAY + "Reload OBX config/messages/data files.",
                        ChatColor.YELLOW + "Logs details to console and notifies you."
                )));
        place(inventory, 14, createMenuItem(new String[]{"REPEATER", "REDSTONE_REPEATER"}, ChatColor.YELLOW + "Toggle Modules",
                loreLines(
                        ChatColor.GRAY + "Enable/disable chat, scoreboard, tablist,",
                        ChatColor.GRAY + "join/leave, MOTD, and hub modules.",
                        ChatColor.YELLOW + "Opens the Module Toggles menu."
                )));
        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItemToServerControl());
        player.openInventory(inventory);
    }

    /**
     * Hub / Lobby Controls — reached from the AdminMenu Hub slot (43) and
     * from {@code /hub menu}. Renders the current hub-mode state, kit
     * options, per-world list, and an enable toggle.
     *
     * <p>Click actions are wired in
     * {@link dev.zcripted.obx.listener.menu.MainMenuListener}.
     */
    public static void openHubMenu(OBX plugin, Player player) {
        Holder holder = new Holder(null, SubMenuType.HUB);
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("Hub / Lobby Controls"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);

        boolean hubEnabled = plugin.getHubService() != null && plugin.getHubService().isEnabled();
        List<String> worlds = plugin.getHubService() == null
                ? java.util.Collections.<String>emptyList()
                : plugin.getHubService().getHubWorlds();
        int worldCount = worlds.size();
        String worldSummary = worldCount == 0 ? "—" : String.join(", ", worlds);
        int cdSeconds = plugin.getHubService() == null ? 3 : plugin.getHubService().launchpadCooldownSeconds();

        place(inventory, 10, createMenuItem(
                new String[]{hubEnabled ? "LIME_DYE" : "GRAY_DYE", "INK_SACK", "DYE"},
                hubEnabled ? ChatColor.GREEN + "Hub Mode: Enabled" : ChatColor.RED + "Hub Mode: Disabled",
                loreLines(
                        statusLine("Hub Mode", hubEnabled),
                        ChatColor.GRAY + "Click to toggle.",
                        ChatColor.YELLOW + "Saved live to systems/hub.yml."
                )));

        place(inventory, 12, createMenuItem(new String[]{"GRASS_BLOCK", "GRASS"}, ChatColor.DARK_PURPLE + "Hub Worlds",
                loreLines(
                        valueLine("Count", worldCount),
                        ChatColor.YELLOW + "Worlds: " + ChatColor.WHITE + worldSummary,
                        ChatColor.YELLOW + "Left-click: " + ChatColor.GRAY + "Add current world",
                        ChatColor.YELLOW + "Right-click: " + ChatColor.GRAY + "Remove current world"
                )));

        place(inventory, 14, createMenuItem(new String[]{"COMPASS"}, ChatColor.DARK_PURPLE + "Open Server Selector",
                loreLines(
                        ChatColor.GRAY + "Preview the live selector GUI.",
                        ChatColor.YELLOW + "Reads selector.servers from hub.yml."
                )));

        place(inventory, 16, createMenuItem(new String[]{"BOOK"}, ChatColor.DARK_PURPLE + "Reload hub.yml",
                loreLines(
                        ChatColor.GRAY + "Re-read systems/hub.yml without restarting.",
                        ChatColor.YELLOW + "Use after editing in a server panel."
                )));

        place(inventory, 19, createMenuItem(new String[]{"CHEST"}, ChatColor.DARK_PURPLE + "Re-apply Kit (All)",
                loreLines(
                        ChatColor.GRAY + "Apply the hub kit to every player",
                        ChatColor.GRAY + "currently in a hub world."
                )));

        place(inventory, 21, createMenuItem(new String[]{"FIREWORK_ROCKET", "FIREWORK", "FEATHER"},
                ChatColor.DARK_PURPLE + "Launchpad Settings",
                loreLines(
                        valueLine("Cooldown", cdSeconds + "s"),
                        ChatColor.YELLOW + "Edit in systems/hub.yml → items.launchpad",
                        ChatColor.GRAY + "Cooldown shows live above the hotbar."
                )));

        place(inventory, 23, createMenuItem(new String[]{"FISHING_ROD"}, ChatColor.DARK_PURPLE + "Jump-To Rod",
                loreLines(
                        statusLine("Enabled",
                                plugin.getHubService() != null
                                        && plugin.getHubService().isItemEnabled(
                                                dev.zcripted.obx.hub.item.HubItems.ID_JUMP_ROD)),
                        ChatColor.GRAY + "Edit in systems/hub.yml → items.jump-rod"
                )));

        place(inventory, 25, createMenuItem(new String[]{"LIME_DYE", "INK_SACK", "DYE"},
                ChatColor.DARK_PURPLE + "Players-Visibility Toggle",
                loreLines(
                        statusLine("Enabled",
                                plugin.getHubService() != null
                                        && plugin.getHubService().isItemEnabled(
                                                dev.zcripted.obx.hub.item.HubItems.ID_VANISH_ALL)),
                        ChatColor.GRAY + "Edit in systems/hub.yml → items.vanish-all"
                )));

        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItem());
        player.openInventory(inventory);
    }

    /**
     * Handles click dispatch for the HUB sub-menu. Called from
     * {@link dev.zcripted.obx.listener.menu.MainMenuListener}
     * when the holder's type is {@link SubMenuType#HUB}.
     */
    public static void handleHubMenuClick(OBX plugin, Player player, int slot,
                                          org.bukkit.event.inventory.ClickType click) {
        if (plugin.getHubService() == null) {
            return;
        }
        switch (slot) {
            case 10: {
                boolean next = plugin.getHubService().toggleEnabled();
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
                    boolean removed = plugin.getHubService().removeHubWorld(worldName);
                    plugin.getLanguageManager().send(player,
                            removed ? "hub.admin.world.removed" : "hub.admin.world.not-listed",
                            placeholders);
                } else {
                    boolean added = plugin.getHubService().addHubWorld(worldName);
                    plugin.getLanguageManager().send(player,
                            added ? "hub.admin.world.added" : "hub.admin.world.already-listed",
                            placeholders);
                }
                openHubMenu(plugin, player);
                return;
            }
            case 14:
                player.closeInventory();
                dev.zcripted.obx.gui.player.ServerSelectorMenu.open(plugin, player);
                return;
            case 16:
                plugin.getHubService().reload();
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

    private static ItemStack createGameruleItem(World world, GameruleEntry entry) {
        Boolean enabled = getBooleanGameRule(world, entry.rule());
        String state;
        List<String> lore;
        if (enabled == null) {
            state = ChatColor.DARK_GRAY + "N/A";
            lore = loreLines(ChatColor.RED + "Unavailable on this server.");
        } else {
            state = enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
            lore = loreLines("Click to toggle", "Current: " + state);
        }
        ItemStack item = new ItemStack(resolveMaterial(entry.materials()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + entry.display() + ChatColor.GRAY + " [" + state + ChatColor.GRAY + "]");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Live status-item builders ────────────────────────────────────────────
    // Rebuilt on every menu open and by AdminMenuRefreshTask so the displayed
    // whitelist / join-lock / redstone / max-player state always matches the
    // server's actual state — even when another admin changes it (or a reload
    // happens) while the menu is open.

    private static ItemStack serverStateOverviewItem() {
        return createMenuItem(new String[]{"LEVER"}, ChatColor.DARK_PURPLE + "Server State",
                Arrays.asList(
                        ChatColor.GRAY + "Stop/Restart/Lock server.",
                        statusLine("Whitelist", Bukkit.hasWhitelist()),
                        statusLine("Join Lock", ServerControlState.isJoinLocked()),
                        statusLine("Redstone", !ServerControlState.isRedstoneFrozen()),
                        valueLine("Max Players", Bukkit.getMaxPlayers())
                ));
    }

    private static ItemStack playerAccessOverviewItem() {
        return createMenuItem(new String[]{"PLAYER_HEAD", "SKULL_ITEM"},
                ChatColor.DARK_PURPLE + "Player Access",
                Arrays.asList(
                        ChatColor.GRAY + "Whitelist & join control:",
                        statusLine("Whitelist", Bukkit.hasWhitelist()),
                        statusLine("Join Lock", ServerControlState.isJoinLocked()),
                        valueLine("Max Players", Bukkit.getMaxPlayers()),
                        ChatColor.YELLOW + "Kick non-ops / Spectator-only"
                ));
    }

    private static ItemStack performanceOverviewItem() {
        return createMenuItem(new String[]{"CLOCK", "WATCH"},
                ChatColor.DARK_PURPLE + "Performance + Health",
                Arrays.asList(
                        ChatColor.GRAY + "Server health tools:",
                        ChatColor.YELLOW + "View TPS / Clear entities",
                        statusLine("Redstone Updates", !ServerControlState.isRedstoneFrozen())
                ));
    }

    private static ItemStack worldControlsOverviewItem() {
        return createMenuItem(new String[]{"GRASS_BLOCK", "GRASS"},
                ChatColor.DARK_PURPLE + "World Controls",
                Arrays.asList(
                        ChatColor.GRAY + "World management:",
                        statusLine("Autosave", detectAutoSave()),
                        ChatColor.YELLOW + "Weather / Time / Gamerules",
                        ChatColor.YELLOW + "World Border"
                ));
    }

    private static ItemStack lockServerItem() {
        return createMenuItem(new String[]{"IRON_DOOR"}, ChatColor.YELLOW + LOCK_ICON + " Lock Server",
                loreLines(
                        ChatColor.GRAY + "Kicks non-ops and enables the whitelist.",
                        statusLine("Whitelist", Bukkit.hasWhitelist()),
                        statusLine("Join Lock", ServerControlState.isJoinLocked())
                ));
    }

    private static ItemStack unlockServerItem() {
        return createMenuItem(new String[]{"OAK_DOOR"}, ChatColor.GREEN + UNLOCK_ICON + " Unlock Server",
                loreLines(
                        ChatColor.GRAY + "Disables the whitelist and allows joins.",
                        statusLine("Whitelist", Bukkit.hasWhitelist()),
                        statusLine("Join Lock", ServerControlState.isJoinLocked())
                ));
    }

    private static ItemStack whitelistToggleItem() {
        return createMenuItem(new String[]{"PAPER"}, ChatColor.DARK_PURPLE + "Toggle Whitelist",
                loreLines(
                        ChatColor.GRAY + "Enable/disable whitelist.",
                        statusLine("Current", Bukkit.hasWhitelist())
                ));
    }

    private static ItemStack joinLockToggleItem() {
        return createMenuItem(new String[]{"IRON_BARS"}, ChatColor.DARK_PURPLE + "Toggle Join Lock",
                loreLines(
                        ChatColor.GRAY + "Blocks new joins; kicks non-ops on enable.",
                        statusLine("Current", ServerControlState.isJoinLocked())
                ));
    }

    private static ItemStack maxPlayersItem() {
        return createMenuItem(new String[]{"PLAYER_HEAD", "SKULL_ITEM"}, ChatColor.YELLOW + "Max Players",
                loreLines(
                        ChatColor.GRAY + "Adjust Paper/Spigot/Velo/Bungee slot cap.",
                        valueLine("Current", Bukkit.getMaxPlayers()),
                        ChatColor.YELLOW + "Left-click: -1 | Right-click: +1"
                ));
    }

    private static ItemStack redstoneToggleItem() {
        return createMenuItem(new String[]{"REDSTONE_TORCH"}, ChatColor.DARK_PURPLE + "Toggle Redstone",
                loreLines(
                        statusLine("Redstone Updates", !ServerControlState.isRedstoneFrozen()),
                        ChatColor.GRAY + "Freezes dust, repeaters, observers, pistons, hoppers, rails, lamps, etc."
                ));
    }

    /**
     * Re-renders the live status items of an open admin submenu so the displayed
     * whitelist / join-lock / redstone / max-player state matches the server.
     * Driven by {@link dev.zcripted.obx.gui.admin.AdminMenuRefreshTask}
     * and called immediately after a click that changes server state.
     */
    public static void refresh(Holder holder) {
        if (holder == null || holder.getType() == null) {
            return;
        }
        Inventory inventory = holder.getInventory();
        if (inventory == null) {
            return;
        }
        switch (holder.getType()) {
            case SERVER_CONTROL:
                place(inventory, 10, serverStateOverviewItem());
                place(inventory, 12, playerAccessOverviewItem());
                place(inventory, 14, performanceOverviewItem());
                place(inventory, 16, worldControlsOverviewItem());
                break;
            case SERVER_STATE:
                place(inventory, 16, lockServerItem());
                place(inventory, 19, unlockServerItem());
                break;
            case PLAYER_ACCESS:
                place(inventory, 10, whitelistToggleItem());
                place(inventory, 12, joinLockToggleItem());
                place(inventory, 14, maxPlayersItem());
                break;
            case PERFORMANCE_HEALTH:
                place(inventory, 14, redstoneToggleItem());
                break;
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

    private static boolean setBooleanGameRule(OBX plugin, World world, String ruleName, boolean value) {
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

    private static String detectWeatherState() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) {
            return "Unknown";
        }
        if (world.isThundering()) {
            return "Thunder";
        }
        if (world.hasStorm()) {
            return "Rain";
        }
        return "Clear";
    }

    private static long detectTime() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        return world == null ? -1L : world.getTime();
    }

    // Action handlers
    public static void handleAction(OBX plugin, Player player, Holder holder, int slot, ClickType click) {
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
            default:
                break;
        }
        // Reflect any state change immediately for the clicking admin (the global
        // AdminMenuRefreshTask covers other admins viewing the same menu).
        refresh(holder);
        try {
            player.updateInventory();
        } catch (Throwable ignored) {
            // updateInventory is deprecated/absent on some forks — non-fatal.
        }
    }

    private static void handleServerControlClick(OBX plugin, Player player, int slot) {
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

    private static void handleServerStateClick(OBX plugin, Player player, int slot) {
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

    private static void handlePlayerAccessClick(OBX plugin, Player player, int slot, ClickType click) {
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

    private static void handlePerformanceClick(OBX plugin, Player player, int slot, ClickType click) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 10) {
            // Identical styled report to the /tps command.
            dev.zcripted.obx.command.admin.TpsCommand.sendReport(plugin, player);
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

    private static void handleWorldControlsClick(OBX plugin, Player player, int slot, ClickType click) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 10) {
            for (World world : Bukkit.getWorlds()) {
                world.save();
            }
            languages.send(player, "admin.world.saved");
        } else if (slot == 12) {
            boolean newState = false;
            if (!Bukkit.getWorlds().isEmpty()) {
                World world = Bukkit.getWorlds().get(0);
                newState = !world.isAutoSave();
            }
            for (World world : Bukkit.getWorlds()) {
                world.setAutoSave(newState);
            }
            String state = languages.get(player, newState ? "admin.world.autosave.state.enabled" : "admin.world.autosave.state.disabled");
            languages.send(player, "admin.world.autosave.status", Placeholders.with("state", state));
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

    private static void handlePluginSystemsClick(OBX plugin, Player player, int slot) {
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

    private static void handleWeatherClick(OBX plugin, Player player, int slot) {
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

    private static void handleTimeClick(OBX plugin, Player player, int slot) {
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
        }
    }

    private static void handleGameruleClick(OBX plugin, Player player, int slot) {
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
        placeholders.put("rule", entry.display());
        placeholders.put("state", state);
        languages.send(player, "admin.gamerule.toggled", placeholders);
        openGameruleMenu(player);
    }

    private static void startRestartCountdown(OBX plugin, Player initiator, int seconds, String labelKey) {
        final BossBar bar = Bukkit.createBossBar(plugin.getLanguageManager().get(initiator, "admin.restart.title", Collections.singletonMap("label", plugin.getLanguageManager().get(initiator, labelKey))), BarColor.RED, BarStyle.SOLID);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("label", plugin.getLanguageManager().get(initiator, labelKey));
        placeholders.put("seconds", String.valueOf(seconds));
        plugin.getLanguageManager().broadcast("admin.restart.countdown-start", placeholders);

        final int[] remaining = {seconds};
        final dev.zcripted.obx.platform.scheduler.SchedulerAdapter.CancellableTask[] taskRef = new dev.zcripted.obx.platform.scheduler.SchedulerAdapter.CancellableTask[1];
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

    private static void toggleRedstone(OBX plugin, Player player) {
        boolean frozen = ServerControlState.toggleRedstoneFrozen();
        LanguageManager languages = plugin.getLanguageManager();
        String state = languages.get(player, frozen ? "admin.redstone.state.frozen" : "admin.redstone.state.resumed");
        ServerControlActions.boxMessage(plugin, player, "Redstone", "admin.redstone.state-message", Placeholders.with("state", state));
        dev.zcripted.obx.util.message.ConsoleLog.info(plugin,
                "Redstone updates " + (frozen ? "frozen" : "resumed") + " by " + player.getName());
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

    private static double fetchTps() {
        try {
            Object server = Bukkit.getServer();
            java.lang.reflect.Method method = server.getClass().getMethod("getTPS");
            Object result = method.invoke(server);
            if (result instanceof double[]) {
                double[] tps = (double[]) result;
                if (tps.length > 0) {
                    return tps[0];
                }
            }
        } catch (Throwable ignored) {
            // Fall through to not available
        }
        return -1;
    }

    public static final class Holder extends MenuHolder {
        private final AdminMenu.PlaceholderView placeholder;
        private final SubMenuType type;

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
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("World Border"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        double size = borderSize(player);
        String world = player.getWorld() == null ? "?" : player.getWorld().getName();
        place(inventory, 10, createMenuItem(new String[]{"RED_CONCRETE", "REDSTONE_BLOCK"}, ChatColor.RED + "− 1000",
                loreLines(ChatColor.GRAY + "Shrink the border by 1000 blocks.")));
        place(inventory, 11, createMenuItem(new String[]{"PINK_CONCRETE", "REDSTONE"}, ChatColor.RED + "− 100",
                loreLines(ChatColor.GRAY + "Shrink the border by 100 blocks.")));
        place(inventory, 13, createMenuItem(new String[]{"MAP"}, ChatColor.DARK_PURPLE + "World Border",
                loreLines(
                        valueLine("World", world),
                        valueLine("Diameter", String.format(java.util.Locale.ENGLISH, "%.0f", size) + " blocks"),
                        ChatColor.GRAY + "Adjust with the buttons.")));
        place(inventory, 15, createMenuItem(new String[]{"LIME_CONCRETE", "EMERALD_BLOCK"}, ChatColor.GREEN + "+ 100",
                loreLines(ChatColor.GRAY + "Grow the border by 100 blocks.")));
        place(inventory, 16, createMenuItem(new String[]{"GREEN_CONCRETE", "EMERALD"}, ChatColor.GREEN + "+ 1000",
                loreLines(ChatColor.GRAY + "Grow the border by 1000 blocks.")));
        place(inventory, 20, createMenuItem(new String[]{"COMPASS"}, ChatColor.YELLOW + "Center On Me",
                loreLines(ChatColor.GRAY + "Recenter the border on your position.")));
        place(inventory, 24, createMenuItem(new String[]{"BARRIER"}, ChatColor.LIGHT_PURPLE + "Reset",
                loreLines(ChatColor.GRAY + "Reset to the vanilla default", ChatColor.GRAY + "(60,000,000 wide, centered 0, 0).")));
        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItemToWorldControls());
        player.openInventory(inventory);
    }

    private static double borderSize(Player player) {
        try {
            return player.getWorld().getWorldBorder().getSize();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static void handleWorldBorderClick(OBX plugin, Player player, int slot) {
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

    public static void openModulesMenu(OBX plugin, Player player) {
        Holder holder = new Holder(null, SubMenuType.MODULES);
        Inventory inventory = Bukkit.createInventory(holder, 36, AdminMenu.gradientTitle("Module Toggles"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);
        for (ModuleEntry module : ModuleEntry.values()) {
            boolean on = module.isEnabled(plugin);
            place(inventory, module.slot(), createMenuItem(module.materials(),
                    (on ? ChatColor.GREEN : ChatColor.RED) + module.display(),
                    loreLines(
                            statusLine("State", on),
                            ChatColor.GRAY + "Click to " + (on ? "disable" : "enable") + ".",
                            ChatColor.YELLOW + "Saved to config and applied live.")));
        }
        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createMenuItem(new String[]{"ARROW", "SPECTRAL_ARROW"},
                ChatColor.DARK_PURPLE + "Back to Plugin + Systems",
                loreLines(ChatColor.GRAY + "Return to the Plugin + Systems menu.")));
        player.openInventory(inventory);
    }

    private static void handleModulesClick(OBX plugin, Player player, int slot) {
        ModuleEntry module = ModuleEntry.forSlot(slot);
        if (module == null) {
            return;
        }
        boolean now = !module.isEnabled(plugin);
        module.setEnabled(plugin, now);
        LanguageManager languages = plugin.getLanguageManager();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("module", module.display());
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
        CHAT("Chat Formatting", 10, new String[]{"WRITABLE_BOOK", "BOOK_AND_QUILL", "BOOK"}),
        SCOREBOARD("Scoreboard", 11, new String[]{"OAK_SIGN", "SIGN"}),
        TABLIST("Tablist", 12, new String[]{"PLAYER_HEAD", "SKULL_ITEM", "PAPER"}),
        JOIN_LEAVE("Join / Leave Broadcasts", 13, new String[]{"OAK_DOOR", "WOODEN_DOOR", "WOOD_DOOR"}),
        JOIN_MOTD("Welcome MOTD", 14, new String[]{"PAINTING"}),
        HUB("Hub / Lobby", 15, new String[]{"COMPASS"});

        private final String display;
        private final int slot;
        private final String[] materials;

        ModuleEntry(String display, int slot, String[] materials) {
            this.display = display;
            this.slot = slot;
            this.materials = materials;
        }

        String display() {
            return display;
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

        boolean isEnabled(OBX plugin) {
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
                    return plugin.getHubService() != null && plugin.getHubService().isEnabled();
                default:
                    return false;
            }
        }

        void setEnabled(OBX plugin, boolean value) {
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
                    if (plugin.getHubService() != null) plugin.getHubService().setEnabled(value);
                    break;
                default:
                    break;
            }
        }
    }

    private static void writeModuleFlag(OBX plugin, String relFile, String key, boolean value) {
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
        private static final int MAX_SLOT = 44;

        static {
            int slot = 0;
            for (GameruleEntry entry : values()) {
                while (slot == AdminMenu.CLOSE_SLOT || slot == SERVER_BACK_SLOT) {
                    slot++; // step over the reserved close / back nav slots
                }
                if (slot > MAX_SLOT) {
                    break; // safety: never overflow the 45-slot editor
                }
                BY_SLOT.put(slot, entry);
                SLOT_OF.put(entry, slot);
                slot++;
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
