package dev.zcripted.obx.feature.mail.staffchat;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.feature.mail.pm.PrivateMessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Staff-chat dispatch plus a per-player toggle mode.
 *
 * <p>{@link #dispatch} delivers a message to every online holder of
 * {@code obx.staffchat} (and the console) using the {@code messaging.staffchat.line}
 * format, and marks each recipient's reply target (via
 * {@link PrivateMessageService#markStaffChatReplyTarget}) so a following {@code /rply}
 * answers straight back into staff chat — regardless of the replier's toggle state.</p>
 *
 * <p>Toggle mode (per player) redirects a staff member's normal chat into staff chat
 * until turned off; the chat redirect itself is wired in
 * {@link PrivateMessageService} (its chat listener), which calls {@link #isToggled} and
 * {@link #dispatch} so there is a single owner of the chat event.</p>
 */
public final class StaffChatService implements Listener {

    private final ObxPlugin plugin;
    private final LanguageManager languages;
    private final Set<UUID> toggled = ConcurrentHashMap.newKeySet();

    public StaffChatService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
    }

    public boolean isToggled(Player player) {
        return player != null && toggled.contains(player.getUniqueId());
    }

    /** Flips toggle mode for {@code player} and returns the resulting state. */
    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (toggled.remove(id)) {
            return false;
        }
        toggled.add(id);
        return true;
    }

    /**
     * Sends {@code message} to every online staff member (and the console) as a
     * staff-chat line, and marks each online recipient so {@code /rply} replies back to
     * staff chat.
     */
    public void dispatch(CommandSender sender, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        message = dev.zcripted.obx.util.text.MessageSanitizer.sanitize(sender, message);
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("sender", sender instanceof Player ? ((Player) sender).getName() : sender.getName());
        ph.put("message", message);
        PrivateMessageService pm = plugin.getServiceRegistry().get(PrivateMessageService.class);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("obx.staffchat")) {
                languages.send(online, "messaging.staffchat.line", ph);
                if (pm != null) {
                    pm.markStaffChatReplyTarget(online);
                }
            }
        }
        languages.send(Bukkit.getConsoleSender(), "messaging.staffchat.line", ph);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        toggled.remove(event.getPlayer().getUniqueId());
    }
}
