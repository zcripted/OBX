package dev.zcripted.obx.feature.moderation.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.moderation.service.ModerationService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enforces the plugin's UUID-keyed ban ledger at login. The native {@code NAME} ban list is
 * bypassable by a name change on Spigot/Paper &lt; 1.20.1 (where {@code PROFILE} bans don't exist),
 * so this listener rejects a banned UUID regardless of the current name — making OBX bans
 * authoritative across the whole supported 1.12 → 1.21 range.
 *
 * <p>Runs on {@link AsyncPlayerPreLoginEvent} (off the main thread); the data store serializes
 * its own access, so the ban lookup here is thread-safe.
 */
public final class BanLoginListener implements Listener {

    private final ObxPlugin plugin;
    private final ModerationService service;

    public BanLoginListener(ObxPlugin plugin, ModerationService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return; // already denied by the server (vanilla ban, whitelist, etc.)
        }
        ModerationService.BanRecord ban = service.findActiveUuidBan(event.getUniqueId());
        if (ban == null) {
            return;
        }
        String reason = ban.reason == null ? service.getDefaultReason() : ban.reason;
        String message;
        if (ban.isPermanent()) {
            message = plugin.getLanguageManager().format(event.getUniqueId(),
                    "player.moderation.ban.kick-message", Placeholders.with("reason", reason));
        } else {
            Map<String, String> placeholders = new LinkedHashMap<>();
            placeholders.put("reason", reason);
            placeholders.put("duration", service.formatRemaining(ban.expiresAt - System.currentTimeMillis()));
            message = plugin.getLanguageManager().format(event.getUniqueId(),
                    "player.moderation.tempban.kick-message", placeholders);
        }
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
    }
}
