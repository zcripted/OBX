package dev.zcripted.obx.command.core;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.language.LanguageManager;
import dev.zcripted.obx.util.message.ConsoleLog;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Operational {@code /obx} subcommands: reload, diagnostics, version, config,
 * and debug, plus their formatting helpers.
 */
class ObxDiagnosticsView {

    private static final DateTimeFormatter DUMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd---HH:mm:ss");

    /** Max config-file rows per page; keeps the whole /obx config message at/under 15 lines. */
    private static final int CONFIG_PER_PAGE = 10;

    private final OBX plugin;
    private final LanguageManager languages;

    ObxDiagnosticsView(OBX plugin, LanguageManager languages) {
        this.plugin = plugin;
        this.languages = languages;
    }

    void handleReload(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("config")) {
            if (!ensurePermission(sender, "obx.admin.reload.config")) {
                return;
            }
            long start = System.nanoTime();
            plugin.reloadConfig();
            languages.reload();
            plugin.getDataService().reload();
            if (plugin.getModerationService() != null) {
                plugin.getModerationService().reload();
            }
            if (plugin.getMotdService() != null) {
                plugin.getMotdService().reload();
            }
            // Welcome message TEXT now lives in the language files, so re-pull the
            // join/leave + MOTD snapshot after languages reload (it reads them raw).
            if (plugin.getJoinLeaveService() != null) {
                plugin.getJoinLeaveService().reload();
            }
            long duration = System.nanoTime() - start;
            Map<String, String> placeholders = Collections.singletonMap("duration", formatMillis(duration));
            sendHoverMessage(sender, languages.get(sender, "commands.obx.reload.config.base", placeholders), languages.list(sender, "commands.obx.reload.config.hover", placeholders), "/obx diagnostics");
            return;
        }
        if (args.length >= 2) {
            if (!ensurePermission(sender, "obx.admin.reload.features")) {
                return;
            }
            String target = args[1];
            File file = new File(plugin.getDataFolder(), target);
            if (!file.exists() || !file.isFile()) {
                languages.send(sender, "commands.obx.reload.file.missing", Collections.singletonMap("file", target));
                return;
            }
            try {
                YamlConfiguration.loadConfiguration(file);
                Map<String, String> placeholders = new LinkedHashMap<>();
                placeholders.put("file", target);
                placeholders.put("path", file.getAbsolutePath());
                placeholders.put("size", String.valueOf(file.length()));
                sendHoverMessage(sender, languages.get(sender, "commands.obx.reload.file.success", placeholders), languages.list(sender, "commands.obx.reload.file.hover", placeholders), null);
            } catch (Exception exception) {
                Map<String, String> placeholders = new LinkedHashMap<>();
                placeholders.put("file", target);
                placeholders.put("error", exception.getMessage());
                languages.send(sender, "commands.obx.reload.file.failure", placeholders);
            }
            return;
        }
        if (!ensurePermission(sender, "obx.admin.reload")) {
            return;
        }
        long start = System.nanoTime();
        Map<String, Long> times = plugin.reloadPlugin();
        long duration = System.nanoTime() - start;
        String totalText = formatMillis(duration);
        String who = (sender instanceof Player) ? sender.getName() : "Console";

        // Player executor: styled in-game message + per-component hover. (Console's
        // feedback is the console summary below.)
        if (sender instanceof Player) {
            sendHoverMessage(sender,
                    languages.get(sender, "commands.obx.reload.full.base", Collections.singletonMap("duration", totalText)),
                    buildReloadHover(sender, times, totalText), "/obx diagnostics");
        }

        // Always log a clean console summary with who executed it.
        logReloadToConsole(who, totalText, times);

        // A console-initiated reload notifies online reload-permission players in-game.
        if (!(sender instanceof Player)) {
            Map<String, String> notify = new LinkedHashMap<>();
            notify.put("who", who);
            notify.put("duration", totalText);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("obx.admin.reload")) {
                    languages.send(player, "commands.obx.reload.notify", notify);
                }
            }
        }
    }

    /** Builds the styled per-component reload hover (alphabetical, small-caps load times). */
    private List<String> buildReloadHover(CommandSender sender, Map<String, Long> times, String totalText) {
        List<String> hover = new ArrayList<>();
        hover.add(languages.get(sender, "commands.obx.reload.full.header"));
        hover.add(boxDivider(sender));
        List<Map.Entry<String, Long>> entries = sortedByName(times);
        for (Map.Entry<String, Long> entry : entries) {
            Map<String, String> ph = new LinkedHashMap<>();
            ph.put("file", entry.getKey());
            ph.put("time", smallCapsMillis(entry.getValue()));
            hover.add(languages.get(sender, "commands.obx.reload.full.entry", ph));
        }
        hover.add(boxDivider(sender));
        hover.add(languages.get(sender, "commands.obx.reload.full.total-line", Collections.singletonMap("duration", totalText)));
        hover.add(languages.get(sender, "commands.obx.reload.full.tip-line"));
        return hover;
    }

    /** Clean console summary of a reload — green success line + a comma list of components. */
    private void logReloadToConsole(String who, String totalText, Map<String, Long> times) {
        ConsoleLog.success(plugin, "Reload", "Reloaded by " + who + " in " + totalText + " (" + times.size() + " components)");
        List<String> items = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sortedByName(times)) {
            items.add(entry.getKey() + " (" + Math.round(entry.getValue() / 1_000_000.0) + "ms)");
        }
        ConsoleLog.list(plugin, "Reloaded:", items);
    }

    private List<Map.Entry<String, Long>> sortedByName(Map<String, Long> times) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(times.entrySet());
        Collections.sort(entries, new java.util.Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                return a.getKey().compareToIgnoreCase(b.getKey());
            }
        });
        return entries;
    }

    /** A per-component load time as a small-caps "ᴍꜱ" string (e.g. {@code 12.4ᴍꜱ}). */
    private String smallCapsMillis(long nanos) {
        double ms = nanos / 1_000_000.0;
        String num;
        if (ms >= 100) {
            num = String.valueOf(Math.round(ms));
        } else {
            num = String.format(Locale.US, "%.1f", ms);
            if (num.endsWith(".0")) {
                num = num.substring(0, num.length() - 2);
            }
        }
        return num + "ᴍꜱ"; // ᴍꜱ (small-caps M + S)
    }

    void handleDiagnostics(CommandSender sender, String[] args) {
        boolean full = args.length >= 2 && args[1].equalsIgnoreCase("full");
        if (full) {
            if (!ensurePermission(sender, "obx.admin.diagnostics.full")) {
                return;
            }
        } else if (!ensurePermission(sender, "obx.admin.diagnostics")) {
            return;
        }
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("version", plugin.getDescription().getVersion());
        placeholders.put("server", Bukkit.getVersion());
        placeholders.put("bukkit", Bukkit.getBukkitVersion());
        placeholders.put("debug", String.valueOf(plugin.getConfig().getBoolean("debug")));
        placeholders.put("data", plugin.getDataFolder().getAbsolutePath());
        for (String line : languages.list(sender, "commands.obx.diagnostics.header", placeholders)) {
            sender.sendMessage(line);
        }
        languages.send(sender, "commands.obx.diagnostics.version", placeholders);
        languages.send(sender, "commands.obx.diagnostics.server", placeholders);
        languages.send(sender, "commands.obx.diagnostics.bukkit", placeholders);
        languages.send(sender, "commands.obx.diagnostics.debug", placeholders);
        languages.send(sender, "commands.obx.diagnostics.data", placeholders);
        if (full) {
            Map<String, String> extra = new LinkedHashMap<>(placeholders);
            extra.put("services", "DataService, TeleportManager, LanguageManager");
            extra.put("players", String.valueOf(Bukkit.getOnlinePlayers().size()));
            extra.put("keys", String.valueOf(plugin.getConfig().getKeys(true).size()));
            languages.send(sender, "commands.obx.diagnostics.services", extra);
            languages.send(sender, "commands.obx.diagnostics.players", extra);
            languages.send(sender, "commands.obx.diagnostics.keys", extra);
        }
        sender.sendMessage("");
    }

    private String formatMillis(long nanos) {
        long millis = Math.round(nanos / 1_000_000.0);
        return millis + "ms";
    }

    /** Slim dark-gray box divider used by the styled "plugin info" reports (matches /pl, /obx info, /obx about). */
    private String boxDivider(CommandSender sender) {
        return languages.get(sender, "core.divider-line");
    }

    private void sendHoverMessage(CommandSender sender, String message, List<String> hover, String clickSuggestion) {
        ComponentMessenger.sendHoverMessage(sender, message, hover, clickSuggestion);
    }

    void handleVersion(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String tag = "RELEASE";
        if (version != null && version.contains("-")) {
            String suffix = version.substring(version.lastIndexOf('-') + 1);
            tag = suffix.toUpperCase(Locale.ENGLISH);
        }
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("version", version);
        placeholders.put("tag", tag);
        for (String line : languages.list(sender, "commands.obx.version", placeholders)) {
            sender.sendMessage(line);
        }
    }

    void handleConfig(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("validate")) {
            if (!ensurePermission(sender, "obx.debug.config.validate")) {
                return;
            }
            String missing = languages.get(sender, "commands.obx.config.data-missing");
            String ok = ChatColor.GREEN + "ok";
            Map<String, String> states = new LinkedHashMap<>();
            states.put("config_state", ok);
            states.put("data_state", new File(plugin.getDataFolder(), "data.yml").exists() ? ok : missing);
            states.put("motd_state", new File(plugin.getDataFolder(), "motd.yml").exists() ? ok : missing);
            states.put("moderation_state", new File(plugin.getDataFolder(), "moderation.yml").exists() ? ok : missing);
            states.put("tablist_state", new File(plugin.getDataFolder(), "systems/tablist.yml").exists() ? ok : missing);
            states.put("scoreboard_state", new File(plugin.getDataFolder(), "systems/scoreboard.yml").exists() ? ok : missing);
            languages.send(sender, "commands.obx.config.validation", states);
            return;
        }
        if (!ensurePermission(sender, "obx.debug.config")) {
            return;
        }
        // Dynamically list every plugin .yml (no clickable links), paginated so the
        // message never exceeds 15 lines.
        List<String> files = listConfigFiles();
        if (files.isEmpty()) {
            languages.send(sender, "commands.obx.config.list-empty");
            return;
        }
        int pages = (files.size() + CONFIG_PER_PAGE - 1) / CONFIG_PER_PAGE;
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        page = Math.max(1, Math.min(page, pages));

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("count", Integer.toString(files.size()));
        meta.put("page", Integer.toString(page));
        meta.put("pages", Integer.toString(pages));
        languages.send(sender, "commands.obx.config.list-header", meta);
        int start = (page - 1) * CONFIG_PER_PAGE;
        int end = Math.min(files.size(), start + CONFIG_PER_PAGE);
        for (int i = start; i < end; i++) {
            languages.send(sender, "commands.obx.config.list-entry", Collections.singletonMap("file", files.get(i)));
        }
        languages.send(sender, "commands.obx.config.list-footer", meta);
    }

    /** Every {@code .yml} under the plugin data folder (recursively), as sorted relative paths. */
    private List<String> listConfigFiles() {
        File folder = plugin.getDataFolder();
        List<String> names = new ArrayList<>();
        collectYml(folder, folder, names);
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private void collectYml(File root, File dir, List<String> out) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                collectYml(root, entry, out);
            } else if (entry.getName().toLowerCase(Locale.ENGLISH).endsWith(".yml")) {
                out.add(root.toURI().relativize(entry.toURI()).getPath());
            }
        }
    }

    void handleDebug(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String action = args[1].toLowerCase(Locale.ENGLISH);
            if ((action.equals("enable") || action.equals("disable"))) {
                if (!ensurePermission(sender, "obx.debug.toggle")) {
                    return;
                }
                boolean enabled = action.equals("enable");
                plugin.getConfig().set("debug", enabled);
                plugin.saveConfig();
                String state = languages.get(sender, enabled ? "commands.obx.debug.state.enabled" : "commands.obx.debug.state.disabled");
                languages.send(sender, "commands.obx.debug.toggled", Collections.singletonMap("state", state));
                return;
            }
            if (action.equals("dump")) {
                if (!ensurePermission(sender, "obx.debug.dump")) {
                    return;
                }
                writeDebugDump(sender);
                return;
            }
        }
        if (!ensurePermission(sender, "obx.debug")) {
            return;
        }
        String debugState = languages.get(sender, plugin.getConfig().getBoolean("debug")
                ? "commands.obx.debug.state.enabled"
                : "commands.obx.debug.state.disabled");
        languages.send(sender, "commands.obx.debug.report", Collections.singletonMap("state", debugState));
    }

    private void writeDebugDump(CommandSender sender) {
        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        String name = (sender instanceof Player) ? ((Player) sender).getName() : "CONSOLE";
        String timestamp = DUMP_FORMAT.format(ZonedDateTime.now(ZoneId.of("America/New_York")));
        File file = new File(logsDir, timestamp + "---" + name + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("plugin.version", plugin.getDescription().getVersion());
        yaml.set("plugin.description", plugin.getDescription().getDescription());
        yaml.set("server.bukkit", Bukkit.getBukkitVersion());
        yaml.set("server.version", Bukkit.getVersion());
        yaml.set("config.debug", plugin.getConfig().getBoolean("debug"));
        yaml.set("players.online", Bukkit.getOnlinePlayers().size());
        try {
            yaml.save(file);
            languages.send(sender, "commands.obx.debug.dump.saved", Collections.singletonMap("file", file.getName()));
            dev.zcripted.obx.util.message.ConsoleLog.info(plugin, "Debug dump created by " + name + " at "
                    + file.getAbsolutePath() + " (" + file.length() + " bytes, server=" + Bukkit.getVersion() + ")");
        } catch (IOException exception) {
            languages.send(sender, "commands.obx.debug.dump.failed", Collections.singletonMap("error", exception.getMessage()));
        }
    }

    private boolean ensurePermission(CommandSender sender, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        if (sender.hasPermission(permission)) {
            return true;
        }
        languages.send(sender, "core.no-permission");
        return false;
    }
}
