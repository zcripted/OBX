package dev.zcripted.obx.core.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.update.UpdateChecker;
import dev.zcripted.obx.util.update.UpdateNotificationService;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Toggleable module {@code /obx} subcommands: updates (check/notify),
 * joinleave, and joinmotd.
 *
 * <p>Also a {@link org.bukkit.event.Listener}: {@link #onJoin} delegates to
 * {@link UpdateNotificationService}, which notifies eligible players (holders of
 * {@code obx.updates.notify} who have not opted out via {@code /obx updates notify})
 * of a newer release on join — notifications are ON by default and the opt-out is
 * persisted across restarts.
 */
class ObxModulesView implements org.bukkit.event.Listener {

    private final ObxPlugin plugin;
    private final LanguageManager languages;
    private final UpdateNotificationService updateService;
    private final UpdateChecker updateChecker;

    ObxModulesView(ObxPlugin plugin, LanguageManager languages, UpdateNotificationService updateService) {
        this.plugin = plugin;
        this.languages = languages;
        this.updateService = updateService;
        this.updateChecker = updateService.checker();
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
            placeholders.put("url", UpdateChecker.DOWNLOAD_URL);
            switch (result.status()) {
                case UPDATE_AVAILABLE:
                    // Shared renderer: players get [Download]/[Release Notes] buttons,
                    // console gets the same destinations as plain text rows.
                    updateService.sendAvailableMessages(sender, result);
                    break;
                case UP_TO_DATE:
                    // Manual check: still close the box with the Download/Release-Notes
                    // actions (buttons in-game, text rows on console).
                    languages.send(sender, "commands.obx.updates.current", placeholders);
                    updateService.sendLinkActions(sender, result);
                    break;
                case FAILED:
                default:
                    // Check unreachable — offer the links so the user can verify manually.
                    languages.send(sender, "commands.obx.updates.failed", placeholders);
                    updateService.sendLinkActions(sender, result);
                    break;
            }
        });
    }

    void handleJoinLeave(CommandSender sender, String[] args) {
        if (!ensurePermission(sender, "obx.admin.modules.joinleave")) {
            return;
        }
        if (plugin.getJoinLeaveService() == null) {
            // The owning (playerinfo) module is disabled — guard against the NPE its getter would throw.
            languages.send(sender, "admin.modules.joinleave.usage");
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
        if (plugin.getJoinLeaveService() == null) {
            languages.send(sender, "admin.modules.joinmotd.usage");
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

    void handleAfk(CommandSender sender, String[] args) {
        if (!ensurePermission(sender, "obx.admin.modules.afk")) {
            return;
        }
        dev.zcripted.obx.api.playerstate.AfkService afk = plugin.getAfkService();
        if (afk == null) {
            languages.send(sender, "admin.modules.afk.usage");
            return;
        }
        if (args.length < 2) {
            sendModuleStatus(sender, "admin.modules.afk.status", afk.isEnabled());
            languages.send(sender, "admin.modules.afk.usage");
            return;
        }
        String action = args[1].toLowerCase(Locale.ENGLISH);
        if (action.equals("on") || action.equals("enable") || action.equals("true")) {
            afk.setEnabled(true);
            languages.send(sender, "admin.modules.afk.enabled");
            logAfkToggle(sender, true);
            return;
        }
        if (action.equals("off") || action.equals("disable") || action.equals("false")) {
            afk.setEnabled(false);
            languages.send(sender, "admin.modules.afk.disabled");
            logAfkToggle(sender, false);
            return;
        }
        if (action.equals("status") || action.equals("info")) {
            sendModuleStatus(sender, "admin.modules.afk.status", afk.isEnabled());
            return;
        }
        languages.send(sender, "admin.modules.afk.usage");
    }

    void handleDeathDrop(CommandSender sender, String[] args) {
        if (!ensurePermission(sender, "obx.admin.modules.deathdrop")) {
            return;
        }
        dev.zcripted.obx.core.module.ModuleManager modules = plugin.getModuleManager();
        if (modules == null || !modules.isRegistered("deathdrop")) {
            sendDeathDropBox(sender, false);
            return;
        }
        boolean current = modules.isEnabled("deathdrop");
        if (args.length < 2) {
            sendDeathDropBox(sender, current);
            return;
        }
        String action = args[1].toLowerCase(Locale.ENGLISH);
        if (action.equals("on") || action.equals("enable") || action.equals("true")) {
            if (!current) {
                modules.setEnabled("deathdrop", true);
            }
            sendDeathDropBox(sender, true);
            logDeathDropToggle(sender, true);
            return;
        }
        if (action.equals("off") || action.equals("disable") || action.equals("false")) {
            if (current) {
                modules.setEnabled("deathdrop", false);
            }
            sendDeathDropBox(sender, false);
            logDeathDropToggle(sender, false);
            return;
        }
        // status / info / anything else → show the current state box.
        sendDeathDropBox(sender, current);
    }

    /**
     * /obx warn <on|off|status> — toggles the unconfigured-webhook startup/join
     * warnings. Takes effect immediately (no reload) and persists to config.yml.
     */
    void handleWarn(CommandSender sender, String[] args) {
        if (!ensurePermission(sender, dev.zcripted.obx.core.diagnostics.WebhookWarningService.PERMISSION)) {
            return;
        }
        dev.zcripted.obx.core.diagnostics.WebhookWarningService warnings = plugin.getServiceRegistry()
                .get(dev.zcripted.obx.core.diagnostics.WebhookWarningService.class);
        if (warnings == null) {
            return;
        }
        boolean current = warnings.isEnabled();
        if (args.length < 2) {
            sendWebhookWarnBox(sender, warnings, current);
            return;
        }
        String action = args[1].toLowerCase(Locale.ENGLISH);
        if (action.equals("on") || action.equals("enable") || action.equals("true")) {
            if (!current) {
                warnings.setEnabled(true);
            }
            sendWebhookWarnBox(sender, warnings, true);
            logWebhookWarnToggle(sender, true);
            return;
        }
        if (action.equals("off") || action.equals("disable") || action.equals("false")) {
            if (current) {
                warnings.setEnabled(false);
            }
            sendWebhookWarnBox(sender, warnings, false);
            logWebhookWarnToggle(sender, false);
            return;
        }
        // status / info / anything else → show the current state box.
        sendWebhookWarnBox(sender, warnings, current);
    }

    /** Box-style webhook-warnings status (with the live unconfigured count) + a toggle button row. */
    private void sendWebhookWarnBox(CommandSender sender,
                                    dev.zcripted.obx.core.diagnostics.WebhookWarningService warnings,
                                    boolean enabled) {
        java.util.Map<String, String> placeholders = Collections.singletonMap(
                "count", String.valueOf(warnings.unconfigured().size()));
        languages.send(sender, enabled ? "admin.modules.warn.enabled" : "admin.modules.warn.disabled",
                placeholders);
        if (!(sender instanceof org.bukkit.entity.Player)) {
            return;
        }
        String opposite = enabled ? "off" : "on";
        java.util.List<dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart> row =
                new java.util.ArrayList<>();
        row.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart.plain(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "  &8» ")));
        row.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart.interactive(
                languages.get(sender, "admin.modules.warn.button"),
                languages.list(sender, "admin.modules.warn.button.hover", Collections.<String, String>emptyMap()),
                "/obx warn " + opposite, true));
        dev.zcripted.obx.util.text.ComponentMessenger.sendJoinedHoverMessages(sender, row);
    }

    /** Console audit line whenever the webhook warnings are toggled (command or GUI). */
    private void logWebhookWarnToggle(CommandSender sender, boolean enabled) {
        dev.zcripted.obx.util.message.ConsoleLog.info(plugin,
                "Webhook warnings " + (enabled ? "§aenabled" : "§cdisabled") + "§7 by §f" + sender.getName());
    }

    /**
     * Sends the box-style death-grouping status/toggle message. In-game it is followed by a
     * one-button toggle row (a click runs {@code /obx deathdrop <opposite>}); console gets the
     * box only — it can't click, so no button row is sent.
     */
    private void sendDeathDropBox(CommandSender sender, boolean enabled) {
        languages.send(sender, enabled ? "admin.modules.deathdrop.enabled" : "admin.modules.deathdrop.disabled");
        if (!(sender instanceof org.bukkit.entity.Player)) {
            return;
        }
        String opposite = enabled ? "off" : "on";
        java.util.List<dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart> row =
                new java.util.ArrayList<>();
        row.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart.plain(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "  &8» ")));
        row.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart.interactive(
                languages.get(sender, "admin.modules.deathdrop.button"),
                languages.list(sender, "admin.modules.deathdrop.button.hover", Collections.<String, String>emptyMap()),
                "/obx deathdrop " + opposite, true));
        dev.zcripted.obx.util.text.ComponentMessenger.sendJoinedHoverMessages(sender, row);
    }

    /** Console audit line whenever death-grouping is toggled (command or GUI). */
    private void logDeathDropToggle(CommandSender sender, boolean enabled) {
        dev.zcripted.obx.util.message.ConsoleLog.info(plugin,
                "Death grouping " + (enabled ? "§aenabled" : "§cdisabled") + "§7 by §f" + sender.getName());
    }

    /** Console audit line whenever the AFK system is toggled (command or GUI). */
    private void logAfkToggle(CommandSender sender, boolean enabled) {
        dev.zcripted.obx.util.message.ConsoleLog.info(plugin,
                "AFK system " + (enabled ? "§aenabled" : "§cdisabled") + "§7 by §f" + sender.getName());
    }

    private void sendModuleStatus(CommandSender sender, String key, boolean enabled) {
        String state = languages.get(sender, enabled ? "admin.modules.state.enabled" : "admin.modules.state.disabled");
        languages.send(sender, key, Collections.singletonMap("state", state));
    }

    private void toggleNotifications(CommandSender sender) {
        // Default-ON for obx.updates.notify holders; the toggle records a persisted OPT-OUT
        // (SQLite) so the preference survives restarts. The service returns the NEW state.
        boolean enabled = updateService.toggle(sender);
        languages.send(sender, enabled
                ? "commands.obx.updates.notify.enabled"
                : "commands.obx.updates.notify.disabled");
    }

    /**
     * Notifies an eligible player on join if a newer OBX release exists. Eligibility, the async
     * check (no network on the main thread), and the main-thread message delivery all live in
     * {@link UpdateNotificationService#notifyOnJoin}: notifications are ON by default for
     * {@code obx.updates.notify} holders unless they opted out via {@code /obx updates notify}.
     */
    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        updateService.notifyOnJoin(event.getPlayer());
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