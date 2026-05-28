package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.gui.player.ServerSelectorMenu;
import dev.sergeantfuzzy.sfcore.hub.HubService;
import dev.sergeantfuzzy.sfcore.hub.kit.HubKitApplier;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.storage.DataService.SpawnInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implements {@code /hub} and the {@code /lobby} alias.
 *
 * <h3>Permission model</h3>
 * <ul>
 *   <li><b>Default players</b> (perm {@code sfcore.hub.use}) — bare command
 *       only. Teleports them to the first configured hub world (or spawn,
 *       if no hub world is loaded). Subcommands are <em>silently ignored</em>
 *       and tab-completion returns an empty list — they never see or learn
 *       about the admin surface.</li>
 *   <li><b>Admins / ops</b> (perm {@code sfcore.hub.admin}) — full
 *       subcommand set: {@code on|off|toggle|reload|give|selector|world add|world remove|world list|world here|menu}.
 *       Tab completion shows every available branch.</li>
 * </ul>
 */
public final class HubCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ADMIN_SUBCOMMANDS = Collections.unmodifiableList(Arrays.asList(
            "on", "off", "toggle", "reload", "give", "selector", "world", "menu"));

    private static final List<String> WORLD_SUBCOMMANDS = Collections.unmodifiableList(Arrays.asList(
            "add", "remove", "list", "here"));

    private final Main plugin;
    private final HubService hub;
    private final HubKitApplier applier;
    private final LanguageManager languages;

    public HubCommand(Main plugin, HubService hub, HubKitApplier applier) {
        this.plugin = plugin;
        this.hub = hub;
        this.applier = applier;
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isAdmin = sender.hasPermission("sfcore.hub.admin");

        // Default-player path: bare /hub or /lobby only — anything past
        // arg[0] is silently rejected as "unknown command" via the base
        // command help (matches the user's spec: subcommands must not
        // appear registered for default players).
        if (!isAdmin) {
            if (!sender.hasPermission("sfcore.hub.use")) {
                languages.send(sender, "core.no-permission");
                return true;
            }
            if (args.length > 0) {
                // Don't echo the admin surface — just behave like an
                // unknown invocation by sending the player to the hub.
                return handleTeleport(sender);
            }
            return handleTeleport(sender);
        }

        // Admin path.
        if (args.length == 0) {
            return handleTeleport(sender);
        }
        String sub = args[0].toLowerCase(Locale.ENGLISH);
        switch (sub) {
            case "on":
                return handleSetEnabled(sender, true);
            case "off":
                return handleSetEnabled(sender, false);
            case "toggle":
                return handleToggle(sender);
            case "reload":
                return handleReload(sender);
            case "give":
                return handleGive(sender, args);
            case "selector":
                return handleSelector(sender);
            case "menu":
                return handleMenu(sender);
            case "world":
                return handleWorld(sender, args);
            default:
                sendAdminUsage(sender);
                return true;
        }
    }

    // ── Teleport (player-facing base behaviour) ────────────────────────

    private boolean handleTeleport(CommandSender sender) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        Location target = resolveHubLocation();
        if (target == null) {
            languages.send(player, "hub.teleport.missing");
            return true;
        }
        DataService data = plugin.getDataService();
        if (data != null) {
            data.setBack(player.getUniqueId(), player.getLocation());
        }
        plugin.getTeleportManager().teleportPlayer(player, target, "hub.teleport.teleporting", null);
        return true;
    }

    /** Picks the first loaded hub world's spawn, falling back to /spawn. */
    private Location resolveHubLocation() {
        for (String name : hub.getHubWorlds()) {
            World world = Bukkit.getWorld(name);
            if (world != null) {
                return world.getSpawnLocation();
            }
        }
        SpawnInfo info = plugin.getDataService() == null ? null : plugin.getDataService().getSpawnInfo();
        return info == null ? null : info.getLocation();
    }

    // ── Admin actions ──────────────────────────────────────────────────

    private boolean handleSetEnabled(CommandSender sender, boolean value) {
        boolean previous = hub.isEnabled();
        if (previous == value) {
            languages.send(sender, value ? "hub.admin.already-on" : "hub.admin.already-off");
            return true;
        }
        hub.setEnabled(value);
        if (value) {
            applier.applyToAllInHubWorlds();
        } else {
            onHubDisabled();
        }
        languages.send(sender, value ? "hub.admin.enabled" : "hub.admin.disabled");
        return true;
    }

    private boolean handleToggle(CommandSender sender) {
        boolean next = hub.toggleEnabled();
        if (next) {
            applier.applyToAllInHubWorlds();
        } else {
            onHubDisabled();
        }
        languages.send(sender, next ? "hub.admin.enabled" : "hub.admin.disabled");
        return true;
    }

    /** Cleans up state that would otherwise leak after the hub is turned off. */
    private void onHubDisabled() {
        // Strip the launchpad's flight grant so players can't keep free-flying.
        applier.revokeFlightInHubWorlds();
        // Re-reveal anyone hidden via the vanish-all toggle.
        if (plugin.getHubItemUseListener() != null) {
            plugin.getHubItemUseListener().resetVisibilityForAll();
        }
    }

    private boolean handleReload(CommandSender sender) {
        hub.reload();
        languages.send(sender, "hub.admin.reloaded");
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[1]);
                languages.send(sender, "hub.admin.target-offline", placeholders);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            languages.send(sender, "hub.admin.give-usage");
            return true;
        }
        applier.applyForce(target);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        languages.send(sender, "hub.admin.gave", placeholders);
        return true;
    }

    private boolean handleSelector(CommandSender sender) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        ServerSelectorMenu.open(plugin, (Player) sender);
        return true;
    }

    private boolean handleMenu(CommandSender sender) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        dev.sergeantfuzzy.sfcore.gui.admin.AdminSubMenu.openHubMenu(plugin, (Player) sender);
        return true;
    }

    private boolean handleWorld(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendAdminUsage(sender);
            return true;
        }
        String action = args[1].toLowerCase(Locale.ENGLISH);
        switch (action) {
            case "list": {
                List<String> worlds = hub.getHubWorlds();
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("worlds", worlds.isEmpty() ? "—" : String.join(", ", worlds));
                languages.send(sender, "hub.admin.world.list", placeholders);
                return true;
            }
            case "here": {
                if (!(sender instanceof Player)) {
                    languages.send(sender, "core.player-only");
                    return true;
                }
                String worldName = ((Player) sender).getWorld().getName();
                boolean added = hub.addHubWorld(worldName);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("world", worldName);
                languages.send(sender, added ? "hub.admin.world.added" : "hub.admin.world.already-listed", placeholders);
                return true;
            }
            case "add": {
                if (args.length < 3) {
                    sendAdminUsage(sender);
                    return true;
                }
                String worldName = args[2];
                boolean added = hub.addHubWorld(worldName);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("world", worldName);
                languages.send(sender, added ? "hub.admin.world.added" : "hub.admin.world.already-listed", placeholders);
                return true;
            }
            case "remove": {
                if (args.length < 3) {
                    sendAdminUsage(sender);
                    return true;
                }
                String worldName = args[2];
                boolean removed = hub.removeHubWorld(worldName);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("world", worldName);
                languages.send(sender, removed ? "hub.admin.world.removed" : "hub.admin.world.not-listed", placeholders);
                return true;
            }
            default:
                sendAdminUsage(sender);
                return true;
        }
    }

    private void sendAdminUsage(CommandSender sender) {
        languages.send(sender, "hub.admin.usage");
    }

    // ── Tab completion ─────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Default players get zero suggestions — admin surface is hidden.
        if (!sender.hasPermission("sfcore.hub.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filterPrefix(ADMIN_SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("world")) {
            return filterPrefix(WORLD_SUBCOMMANDS, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("remove")) {
            return filterPrefix(hub.getHubWorlds(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("add")) {
            List<String> names = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                names.add(world.getName());
            }
            return filterPrefix(names, args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return filterPrefix(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> source, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>(source);
        }
        String normalized = prefix.toLowerCase(Locale.ENGLISH);
        List<String> out = new ArrayList<>();
        for (String value : source) {
            if (value.toLowerCase(Locale.ENGLISH).startsWith(normalized)) {
                out.add(value);
            }
        }
        return out;
    }
}
