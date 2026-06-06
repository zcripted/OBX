package dev.zcripted.obx.feature.chat.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.chat.format.ChatFormatter;
import dev.zcripted.obx.api.chat.ChatService;
import dev.zcripted.obx.util.message.AdventureMessageUtil;
import dev.zcripted.obx.util.message.ConsoleTimestamp;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Owns the chat pipeline: enforces moderation mute checks, applies the
 * customisable chat layout from {@link ChatService}, dispatches the
 * Adventure-rendered message to recipients, and mirrors the same line to
 * the server console using ANSI escapes.
 *
 * <p>Runs at {@link EventPriority#HIGHEST} with {@code ignoreCancelled = true}
 * so other plugins (chat managers, prefix providers, anti-spam) get to see
 * the event first.
 */
public final class ChatManagementListener implements Listener {

    private final ObxPlugin plugin;
    private final ChatService service;

    public ChatManagementListener(ObxPlugin plugin, ChatService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        dev.zcripted.obx.api.moderation.ModerationApi moderation =
                plugin.getServiceRegistry().get(dev.zcripted.obx.api.moderation.ModerationApi.class);
        if (moderation != null && moderation.isMuted(player.getUniqueId())) {
            // UUID-keyed (join-loaded cache) — avoids a synchronous name->UUID DB lookup per chat message.
            event.setCancelled(true);
            String reason = moderation.getMuteReason(player.getUniqueId());
            plugin.getLanguageManager().send(player, "player.moderation.mute.chat-blocked", Placeholders.with("reason", reason));
            return;
        }

        if (!service.isEnabled()) {
            return;
        }

        Map<String, String> placeholders = buildPlaceholders(player, event.getMessage());
        // Two renders: a plain one for the sender + console, and one whose name is a
        // click-to-message (suggests /msg <sender> ) with a hover, for everyone else.
        String composed = ChatFormatter.compose(service, placeholders);
        String hover = plugin.getLanguageManager().get(player, "chat.message-hover",
                Placeholders.with("player", player.getName()));
        String composedClickable = ChatFormatter.compose(service, placeholders, player.getName(), hover);

        // Snapshot the recipients into a small array so we can cancel the event without
        // losing the iteration (avoids the per-message HashSet allocation that we used to
        // make defensively).
        Player[] recipients = event.getRecipients().toArray(new Player[0]);
        event.setCancelled(true);

        Map<String, String> empty = Collections.emptyMap();
        for (int i = 0; i < recipients.length; i++) {
            Player recipient = recipients[i];
            if (recipient != null) {
                // The sender can't message themselves, so they see the plain line.
                AdventureMessageUtil.send(recipient, recipient.equals(player) ? composed : composedClickable, empty);
            }
        }

        if (service.isConsoleMirror()) {
            String body = AdventureMessageUtil.renderAnsi(composed, empty);
            String prefix = ConsoleTimestamp.prefix(plugin, service.isConsoleTimestampEnabled(), service.getConsoleTimestampFormat());
            plugin.writeConsoleLine(prefix + body);
        }
    }

    private Map<String, String> buildPlaceholders(Player player, String rawMessage) {
        Map<String, String> placeholders = new LinkedHashMap<>(8);
        placeholders.put("player", player.getName());
        // Display name may be set by another plugin and could carry MiniMessage tags — neutralize.
        placeholders.put("displayname", dev.zcripted.obx.util.text.MessageSanitizer.neutralizeTags(player.getDisplayName()));
        placeholders.put("world", player.getWorld() == null ? "" : player.getWorld().getName());
        placeholders.put("uuid", player.getUniqueId().toString());
        // Honor obx.message.color (consistent with /msg, /me, etc.): plain players can't colorize,
        // obfuscate, or inject interactive MiniMessage tags via normal chat.
        placeholders.put("message",
                dev.zcripted.obx.util.text.MessageSanitizer.sanitizeChat(player, rawMessage, service.allowFormattingInMessages()));
        // Staff tag: players with obx.chat.staff (default op) get the configured prefix
        // (default red/bold "ѕᴛᴀꜰꜰ┃") before their name; everyone else gets an empty string.
        // Permission-driven (not bare isOp()) so permission-managed servers work correctly.
        placeholders.put("prefix",
                (service.isStaffPrefixEnabled() && player.hasPermission("obx.chat.staff")) ? service.getStaffPrefix() : "");
        return placeholders;
    }
}
