package dev.zcripted.obx.command.core;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.language.LanguageManager;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Display logic for the informational {@code /obx} subcommands: help, info,
 * about, permissions, and commands. Hosts the shared {@link Category} enum,
 * the {@link CommandEntry} catalog, and the help-rendering helpers.
 */
class ObxHelpView {

    private final Main plugin;
    private final LanguageManager languages;

    enum Category {
        INFORMATION("commands.obx.category.information", "information"),
        RELOAD_DIAGNOSTICS("commands.obx.category.reload", "reload diagnostics"),
        UPDATES_VERSION("commands.obx.category.updates", "updates version"),
        CONFIG_DEBUG("commands.obx.category.config", "config debug");

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

    static final List<CommandEntry> COMMANDS = Arrays.asList(
            entry("help", "commands.obx.entry.help.usage", "commands.obx.entry.help.description", "obx.help", Category.INFORMATION, PermissionDefault.TRUE, "/obx help", "h"),
            entry("info", "commands.obx.entry.info.usage", "commands.obx.entry.info.description", "obx.info", Category.INFORMATION, PermissionDefault.TRUE, "/obx info"),
            entry("about", "commands.obx.entry.about.usage", "commands.obx.entry.about.description", "obx.about", Category.INFORMATION, PermissionDefault.TRUE, "/obx about"),
            entry("permissions", "commands.obx.entry.permissions.usage", "commands.obx.entry.permissions.description", "obx.permissions.view", Category.INFORMATION, PermissionDefault.FALSE, "/obx permissions"),
            entry("commands", "commands.obx.entry.commands.usage", "commands.obx.entry.commands.description", "obx.commands.list", Category.INFORMATION, PermissionDefault.TRUE, "/obx commands"),

            entry("reload", "commands.obx.entry.reload.usage", "commands.obx.entry.reload.description", "obx.admin.reload", Category.RELOAD_DIAGNOSTICS, PermissionDefault.OP, "/obx reload"),
            entry("reload config", "commands.obx.entry.reload-config.usage", "commands.obx.entry.reload-config.description", "obx.admin.reload.config", Category.RELOAD_DIAGNOSTICS, PermissionDefault.OP, "/obx reload config"),
            entry("reload file", "commands.obx.entry.reload-file.usage", "commands.obx.entry.reload-file.description", "obx.admin.reload.features", Category.RELOAD_DIAGNOSTICS, PermissionDefault.OP, "/obx reload config.yml", "reload resource"),
            entry("diagnostics", "commands.obx.entry.diagnostics.usage", "commands.obx.entry.diagnostics.description", "obx.admin.diagnostics", Category.RELOAD_DIAGNOSTICS, PermissionDefault.OP, "/obx diagnostics"),
            entry("diagnostics full", "commands.obx.entry.diagnostics-full.usage", "commands.obx.entry.diagnostics-full.description", "obx.admin.diagnostics.full", Category.RELOAD_DIAGNOSTICS, PermissionDefault.OP, "/obx diagnostics full"),

            entry("version", "commands.obx.entry.version.usage", "commands.obx.entry.version.description", "obx.version", Category.UPDATES_VERSION, PermissionDefault.OP, "/obx version"),
            entry("updates", "commands.obx.entry.updates.usage", "commands.obx.entry.updates.description", "obx.updates.check", Category.UPDATES_VERSION, PermissionDefault.OP, "/obx updates"),
            entry("updates check", "commands.obx.entry.updates-check.usage", "commands.obx.entry.updates-check.description", "obx.updates.check", Category.UPDATES_VERSION, PermissionDefault.OP, "/obx updates check"),
            entry("updates notify", "commands.obx.entry.updates-notify.usage", "commands.obx.entry.updates-notify.description", "obx.updates.notify", Category.UPDATES_VERSION, PermissionDefault.OP, "/obx updates notify"),

            entry("config", "commands.obx.entry.config.usage", "commands.obx.entry.config.description", "obx.debug.config", Category.CONFIG_DEBUG, PermissionDefault.OP, "/obx config"),
            entry("config validate", "commands.obx.entry.config-validate.usage", "commands.obx.entry.config-validate.description", "obx.debug.config.validate", Category.CONFIG_DEBUG, PermissionDefault.OP, "/obx config validate"),
            entry("debug", "commands.obx.entry.debug.usage", "commands.obx.entry.debug.description", "obx.debug", Category.CONFIG_DEBUG, PermissionDefault.OP, "/obx debug"),
            entry("debug enable", "commands.obx.entry.debug-enable.usage", "commands.obx.entry.debug-enable.description", "obx.debug.toggle", Category.CONFIG_DEBUG, PermissionDefault.OP, "/obx debug enable"),
            entry("debug disable", "commands.obx.entry.debug-disable.usage", "commands.obx.entry.debug-disable.description", "obx.debug.toggle", Category.CONFIG_DEBUG, PermissionDefault.OP, "/obx debug disable"),
            entry("debug dump", "commands.obx.entry.debug-dump.usage", "commands.obx.entry.debug-dump.description", "obx.debug.dump", Category.CONFIG_DEBUG, PermissionDefault.OP, "/obx debug dump")
    );

    ObxHelpView(Main plugin, LanguageManager languages) {
        this.plugin = plugin;
        this.languages = languages;
    }

    /** Renders the default first page of help (no category filter). */
    void sendDefaultHelp(CommandSender sender) {
        sendHelp(sender, 1, null);
    }

    void handleHelp(CommandSender sender, String[] args) {
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

    void handleInfo(CommandSender sender) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("version", plugin.getDescription().getVersion());
        placeholders.put("description", orFallback(plugin.getDescription().getDescription()));
        placeholders.put("authors", String.join(", ", plugin.getDescription().getAuthors()));
        placeholders.put("website", orFallback(plugin.getDescription().getWebsite()));
        for (String line : languages.list(sender, "commands.obx.info.lines", placeholders)) {
            sender.sendMessage(line);
        }
    }

    void handleAbout(CommandSender sender) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("authors", String.join(", ", plugin.getDescription().getAuthors()));
        placeholders.put("website", orFallback(plugin.getDescription().getWebsite()));
        for (String line : languages.list(sender, "commands.obx.about.lines", placeholders)) {
            sender.sendMessage(line);
        }
    }

    void handlePermissions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            languages.send(sender, "commands.obx.permissions.usage");
            languages.send(sender, "commands.obx.permissions.suggestion");
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
            languages.send(sender, "commands.obx.permissions.no-match");
            return;
        }
        sender.sendMessage(" ");
        sender.sendMessage(languages.get(sender, "commands.obx.permissions.title"));
        sender.sendMessage(boxDivider(sender));
        sender.sendMessage(" ");
        for (CommandEntry entry : entries) {
            Map<String, String> rowPlaceholders = new LinkedHashMap<>();
            rowPlaceholders.put("usage", entry.usage(languages, sender));
            rowPlaceholders.put("permission", entry.permission());
            sender.sendMessage(languages.get(sender, "commands.obx.permissions.entry", rowPlaceholders));
        }
        sender.sendMessage(" ");
        sender.sendMessage(boxDivider(sender));
    }

    void handleCommandsList(CommandSender sender, String[] args) {
        Category category = args.length >= 2 ? Category.fromInput(languages, sender, args[1]) : null;
        List<CommandEntry> visible = COMMANDS.stream()
                .filter(entry -> category == null || entry.category() == category)
                .filter(entry -> isVisible(sender, entry))
                .collect(Collectors.toList());
        if (visible.isEmpty()) {
            languages.send(sender, "commands.obx.commands.none");
            return;
        }
        sender.sendMessage(" ");
        sender.sendMessage(languages.get(sender, "commands.obx.commands.title"));
        sender.sendMessage(boxDivider(sender));
        sender.sendMessage(" ");
        for (CommandEntry entry : visible) {
            Map<String, String> rowPlaceholders = new LinkedHashMap<>();
            rowPlaceholders.put("usage", entry.usage(languages, sender));
            rowPlaceholders.put("description", entry.description(languages, sender));
            sender.sendMessage(languages.get(sender, "commands.obx.commands.entry", rowPlaceholders));
        }
        sender.sendMessage(" ");
        sender.sendMessage(boxDivider(sender));
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
            categorySuffix = languages.get(sender, "commands.obx.help.header-category", Collections.singletonMap("category", categoryLabel));
        }
        placeholders.put("category", categorySuffix);
        String header = languages.get(sender, "commands.obx.help.header", placeholders);

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
        sender.sendMessage(languages.get(sender, "commands.obx.help.detail.title", placeholders));
        sender.sendMessage(boxDivider(sender));
        sender.sendMessage(" ");
        languages.send(sender, "commands.obx.help.detail.description", placeholders);
        languages.send(sender, "commands.obx.help.detail.category", placeholders);
        languages.send(sender, "commands.obx.help.detail.permission", placeholders);
        sender.sendMessage(" ");
        sender.sendMessage(boxDivider(sender));
    }

    private void sendHelpLine(CommandSender sender, CommandEntry entry) {
        String usage = entry.usage(languages, sender);
        String description = entry.description(languages, sender);
        Map<String, String> hoverPlaceholders = new LinkedHashMap<>();
        hoverPlaceholders.put("description", description);
        hoverPlaceholders.put("permission", entry.permission());
        String line = "  " + ChatColor.DARK_PURPLE + usage
                + languages.get(sender, "commands.obx.help.line-suffix", Collections.singletonMap("description", description));
        ComponentMessenger.sendHoverMessage(
                sender,
                line,
                Collections.singletonList(languages.get(sender, "commands.obx.help.hover", hoverPlaceholders)),
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
            parts.add(buildHelpNavButton(sender, "commands.obx.help.nav.previous", "commands.obx.help.nav.previous-hover", page - 1, maxPage, category));
        }
        if (hasPrevious && hasNext) {
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(ChatColor.GRAY + "   "));
        }
        if (hasNext) {
            parts.add(buildHelpNavButton(sender, "commands.obx.help.nav.next", "commands.obx.help.nav.next-hover", page + 1, maxPage, category));
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
            return "/obx help " + page;
        }
        return "/obx help " + category.commandToken() + " " + page;
    }

    private Integer parsePage(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String boxDivider(CommandSender sender) {
        return languages.get(sender, "core.divider-line");
    }

    private String orFallback(String value) {
        return (value == null || value.trim().isEmpty()) ? "N/A" : value;
    }

    static boolean isVisible(CommandSender sender, CommandEntry entry) {
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

    static CommandEntry findEntry(String name) {
        String normalized = name.toLowerCase(Locale.ENGLISH);
        for (CommandEntry entry : COMMANDS) {
            if (entry.matches(normalized)) {
                return entry;
            }
        }
        return null;
    }

    private static CommandEntry entry(String name, String usageKey, String descriptionKey, String permission, Category category, PermissionDefault access, String suggestion, String... aliases) {
        return new CommandEntry(name, usageKey, descriptionKey, permission, category, access, suggestion, Arrays.asList(aliases));
    }

    static final class CommandEntry {
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

        String name() {
            return name;
        }

        String usage(LanguageManager languages, CommandSender sender) {
            return languages.get(sender, usageKey);
        }

        String description(LanguageManager languages, CommandSender sender) {
            return languages.get(sender, descriptionKey);
        }

        String permission() {
            return permission;
        }

        Category category() {
            return category;
        }

        PermissionDefault defaultAccess() {
            return defaultAccess;
        }

        String suggestion() {
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
