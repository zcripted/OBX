package dev.sergeantfuzzy.sfcore.gui.admin;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.control.DaylightCycleFallback;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import dev.sergeantfuzzy.sfcore.util.control.ServerControlState;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
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
        HUB
    }

    private AdminSubMenu() {
    }

    public static void open(Main plugin, Player player, AdminMenu.PlaceholderView placeholder) {
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

    public static void openJailCenterMenu(Main plugin, Player player, AdminMenu.PlaceholderView placeholder) {
        Holder holder = new Holder(placeholder, SubMenuType.JAIL_CENTER);
        Inventory inventory = Bukkit.createInventory(holder, 27, AdminMenu.gradientTitle("Jail Center"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);

        int jailCount = plugin.getJailService() == null ? 0 : plugin.getJailService().getJails().size();
        StringBuilder jailList = new StringBuilder();
        if (plugin.getJailService() != null) {
            int i = 0;
            for (dev.sergeantfuzzy.sfcore.jail.Jail jail : plugin.getJailService().getJails()) {
                if (i++ > 0) jailList.append(", ");
                jailList.append(jail.getName());
            }
        }

        place(inventory, 10, createMenuItem(new String[]{"IRON_BARS"}, ChatColor.GOLD + "Jail Anchors",
                loreLines(
                        ChatColor.GRAY + "Configured jails: " + ChatColor.YELLOW + jailCount,
                        jailList.length() == 0 ? ChatColor.DARK_GRAY + "(none yet)" : ChatColor.GRAY + jailList.toString(),
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "run /jails"
                )));
        place(inventory, 12, createMenuItem(new String[]{"COMPASS"}, ChatColor.GOLD + "Set Jail Here",
                loreLines(
                        ChatColor.GRAY + "Save this location as a jail anchor.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "suggests /setjail <name>"
                )));
        place(inventory, 14, createMenuItem(new String[]{"REDSTONE_BLOCK"}, ChatColor.RED + "Delete Jail",
                loreLines(
                        ChatColor.GRAY + "Remove a jail anchor by name.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "suggests /deljail <name>"
                )));
        place(inventory, 16, createMenuItem(new String[]{"CLOCK", "WATCH"}, ChatColor.AQUA + "Check Jail Time",
                loreLines(
                        ChatColor.GRAY + "View remaining jail time for a player.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "suggests /jailtime <player>"
                )));
        place(inventory, BACK_SLOT, createBackItem());
        place(inventory, CLOSE_SLOT, createCloseItem());
        player.openInventory(inventory);
    }

    public static void openMobToolsMenu(Main plugin, Player player, AdminMenu.PlaceholderView placeholder) {
        Holder holder = new Holder(placeholder, SubMenuType.MOB_TOOLS);
        Inventory inventory = Bukkit.createInventory(holder, 27, AdminMenu.gradientTitle("Mob Tools"));
        holder.setInventory(inventory);
        fillWithFiller(inventory);

        place(inventory, 10, createMenuItem(new String[]{"DIAMOND_SWORD"}, ChatColor.RED + "Butcher Nearby",
                loreLines(
                        ChatColor.GRAY + "Kill mobs within 32 blocks.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "runs /butcher 32"
                )));
        place(inventory, 12, createMenuItem(new String[]{"BLAZE_POWDER"}, ChatColor.GOLD + "Spawn Mob",
                loreLines(
                        ChatColor.GRAY + "Spawn a mob at your crosshair.",
                        ChatColor.YELLOW + "Click: " + ChatColor.GRAY + "suggests /spawnmob <type>"
                )));
        place(inventory, 14, createMenuItem(new String[]{"LIGHTNING_ROD", "TRIDENT"}, ChatColor.AQUA + "Smite at Crosshair",
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
                ChatColor.GOLD + "Plugin + Systems",
                Arrays.asList(
                        ChatColor.GRAY + "Plugin/system tools:",
                        ChatColor.YELLOW + "- Reload SF-Core configs",
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
        place(inventory, 12, createMenuItem(new String[]{"REDSTONE_BLOCK"}, ChatColor.GOLD + "Restart (5s)",
                loreLines(
                        ChatColor.GRAY + "Broadcast 5s warning then shutdown.",
                        ChatColor.YELLOW + "Not a graceful restart."
                )));
        place(inventory, 14, createMenuItem(new String[]{"TOTEM_OF_UNDYING"}, ChatColor.GOLD + "Safe Restart",
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
        place(inventory, 19, createMenuItem(new String[]{"ENDER_EYE"}, ChatColor.AQUA + "Spectator Only",
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
        place(inventory, 10, createMenuItem(new String[]{"CLOCK", "WATCH"}, ChatColor.GOLD + "View TPS",
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
        place(inventory, 10, createMenuItem(new String[]{"BOOK"}, ChatColor.GOLD + "Save Worlds",
                loreLines(
                        ChatColor.GRAY + "Flush chunks to disk for all worlds.",
                        ChatColor.YELLOW + "Use before restart or backups."
                )));
        place(inventory, 12, createMenuItem(new String[]{"REDSTONE_COMPARATOR", "COMPARATOR"}, ChatColor.GOLD + "Autosave Toggle",
                loreLines(
                        statusLine("Autosave", autoSave),
                        ChatColor.GRAY + "Flip auto-save for every loaded world."
                )));
        place(inventory, 14, createMenuItem(new String[]{"MAP"}, ChatColor.YELLOW + "World Border",
                loreLines(
                        ChatColor.GRAY + "Adjust world borders.",
                        ChatColor.YELLOW + "Placeholder (no action yet)."
                )));
        place(inventory, 16, createMenuItem(new String[]{"WATER_BUCKET"}, ChatColor.AQUA + "Weather Control",
                loreLines(
                        ChatColor.GRAY + "Clear / Rain / Thunder for all worlds.",
                        ChatColor.YELLOW + "Opens weather submenu."
                )));
        place(inventory, 19, createMenuItem(new String[]{"CLOCK", "WATCH"}, ChatColor.GOLD + "Time Control",
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
        place(inventory, 11, createMenuItem(new String[]{"SUNFLOWER", "GLOWSTONE_DUST"}, ChatColor.GOLD + "Clear",
                loreLines(
                        ChatColor.GRAY + "Set all worlds to clear weather.",
                        valueLine("Current", current)
                )));
        place(inventory, 13, createMenuItem(new String[]{"WATER_BUCKET"}, ChatColor.AQUA + "Rain",
                loreLines(
                        ChatColor.GRAY + "Set all worlds to rain.",
                        valueLine("Current", current)
                )));
        place(inventory, 15, createMenuItem(new String[]{"TRIDENT", "NETHER_STAR"}, ChatColor.DARK_AQUA + "Thunder",
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
        place(inventory, 11, createMenuItem(new String[]{"HONEYCOMB", "TORCH"}, ChatColor.GOLD + "Morning",
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
        place(inventory, 20, createMenuItem(new String[]{"PACKED_ICE", "ICE"}, ChatColor.AQUA + "Freeze/Unfreeze",
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
        place(inventory, 12, createMenuItem(new String[]{"BOOK"}, ChatColor.GOLD + "Reload Configs",
                loreLines(
                        ChatColor.GRAY + "Reload SF-Core config/messages/data files.",
                        ChatColor.YELLOW + "Logs details to console and notifies you."
                )));
        place(inventory, 14, createMenuItem(new String[]{"REPEATER", "REDSTONE_REPEATER"}, ChatColor.YELLOW + "Toggle Modules",
                loreLines(
                        ChatColor.GRAY + "Open module toggles (chat/economy/etc.).",
                        ChatColor.YELLOW + "Placeholder (no action yet)."
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
     * {@link dev.sergeantfuzzy.sfcore.listener.menu.MainMenuListener}.
     */
    public static void openHubMenu(Main plugin, Player player) {
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

        place(inventory, 12, createMenuItem(new String[]{"GRASS_BLOCK", "GRASS"}, ChatColor.GOLD + "Hub Worlds",
                loreLines(
                        valueLine("Count", worldCount),
                        ChatColor.YELLOW + "Worlds: " + ChatColor.WHITE + worldSummary,
                        ChatColor.YELLOW + "Left-click: " + ChatColor.GRAY + "Add current world",
                        ChatColor.YELLOW + "Right-click: " + ChatColor.GRAY + "Remove current world"
                )));

        place(inventory, 14, createMenuItem(new String[]{"COMPASS"}, ChatColor.GOLD + "Open Server Selector",
                loreLines(
                        ChatColor.GRAY + "Preview the live selector GUI.",
                        ChatColor.YELLOW + "Reads selector.servers from hub.yml."
                )));

        place(inventory, 16, createMenuItem(new String[]{"BOOK"}, ChatColor.GOLD + "Reload hub.yml",
                loreLines(
                        ChatColor.GRAY + "Re-read systems/hub.yml without restarting.",
                        ChatColor.YELLOW + "Use after editing in a server panel."
                )));

        place(inventory, 19, createMenuItem(new String[]{"CHEST"}, ChatColor.GOLD + "Re-apply Kit (All)",
                loreLines(
                        ChatColor.GRAY + "Apply the hub kit to every player",
                        ChatColor.GRAY + "currently in a hub world."
                )));

        place(inventory, 21, createMenuItem(new String[]{"FIREWORK_ROCKET", "FIREWORK", "FEATHER"},
                ChatColor.GOLD + "Launchpad Settings",
                loreLines(
                        valueLine("Cooldown", cdSeconds + "s"),
                        ChatColor.YELLOW + "Edit in systems/hub.yml → items.launchpad",
                        ChatColor.GRAY + "Cooldown shows live above the hotbar."
                )));

        place(inventory, 23, createMenuItem(new String[]{"FISHING_ROD"}, ChatColor.GOLD + "Jump-To Rod",
                loreLines(
                        statusLine("Enabled",
                                plugin.getHubService() != null
                                        && plugin.getHubService().isItemEnabled(
                                                dev.sergeantfuzzy.sfcore.hub.item.HubItems.ID_JUMP_ROD)),
                        ChatColor.GRAY + "Edit in systems/hub.yml → items.jump-rod"
                )));

        place(inventory, 25, createMenuItem(new String[]{"LIME_DYE", "INK_SACK", "DYE"},
                ChatColor.GOLD + "Players-Visibility Toggle",
                loreLines(
                        statusLine("Enabled",
                                plugin.getHubService() != null
                                        && plugin.getHubService().isItemEnabled(
                                                dev.sergeantfuzzy.sfcore.hub.item.HubItems.ID_VANISH_ALL)),
                        ChatColor.GRAY + "Edit in systems/hub.yml → items.vanish-all"
                )));

        place(inventory, CLOSE_SLOT, createCloseItem());
        place(inventory, SERVER_BACK_SLOT, createBackItem());
        player.openInventory(inventory);
    }

    /**
     * Handles click dispatch for the HUB sub-menu. Called from
     * {@link dev.sergeantfuzzy.sfcore.listener.menu.MainMenuListener}
     * when the holder's type is {@link SubMenuType#HUB}.
     */
    public static void handleHubMenuClick(Main plugin, Player player, int slot,
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
                dev.sergeantfuzzy.sfcore.gui.player.ServerSelectorMenu.open(plugin, player);
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

    private static void fillWithFiller(Inventory inventory) {
        ItemStack filler = createPane(" ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler.clone());
        }
    }

    private static ItemStack createPane(String name, List<String> lore) {
        Material material = resolveMaterial(new String[]{"GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "GLASS_PANE", "THIN_GLASS", "AIR"});
        boolean legacy = material != null && material.name().equalsIgnoreCase("STAINED_GLASS_PANE");
        ItemStack pane = legacy ? new ItemStack(material, 1, (short) 7) : new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack createMenuItem(String[] materials, String name, List<String> lore) {
        ItemStack item = new ItemStack(resolveMaterial(materials));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
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
            meta.setDisplayName(ChatColor.GOLD + entry.display() + ChatColor.GRAY + " [" + state + ChatColor.GRAY + "]");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void place(Inventory inventory, int slot, ItemStack item) {
        if (inventory == null || item == null) {
            return;
        }
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
    }

    private static ItemStack createBackItem() {
        ItemStack back = new ItemStack(resolveMaterial(new String[]{"ARROW", "SPECTRAL_ARROW"}));
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "Back to Admin Panel");
            backMeta.setLore(Arrays.asList(ChatColor.GRAY + "Return to the main Admin menu."));
            back.setItemMeta(backMeta);
        }
        return back;
    }

    private static ItemStack createBackItemToServerControl() {
        ItemStack back = new ItemStack(resolveMaterial(new String[]{"ARROW", "SPECTRAL_ARROW"}));
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "Back to Server Control");
            backMeta.setLore(Arrays.asList(ChatColor.GRAY + "Return to server control options."));
            back.setItemMeta(backMeta);
        }
        return back;
    }

    private static ItemStack createBackItemToWorldControls() {
        ItemStack back = new ItemStack(resolveMaterial(new String[]{"ARROW", "SPECTRAL_ARROW"}));
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "Back to World Controls");
            backMeta.setLore(Arrays.asList(ChatColor.GRAY + "Return to world controls."));
            back.setItemMeta(backMeta);
        }
        return back;
    }

    private static ItemStack createCloseItem() {
        ItemStack close = new ItemStack(resolveMaterial(new String[]{"BARRIER"}));
        ItemMeta meta = close.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Close");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to close this menu"));
            close.setItemMeta(meta);
        }
        return close;
    }

    private static Material resolveMaterial(String[] names) {
        if (names == null) {
            return Material.AIR;
        }
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                return material;
            }
        }
        return Material.AIR;
    }

    private static List<String> loreLines(String... lines) {
        return Arrays.asList(lines);
    }

    private static String statusLine(String label, boolean enabled) {
        return ChatColor.GRAY + label + ": " + (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED");
    }

    private static String valueLine(String label, Object value) {
        return ChatColor.GRAY + label + ": " + ChatColor.YELLOW + String.valueOf(value);
    }

    // ── Live status-item builders ────────────────────────────────────────────
    // Rebuilt on every menu open and by AdminMenuRefreshTask so the displayed
    // whitelist / join-lock / redstone / max-player state always matches the
    // server's actual state — even when another admin changes it (or a reload
    // happens) while the menu is open.

    private static ItemStack serverStateOverviewItem() {
        return createMenuItem(new String[]{"LEVER"}, ChatColor.GOLD + "Server State",
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
                ChatColor.GOLD + "Player Access",
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
                ChatColor.GOLD + "Performance + Health",
                Arrays.asList(
                        ChatColor.GRAY + "Server health tools:",
                        ChatColor.YELLOW + "View TPS / Clear entities",
                        statusLine("Redstone Updates", !ServerControlState.isRedstoneFrozen())
                ));
    }

    private static ItemStack worldControlsOverviewItem() {
        return createMenuItem(new String[]{"GRASS_BLOCK", "GRASS"},
                ChatColor.GOLD + "World Controls",
                Arrays.asList(
                        ChatColor.GRAY + "World management:",
                        statusLine("Autosave", detectAutoSave()),
                        ChatColor.YELLOW + "Weather / Time / Gamerules",
                        ChatColor.YELLOW + "World Border (placeholder)"
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
        return createMenuItem(new String[]{"PAPER"}, ChatColor.GOLD + "Toggle Whitelist",
                loreLines(
                        ChatColor.GRAY + "Enable/disable whitelist.",
                        statusLine("Current", Bukkit.hasWhitelist())
                ));
    }

    private static ItemStack joinLockToggleItem() {
        return createMenuItem(new String[]{"IRON_BARS"}, ChatColor.GOLD + "Toggle Join Lock",
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
        return createMenuItem(new String[]{"REDSTONE_TORCH"}, ChatColor.GOLD + "Toggle Redstone",
                loreLines(
                        statusLine("Redstone Updates", !ServerControlState.isRedstoneFrozen()),
                        ChatColor.GRAY + "Freezes dust, repeaters, observers, pistons, hoppers, rails, lamps, etc."
                ));
    }

    /**
     * Re-renders the live status items of an open admin submenu so the displayed
     * whitelist / join-lock / redstone / max-player state matches the server.
     * Driven by {@link dev.sergeantfuzzy.sfcore.gui.admin.AdminMenuRefreshTask}
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

    private static boolean setBooleanGameRule(Main plugin, World world, String ruleName, boolean value) {
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
    public static void handleAction(Main plugin, Player player, Holder holder, int slot, ClickType click) {
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

    private static void handleServerControlClick(Main plugin, Player player, int slot) {
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

    private static void handleServerStateClick(Main plugin, Player player, int slot) {
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
            Bukkit.setWhitelist(true);
            if (!ServerControlState.isJoinLocked()) {
                ServerControlState.toggleJoinLock();
            }
            kickNonOps(plugin, "admin.joinlock.kick-reason");
            languages.send(player, "admin.whitelist.toggled", Placeholders.with("state", languages.get(player, "admin.whitelist.state.enabled")));
            languages.send(player, "admin.joinlock.enabled", Placeholders.with("state", languages.get(player, "admin.joinlock.state.enabled")));
        } else if (slot == 19) {
            Bukkit.setWhitelist(false);
            if (ServerControlState.isJoinLocked()) {
                ServerControlState.toggleJoinLock();
            }
            languages.send(player, "admin.whitelist.toggled", Placeholders.with("state", languages.get(player, "admin.whitelist.state.disabled")));
            languages.send(player, "admin.joinlock.enabled", Placeholders.with("state", languages.get(player, "admin.joinlock.state.disabled")));
        }
    }

    private static void handlePlayerAccessClick(Main plugin, Player player, int slot, ClickType click) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 10) {
            boolean now = !Bukkit.hasWhitelist();
            Bukkit.setWhitelist(now);
            String state = languages.get(player, now ? "admin.whitelist.state.enabled" : "admin.whitelist.state.disabled");
            languages.send(player, "admin.whitelist.toggled", Placeholders.with("state", state));
        } else if (slot == 12) {
            boolean locked = ServerControlState.toggleJoinLock();
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.isOp() && locked) {
                    target.kickPlayer(languages.get(target, "admin.joinlock.kick-reason"));
                }
            }
            String state = languages.get(player, locked ? "admin.joinlock.state.enabled" : "admin.joinlock.state.disabled");
            languages.send(player, "admin.joinlock.enabled", Placeholders.with("state", state));
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
            kickNonOps(plugin, "admin.kick-nonops.reason");
            languages.send(player, "admin.kick-nonops.done");
        } else if (slot == 19) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.isOp()) {
                    target.setGameMode(GameMode.SPECTATOR);
                }
            }
            languages.send(player, "admin.spectator.applied");
        }
    }

    private static void handlePerformanceClick(Main plugin, Player player, int slot, ClickType click) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 10) {
            double tps = fetchTps();
            if (tps >= 0) {
                languages.send(player, "admin.performance.tps", Placeholders.with("tps", String.format("%.2f", tps)));
            } else {
                languages.send(player, "admin.performance.tps-missing");
            }
            player.closeInventory();
        } else if (slot == 12) {
            ClearMode mode = ClearMode.ALL;
            if (click.isRightClick() && !click.isLeftClick()) {
                mode = ClearMode.MOBS_ONLY;
            }
            if (click.isShiftClick() && click.isLeftClick()) {
                mode = ClearMode.ITEMS_ONLY;
            }
            int removed = clearEntities(mode);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("removed", String.valueOf(removed));
            String modeKey;
            switch (mode) {
                case MOBS_ONLY:
                    modeKey = "admin.performance.mode.mobs";
                    break;
                case ITEMS_ONLY:
                    modeKey = "admin.performance.mode.items";
                    break;
                case ALL:
                default:
                    modeKey = "admin.performance.mode.all";
                    break;
            }
            placeholders.put("mode", languages.get(player, modeKey));
            languages.send(player, "admin.performance.cleared", placeholders);
        } else if (slot == 14) {
            toggleRedstone(plugin, player);
        }
    }

    private static void handleWorldControlsClick(Main plugin, Player player, int slot, ClickType click) {
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
            languages.send(player, "admin.world.border-placeholder");
        } else if (slot == 16) {
            openWeatherMenu(player);
        } else if (slot == 19) {
            openTimeMenu(player);
        } else if (slot == 21) {
            openGameruleMenu(player);
        }
    }

    private static void handlePluginSystemsClick(Main plugin, Player player, int slot) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 12) {
            plugin.reloadPlugin();
            languages.send(player, "admin.plugin.reloaded");
            String message = languages.formatConsole("admin.plugin.reloaded-console", Placeholders.with("player", player.getName()));
            Bukkit.getServer().getConsoleSender().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        } else if (slot == 14) {
            languages.send(player, "admin.plugin.modules-placeholder");
        }
    }

    private static void handleWeatherClick(Main plugin, Player player, int slot) {
        LanguageManager languages = plugin.getLanguageManager();
        if (slot == 11) {
            for (World world : Bukkit.getWorlds()) {
                world.setStorm(false);
                world.setThundering(false);
            }
            languages.send(player, "admin.weather.clear");
        } else if (slot == 13) {
            for (World world : Bukkit.getWorlds()) {
                world.setStorm(true);
                world.setThundering(false);
            }
            languages.send(player, "admin.weather.rain");
        } else if (slot == 15) {
            for (World world : Bukkit.getWorlds()) {
                world.setStorm(true);
                world.setThundering(true);
            }
            languages.send(player, "admin.weather.thunder");
        }
    }

    private static void handleTimeClick(Main plugin, Player player, int slot) {
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

    private static void handleGameruleClick(Main plugin, Player player, int slot) {
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

    private static void startRestartCountdown(Main plugin, Player initiator, int seconds, String labelKey) {
        final BossBar bar = Bukkit.createBossBar(plugin.getLanguageManager().get(initiator, "admin.restart.title", Collections.singletonMap("label", plugin.getLanguageManager().get(initiator, labelKey))), BarColor.RED, BarStyle.SOLID);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("label", plugin.getLanguageManager().get(initiator, labelKey));
        placeholders.put("seconds", String.valueOf(seconds));
        plugin.getLanguageManager().broadcast("admin.restart.countdown-start", placeholders);

        final int[] remaining = {seconds};
        final dev.sergeantfuzzy.sfcore.platform.scheduler.SchedulerAdapter.CancellableTask[] taskRef = new dev.sergeantfuzzy.sfcore.platform.scheduler.SchedulerAdapter.CancellableTask[1];
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

    private static void toggleRedstone(Main plugin, Player player) {
        boolean frozen = ServerControlState.toggleRedstoneFrozen();
        LanguageManager languages = plugin.getLanguageManager();
        String state = languages.get(player, frozen ? "admin.redstone.state.frozen" : "admin.redstone.state.resumed");
        languages.send(player, "admin.redstone.state-message", Placeholders.with("state", state));
    }

    private static void kickNonOps(Main plugin, String reasonKey) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.isOp()) {
                target.kickPlayer(plugin.getLanguageManager().get(target, reasonKey));
            }
        }
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

    private enum ClearMode {ALL, MOBS_ONLY, ITEMS_ONLY}

    private static int clearEntities(ClearMode mode) {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player) {
                    continue;
                }
                if (mode == ClearMode.MOBS_ONLY && !(entity instanceof LivingEntity)) {
                    continue;
                }
                if (mode == ClearMode.ITEMS_ONLY && !(entity instanceof Item)) {
                    continue;
                }
                if (entity.getType() == EntityType.ARMOR_STAND) {
                    continue;
                }
                entity.remove();
                removed++;
            }
        }
        return removed;
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

    public static final class Holder implements InventoryHolder {
        private final AdminMenu.PlaceholderView placeholder;
        private final SubMenuType type;
        private Inventory inventory;

        public Holder(AdminMenu.PlaceholderView placeholder, SubMenuType type) {
            this.placeholder = placeholder;
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public AdminMenu.PlaceholderView getPlaceholder() {
            return placeholder;
        }

        public SubMenuType getType() {
            return type;
        }
    }

    private enum GameruleEntry {
        KEEP_INVENTORY("keepInventory", "Keep Inventory", 10, new String[]{"CHEST"}),
        MOB_GRIEFING("mobGriefing", "Mob Griefing", 11, new String[]{"CREEPER_SPAWN_EGG", "MONSTER_EGG"}),
        DO_DAYLIGHT("doDaylightCycle", "Daylight Cycle", 12, new String[]{"CLOCK", "WATCH"}),
        DO_WEATHER("doWeatherCycle", "Weather Cycle", 13, new String[]{"WATER_BUCKET"}),
        DO_FIRE_TICK("doFireTick", "Fire Spread", 14, new String[]{"FLINT_AND_STEEL"}),
        DO_MOB_SPAWNING("doMobSpawning", "Mob Spawning", 15, new String[]{"SPAWNER", "MOB_SPAWNER"}),
        NATURAL_REGEN("naturalRegeneration", "Natural Regeneration", 16, new String[]{"GOLDEN_APPLE"}),
        DO_ENTITY_DROPS("doEntityDrops", "Entity Drops", 19, new String[]{"HOPPER"}),
        DO_TILE_DROPS("doTileDrops", "Block Drops", 20, new String[]{"PISTON"}),
        IMMEDIATE_RESPAWN("doImmediateRespawn", "Immediate Respawn", 21, new String[]{"TOTEM_OF_UNDYING"}),
        INSOMNIA("doInsomnia", "Phantoms", 22, new String[]{"PHANTOM_MEMBRANE"}),
        ANNOUNCE_ADV("announceAdvancements", "Announce Advancements", 23, new String[]{"PAPER"}),
        FALL_DAMAGE("fallDamage", "Fall Damage", 24, new String[]{"FEATHER"});

        private static final Map<Integer, GameruleEntry> BY_SLOT = new HashMap<>();

        static {
            for (GameruleEntry entry : values()) {
                BY_SLOT.put(entry.slot, entry);
            }
        }

        private final String rule;
        private final String display;
        private final int slot;
        private final String[] materials;

        GameruleEntry(String rule, String display, int slot, String[] materials) {
            this.rule = rule;
            this.display = display;
            this.slot = slot;
            this.materials = materials;
        }

        public String rule() {
            return rule;
        }

        public String display() {
            return display;
        }

        public int slot() {
            return slot;
        }

        public String[] materials() {
            return materials;
        }

        public static GameruleEntry forSlot(int slot) {
            return BY_SLOT.get(slot);
        }
    }
}
