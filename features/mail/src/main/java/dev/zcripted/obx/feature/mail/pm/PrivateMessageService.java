package dev.zcripted.obx.feature.mail.pm;
import dev.zcripted.obx.feature.mail.pm.gui.InboxMenu;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.MessageSanitizer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Private-message system: live delivery, reply targets and a 60-second click-to-reply
 * draft (chat-captured), plus an offline inbox queued in {@link MessageStore} and a
 * join notification. The console may send messages "as Console", but players cannot
 * message the console (so console messages never get a reply button).
 */
public final class PrivateMessageService implements Listener {

    /** Sentinel reply-target for messages sent by the console (can't be replied to). */
    private static final UUID CONSOLE_ID = new UUID(0L, 0L);
    /** Sentinel reply-target meaning "the last message was staff chat" — {@code /rply} answers into staff chat. */
    private static final UUID STAFF_CHAT_ID = new UUID(0L, 1L);
    private static final long DRAFT_MS = 60_000L;

    private final ObxPlugin plugin;
    private final LanguageManager languages;
    private final MessageStore store;
    private final Map<UUID, UUID> replyTarget = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, String> replyTargetName = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, Draft> drafts = new ConcurrentHashMap<UUID, Draft>();
    // Reply-channel recency, kept separate so a staff-chat line never clobbers a private
    // reply target (which would silently redirect a /rply into staff chat). /rply answers
    // whichever channel the player most recently *received* a message on.
    private final Map<UUID, Long> pmReplyAt = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> staffChatAt = new ConcurrentHashMap<UUID, Long>();

    public PrivateMessageService(ObxPlugin plugin, MessageStore store) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
        this.store = store;
    }

    private static final class Draft {
        private final UUID targetId;
        private final String targetName;
        private final long at;

        Draft(UUID targetId, String targetName, long at) {
            this.targetId = targetId;
            this.targetName = targetName;
            this.at = at;
        }
    }

    public MessageStore getStore() {
        return store;
    }

    // ── /msg ────────────────────────────────────────────────────────────────────

    /** Hard cap on a single private-message body, so a muted/abusive sender can't flood an inbox. */
    private static final int MAX_PM_LENGTH = 256;

    public void sendMessage(CommandSender sender, String targetName, String message) {
        if (message == null || message.trim().isEmpty()) {
            languages.send(sender, "message.usage");
            return;
        }
        // Defense-in-depth: block muted players here too, not only via the command-blocklist listener
        // (which can be misconfigured). PMs are a chat channel and a mute must cover them.
        if (blockedByMute(sender)) {
            return;
        }
        message = MessageSanitizer.sanitize(sender, message);
        if (message.length() > MAX_PM_LENGTH) {
            message = message.substring(0, MAX_PM_LENGTH);
        }
        if (targetName.equalsIgnoreCase("console") || targetName.equalsIgnoreCase("server")) {
            languages.send(sender, "message.console-blocked");
            return;
        }
        boolean fromConsole = !(sender instanceof Player);
        UUID fromId = fromConsole ? CONSOLE_ID : ((Player) sender).getUniqueId();
        String fromName = fromConsole ? "Console" : sender.getName();
        if (!fromConsole && sender.getName().equalsIgnoreCase(targetName)) {
            languages.send(sender, "message.self");
            return;
        }

        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            deliverLive(sender, fromId, fromName, online, message);
            return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        if (offline == null || (!offline.hasPlayedBefore() && !offline.isOnline())) {
            languages.send(sender, "message.not-found", one("player", targetName));
            return;
        }
        String shownName = offline.getName() != null ? offline.getName() : targetName;
        // Honor /ignore for offline targets too, but fake success so the ignore can't be
        // probed by who-does-and-doesn't-receive.
        if (isIgnored(sender, fromId, offline.getUniqueId())) {
            languages.send(sender, "message.stored", one("player", shownName));
            return;
        }
        store.add(offline.getUniqueId(), new InboxMessage(
                fromConsole ? InboxMessage.CONSOLE : fromId.toString(), fromName, message, System.currentTimeMillis()));
        languages.send(sender, "message.stored", one("player", shownName));
        // Social-spy covers offline-stored PMs too, for consistent moderation visibility.
        notifySpies(fromId, fromName, offline.getUniqueId(), shownName, message);
    }

    /** Whether {@code sender} is currently muted — and if so, sends them the mute notice. */
    private boolean blockedByMute(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return false; // console is never muted
        }
        dev.zcripted.obx.api.moderation.ModerationApi mod =
                plugin.getServiceRegistry().get(dev.zcripted.obx.api.moderation.ModerationApi.class);
        // UUID lookup, not name: name-based checks are bypassable via name changes.
        java.util.UUID uuid = ((Player) sender).getUniqueId();
        if (mod == null || !mod.isMuted(uuid)) {
            return false;
        }
        String reason = mod.getMuteReason(uuid);
        languages.send(sender, "player.moderation.mute.chat-blocked", one("reason", reason == null ? "" : reason));
        return true;
    }

    /** Whether {@code recipientId} is ignoring {@code fromId} (console + bypass perm are never ignored). */
    private boolean isIgnored(CommandSender sender, UUID fromId, UUID recipientId) {
        if (fromId.equals(CONSOLE_ID)) {
            return false;
        }
        if (sender instanceof Player && sender.hasPermission("obx.message.ignore.bypass")) {
            return false;
        }
        dev.zcripted.obx.feature.mail.mail.MailService mail =
                plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.mail.MailService.class);
        return mail != null && mail.isIgnoring(recipientId, fromId);
    }

    private void deliverLive(CommandSender sender, UUID fromId, String fromName, Player to, String message) {
        Map<String, String> sent = new LinkedHashMap<String, String>();
        sent.put("player", to.getName());
        sent.put("message", message);

        // Ignore enforcement: if the recipient has /ignore'd the sender, fake success (show
        // the normal "sent" line) but don't deliver to the recipient — so the ignore can't be
        // detected. Console can't be ignored; bypass-perm senders get through. Social-spy STILL
        // fires (spies are invisible to the sender, so it can't reveal the ignore, and moderators
        // should see ignored PMs — matching the offline-delivery path).
        if (isIgnored(sender, fromId, to.getUniqueId())) {
            languages.send(sender, "message.format.sent", sent);
            notifySpies(fromId, fromName, to.getUniqueId(), to.getName(), message);
            return;
        }

        languages.send(sender, "message.format.sent", sent);

        Map<String, String> received = new LinkedHashMap<String, String>();
        received.put("player", fromName);
        received.put("message", message);
        String line = languages.get(to, "message.format.received", received);

        if (!fromId.equals(CONSOLE_ID)) {
            replyTarget.put(to.getUniqueId(), fromId);
            replyTargetName.put(to.getUniqueId(), fromName);
            pmReplyAt.put(to.getUniqueId(), System.currentTimeMillis());
            // Line 1: the message itself.
            to.sendMessage(line);
            // Line 2: the reply button on its own line.
            String lead = languages.get(to, "message.reply-lead");
            String button = languages.get(to, "message.reply-button");
            String hover = languages.get(to, "message.reply-hover", one("player", fromName));
            List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<ComponentMessenger.InteractiveMessagePart>();
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(lead));
            parts.add(ComponentMessenger.InteractiveMessagePart.interactive(button, Collections.singletonList(hover), "/rply", true));
            ComponentMessenger.sendJoinedHoverMessages(to, parts);
        } else {
            to.sendMessage(line);
        }

        notifySpies(fromId, fromName, to.getUniqueId(), to.getName(), message);
    }

    /** Broadcasts a copy of a delivered PM to online social-spy staff (never the sender or recipient). */
    private void notifySpies(UUID fromId, String fromName, UUID toId, String toName, String message) {
        dev.zcripted.obx.feature.mail.mail.MailService mail = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.mail.MailService.class);
        if (mail == null) {
            return;
        }
        java.util.Set<UUID> spies = mail.getSocialSpies();
        if (spies == null || spies.isEmpty()) {
            return;
        }
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("sender", fromName);
        ph.put("receiver", toName);
        ph.put("message", message);
        for (UUID spyId : spies) {
            if (spyId.equals(fromId) || spyId.equals(toId)) {
                continue; // never spy on a message you sent or received
            }
            Player spy = Bukkit.getPlayer(spyId);
            if (spy != null && spy.isOnline()) {
                languages.send(spy, "message.socialspy.format", ph);
            }
        }
    }

    // ── /rply ─────────────────────────────────────────────────────────────────

    public void reply(CommandSender sender, String message) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "reply.no-target");
            return;
        }
        Player player = (Player) sender;
        UUID id = player.getUniqueId();
        // Answer whichever channel was most recently received on. Staff-chat receipt is
        // tracked separately so it never overwrites a private reply target.
        if (staffChatIsMostRecent(id)) {
            deliverToId(player, STAFF_CHAT_ID, null, message);
            return;
        }
        UUID target = replyTarget.get(id);
        if (target == null) {
            languages.send(player, "reply.no-target");
            return;
        }
        if (target.equals(CONSOLE_ID)) {
            languages.send(player, "reply.cant-console");
            return;
        }
        deliverToId(player, target, replyTargetName.get(id), message);
    }

    /** True if the player's most recent received message was a staff-chat line. */
    private boolean staffChatIsMostRecent(UUID id) {
        long sc = staffChatAt.getOrDefault(id, 0L);
        return sc > 0L && sc >= pmReplyAt.getOrDefault(id, 0L);
    }

    private void deliverToId(Player from, UUID targetId, String targetName, String message) {
        message = MessageSanitizer.sanitize(from, message);
        if (STAFF_CHAT_ID.equals(targetId)) {
            // The most recent message was staff chat — reply straight back into staff chat.
            if (!from.hasPermission("obx.staffchat")) {
                languages.send(from, "core.no-permission");
                return;
            }
            dev.zcripted.obx.feature.mail.staffchat.StaffChatService staffChat =
                    plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.staffchat.StaffChatService.class);
            if (staffChat != null) {
                staffChat.dispatch(from, message);
            }
            return;
        }
        Player to = Bukkit.getPlayer(targetId);
        if (to != null && to.isOnline()) {
            deliverLive(from, from.getUniqueId(), from.getName(), to, message);
        } else {
            store.add(targetId, new InboxMessage(from.getUniqueId().toString(), from.getName(), message, System.currentTimeMillis()));
            languages.send(from, "message.stored", one("player", targetName == null ? "player" : targetName));
        }
    }

    /**
     * Records that {@code recipient} just received a staff-chat line, so a following
     * {@code /rply} answers into staff chat — but only while staff chat is their most
     * recent message. This is kept separate from the private reply target so it never
     * silently redirects an in-progress private conversation into staff chat.
     */
    public void markStaffChatReplyTarget(Player recipient) {
        staffChatAt.put(recipient.getUniqueId(), System.currentTimeMillis());
    }

    /** {@code /rply} with no message — opens a 60s reply draft to the most recent channel. */
    public void startDraft(Player player) {
        final UUID id = player.getUniqueId();
        final UUID target;
        final String name;
        if (staffChatIsMostRecent(id)) {
            target = STAFF_CHAT_ID;
            name = languages.get(player, "messaging.staffchat.reply-label");
        } else {
            target = replyTarget.get(id);
            if (target == null) {
                languages.send(player, "reply.no-target");
                return;
            }
            if (target.equals(CONSOLE_ID)) {
                languages.send(player, "reply.cant-console");
                return;
            }
            name = replyTargetName.get(id);
        }
        final long stamp = System.currentTimeMillis();
        drafts.put(player.getUniqueId(), new Draft(target, name == null ? "player" : name, stamp));

        String line = languages.get(player, "reply.draft-started", one("player", name == null ? "player" : name));
        String cancelButton = languages.get(player, "reply.cancel-button");
        String cancelHover = languages.get(player, "reply.cancel-hover");
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<ComponentMessenger.InteractiveMessagePart>();
        parts.add(ComponentMessenger.InteractiveMessagePart.plain(line + " "));
        parts.add(ComponentMessenger.InteractiveMessagePart.interactive(cancelButton, Collections.singletonList(cancelHover), "/rply cancel", true));
        ComponentMessenger.sendJoinedHoverMessages(player, parts);

        plugin.getSchedulerAdapter().runLater(new Runnable() {
            @Override
            public void run() {
                Draft current = drafts.get(id);
                if (current != null && current.at == stamp) {
                    drafts.remove(id);
                    Player online = Bukkit.getPlayer(id);
                    if (online != null && online.isOnline()) {
                        languages.send(online, "reply.draft-expired");
                    }
                }
            }
        }, DRAFT_MS / 50L);
    }

    public void cancelDraft(Player player, boolean notify) {
        if (drafts.remove(player.getUniqueId()) != null && notify) {
            languages.send(player, "reply.draft-cancelled");
        }
    }

    // ── Inbox ─────────────────────────────────────────────────────────────────

    public List<InboxMessage> inbox(UUID id) {
        return store.get(id);
    }

    public void readMessage(Player player, InboxMessage message) {
        if (message == null) {
            return;
        }
        // Mark as read (persisted) the moment the player opens it.
        store.markRead(player.getUniqueId(), message.getId());
        message.setRead(true);
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("player", message.getSenderName());
        ph.put("date", message.dateLabel());
        ph.put("time", message.timeLabel());
        ph.put("message", message.getText());
        player.closeInventory();
        for (String line : languages.list(player, "inbox.read", ph)) {
            player.sendMessage(line);
        }
        // Allow /rply to answer a read inbox message (unless it was from console).
        if (!message.isFromConsole()) {
            try {
                UUID senderId = UUID.fromString(message.getSenderId());
                replyTarget.put(player.getUniqueId(), senderId);
                replyTargetName.put(player.getUniqueId(), message.getSenderName());
                pmReplyAt.put(player.getUniqueId(), System.currentTimeMillis());
            } catch (IllegalArgumentException ignored) {
                // malformed id — skip reply target
            }
        }
    }

    /** Right-click an inbox entry — delete it and refresh the menu. */
    public void deleteMessage(Player player, InboxMessage message) {
        if (message == null) {
            return;
        }
        store.delete(player.getUniqueId(), message.getId());
        languages.send(player, "inbox.deleted");
        reopenInbox(player);
    }

    /** Shift-click an inbox entry — toggle its bookmark (kept when clearing) and refresh. */
    public void toggleBookmark(Player player, InboxMessage message) {
        if (message == null) {
            return;
        }
        boolean now = !message.isBookmarked();
        store.setBookmarked(player.getUniqueId(), message.getId(), now);
        message.setBookmarked(now);
        languages.send(player, now ? "inbox.bookmarked" : "inbox.unbookmarked");
        reopenInbox(player);
    }

    /** Clear all non-bookmarked inbox messages and refresh the menu. */
    public void clearInbox(Player player) {
        int total = store.count(player.getUniqueId());
        if (total == 0) {
            // Nothing to clear — surface a formal error rather than a fake "cleared 0" success.
            languages.send(player, "inbox.clear-empty");
            reopenInbox(player);
            return;
        }
        int removed = store.clearNonBookmarked(player.getUniqueId());
        if (removed == 0) {
            // Inbox isn't empty, but everything left is bookmarked (protected from clearing).
            languages.send(player, "inbox.clear-bookmarked-only");
            reopenInbox(player);
            return;
        }
        languages.send(player, "inbox.cleared", one("count", Integer.toString(removed)));
        reopenInbox(player);
    }

    private void reopenInbox(final Player player) {
        plugin.getSchedulerAdapter().runAtEntity(player, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    InboxMenu.open(plugin, player);
                }
            }
        });
    }

    // ── Listeners ───────────────────────────────────────────────────────────────

    // HIGH (not HIGHEST): the chat formatter (ChatManagementListener) runs at HIGHEST and
    // broadcasts the message to all recipients, so a draft/staff-chat-toggle redirect must
    // cancel the event *before* it — otherwise a toggled staff message would also leak out
    // as normal chat to non-staff players.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) {
            // No reply draft pending — if the player has staff-chat toggle mode on, route
            // their normal chat into staff chat instead. (A draft always takes precedence.)
            final dev.zcripted.obx.feature.mail.staffchat.StaffChatService staffChat =
                    plugin.getServiceRegistry().get(dev.zcripted.obx.feature.mail.staffchat.StaffChatService.class);
            if (staffChat != null && staffChat.isToggled(player)) {
                final String toggledMessage = event.getMessage();
                event.setCancelled(true);
                plugin.getSchedulerAdapter().runAtEntity(player, new Runnable() {
                    @Override
                    public void run() {
                        staffChat.dispatch(player, toggledMessage);
                    }
                });
            }
            return;
        }
        final String message = event.getMessage();
        event.setCancelled(true);
        drafts.remove(player.getUniqueId());
        plugin.getSchedulerAdapter().runAtEntity(player, new Runnable() {
            @Override
            public void run() {
                if (message.trim().equalsIgnoreCase("cancel")) {
                    languages.send(player, "reply.draft-cancelled");
                    return;
                }
                // Mute gate: the draft-reply path delivers like /msg and must be
                // blocked for muted players exactly like the direct path.
                if (blockedByMute(player)) {
                    return;
                }
                deliverToId(player, draft.targetId, draft.targetName, message);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final int count = store.count(player.getUniqueId());
        if (count <= 0) {
            return;
        }
        plugin.getSchedulerAdapter().runLater(new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                String line = languages.get(player, "inbox.join-notify", one("count", Integer.toString(count)));
                String button = languages.get(player, "inbox.open-button");
                String hover = languages.get(player, "inbox.open-hover");
                List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<ComponentMessenger.InteractiveMessagePart>();
                parts.add(ComponentMessenger.InteractiveMessagePart.plain(line + " "));
                parts.add(ComponentMessenger.InteractiveMessagePart.interactive(button, Collections.singletonList(hover), "/inbox", true));
                ComponentMessenger.sendJoinedHoverMessages(player, parts);
            }
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Drop reply targets/drafts so a reconnecting player can't /rply to a stale target.
        clear(event.getPlayer().getUniqueId());
    }

    public void clear(UUID id) {
        replyTarget.remove(id);
        replyTargetName.remove(id);
        drafts.remove(id);
        pmReplyAt.remove(id);
        staffChatAt.remove(id);
    }

    private static Map<String, String> one(String key, String value) {
        return Collections.singletonMap(key, value);
    }
}
