package dev.zcripted.obx.feature.teleport.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.storage.DataService;
import dev.zcripted.obx.core.storage.DataService.SpawnInfo;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnCommand extends AbstractObxCommand implements TabCompleter, Listener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final Map<String, String> SUB_ALIASES = createAliasMap();
    /** Internal click-target token (the chat "confirm" word and the click both route here). */
    private static final String CONFIRM_TOKEN = "confirmdelete";
    private static final long CONFIRM_WINDOW_MS = 10_000L;

    private final DataService dataService;
    /** Player UUID → request timestamp of a pending /spawn delete confirmation. */
    private final Map<UUID, Long> deleteConfirmations = new ConcurrentHashMap<>();

    public SpawnCommand(OBX plugin) {
        super(plugin);
        this.dataService = plugin.getDataService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String baseName = command.getName().toLowerCase(Locale.ENGLISH);
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        // Hidden click-target: the "confirm" word in the delete prompt runs `/spawn confirmdelete`.
        if (!argList.isEmpty() && argList.get(0).equalsIgnoreCase(CONFIRM_TOKEN)) {
            if (sender instanceof Player) {
                confirmDelete((Player) sender);
            }
            return true;
        }
        if (baseName.equals("setspawn") && argList.isEmpty()) {
            argList.add("set");
        }
        String sub = argList.isEmpty() ? "tp" : resolveSubcommand(argList.get(0));
        if (sub == null) {
            sub = "tp";
        } else if (!argList.isEmpty()) {
            argList.remove(0);
        }

        switch (sub) {
            case "set":
                return handleSet(sender);
            case "delete":
                return handleDelete(sender, argList);
            case "info":
                return handleInfo(sender);
            case "tp":
            default:
                return handleTeleport(sender);
        }
    }

    private boolean handleTeleport(CommandSender sender) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.spawn.tp")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        SpawnInfo info = dataService.getSpawnInfo();
        if (info == null || info.getLocation() == null) {
            sendSpawnMissing(player, true);
            return true;
        }
        dataService.setBack(player.getUniqueId(), player.getLocation());
        plugin.getTeleportManager().teleportPlayer(player, info.getLocation(), "teleport.spawn.teleporting", null);
        return true;
    }

    private boolean handleSet(CommandSender sender) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.spawn.set")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        Location location = player.getLocation();
        dataService.setSpawn(location, player.getUniqueId(), player.getName(), DATE_FORMAT.format(Instant.now()));
        languages.send(player, "teleport.spawn.set");
        return true;
    }

    private boolean handleDelete(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("obx.spawn.delete")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        SpawnInfo info = dataService.getSpawnInfo();
        if (info == null || info.getLocation() == null) {
            languages.send(sender, "teleport.spawn.missing");
            return true;
        }
        if (!(sender instanceof Player)) {
            // Console can't click or chat "confirm" — delete immediately.
            dataService.deleteSpawn();
            languages.send(sender, "teleport.spawn.deleted");
            return true;
        }
        final Player player = (Player) sender;
        final UUID id = player.getUniqueId();
        final long stamp = System.currentTimeMillis();
        deleteConfirmations.put(id, stamp);
        // Prompt: click the "confirm" word, or type "confirm" in chat (captured below).
        sendClickable(player, "teleport.spawn.delete-confirm", "teleport.spawn.delete-confirm-hover",
                "&5&nconfirm", "/spawn " + CONFIRM_TOKEN);
        // 10s timeout: if still pending (unconfirmed), notify with a clickable re-issue.
        plugin.getSchedulerAdapter().runLater(new Runnable() {
            @Override
            public void run() {
                Long current = deleteConfirmations.get(id);
                if (current != null && current.longValue() == stamp) {
                    deleteConfirmations.remove(id);
                    Player online = Bukkit.getPlayer(id);
                    if (online != null && online.isOnline()) {
                        sendClickable(online, "teleport.spawn.delete-timeout", "teleport.spawn.delete-timeout-hover",
                                "&5&nretry", "/spawn del");
                    }
                }
            }
        }, 200L);
        return true;
    }

    /** Confirms a pending /spawn delete (from the chat "confirm" or the clickable word). */
    private void confirmDelete(Player player) {
        Long requested = deleteConfirmations.get(player.getUniqueId());
        if (requested == null || System.currentTimeMillis() - requested.longValue() > CONFIRM_WINDOW_MS) {
            // Expired or never requested — the timeout notice (if any) already covered it.
            return;
        }
        deleteConfirmations.remove(player.getUniqueId());
        SpawnInfo info = dataService.getSpawnInfo();
        if (info == null || info.getLocation() == null) {
            languages.send(player, "teleport.spawn.missing");
            return;
        }
        dataService.deleteSpawn();
        languages.send(player, "teleport.spawn.deleted");
    }

    /** Captures a bare "confirm" chat line while a delete confirmation is pending. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onConfirmChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if (!deleteConfirmations.containsKey(player.getUniqueId())) {
            return;
        }
        if (!event.getMessage().trim().equalsIgnoreCase("confirm")) {
            return; // any other message passes through as normal chat
        }
        event.setCancelled(true);
        // Chat is async — run the delete on the player's region / main thread.
        plugin.getSchedulerAdapter().runAtEntity(player, new Runnable() {
            @Override
            public void run() {
                confirmDelete(player);
            }
        });
    }

    /**
     * Sends a message with one word made clickable (RUN_COMMAND). The clickable word
     * replaces the {@code {click}} placeholder; console receives the plain text.
     */
    private void sendClickable(CommandSender sender, String messageKey, String hoverKey, String wordText, String command) {
        String sentinel = "[[CLICK]]";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("click", sentinel);
        String rendered = languages.get(sender, messageKey, placeholders);
        if (!(sender instanceof Player)) {
            sender.sendMessage(rendered.replace(sentinel, org.bukkit.ChatColor.stripColor(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', wordText))));
            return;
        }
        Player player = (Player) sender;
        String hover = languages.get(player, hoverKey);
        String word = org.bukkit.ChatColor.translateAlternateColorCodes('&', wordText);
        int index = rendered.indexOf(sentinel);
        if (index < 0) {
            ComponentMessenger.sendHoverMessage(player, rendered.replace(sentinel, word),
                    Collections.singletonList(hover), command, true);
            return;
        }
        String before = rendered.substring(0, index);
        String after = rendered.substring(index + sentinel.length());
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<>();
        if (!before.isEmpty()) {
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(before));
        }
        parts.add(ComponentMessenger.InteractiveMessagePart.interactive(
                word, Collections.singletonList(hover), command, true));
        if (!after.isEmpty()) {
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(after));
        }
        ComponentMessenger.sendJoinedHoverMessages(player, parts);
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("obx.spawn.info")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        SpawnInfo info = dataService.getSpawnInfo();
        if (info == null || info.getLocation() == null) {
            languages.send(sender, "teleport.spawn.info-missing");
            return true;
        }
        Location loc = info.getLocation();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("world", loc.getWorld() == null ? "world" : loc.getWorld().getName());
        placeholders.put("x", String.format(Locale.ENGLISH, "%.2f", loc.getX()));
        placeholders.put("y", String.format(Locale.ENGLISH, "%.2f", loc.getY()));
        placeholders.put("z", String.format(Locale.ENGLISH, "%.2f", loc.getZ()));
        placeholders.put("yaw", String.format(Locale.ENGLISH, "%.1f", loc.getYaw()));
        placeholders.put("pitch", String.format(Locale.ENGLISH, "%.1f", loc.getPitch()));
        placeholders.put("setBy", info.getSetByName() != null ? info.getSetByName() : (info.getSetBy() != null ? info.getSetBy().toString() : languages.get(sender, "teleport.spawn.info-unknown")));
        placeholders.put("setAt", info.getSetAt() == null ? languages.get(sender, "teleport.spawn.info-unknown") : info.getSetAt());
        languages.send(sender, "teleport.spawn.info", placeholders);
        return true;
    }

    private void sendSpawnMissing(Player player, boolean clickable) {
        if (player.hasPermission("obx.spawn.set")) {
            if (clickable) {
                ComponentMessenger.sendHoverMessage(
                        player,
                        languages.get(player, "teleport.spawn.missing-op"),
                        Collections.singletonList(languages.get(player, "teleport.spawn.missing-op-hover")),
                        "/spawn set"
                );
                return;
            }
            languages.send(player, "teleport.spawn.missing-op");
        } else {
            languages.send(player, "teleport.spawn.missing");
        }
    }

    private String resolveSubcommand(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        String normalized = input.toLowerCase(Locale.ENGLISH);
        if (SUB_ALIASES.containsKey(normalized)) {
            return SUB_ALIASES.get(normalized);
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        List<String> subcommands = Arrays.asList("tp", "set", "delete", "info");
        if (args.length == 0) {
            return suggestions;
        }
        if (args.length == 1) {
            String current = args[0].toLowerCase(Locale.ENGLISH);
            for (String sub : subcommands) {
                if (sub.startsWith(current)) {
                    suggestions.add(sub);
                }
            }
            for (String aliasKey : SUB_ALIASES.keySet()) {
                if (aliasKey.startsWith(current) && !suggestions.contains(aliasKey)) {
                    suggestions.add(aliasKey);
                }
            }
        }
        // /spawn delete takes no further arguments — confirmation is via click or chat "confirm".
        return suggestions;
    }

    private static Map<String, String> createAliasMap() {
        Map<String, String> map = new HashMap<>();
        map.put("tp", "tp");
        map.put("teleport", "tp");
        map.put("go", "tp");
        map.put("goto", "tp");
        map.put("set", "set");
        map.put("create", "set");
        map.put("new", "set");
        map.put("setup", "set");
        map.put("delete", "delete");
        map.put("remove", "delete");
        map.put("del", "delete");
        map.put("clear", "delete");
        map.put("reset", "delete");
        map.put("info", "info");
        map.put("information", "info");
        map.put("details", "info");
        map.put("about", "info");
        return map;
    }
}
