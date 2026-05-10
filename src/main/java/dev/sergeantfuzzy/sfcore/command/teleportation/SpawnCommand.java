package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.storage.DataService.SpawnInfo;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SpawnCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final Map<String, String> SUB_ALIASES = createAliasMap();
    private static final Set<String> CONFIRM_WORDS = new HashSet<>(Arrays.asList("confirm", "yes", "y"));
    private static final long CONFIRM_WINDOW_MS = 15_000L;

    private final Main plugin;
    private final DataService dataService;
    private final LanguageManager languages;
    private final Map<UUID, Long> deleteConfirmations = new HashMap<>();

    public SpawnCommand(Main plugin) {
        this.plugin = plugin;
        this.dataService = plugin.getDataService();
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String baseName = command.getName().toLowerCase(Locale.ENGLISH);
        List<String> argList = new ArrayList<>(Arrays.asList(args));
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
        if (!player.hasPermission("sfcore.spawn.tp")) {
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
        if (!player.hasPermission("sfcore.spawn.set")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        Location location = player.getLocation();
        dataService.setSpawn(location, player.getUniqueId(), player.getName(), DATE_FORMAT.format(Instant.now()));
        languages.send(player, "teleport.spawn.set");
        return true;
    }

    private boolean handleDelete(CommandSender sender, List<String> args) {
        if (!sender.hasPermission("sfcore.spawn.delete")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        SpawnInfo info = dataService.getSpawnInfo();
        if (info == null || info.getLocation() == null) {
            languages.send(sender, "teleport.spawn.missing");
            return true;
        }
        UUID key = (sender instanceof Player) ? ((Player) sender).getUniqueId() : UUID.randomUUID();
        long now = System.currentTimeMillis();
        if (!args.isEmpty() && CONFIRM_WORDS.contains(args.get(0).toLowerCase(Locale.ENGLISH))) {
            Long requested = deleteConfirmations.get(key);
            if (requested == null || now - requested > CONFIRM_WINDOW_MS) {
                languages.send(sender, "teleport.spawn.delete-confirm-needed");
                deleteConfirmations.put(key, now);
                return true;
            }
            dataService.deleteSpawn();
            deleteConfirmations.remove(key);
            languages.send(sender, "teleport.spawn.deleted");
            return true;
        }
        deleteConfirmations.put(key, now);
        languages.send(sender, "teleport.spawn.delete-confirm");
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("sfcore.spawn.info")) {
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
        if (player.hasPermission("sfcore.spawn.set")) {
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
        } else if (args.length == 2) {
            String resolved = resolveSubcommand(args[0]);
            if ("delete".equals(resolved)) {
                for (String word : CONFIRM_WORDS) {
                    if (word.startsWith(args[1].toLowerCase(Locale.ENGLISH))) {
                        suggestions.add(word);
                    }
                }
            }
        }
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
