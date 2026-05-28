package dev.sergeantfuzzy.sfcore.command.core;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.gui.admin.AdminMenu;
import dev.sergeantfuzzy.sfcore.gui.player.MainMenu;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.message.ConsoleLog;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SFCoreCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter DUMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd---HH:mm:ss");

    private final Main plugin;
    private final LanguageManager languages;
    private final Set<String> updateNotificationToggles = new HashSet<>();

    private enum Category {
        INFORMATION("commands.sf.category.information", "information"),
        RELOAD_DIAGNOSTICS("commands.sf.category.reload", "reload diagnostics"),
        UPDATES_VERSION("commands.sf.category.updates", "updates version"),
        CONFIG_DEBUG("commands.sf.category.config", "config debug");

        private final String labelKey;
        private final String searchKey;

        Category(String labelKey, String searchKey) {
            this.labelKey = labelKey;
            this.searchKey = searchKey;
        }

        public String label(LanguageManager languages, CommandSender sender) {
            return languages.get(sender, labelKey);
        }

        public String commandToken() {
            int lastDot = labelKey.lastIndexOf('.');
            return lastDot >= 0 ? labelKey.substring(lastDot + 1) : searchKey;
        }

        public static Category fromInput(LanguageManager languages, CommandSender sender, String input) {
            if (input == null) {
                return null;
            }
            String normalized = input.trim().toLowerCase(Locale.ENGLISH);
            for (Category category : values()) {
                String label = languages.get(sender, category.labelKey).toLowerCase(Locale.ENGLISH);
                if (label.startsWith(normalized)) {
                    return category;
                }
                if (category.searchKey.toLowerCase(Locale.ENGLISH).startsWith(normalized)) {
                    return category;
                }
            }
            return null;
        }
    }

    private static final List<CommandEntry> COMMANDS = Arrays.asList(
            entry("help", "commands.sf.entry.help.usage", "commands.sf.entry.help.description", "sfcore.help", Category.INFORMATION, PermissionDefault.TRUE, "/sf help", "h"),
            entry("info", "commands.sf.entry.info.usage", "commands.sf.entry.info.description", "sfcore.info", Category.INFORMATION, PermissionDefault.TRUE, "/sf info"),
            entry("about", "commands.sf.entry.about.usage", "commands.sf.entry.about.description", "sfcore.about", Category.INFORMATION, PermissionDefault.TRUE, "/sf about"),
            entry("permissions", "commands.sf.entry.permissions.usage", "commands.sf.entry.permissions.description", "sfcore.permissions.view", Category.INFORMATION, PermissionDefault.FALSE, "/sf permissions"),
            entry("commands", "commands.sf.entry.commands.usage", "commands.sf.entry.commands.description", "sfcore.commands.list", Category.INFORMATION, PermissionDefault.TRUE, "/sf commands"),

            entry("reload", "commands.sf.entry.reload.usage", "commands.sf.entry.reload.description", "sfcore.admin.reload", Category.RELOAD_DIAGNOSTICS, PermissionDefault.OP, "/sf reload"),
            entry("reload config", "commands.sf.entry.reload-config.usage", "commands.sf.entry.reload-config.description", "sfcore.admin.reload.config", Category.RELOAD_DIAGNOSTICS, PermissionDefault.OP, "/sf reload config"),
            entry("reload file", "commands.sf.entry.reload-file.usage", "commands.sf.entry.reload-file.description", "sfcore.admin.reload.features", Category.RELOAD_DIAGNOSTICS, PermissionDefault.OP, "/sf reload config.yml", "reload resource"),
            entry("diagnostics", "commands.sf.entry.diagnostics.usage", "commands.sf.entry.diagnostics.description", "sfcore.admin.diagnostics", Category.RELOAD_DIAGNOSTICS, PermissionDefault.OP, "/sf diagnostics"),
            entry("diagnostics full", "commands.sf.entry.diagnostics-full.usage", "commands.sf.entry.diagnostics-full.description", "sfcore.admin.diagnostics.full", Category.RELOAD_DIAGNOSTICS, PermissionDefault.OP, "/sf diagnostics full"),

            entry("version", "commands.sf.entry.version.usage", "commands.sf.entry.version.description", "sfcore.version", Category.UPDATES_VERSION, PermissionDefault.OP, "/sf version"),
            entry("updates", "commands.sf.entry.updates.usage", "commands.sf.entry.updates.description", "sfcore.updates.check", Category.UPDATES_VERSION, PermissionDefault.OP, "/sf updates"),
            entry("updates check", "commands.sf.entry.updates-check.usage", "commands.sf.entry.updates-check.description", "sfcore.updates.check", Category.UPDATES_VERSION, PermissionDefault.OP, "/sf updates check"),
            entry("updates notify", "commands.sf.entry.updates-notify.usage", "commands.sf.entry.updates-notify.description", "sfcore.updates.notify", Category.UPDATES_VERSION, PermissionDefault.OP, "/sf updates notify"),

            entry("config", "commands.sf.entry.config.usage", "commands.sf.entry.config.description", "sfcore.debug.config", Category.CONFIG_DEBUG, PermissionDefault.OP, "/sf config"),
            entry("config validate", "commands.sf.entry.config-validate.usage", "commands.sf.entry.config-validate.description", "sfcore.debug.config.validate", Category.CONFIG_DEBUG, PermissionDefault.OP, "/sf config validate"),
            entry("debug", "commands.sf.entry.debug.usage", "commands.sf.entry.debug.description", "sfcore.debug", Category.CONFIG_DEBUG, PermissionDefault.OP, "/sf debug"),
            entry("debug enable", "commands.sf.entry.debug-enable.usage", "commands.sf.entry.debug-enable.description", "sfcore.debug.toggle", Category.CONFIG_DEBUG, PermissionDefault.OP, "/sf debug enable"),
            entry("debug disable", "commands.sf.entry.debug-disable.usage", "commands.sf.entry.debug-disable.description", "sfcore.debug.toggle", Category.CONFIG_DEBUG, PermissionDefault.OP, "/sf debug disable"),
            entry("debug dump", "commands.sf.entry.debug-dump.usage", "commands.sf.entry.debug-dump.description", "sfcore.debug.dump", Category.CONFIG_DEBUG, PermissionDefault.OP, "/sf debug dump")
    );

    public SFCoreCommand(Main plugin, LanguageManager languages) {
        this.plugin = plugin;
        this.languages = languages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("sfcore.admin.menu")) {
                    AdminMenu.open(plugin, player);
                } else {
                    MainMenu.open(plugin, player);
                }
                return true;
            }
            sendHelp(sender, 1, null);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ENGLISH);
        switch (sub) {
            case "help":
                handleHelp(sender, args);
                return true;
            case "info":
                if (ensurePermission(sender, "sfcore.info")) {
                    handleInfo(sender);
                }
                return true;
            case "about":
                if (ensurePermission(sender, "sfcore.about")) {
                    handleAbout(sender);
                }
                return true;
            case "permissions":
                if (ensurePermission(sender, "sfcore.permissions.view")) {
                    handlePermissions(sender, args);
                }
                return true;
            case "commands":
                if (ensurePermission(sender, "sfcore.commands.list")) {
                    handleCommandsList(sender, args);
                }
                return true;
            case "reload":
                handleReload(sender, args);
                return true;
            case "diagnostics":
                handleDiagnostics(sender, args);
                return true;
            case "version":
                if (ensurePermission(sender, "sfcore.version")) {
                    handleVersion(sender);
                }
                return true;
            case "updates":
                handleUpdates(sender, args);
                return true;
            case "config":
                handleConfig(sender, args);
                return true;
            case "debug":
                handleDebug(sender, args);
                return true;
            case "joinleave":
                handleJoinLeave(sender, args);
                return true;
            case "joinmotd":
                handleJoinMotd(sender, args);
                return true;
            default:
                languages.send(sender, "commands.sf.unknown");
                sendHelp(sender, 1, null);
                return true;
        }
    }

    private void handleHelp(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Category category = Category.fromInput(languages, sender, args[1]);
            if (category != null) {
                int page = 1;
                if (args.length >= 3) {
                    Integer parsed = parsePage(args[2]);
                    if (parsed != null) {
                        page = parsed;
                    }
                }
                sendHelp(sender, page, category);
                return;
            }
            CommandEntry entry = findEntry(args[1]);
            if (entry != null) {
                sendHelpDetail(sender, entry);
                return;
            }
            Integer page = parsePage(args[1]);
            if (page != null) {
                Category pageCategory = null;
                if (args.length >= 3) {
                    pageCategory = Category.fromInput(languages, sender, args[2]);
                }
                sendHelp(sender, page, pageCategory);
                return;
            }
        }
        sendHelp(sender, 1, null);
    }

    private void handleInfo(CommandSender sender) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("version", plugin.getDescription().getVersion());
        placeholders.put("description", orFallback(plugin.getDescription().getDescription()));
        placeholders.put("authors", String.join(", ", plugin.getDescription().getAuthors()));
        placeholders.put("website", orFallback(plugin.getDescription().getWebsite()));
        for (String line : languages.list(sender, "commands.sf.info.lines", placeholders)) {
            sender.sendMessage(line);
        }
    }

    private void handleAbout(CommandSender sender) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("authors", String.join(", ", plugin.getDescription().getAuthors()));
        placeholders.put("website", orFallback(plugin.getDescription().getWebsite()));
        for (String line : languages.list(sender, "commands.sf.about.lines", placeholders)) {
            sender.sendMessage(line);
        }
    }

    private void handlePermissions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            languages.send(sender, "commands.sf.permissions.usage");
            languages.send(sender, "commands.sf.permissions.suggestion");
            return;
        }
        Category category = Category.fromInput(languages, sender, args[1]);
        List<CommandEntry> entries;
        if (category != null) {
            entries = COMMANDS.stream().filter(entry -> entry.category() == category).collect(Collectors.toList());
        } else {
            CommandEntry entry = findEntry(args[1]);
            entries = entry == null ? Collections.<CommandEntry>emptyList() : Collections.singletonList(entry);
        }
        if (entries.isEmpty()) {
            languages.send(sender, "commands.sf.permissions.no-match");
            return;
        }
        sender.sendMessage(" ");
        sender.sendMessage(languages.get(sender, "commands.sf.permissions.title"));
        sender.sendMessage(boxDivider(sender));
        sender.sendMessage(" ");
        for (CommandEntry entry : entries) {
            Map<String, String> rowPlaceholders = new LinkedHashMap<>();
            rowPlaceholders.put("usage", entry.usage(languages, sender));
            rowPlaceholders.put("permission", entry.permission());
            sender.sendMessage(languages.get(sender, "commands.sf.permissions.entry", rowPlaceholders));
        }
        sender.sendMessage(" ");
        sender.sendMessage(boxDivider(sender));
    }

    private void handleCommandsList(CommandSender sender, String[] args) {
        Category category = args.length >= 2 ? Category.fromInput(languages, sender, args[1]) : null;
        List<CommandEntry> visible = COMMANDS.stream()
                .filter(entry -> category == null || entry.category() == category)
                .filter(entry -> isVisible(sender, entry))
                .collect(Collectors.toList());
        if (visible.isEmpty()) {
            languages.send(sender, "commands.sf.commands.none");
            return;
        }
        sender.sendMessage(" ");
        sender.sendMessage(languages.get(sender, "commands.sf.commands.title"));
        sender.sendMessage(boxDivider(sender));
        sender.sendMessage(" ");
        for (CommandEntry entry : visible) {
            Map<String, String> rowPlaceholders = new LinkedHashMap<>();
            rowPlaceholders.put("usage", entry.usage(languages, sender));
            rowPlaceholders.put("description", entry.description(languages, sender));
            sender.sendMessage(languages.get(sender, "commands.sf.commands.entry", rowPlaceholders));
        }
        sender.sendMessage(" ");
        sender.sendMessage(boxDivider(sender));
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("config")) {
            if (!ensurePermission(sender, "sfcore.admin.reload.config")) {
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
            long duration = System.nanoTime() - start;
            Map<String, String> placeholders = Collections.singletonMap("duration", formatMillis(duration));
            sendHoverMessage(sender, languages.get(sender, "commands.sf.reload.config.base", placeholders), languages.list(sender, "commands.sf.reload.config.hover", placeholders), "/sf diagnostics");
            return;
        }
        if (args.length >= 2) {
            if (!ensurePermission(sender, "sfcore.admin.reload.features")) {
                return;
            }
            String target = args[1];
            File file = new File(plugin.getDataFolder(), target);
            if (!file.exists() || !file.isFile()) {
                languages.send(sender, "commands.sf.reload.file.missing", Collections.singletonMap("file", target));
                return;
            }
            try {
                YamlConfiguration.loadConfiguration(file);
                Map<String, String> placeholders = new LinkedHashMap<>();
                placeholders.put("file", target);
                placeholders.put("path", file.getAbsolutePath());
                placeholders.put("size", String.valueOf(file.length()));
                sendHoverMessage(sender, languages.get(sender, "commands.sf.reload.file.success", placeholders), languages.list(sender, "commands.sf.reload.file.hover", placeholders), null);
            } catch (Exception exception) {
                Map<String, String> placeholders = new LinkedHashMap<>();
                placeholders.put("file", target);
                placeholders.put("error", exception.getMessage());
                languages.send(sender, "commands.sf.reload.file.failure", placeholders);
            }
            return;
        }
        if (!ensurePermission(sender, "sfcore.admin.reload")) {
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
                    languages.get(sender, "commands.sf.reload.full.base", Collections.singletonMap("duration", totalText)),
                    buildReloadHover(sender, times, totalText), "/sf diagnostics");
        }

        // Always log a clean console summary with who executed it.
        logReloadToConsole(who, totalText, times);

        // A console-initiated reload notifies online reload-permission players in-game.
        if (!(sender instanceof Player)) {
            Map<String, String> notify = new LinkedHashMap<>();
            notify.put("who", who);
            notify.put("duration", totalText);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("sfcore.admin.reload")) {
                    languages.send(player, "commands.sf.reload.notify", notify);
                }
            }
        }
    }

    /** Builds the styled per-component reload hover (alphabetical, small-caps load times). */
    private List<String> buildReloadHover(CommandSender sender, Map<String, Long> times, String totalText) {
        List<String> hover = new ArrayList<>();
        hover.add(languages.get(sender, "commands.sf.reload.full.header"));
        hover.add(boxDivider(sender));
        List<Map.Entry<String, Long>> entries = sortedByName(times);
        for (Map.Entry<String, Long> entry : entries) {
            Map<String, String> ph = new LinkedHashMap<>();
            ph.put("file", entry.getKey());
            ph.put("time", smallCapsMillis(entry.getValue()));
            hover.add(languages.get(sender, "commands.sf.reload.full.entry", ph));
        }
        hover.add(boxDivider(sender));
        hover.add(languages.get(sender, "commands.sf.reload.full.total-line", Collections.singletonMap("duration", totalText)));
        hover.add(languages.get(sender, "commands.sf.reload.full.tip-line"));
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

    private void handleDiagnostics(CommandSender sender, String[] args) {
        boolean full = args.length >= 2 && args[1].equalsIgnoreCase("full");
        if (full) {
            if (!ensurePermission(sender, "sfcore.admin.diagnostics.full")) {
                return;
            }
        } else if (!ensurePermission(sender, "sfcore.admin.diagnostics")) {
            return;
        }
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("version", plugin.getDescription().getVersion());
        placeholders.put("server", Bukkit.getVersion());
        placeholders.put("bukkit", Bukkit.getBukkitVersion());
        placeholders.put("debug", String.valueOf(plugin.getConfig().getBoolean("debug")));
        placeholders.put("data", plugin.getDataFolder().getAbsolutePath());
        for (String line : languages.list(sender, "commands.sf.diagnostics.header", placeholders)) {
            sender.sendMessage(line);
        }
        languages.send(sender, "commands.sf.diagnostics.version", placeholders);
        languages.send(sender, "commands.sf.diagnostics.server", placeholders);
        languages.send(sender, "commands.sf.diagnostics.bukkit", placeholders);
        languages.send(sender, "commands.sf.diagnostics.debug", placeholders);
        languages.send(sender, "commands.sf.diagnostics.data", placeholders);
        if (full) {
            Map<String, String> extra = new LinkedHashMap<>(placeholders);
            extra.put("services", "DataService, TeleportManager, LanguageManager");
            extra.put("players", String.valueOf(Bukkit.getOnlinePlayers().size()));
            extra.put("keys", String.valueOf(plugin.getConfig().getKeys(true).size()));
            languages.send(sender, "commands.sf.diagnostics.services", extra);
            languages.send(sender, "commands.sf.diagnostics.players", extra);
            languages.send(sender, "commands.sf.diagnostics.keys", extra);
        }
        sender.sendMessage("");
    }

    private String formatMillis(long nanos) {
        long millis = Math.round(nanos / 1_000_000.0);
        return millis + "ms";
    }

    /** Slim dark-gray box divider used by the styled "plugin info" reports (matches /pl, /sf info, /sf about). */
    private String boxDivider(CommandSender sender) {
        return languages.get(sender, "core.divider-line");
    }

    private String orFallback(String value) {
        return (value == null || value.trim().isEmpty()) ? "N/A" : value;
    }

    private void sendHoverMessage(CommandSender sender, String message, List<String> hover, String clickSuggestion) {
        ComponentMessenger.sendHoverMessage(sender, message, hover, clickSuggestion);
    }

    private void handleVersion(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String tag = "RELEASE";
        if (version != null && version.contains("-")) {
            String suffix = version.substring(version.lastIndexOf('-') + 1);
            tag = suffix.toUpperCase(Locale.ENGLISH);
        }
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("version", version);
        placeholders.put("tag", tag);
        for (String line : languages.list(sender, "commands.sf.version", placeholders)) {
            sender.sendMessage(line);
        }
    }

    private void handleUpdates(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("notify")) {
            if (!ensurePermission(sender, "sfcore.updates.notify")) {
                return;
            }
            toggleNotifications(sender);
            return;
        }
        if (!ensurePermission(sender, "sfcore.updates.check")) {
            return;
        }
        languages.send(sender, "commands.sf.updates.header");
        languages.send(sender, "commands.sf.updates.listing");
        if (args.length >= 2 && args[1].equalsIgnoreCase("check")) {
            languages.send(sender, "commands.sf.updates.forced");
        } else {
            languages.send(sender, "commands.sf.updates.hint");
        }
    }

    /** Max config-file rows per page; keeps the whole /sf config message at/under 15 lines. */
    private static final int CONFIG_PER_PAGE = 10;

    private void handleConfig(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("validate")) {
            if (!ensurePermission(sender, "sfcore.debug.config.validate")) {
                return;
            }
            String missing = languages.get(sender, "commands.sf.config.data-missing");
            String ok = ChatColor.GREEN + "ok";
            Map<String, String> states = new LinkedHashMap<>();
            states.put("config_state", ok);
            states.put("data_state", new File(plugin.getDataFolder(), "data.yml").exists() ? ok : missing);
            states.put("motd_state", new File(plugin.getDataFolder(), "motd.yml").exists() ? ok : missing);
            states.put("moderation_state", new File(plugin.getDataFolder(), "moderation.yml").exists() ? ok : missing);
            states.put("tablist_state", new File(plugin.getDataFolder(), "systems/tablist.yml").exists() ? ok : missing);
            states.put("scoreboard_state", new File(plugin.getDataFolder(), "systems/scoreboard.yml").exists() ? ok : missing);
            languages.send(sender, "commands.sf.config.validation", states);
            return;
        }
        if (!ensurePermission(sender, "sfcore.debug.config")) {
            return;
        }
        // Dynamically list every plugin .yml (no clickable links), paginated so the
        // message never exceeds 15 lines.
        List<String> files = listConfigFiles();
        if (files.isEmpty()) {
            languages.send(sender, "commands.sf.config.list-empty");
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
        languages.send(sender, "commands.sf.config.list-header", meta);
        int start = (page - 1) * CONFIG_PER_PAGE;
        int end = Math.min(files.size(), start + CONFIG_PER_PAGE);
        for (int i = start; i < end; i++) {
            languages.send(sender, "commands.sf.config.list-entry", Collections.singletonMap("file", files.get(i)));
        }
        languages.send(sender, "commands.sf.config.list-footer", meta);
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

    private void handleDebug(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String action = args[1].toLowerCase(Locale.ENGLISH);
            if ((action.equals("enable") || action.equals("disable"))) {
                if (!ensurePermission(sender, "sfcore.debug.toggle")) {
                    return;
                }
                boolean enabled = action.equals("enable");
                plugin.getConfig().set("debug", enabled);
                plugin.saveConfig();
                String state = languages.get(sender, enabled ? "commands.sf.debug.state.enabled" : "commands.sf.debug.state.disabled");
                languages.send(sender, "commands.sf.debug.toggled", Collections.singletonMap("state", state));
                return;
            }
            if (action.equals("dump")) {
                if (!ensurePermission(sender, "sfcore.debug.dump")) {
                    return;
                }
                writeDebugDump(sender);
                return;
            }
        }
        if (!ensurePermission(sender, "sfcore.debug")) {
            return;
        }
        String debugState = languages.get(sender, plugin.getConfig().getBoolean("debug")
                ? "commands.sf.debug.state.enabled"
                : "commands.sf.debug.state.disabled");
        languages.send(sender, "commands.sf.debug.report", Collections.singletonMap("state", debugState));
    }

    private void handleJoinLeave(CommandSender sender, String[] args) {
        if (!ensurePermission(sender, "sfcore.admin.modules.joinleave")) {
            return;
        }
        if (args.length < 2) {
            sendModuleStatus(sender, "admin.modules.joinleave.status", plugin.getJoinLeaveService().isJoinLeaveEnabled());
            languages.send(sender, "admin.modules.joinleave.usage");
            return;
        }
        String action = args[1].toLowerCase(Locale.ENGLISH);
        if (action.equals("on") || action.equals("enable") || action.equals("true")) {
            plugin.getJoinLeaveService().setJoinLeaveEnabled(true);
            languages.send(sender, "admin.modules.joinleave.enabled");
            return;
        }
        if (action.equals("off") || action.equals("disable") || action.equals("false")) {
            plugin.getJoinLeaveService().setJoinLeaveEnabled(false);
            languages.send(sender, "admin.modules.joinleave.disabled");
            return;
        }
        if (action.equals("status") || action.equals("info")) {
            sendModuleStatus(sender, "admin.modules.joinleave.status", plugin.getJoinLeaveService().isJoinLeaveEnabled());
            return;
        }
        languages.send(sender, "admin.modules.joinleave.usage");
    }

    private void handleJoinMotd(CommandSender sender, String[] args) {
        if (!ensurePermission(sender, "sfcore.admin.modules.joinmotd")) {
            return;
        }
        if (args.length < 2) {
            sendModuleStatus(sender, "admin.modules.joinmotd.status", plugin.getJoinLeaveService().isJoinMotdEnabled());
            languages.send(sender, "admin.modules.joinmotd.usage");
            return;
        }
        String action = args[1].toLowerCase(Locale.ENGLISH);
        if (action.equals("on") || action.equals("enable") || action.equals("true")) {
            plugin.getJoinLeaveService().setJoinMotdEnabled(true);
            languages.send(sender, "admin.modules.joinmotd.enabled");
            return;
        }
        if (action.equals("off") || action.equals("disable") || action.equals("false")) {
            plugin.getJoinLeaveService().setJoinMotdEnabled(false);
            languages.send(sender, "admin.modules.joinmotd.disabled");
            return;
        }
        if (action.equals("status") || action.equals("info")) {
            sendModuleStatus(sender, "admin.modules.joinmotd.status", plugin.getJoinLeaveService().isJoinMotdEnabled());
            return;
        }
        languages.send(sender, "admin.modules.joinmotd.usage");
    }

    private void sendModuleStatus(CommandSender sender, String key, boolean enabled) {
        String state = languages.get(sender, enabled ? "admin.modules.state.enabled" : "admin.modules.state.disabled");
        languages.send(sender, key, Collections.singletonMap("state", state));
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
            languages.send(sender, "commands.sf.debug.dump.saved", Collections.singletonMap("file", file.getName()));
            dev.sergeantfuzzy.sfcore.util.message.ConsoleLog.info(plugin, "Debug dump created by " + name + " at "
                    + file.getAbsolutePath() + " (" + file.length() + " bytes, server=" + Bukkit.getVersion() + ")");
        } catch (IOException exception) {
            languages.send(sender, "commands.sf.debug.dump.failed", Collections.singletonMap("error", exception.getMessage()));
        }
    }

    private void toggleNotifications(CommandSender sender) {
        String key = (sender instanceof ConsoleCommandSender) ? "CONSOLE" : sender.getName();
        if (updateNotificationToggles.contains(key)) {
            updateNotificationToggles.remove(key);
            languages.send(sender, "commands.sf.updates.notify.disabled");
        } else {
            updateNotificationToggles.add(key);
            languages.send(sender, "commands.sf.updates.notify.enabled");
        }
    }

    private void sendHelp(CommandSender sender, int page, Category category) {
        List<CommandEntry> visible = COMMANDS.stream()
                .filter(entry -> category == null || entry.category() == category)
                .filter(entry -> isVisible(sender, entry))
                .collect(Collectors.toList());
        int pageSize = 6;
        int maxPage = Math.max(1, (int) Math.ceil(visible.size() / (double) pageSize));
        int index = Math.max(0, Math.min(page - 1, maxPage - 1)) * pageSize;
        int end = Math.min(index + pageSize, visible.size());

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("page", String.valueOf(index / pageSize + 1));
        placeholders.put("pages", String.valueOf(maxPage));
        String categorySuffix = "";
        if (category != null) {
            String categoryLabel = category.label(languages, sender);
            categorySuffix = languages.get(sender, "commands.sf.help.header-category", Collections.singletonMap("category", categoryLabel));
        }
        placeholders.put("category", categorySuffix);
        String header = languages.get(sender, "commands.sf.help.header", placeholders);

        sender.sendMessage(" ");
        sender.sendMessage(header);
        sender.sendMessage(boxDivider(sender));
        sender.sendMessage(" ");
        for (int i = index; i < end; i++) {
            sendHelpLine(sender, visible.get(i));
        }
        if (maxPage > 1) {
            sender.sendMessage(" ");
            sendHelpNavigation(sender, index / pageSize + 1, maxPage, category);
        }
        sender.sendMessage(boxDivider(sender));
    }

    private void sendHelpDetail(CommandSender sender, CommandEntry entry) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("usage", entry.usage(languages, sender));
        placeholders.put("description", entry.description(languages, sender));
        placeholders.put("category", entry.category().label(languages, sender));
        placeholders.put("permission", entry.permission());
        sender.sendMessage(" ");
        sender.sendMessage(languages.get(sender, "commands.sf.help.detail.title", placeholders));
        sender.sendMessage(boxDivider(sender));
        sender.sendMessage(" ");
        languages.send(sender, "commands.sf.help.detail.description", placeholders);
        languages.send(sender, "commands.sf.help.detail.category", placeholders);
        languages.send(sender, "commands.sf.help.detail.permission", placeholders);
        sender.sendMessage(" ");
        sender.sendMessage(boxDivider(sender));
    }

    private void sendHelpLine(CommandSender sender, CommandEntry entry) {
        String usage = entry.usage(languages, sender);
        String description = entry.description(languages, sender);
        Map<String, String> hoverPlaceholders = new LinkedHashMap<>();
        hoverPlaceholders.put("description", description);
        hoverPlaceholders.put("permission", entry.permission());
        String line = "  " + ChatColor.GOLD + usage
                + languages.get(sender, "commands.sf.help.line-suffix", Collections.singletonMap("description", description));
        ComponentMessenger.sendHoverMessage(
                sender,
                line,
                Collections.singletonList(languages.get(sender, "commands.sf.help.hover", hoverPlaceholders)),
                entry.suggestion()
        );
    }

    private void sendHelpNavigation(CommandSender sender, int page, int maxPage, Category category) {
        if (maxPage <= 1) {
            return;
        }
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(ComponentMessenger.InteractiveMessagePart.plain("  "));
        boolean hasPrevious = page > 1;
        boolean hasNext = page < maxPage;
        if (hasPrevious) {
            parts.add(buildHelpNavButton(sender, "commands.sf.help.nav.previous", "commands.sf.help.nav.previous-hover", page - 1, maxPage, category));
        }
        if (hasPrevious && hasNext) {
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(ChatColor.GRAY + "   "));
        }
        if (hasNext) {
            parts.add(buildHelpNavButton(sender, "commands.sf.help.nav.next", "commands.sf.help.nav.next-hover", page + 1, maxPage, category));
        }
        ComponentMessenger.sendJoinedHoverMessages(sender, parts);
    }

    private ComponentMessenger.InteractiveMessagePart buildHelpNavButton(CommandSender sender, String labelKey, String hoverKey, int targetPage, int maxPage, Category category) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("page", String.valueOf(targetPage));
        placeholders.put("pages", String.valueOf(maxPage));
        String label = languages.get(sender, labelKey, placeholders);
        List<String> hover = languages.list(sender, hoverKey, placeholders);
        return ComponentMessenger.InteractiveMessagePart.interactive(label, hover, buildHelpCommand(category, targetPage), true);
    }

    private String buildHelpCommand(Category category, int page) {
        if (category == null) {
            return "/sf help " + page;
        }
        return "/sf help " + category.commandToken() + " " + page;
    }

    private Integer parsePage(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            return null;
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

    private boolean isVisible(CommandSender sender, CommandEntry entry) {
        if (sender.hasPermission(entry.permission())) {
            return true;
        }
        if (sender instanceof ConsoleCommandSender) {
            return true;
        }
        if (entry.defaultAccess() == PermissionDefault.TRUE) {
            return true;
        }
        return entry.defaultAccess() == PermissionDefault.OP && sender.isOp();
    }

    private CommandEntry findEntry(String name) {
        String normalized = name.toLowerCase(Locale.ENGLISH);
        for (CommandEntry entry : COMMANDS) {
            if (entry.matches(normalized)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("help", "info", "about", "permissions", "commands", "reload", "diagnostics", "version", "updates", "config", "debug", "joinleave", "joinmotd"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ENGLISH);
            switch (sub) {
                case "help":
                case "permissions":
                case "commands":
                    suggestions.addAll(categoryNames(sender));
                    suggestions.addAll(COMMANDS.stream().map(CommandEntry::name).collect(Collectors.toList()));
                    break;
                case "reload":
                    suggestions.add("config");
                    suggestions.addAll(listDataFiles());
                    break;
                case "diagnostics":
                    suggestions.add("full");
                    break;
                case "updates":
                    suggestions.addAll(Arrays.asList("check", "notify"));
                    break;
                case "config":
                    suggestions.add("validate");
                    break;
                case "debug":
                    suggestions.addAll(Arrays.asList("enable", "disable", "dump"));
                    break;
                case "joinleave":
                case "joinmotd":
                    suggestions.addAll(Arrays.asList("on", "off", "status"));
                    break;
                default:
                    break;
            }
        }
        return suggestions.stream()
                .filter(s -> s.toLowerCase(Locale.ENGLISH).startsWith(args[args.length - 1].toLowerCase(Locale.ENGLISH)))
                .collect(Collectors.toList());
    }

    private List<String> listDataFiles() {
        File folder = plugin.getDataFolder();
        if (!folder.exists() || !folder.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ENGLISH).endsWith(".yml"));
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (File file : files) {
            names.add(file.getName());
        }
        return names;
    }

    private List<String> categoryNames(CommandSender sender) {
        List<String> names = new ArrayList<>();
        for (Category category : Category.values()) {
            names.add(category.label(languages, sender));
        }
        return names;
    }

    private static CommandEntry entry(String name, String usageKey, String descriptionKey, String permission, Category category, PermissionDefault access, String suggestion, String... aliases) {
        return new CommandEntry(name, usageKey, descriptionKey, permission, category, access, suggestion, Arrays.asList(aliases));
    }

    private static final class CommandEntry {
        private final String name;
        private final String usageKey;
        private final String descriptionKey;
        private final String permission;
        private final Category category;
        private final PermissionDefault defaultAccess;
        private final String suggestion;
        private final List<String> aliases;

        private CommandEntry(String name, String usageKey, String descriptionKey, String permission, Category category, PermissionDefault defaultAccess, String suggestion, List<String> aliases) {
            this.name = name;
            this.usageKey = usageKey;
            this.descriptionKey = descriptionKey;
            this.permission = permission;
            this.category = category;
            this.defaultAccess = defaultAccess;
            this.suggestion = suggestion;
            this.aliases = aliases == null ? Collections.<String>emptyList() : aliases;
        }

        private String name() {
            return name;
        }

        private String usage(LanguageManager languages, CommandSender sender) {
            return languages.get(sender, usageKey);
        }

        private String description(LanguageManager languages, CommandSender sender) {
            return languages.get(sender, descriptionKey);
        }

        private String permission() {
            return permission;
        }

        private Category category() {
            return category;
        }

        private PermissionDefault defaultAccess() {
            return defaultAccess;
        }

        private String suggestion() {
            return suggestion;
        }

        private boolean matches(String input) {
            if (name.equalsIgnoreCase(input)) {
                return true;
            }
            if (name.replace(" ", "").equalsIgnoreCase(input)) {
                return true;
            }
            for (String alias : aliases) {
                if (alias.equalsIgnoreCase(input)) {
                    return true;
                }
            }
            return false;
        }
    }
}


