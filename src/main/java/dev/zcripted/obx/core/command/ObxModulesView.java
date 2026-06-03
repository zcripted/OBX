package dev.zcripted.obx.core.command;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.update.UpdateChecker;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Toggleable module {@code /obx} subcommands: updates (check/notify),
 * joinleave, and joinmotd.
 */
class ObxModulesView {

    private final OBX plugin;
    private final LanguageManager languages;
    private final UpdateChecker updateChecker;
    private final Set<String> updateNotificationToggles = new HashSet<>();

    ObxModulesView(OBX plugin, LanguageManager languages, UpdateChecker updateChecker) {
        this.plugin = plugin;
        this.languages = languages;
        this.updateChecker = updateChecker;
    }

    void handleUpdates(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("notify")) {
            if (!ensurePermission(sender, "obx.updates.notify")) {
                return;
            }
            toggleNotifications(sender);
            return;
        }
        if (!ensurePermission(sender, "obx.updates.check")) {
            return;
        }
        languages.send(sender, "commands.obx.updates.header");
        // Query the PUBLIC OBX repo's latest release off-thread; the callback
        // runs back on the main/global thread so sending messages here is safe.
        updateChecker.checkAsync(result -> {
            Map<String, String> placeholders = new LinkedHashMap<>();
            placeholders.put("current", result.currentVersion());
            placeholders.put("version", result.currentVersion());
            placeholders.put("latest", result.latestVersion() == null ? "?" : result.latestVersion());
            placeholders.put("url", UpdateChecker.RELEASES_URL);
            switch (result.status()) {
                case UPDATE_AVAILABLE:
                    languages.send(sender, "commands.obx.updates.available", placeholders);
                    languages.send(sender, "commands.obx.updates.available-link", placeholders);
                    break;
                case UP_TO_DATE:
                    languages.send(sender, "commands.obx.updates.current", placeholders);
                    break;
                case FAILED:
                default:
                    languages.send(sender, "commands.obx.updates.failed", placeholders);
                    break;
            }
        });
    }

    void handleJoinLeave(CommandSender sender, String[] args) {
        if (!ensurePermission(sender, "obx.admin.modules.joinleave")) {
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

    void handleJoinMotd(CommandSender sender, String[] args) {
        if (!ensurePermission(sender, "obx.admin.modules.joinmotd")) {
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

    private void toggleNotifications(CommandSender sender) {
        String key = (sender instanceof ConsoleCommandSender) ? "CONSOLE" : sender.getName();
        if (updateNotificationToggles.contains(key)) {
            updateNotificationToggles.remove(key);
            languages.send(sender, "commands.obx.updates.notify.disabled");
        } else {
            updateNotificationToggles.add(key);
            languages.send(sender, "commands.obx.updates.notify.enabled");
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
