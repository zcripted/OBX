package dev.zcripted.obx.core.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
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

    private final ObxPlugin plugin;
    private final LanguageManager languages;

    ObxDiagnosticsView(ObxPlugin plugin, LanguageManager languages) {
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
            if (plugin.getServiceRegistry().get(dev.zcripted.obx.api.moderation.ModerationApi.class) != null) {
                plugin.getServiceRegistry().get(dev.zcripted.obx.api.moderation.ModerationApi.class).reload();
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
            // Path-traversal guard: reject any target whose resolved path escapes the data folder
            // (e.g. "../../server.properties") so this admin command can't probe arbitrary files.
            try {
                String dataRoot = plugin.getDataFolder().getCanonicalPath() + File.separator;
                if (!file.getCanonicalPath().startsWith(dataRoot)) {
                    languages.send(sender, "commands.obx.reload.file.missing", Collections.singletonMap("file", target));
                    return;
                }
            } catch (java.io.IOException pathError) {
                languages.send(sender, "commands.obx.reload.file.missing", Collections.singletonMap("file", target));
                return;
            }
            if (!file.exists() || !file.isFile()) {
                languages.send(sender, "commands.obx.reload.file.missing", Collections.singletonMap("file", target));
                return;
            }
            try {
                long fileStart = System.nanoTime();
                YamlConfiguration.loadConfiguration(file);
                String fileDuration = formatMillis(System.nanoTime() - fileStart);
                Map<String, String> placeholders = new LinkedHashMap<>();
                placeholders.put("file", target);
                placeholders.put("path", file.getAbsolutePath());
                placeholders.put("size", String.valueOf(file.length()));
                placeholders.put("duration", fileDuration);
                if (sender instanceof Player) {
                    // Hover scoped to ONLY the "Reloaded file <file> (time)" segment, prefix plain.
                    List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<>();
                    parts.add(ComponentMessenger.InteractiveMessagePart.plain(languages.get(sender, "system.prefix")));
                    parts.add(ComponentMessenger.InteractiveMessagePart.interactive(
                            languages.get(sender, "commands.obx.reload.file.body", placeholders),
                            languages.list(sender, "commands.obx.reload.file.hover", placeholders), null, false));
                    ComponentMessenger.sendJoinedHoverMessages(sender, parts);
                } else {
                    languages.send(sender, "commands.obx.reload.file.body", placeholders);
                }
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

        // Player executor: styled in-game message with the hover scoped to ONLY the
        // "reload complete (time)" segment — the prefix stays a plain, non-hover part.
        if (sender instanceof Player) {
            List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<>();
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(languages.get(sender, "system.prefix")));
            parts.add(ComponentMessenger.InteractiveMessagePart.interactive(
                    languages.get(sender, "commands.obx.reload.full.body", Collections.singletonMap("duration", totalText)),
                    buildReloadHover(sender, times, totalText), "/obx diagnostics", false));
            ComponentMessenger.sendJoinedHoverMessages(sender, parts);
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
        // ── Quick health check: platform, modules, config, storage, overall status ──
        dev.zcripted.obx.core.platform.PlatformInfo platform = plugin.getPlatformInfo();
        Map<String, Boolean> moduleStates = plugin.getModuleManager().states();
        int total = moduleStates.size();
        int enabledCount = 0;
        List<String> disabledModules = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : moduleStates.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                enabledCount++;
            } else {
                disabledModules.add(entry.getKey());
            }
        }
        Collections.sort(disabledModules, String.CASE_INSENSITIVE_ORDER);

        // moderation.yml is deliberately absent: moderation data lives in SQLite, and the
        // yml is a legacy file that is migrated once and renamed to .migrated — don't flag it.
        List<String> missingConfigs = new ArrayList<>();
        for (String keyFile : new String[]{"config.yml", "systems/tablist.yml", "systems/scoreboard.yml"}) {
            if (!new File(plugin.getDataFolder(), keyFile).exists()) {
                missingConfigs.add(keyFile);
            }
        }
        boolean storageOk = plugin.getDataStore() != null && plugin.getDataStore().isAvailable();
        int issues = disabledModules.size() + missingConfigs.size() + (storageOk ? 0 : 1);

        String platformStr = (platform == null)
                ? Bukkit.getName() + " " + Bukkit.getBukkitVersion()
                : platform.getServerName() + " " + platform.getMinecraftVersion();
        if (platform != null && platform.isFolia()) {
            platformStr += " &8(&dFolia&8)";
        }

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("platform", platformStr);
        placeholders.put("modules", "&f" + enabledCount + "&8/&f" + total + " &7enabled"
                + (disabledModules.isEmpty() ? "" : "  &8(&c" + disabledModules.size() + " off&8)"));
        placeholders.put("config", missingConfigs.isEmpty() ? "&aOK" : "&c" + missingConfigs.size() + " missing");
        placeholders.put("storage", storageOk ? "&aSQLite ready" : "&cunavailable");
        placeholders.put("health", issues == 0 ? "&aHealthy" : "&e" + issues + " issue(s)");

        for (String line : languages.list(sender, "commands.obx.diagnostics.header", placeholders)) {
            sender.sendMessage(line);
        }
        languages.send(sender, "commands.obx.diagnostics.platform", placeholders);
        // Modules row: hover (on just the "X/X enabled" value) lists every module A→Z with its state.
        sendHoverValueLine(sender, "commands.obx.diagnostics.modules", "modules", placeholders,
                placeholders.get("modules"), buildModulesHover(moduleStates));
        languages.send(sender, "commands.obx.diagnostics.config-status", placeholders);
        languages.send(sender, "commands.obx.diagnostics.storage", placeholders);
        languages.send(sender, "commands.obx.diagnostics.health", placeholders);

        if (!full) {
            // A clickable button to jump to the extended report.
            List<ComponentMessenger.InteractiveMessagePart> btn = new ArrayList<>();
            btn.add(ComponentMessenger.InteractiveMessagePart.plain(ChatColor.translateAlternateColorCodes('&', "  &8» ")));
            btn.add(ComponentMessenger.InteractiveMessagePart.interactive(
                    languages.get(sender, "commands.obx.diagnostics.full-button"),
                    languages.list(sender, "commands.obx.diagnostics.full-button-hover", Collections.<String, String>emptyMap()),
                    "/obx diagnostics full", true));
            ComponentMessenger.sendJoinedHoverMessages(sender, btn);
        }

        if (full) {
            // ── Extended: registered services, detected third-party hooks, recorded issues ──
            List<String> services = new ArrayList<>();
            String[][] known = {
                    {"Economy", "dev.zcripted.obx.api.economy.EconomyService"},
                    {"Moderation", "dev.zcripted.obx.api.moderation.ModerationApi"},
                    {"Hub", "dev.zcripted.obx.api.hub.HubApi"},
                    {"Jail", "dev.zcripted.obx.api.jail.JailApi"},
                    {"Scoreboard", "dev.zcripted.obx.api.scoreboard.ScoreboardService"},
                    {"Tablist", "dev.zcripted.obx.api.tablist.TablistService"},
                    {"Teleport", "dev.zcripted.obx.api.teleport.TeleportManager"},
                    {"Chat", "dev.zcripted.obx.api.chat.ChatService"},
            };
            for (String[] svc : known) {
                try {
                    if (plugin.getServiceRegistry().has(Class.forName(svc[1]))) {
                        services.add(svc[0]);
                    }
                } catch (Throwable ignored) {
                    // interface not on the classpath — skip
                }
            }
            List<String> hooks = new ArrayList<>();
            for (String hook : new String[]{"Vault", "PlaceholderAPI", "ProtocolLib", "LuckPerms"}) {
                if (Bukkit.getPluginManager().getPlugin(hook) != null) {
                    hooks.add(hook);
                }
            }
            List<String> problems = new ArrayList<>();
            if (!storageOk) problems.add("storage unavailable");
            if (!missingConfigs.isEmpty()) problems.add(missingConfigs.size() + " config file(s) missing");
            if (!disabledModules.isEmpty()) problems.add(disabledModules.size() + " module(s) disabled");

            Map<String, String> extra = new LinkedHashMap<>(placeholders);
            extra.put("services", services.isEmpty() ? "&7none" : "&f" + String.join("&7, &f", services));
            extra.put("hooks", hooks.isEmpty() ? "&7none detected" : "&f" + String.join("&7, &f", hooks));
            extra.put("errors", problems.isEmpty() ? "&anone recorded" : "&c" + String.join("&7, &c", problems));

            for (String line : languages.list(sender, "commands.obx.diagnostics.extended", extra)) {
                sender.sendMessage(line);
            }
            languages.send(sender, "commands.obx.diagnostics.services", extra);
            languages.send(sender, "commands.obx.diagnostics.hooks", extra);
            if (problems.isEmpty()) {
                languages.send(sender, "commands.obx.diagnostics.errors", extra);
            } else {
                // Errors row: every listed issue is its OWN hover part — hovering
                // "1 config file(s) missing" shows just the missing files, hovering
                // "2 module(s) disabled" shows just those modules, etc.
                sendErrorsLine(sender, extra, storageOk, missingConfigs, disabledModules);
            }
        }
        sender.sendMessage("");
    }

    /**
     * Sends a "label › value" diagnostics row where the hover is scoped to ONLY the value (the
     * label is a plain, non-hover part). Falls back to a plain joined line for console.
     */
    private void sendHoverValueLine(CommandSender sender, String lineKey, String placeholderName,
                                    Map<String, String> placeholders, String value, List<String> hoverLines) {
        Map<String, String> labelOnly = new LinkedHashMap<>(placeholders);
        labelOnly.put(placeholderName, "");
        String label = languages.get(sender, lineKey, labelOnly);
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(ComponentMessenger.InteractiveMessagePart.plain(label));
        parts.add(ComponentMessenger.InteractiveMessagePart.interactive(
                ChatColor.translateAlternateColorCodes('&', value), colorizeLines(hoverLines), null, false));
        ComponentMessenger.sendJoinedHoverMessages(sender, parts);
    }

    /**
     * Renders each hover line from legacy {@code &}/{@code &#RRGGBB} markup to section codes.
     * The hover transport ({@code TextComponent.fromLegacyText}) only understands {@code §} codes,
     * so we run the full {@link dev.zcripted.obx.util.message.AdventureMessageUtil#renderLegacy}
     * colorizer (which also handles {@code &#hex}, used by the reload-style purple header) rather
     * than a plain {@code &}→{@code §} translate.
     */
    private static List<String> colorizeLines(List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>(raw.size());
        for (String line : raw) {
            out.add(dev.zcripted.obx.util.message.AdventureMessageUtil.renderLegacy(line == null ? "" : line));
        }
        return out;
    }

    /** Dark-gray hover divider, matching {@code core.divider-line} used by the /obx reload hover. */
    private static final String HOVER_DIVIDER = "&8──────────────────────────────";

    /** Reload-style header bar: {@code &#6A1B9A▍ 𝗢𝗕𝗫  &8›  &f<title>} (no &l — bold leaks across hover lines). */
    private static String hoverHeader(String title) {
        return "&#6A1B9A▍ 𝗢𝗕𝗫  &8›  &f" + title;
    }

    /**
     * Hover body: every module A→Z with a green/red dot marker. Styled like the /obx reload hover —
     * a hex header bar, a {@code core.divider-line} rule, then clean {@code  ● name} rows (no bold
     * format codes, which previously leaked down the whole tooltip via legacy style inheritance).
     */
    private List<String> buildModulesHover(Map<String, Boolean> moduleStates) {
        List<String> names = new ArrayList<>(moduleStates.keySet());
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        List<String> hover = new ArrayList<>();
        hover.add(hoverHeader("Module States"));
        hover.add(HOVER_DIVIDER);
        for (String name : names) {
            boolean on = Boolean.TRUE.equals(moduleStates.get(name));
            hover.add(on ? "  &a● &7" + name : "  &c○ &8" + name + "  &8(off)");
        }
        return hover;
    }

    /**
     * Sends the "Errors ›" diagnostics row where EACH recorded issue is its own hover-bearing
     * part: hovering an item shows the details for that item only (storage cause, the actual
     * missing files, the actual disabled modules). Items are joined with gray commas; console
     * receives the plain joined line.
     */
    private void sendErrorsLine(CommandSender sender, Map<String, String> placeholders,
                                boolean storageOk, List<String> missingConfigs, List<String> disabledModules) {
        Map<String, String> labelOnly = new LinkedHashMap<>(placeholders);
        labelOnly.put("errors", "");
        String label = languages.get(sender, "commands.obx.diagnostics.errors", labelOnly);
        String separator = ChatColor.translateAlternateColorCodes('&', "&7, ");
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(ComponentMessenger.InteractiveMessagePart.plain(label));
        if (!storageOk) {
            List<String> hover = new ArrayList<>();
            hover.add(hoverHeader("Storage Unavailable"));
            hover.add(HOVER_DIVIDER);
            hover.add("  &c● &7The SQLite data store failed to open.");
            hover.add("  &8source: &7SqliteDataStore &8(&7obx.db&8)");
            hover.add("  &8fix: &7check disk space / file permissions, then restart");
            addErrorPart(parts, separator, "&cstorage unavailable", hover);
        }
        if (!missingConfigs.isEmpty()) {
            List<String> hover = new ArrayList<>();
            hover.add(hoverHeader("Missing Config Files"));
            hover.add(HOVER_DIVIDER);
            for (String file : missingConfigs) {
                hover.add("  &c● &7" + file);
            }
            hover.add("  &8fix: &7run &d/obx reload &7or restart to regenerate");
            addErrorPart(parts, separator, "&c" + missingConfigs.size() + " config file(s) missing", hover);
        }
        if (!disabledModules.isEmpty()) {
            List<String> hover = new ArrayList<>();
            hover.add(hoverHeader("Disabled Modules"));
            hover.add(HOVER_DIVIDER);
            for (String module : disabledModules) {
                hover.add("  &c○ &8" + module);
            }
            hover.add("  &8cause: &7toggled off &8(&7config or /obx <module> off&8)");
            addErrorPart(parts, separator, "&c" + disabledModules.size() + " module(s) disabled", hover);
        }
        ComponentMessenger.sendJoinedHoverMessages(sender, parts);
    }

    /** Appends one hover-bearing error item, prefixing a gray comma when it isn't the first item. */
    private void addErrorPart(List<ComponentMessenger.InteractiveMessagePart> parts, String separator,
                              String text, List<String> hoverLines) {
        if (parts.size() > 1) { // index 0 is always the "Errors ›" label
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(separator));
        }
        parts.add(ComponentMessenger.InteractiveMessagePart.interactive(
                ChatColor.translateAlternateColorCodes('&', text), colorizeLines(hoverLines), null, false));
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
            // Moderation moved to SQLite (moderation.yml is migrate-once legacy) — validate the store instead.
            states.put("moderation_state", plugin.getDataStore() != null && plugin.getDataStore().isAvailable() ? ok : missing);
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
        sendConfigPagination(sender, page, pages, meta);
    }

    /** Clickable « Prev / Next » pagination row at the bottom of the /obx config box. */
    private void sendConfigPagination(CommandSender sender, int page, int pages, Map<String, String> meta) {
        List<ComponentMessenger.InteractiveMessagePart> nav = new ArrayList<>();
        if (page > 1) {
            nav.add(ComponentMessenger.InteractiveMessagePart.interactive(
                    languages.get(sender, "commands.obx.config.nav.prev"),
                    languages.list(sender, "commands.obx.config.nav.hover", Collections.singletonMap("target", String.valueOf(page - 1))),
                    "/obx config " + (page - 1), true));
        } else {
            nav.add(ComponentMessenger.InteractiveMessagePart.plain(languages.get(sender, "commands.obx.config.nav.prev-off")));
        }
        nav.add(ComponentMessenger.InteractiveMessagePart.plain(languages.get(sender, "commands.obx.config.nav.middle", meta)));
        if (page < pages) {
            nav.add(ComponentMessenger.InteractiveMessagePart.interactive(
                    languages.get(sender, "commands.obx.config.nav.next"),
                    languages.list(sender, "commands.obx.config.nav.hover", Collections.singletonMap("target", String.valueOf(page + 1))),
                    "/obx config " + (page + 1), true));
        } else {
            nav.add(ComponentMessenger.InteractiveMessagePart.plain(languages.get(sender, "commands.obx.config.nav.next-off")));
        }
        ComponentMessenger.sendJoinedHoverMessages(sender, nav);
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