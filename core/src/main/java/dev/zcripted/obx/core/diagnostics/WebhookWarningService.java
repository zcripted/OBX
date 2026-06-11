package dev.zcripted.obx.core.diagnostics;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Warns server owners while Discord webhook settings still hold their shipped
 * blank/placeholder values:
 *
 * <ul>
 *   <li><b>Startup console</b> — {@link #logStartupWarning()} prints the unconfigured
 *       paths as WARN lines during {@code onEnable}.</li>
 *   <li><b>Admin join</b> — holders of {@code obx.admin.warnings} get a chat line with
 *       an OBX-style hover (the affected settings + fix/hide hints), sent synchronously
 *       on join so it always lands <em>above</em> the welcome MOTD (which is dispatched
 *       10 ticks later).</li>
 * </ul>
 *
 * <p>The warnings stop on their own once every checked setting holds a real value.
 * They can also be disabled three ways: {@code /obx warn off} or the Admin GUI
 * Module Toggles tile (both take effect immediately and persist to {@code config.yml}),
 * or by editing {@code warnings.webhook-unconfigured} in {@code config.yml} directly
 * (applies on {@code /obx reload} / {@code /obx reload config}).
 */
public final class WebhookWarningService implements Listener {

    public static final String CONFIG_KEY = "warnings.webhook-unconfigured";
    public static final String PERMISSION = "obx.admin.warnings";

    /** Config paths checked, in display order: three moderation-mirror values + the economy digest URL. */
    private static final String[] WEBHOOK_URL_PATHS = {
            "discord.moderation.webhook-url",
            "economy.reporting.discord-webhook",
    };
    private static final String[] SNOWFLAKE_PATHS = {
            "discord.moderation.server-id",
            "discord.moderation.channel-id",
    };

    private final ObxPlugin plugin;
    private volatile boolean enabled;

    public WebhookWarningService(ObxPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Re-reads the config toggle — direct config-file edits apply on /obx reload. */
    public void reload() {
        enabled = plugin.getConfig().getBoolean(CONFIG_KEY, true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Command/GUI toggle: takes effect immediately and persists to config.yml (no reload). */
    public void setEnabled(boolean value) {
        enabled = value;
        plugin.getConfig().set(CONFIG_KEY, value);
        plugin.saveConfig();
    }

    /** Config paths whose webhook values are still blank or placeholder-looking. */
    public List<String> unconfigured() {
        List<String> issues = new ArrayList<>();
        for (String path : WEBHOOK_URL_PATHS) {
            if (!looksLikeWebhookUrl(plugin.getConfig().getString(path, ""))) {
                issues.add(path);
            }
        }
        for (String path : SNOWFLAKE_PATHS) {
            if (!looksLikeSnowflake(plugin.getConfig().getString(path, ""))) {
                issues.add(path);
            }
        }
        return issues;
    }

    /** A real Discord webhook URL (not blank, not placeholder text). */
    private static boolean looksLikeWebhookUrl(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty()
                && trimmed.startsWith("https://")
                && trimmed.contains("/api/webhooks/")
                && !containsPlaceholderText(trimmed);
    }

    /** A real Discord snowflake id (digits only, not blank/placeholder). */
    private static boolean looksLikeSnowflake(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 5) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Obvious template text an owner forgot to replace. */
    private static boolean containsPlaceholderText(String value) {
        String upper = value.toUpperCase(java.util.Locale.ENGLISH);
        return upper.contains("YOUR") || upper.contains("PASTE") || upper.contains("EXAMPLE")
                || upper.contains("CHANGEME") || upper.contains("PLACEHOLDER")
                || value.indexOf('<') >= 0 || value.indexOf('{') >= 0;
    }

    /** Startup console warning (plain WARN lines, before the boot banner). */
    public void logStartupWarning() {
        if (!enabled) {
            return;
        }
        List<String> missing = unconfigured();
        if (missing.isEmpty()) {
            return;
        }
        plugin.getLogger().warning("Discord webhook settings not configured (" + missing.size() + "):");
        for (String path : missing) {
            plugin.getLogger().warning("  - " + path);
        }
        plugin.getLogger().warning("Set them in config.yml (then /obx reload), or hide these warnings with /obx warn off");
    }

    /**
     * Admin join warning. HIGH priority and synchronous — the welcome MOTD is sent on a
     * 10-tick delay, so this line is always visible above it.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission(PERMISSION)) {
            return;
        }
        List<String> missing = unconfigured();
        if (missing.isEmpty()) {
            return;
        }
        dev.zcripted.obx.core.language.LanguageManager languages = plugin.getLanguageManager();
        String line = languages.get(player, "admin.warnings.webhook.line",
                Collections.singletonMap("count", String.valueOf(missing.size())));
        List<String> hover = new ArrayList<>();
        hover.add(languages.get(player, "admin.warnings.webhook.hover.title"));
        hover.add(languages.get(player, "core.divider-line"));
        for (String path : missing) {
            hover.add(languages.get(player, "admin.warnings.webhook.hover.entry",
                    Collections.singletonMap("path", path)));
        }
        hover.add("");
        hover.add(languages.get(player, "admin.warnings.webhook.hover.fix"));
        hover.add(languages.get(player, "admin.warnings.webhook.hover.hide"));
        // Click suggests the hide command; hover carries the per-setting breakdown.
        ComponentMessenger.sendHoverMessage(player, line, hover, "/obx warn off");
    }
}