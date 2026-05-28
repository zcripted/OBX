package dev.sergeantfuzzy.sfcore.message;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

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
public final class MessageService implements Listener {

    /** Sentinel reply-target for messages sent by the console (can't be replied to). */
    private static final UUID CONSOLE_ID = new UUID(0L, 0L);
    private static final long DRAFT_MS = 60_000L;

    private final Main plugin;
    private final LanguageManager languages;
    private final MessageStore store;
    private final Map<UUID, UUID> replyTarget = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, String> replyTargetName = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, Draft> drafts = new ConcurrentHashMap<UUID, Draft>();

    public MessageService(Main plugin, MessageStore store) {
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

    public void sendMessage(CommandSender sender, String targetName, String message) {
        if (message == null || message.trim().isEmpty()) {
            languages.send(sender, "message.usage");
            return;
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
        store.add(offline.getUniqueId(), new InboxMessage(
                fromConsole ? InboxMessage.CONSOLE : fromId.toString(), fromName, message, System.currentTimeMillis()));
        languages.send(sender, "message.stored",
                one("player", offline.getName() != null ? offline.getName() : targetName));
    }

    private void deliverLive(CommandSender sender, UUID fromId, String fromName, Player to, String message) {
        Map<String, String> sent = new LinkedHashMap<String, String>();
        sent.put("player", to.getName());
        sent.put("message", message);
        languages.send(sender, "message.format.sent", sent);

        Map<String, String> received = new LinkedHashMap<String, String>();
        received.put("player", fromName);
        received.put("message", message);
        String line = languages.get(to, "message.format.received", received);

        if (!fromId.equals(CONSOLE_ID)) {
            replyTarget.put(to.getUniqueId(), fromId);
            replyTargetName.put(to.getUniqueId(), fromName);
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
    }

    // ── /rply ─────────────────────────────────────────────────────────────────

    public void reply(CommandSender sender, String message) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "reply.no-target");
            return;
        }
        Player player = (Player) sender;
        UUID target = replyTarget.get(player.getUniqueId());
        if (target == null) {
            languages.send(player, "reply.no-target");
            return;
        }
        if (target.equals(CONSOLE_ID)) {
            languages.send(player, "reply.cant-console");
            return;
        }
        deliverToId(player, target, replyTargetName.get(player.getUniqueId()), message);
    }

    private void deliverToId(Player from, UUID targetId, String targetName, String message) {
        Player to = Bukkit.getPlayer(targetId);
        if (to != null && to.isOnline()) {
            deliverLive(from, from.getUniqueId(), from.getName(), to, message);
        } else {
            store.add(targetId, new InboxMessage(from.getUniqueId().toString(), from.getName(), message, System.currentTimeMillis()));
            languages.send(from, "message.stored", one("player", targetName == null ? "player" : targetName));
        }
    }

    /** {@code /rply} with no message — opens a 60s reply draft to the last sender. */
    public void startDraft(Player player) {
        UUID target = replyTarget.get(player.getUniqueId());
        if (target == null) {
            languages.send(player, "reply.no-target");
            return;
        }
        if (target.equals(CONSOLE_ID)) {
            languages.send(player, "reply.cant-console");
            return;
        }
        final String name = replyTargetName.get(player.getUniqueId());
        final long stamp = System.currentTimeMillis();
        drafts.put(player.getUniqueId(), new Draft(target, name == null ? "player" : name, stamp));

        String line = languages.get(player, "reply.draft-started", one("player", name == null ? "player" : name));
        String cancelButton = languages.get(player, "reply.cancel-button");
        String cancelHover = languages.get(player, "reply.cancel-hover");
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<ComponentMessenger.InteractiveMessagePart>();
        parts.add(ComponentMessenger.InteractiveMessagePart.plain(line + " "));
        parts.add(ComponentMessenger.InteractiveMessagePart.interactive(cancelButton, Collections.singletonList(cancelHover), "/rply cancel", true));
        ComponentMessenger.sendJoinedHoverMessages(player, parts);

        final UUID id = player.getUniqueId();
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
        int removed = store.clearNonBookmarked(player.getUniqueId());
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) {
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

    public void clear(UUID id) {
        replyTarget.remove(id);
        replyTargetName.remove(id);
        drafts.remove(id);
    }

    private static Map<String, String> one(String key, String value) {
        return Collections.singletonMap(key, value);
    }
}
