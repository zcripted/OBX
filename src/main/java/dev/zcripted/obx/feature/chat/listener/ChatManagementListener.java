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

        if (plugin.getModerationService() != null && plugin.getModerationService().isMuted(player.getName())) {
            event.setCancelled(true);
            String reason = plugin.getModerationService().getMuteReason(player.getName());
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
        placeholders.put("displayname", player.getDisplayName());
        placeholders.put("world", player.getWorld() == null ? "" : player.getWorld().getName());
        placeholders.put("uuid", player.getUniqueId().toString());
        placeholders.put("message", ChatFormatter.sanitiseMessage(rawMessage, service.allowFormattingInMessages()));
        // Staff tag: OP players get the configured prefix (default red/bold
        // "ѕᴛᴀꜰꜰ┃") before their name; everyone else gets an empty string.
        placeholders.put("prefix",
                (player.isOp() && service.isStaffPrefixEnabled()) ? service.getStaffPrefix() : "");
        return placeholders;
    }
}
