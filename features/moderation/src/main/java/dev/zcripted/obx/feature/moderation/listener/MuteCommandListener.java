package dev.zcripted.obx.feature.moderation.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.moderation.service.ModerationService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.Set;

/**
 * Closes the mute bypass where a silenced player keeps talking through commands
 * ({@code /msg}, {@code /mail}, {@code /me}, …). Mute already cancels
 * {@code AsyncPlayerChatEvent} in the chat feature; this blocks the equivalent
 * command-based channels.
 *
 * <p>Only the communication commands listed in
 * {@code moderation.muted-blocked-commands} are blocked — a muted player can
 * still use non-chat commands like {@code /spawn} or {@code /home}, matching how
 * EssentialsX-style mutes behave. The namespace prefix ({@code minecraft:},
 * {@code obx:}) is stripped before matching so it can't be used to dodge the
 * check.
 */
public final class MuteCommandListener implements Listener {

    private final ObxPlugin plugin;
    private final ModerationService service;

    public MuteCommandListener(ObxPlugin plugin, ModerationService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (service != null) {
            service.refreshMuteCache(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (service != null) {
            service.evictMuteCache(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (service == null) {
            return;
        }
        // Cheap, in-memory checks first: only a configured communication command can be blocked,
        // so resolve the label and consult the blocked set BEFORE the (cached) mute lookup — this
        // keeps the common case (any non-comm command) free of even a cache probe.
        String label = baseLabel(event.getMessage());
        if (label.isEmpty()) {
            return;
        }
        Set<String> blocked = service.getMutedBlockedCommands();
        if (!blocked.contains(label)) {
            return;
        }
        Player player = event.getPlayer();
        if (!service.isMuted(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String reason = service.getMuteReason(player.getUniqueId());
        plugin.getLanguageManager().send(player, "player.moderation.mute.command-blocked",
                Placeholders.with("reason", reason));
    }

    /** Extract the lowercase command word from a raw message, stripping '/' and any namespace. */
    private static String baseLabel(String message) {
        if (message == null || message.length() < 2 || message.charAt(0) != '/') {
            return "";
        }
        int space = message.indexOf(' ');
        String label = (space == -1 ? message.substring(1) : message.substring(1, space))
                .toLowerCase(Locale.ENGLISH);
        int colon = label.indexOf(':');
        if (colon >= 0 && colon < label.length() - 1) {
            label = label.substring(colon + 1);
        }
        return label;
    }
}
