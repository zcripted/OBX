package dev.zcripted.obx.core.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.command.AbstractObxCommand;
import dev.zcripted.obx.feature.staff.gui.AdminMenu;
import dev.zcripted.obx.core.gui.main.MainMenu;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.update.UpdateNotificationService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ObxCommand extends AbstractObxCommand implements TabCompleter {

    private final ObxHelpView helpView;
    private final ObxDiagnosticsView diagnosticsView;
    private final ObxModulesView modulesView;
    private final ObxAdminActions adminActions;
    private final dev.zcripted.obx.core.diagnostics.HealthCommand healthCommand;

    public ObxCommand(ObxPlugin plugin, LanguageManager languages) {
        super(plugin);
        // The bootstrap registers the shared UpdateNotificationService (startup check, periodic
        // re-check, persisted notify opt-outs) before commands bind; fall back to a fresh,
        // unstarted instance so the command still works if construction order ever changes.
        UpdateNotificationService updateService =
                plugin.getServiceRegistry().get(UpdateNotificationService.class);
        if (updateService == null) {
            updateService = new UpdateNotificationService(plugin);
        }
        this.helpView = new ObxHelpView(plugin, languages);
        this.diagnosticsView = new ObxDiagnosticsView(plugin, languages);
        this.modulesView = new ObxModulesView(plugin, languages, updateService);
        // ObxModulesView is also a join listener (delivers /obx updates notify alerts on join).
        plugin.getServer().getPluginManager().registerEvents(modulesView, plugin);
        this.adminActions = new ObxAdminActions(plugin);
        this.healthCommand = new dev.zcripted.obx.core.diagnostics.HealthCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("obx.admin.menu")) {
                    AdminMenu.open(plugin, player);
                } else {
                    MainMenu.open(plugin, player);
                }
                return true;
            }
            helpView.sendDefaultHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ENGLISH);
        switch (sub) {
            case "help":
                helpView.handleHelp(sender, args);
                return true;
            case "info":
                if (ensurePermission(sender, "obx.info")) {
                    helpView.handleInfo(sender);
                }
                return true;
            case "about":
                if (ensurePermission(sender, "obx.about")) {
                    helpView.handleAbout(sender);
                }
                return true;
            case "permissions":
                if (ensurePermission(sender, "obx.permissions.view")) {
                    helpView.handlePermissions(sender, args);
                }
                return true;
            case "commands":
                if (ensurePermission(sender, "obx.commands.list")) {
                    helpView.handleCommandsList(sender, args);
                }
                return true;
            case "reload":
                diagnosticsView.handleReload(sender, args);
                return true;
            case "diagnostics":
                diagnosticsView.handleDiagnostics(sender, args);
                return true;
            case "health":
                // Same report as the standalone /health command (obx.admin.health).
                if (ensurePermission(sender, dev.zcripted.obx.core.diagnostics.HealthCommand.PERMISSION)) {
                    healthCommand.report(sender);
                }
                return true;
            case "version":
                if (ensurePermission(sender, "obx.version")) {
                    diagnosticsView.handleVersion(sender);
                }
                return true;
            case "updates":
                modulesView.handleUpdates(sender, args);
                return true;
            case "config":
                diagnosticsView.handleConfig(sender, args);
                return true;
            case "debug":
                diagnosticsView.handleDebug(sender, args);
                return true;
            case "joinleave":
                modulesView.handleJoinLeave(sender, args);
                return true;
            case "joinmotd":
                modulesView.handleJoinMotd(sender, args);
                return true;
            case "afk":
                modulesView.handleAfk(sender, args);
                return true;
            case "deathdrop":
                modulesView.handleDeathDrop(sender, args);
                return true;
            case "plugininfo":
                // Hidden click-bridge from the /pl plugin list: render one plugin's
                // detailed info box. Permission (obx.pl) is enforced inside the handler.
                // Not advertised in tab-complete, /obx help, or the command docs.
                new PluginListCommand(plugin).sendPluginInfo(sender,
                        args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "");
                return true;
            case ObxAdminActions.BRIDGE_TOKEN:
                // Hidden internal click-bridge: the only purpose is to let the
                // styled chat buttons (whitelist/join-lock/clear-entity toggles)
                // invoke the server-control source directly. Not advertised in
                // tab-complete, /obx help, or the command docs.
                if (ensurePermission(sender, "obx.admin.menu")) {
                    adminActions.bridge(sender, args);
                }
                return true;
            default:
                languages.send(sender, "commands.obx.unknown");
                helpView.sendDefaultHelp(sender);
                return true;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("help", "info", "about", "permissions", "commands", "reload", "diagnostics", "health", "version", "updates", "config", "debug", "joinleave", "joinmotd", "afk", "deathdrop"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ENGLISH);
            switch (sub) {
                case "help":
                case "permissions":
                case "commands":
                    suggestions.addAll(categoryNames(sender));
                    suggestions.addAll(ObxHelpView.COMMANDS.stream().map(ObxHelpView.CommandEntry::name).collect(Collectors.toList()));
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
                case "afk":
                case "deathdrop":
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
        for (ObxHelpView.Category category : ObxHelpView.Category.values()) {
            names.add(category.label(languages, sender));
        }
        return names;
    }
}
