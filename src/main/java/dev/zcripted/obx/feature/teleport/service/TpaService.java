package dev.zcripted.obx.feature.teleport.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TpaService implements Listener {

    public enum RequestType { TO, HERE }

    public static final class Request {
        final UUID sender;
        final UUID receiver;
        final String senderName;
        final String receiverName;
        final RequestType type;
        final long expiresAtMillis;

        Request(UUID sender, String senderName, UUID receiver, String receiverName, RequestType type, long expiresAtMillis) {
            this.sender = sender;
            this.senderName = senderName;
            this.receiver = receiver;
            this.receiverName = receiverName;
            this.type = type;
            this.expiresAtMillis = expiresAtMillis;
        }

        public UUID getSenderUuid() { return sender; }
        public UUID getReceiverUuid() { return receiver; }
        public String getSenderName() { return senderName; }
        public String getReceiverName() { return receiverName; }
        public RequestType getType() { return type; }
        public boolean isExpired() { return System.currentTimeMillis() >= expiresAtMillis; }
        public long getSecondsRemaining() {
            return Math.max(0L, (expiresAtMillis - System.currentTimeMillis() + 999L) / 1000L);
        }
    }

    private final ObxPlugin plugin;
    // Sender → their single outgoing pending request.
    private final Map<UUID, Request> outgoing = new ConcurrentHashMap<>();
    // Receiver → queue of incoming pending requests (most recent at tail).
    private final Map<UUID, Deque<Request>> incoming = new ConcurrentHashMap<>();
    // Players who toggled themselves off — incoming requests bounce.
    private final Set<UUID> toggledOff = ConcurrentHashMap.newKeySet();

    private SchedulerAdapter.CancellableTask sweeper;

    public TpaService(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (sweeper != null) {
            sweeper.cancel();
        }
        sweeper = plugin.getSchedulerAdapter().runRepeating(this::sweepExpired, 200L, 200L);
    }

    public void stop() {
        if (sweeper != null) {
            sweeper.cancel();
            sweeper = null;
        }
        outgoing.clear();
        incoming.clear();
        toggledOff.clear();
    }

    public int getExpirySeconds() {
        return Math.max(5, plugin.getConfig().getInt("tpa.expiry-seconds", 60));
    }

    public boolean isAcceptingRequests(UUID uuid) {
        return uuid != null && !toggledOff.contains(uuid);
    }

    public boolean toggle(UUID uuid) {
        if (uuid == null) {
            return true;
        }
        if (toggledOff.remove(uuid)) {
            return true;
        }
        toggledOff.add(uuid);
        return false;
    }

    public enum SendResult { OK, SAME_PLAYER, ALREADY_PENDING, RECEIVER_OFF }

    public SendResult send(Player sender, Player receiver, RequestType type) {
        if (sender == null || receiver == null || type == null) {
            return SendResult.SAME_PLAYER;
        }
        if (sender.getUniqueId().equals(receiver.getUniqueId())) {
            return SendResult.SAME_PLAYER;
        }
        if (!isAcceptingRequests(receiver.getUniqueId())) {
            return SendResult.RECEIVER_OFF;
        }
        if (outgoing.containsKey(sender.getUniqueId())) {
            Request existing = outgoing.get(sender.getUniqueId());
            if (existing != null && !existing.isExpired()) {
                return SendResult.ALREADY_PENDING;
            }
            remove(existing);
        }
        long expiresAt = System.currentTimeMillis() + getExpirySeconds() * 1000L;
        Request request = new Request(sender.getUniqueId(), sender.getName(), receiver.getUniqueId(), receiver.getName(), type, expiresAt);
        outgoing.put(sender.getUniqueId(), request);
        incoming.computeIfAbsent(receiver.getUniqueId(), k -> new ArrayDeque<>()).addLast(request);
        return SendResult.OK;
    }

    public Request popLatestIncomingFor(UUID receiver) {
        Deque<Request> queue = incoming.get(receiver);
        while (queue != null && !queue.isEmpty()) {
            Request candidate = queue.pollLast();
            if (candidate != null && !candidate.isExpired()) {
                outgoing.remove(candidate.sender);
                if (queue.isEmpty()) {
                    incoming.remove(receiver);
                }
                return candidate;
            }
            if (candidate != null) {
                outgoing.remove(candidate.sender);
            }
        }
        return null;
    }

    public Request popIncomingFromSender(UUID receiver, UUID sender) {
        Deque<Request> queue = incoming.get(receiver);
        if (queue == null) {
            return null;
        }
        Iterator<Request> iterator = queue.iterator();
        while (iterator.hasNext()) {
            Request candidate = iterator.next();
            if (candidate.sender.equals(sender)) {
                iterator.remove();
                outgoing.remove(candidate.sender);
                if (queue.isEmpty()) {
                    incoming.remove(receiver);
                }
                if (candidate.isExpired()) {
                    return null;
                }
                return candidate;
            }
        }
        return null;
    }

    public Request cancelOutgoing(UUID sender) {
        Request request = outgoing.remove(sender);
        if (request == null) {
            return null;
        }
        Deque<Request> queue = incoming.get(request.receiver);
        if (queue != null) {
            queue.remove(request);
            if (queue.isEmpty()) {
                incoming.remove(request.receiver);
            }
        }
        return request.isExpired() ? null : request;
    }

    public Collection<Request> incomingFor(UUID receiver) {
        Deque<Request> queue = incoming.get(receiver);
        if (queue == null) {
            return java.util.Collections.emptyList();
        }
        return new java.util.ArrayList<>(queue);
    }

    private void remove(Request request) {
        if (request == null) {
            return;
        }
        outgoing.remove(request.sender);
        Deque<Request> queue = incoming.get(request.receiver);
        if (queue != null) {
            queue.remove(request);
            if (queue.isEmpty()) {
                incoming.remove(request.receiver);
            }
        }
    }

    private void sweepExpired() {
        Iterator<Map.Entry<UUID, Deque<Request>>> entries = incoming.entrySet().iterator();
        Set<UUID> notify = new HashSet<>();
        while (entries.hasNext()) {
            Map.Entry<UUID, Deque<Request>> entry = entries.next();
            Deque<Request> queue = entry.getValue();
            Iterator<Request> iterator = queue.iterator();
            while (iterator.hasNext()) {
                Request request = iterator.next();
                if (request.isExpired()) {
                    iterator.remove();
                    outgoing.remove(request.sender);
                    notify.add(request.sender);
                }
            }
            if (queue.isEmpty()) {
                entries.remove();
            }
        }
        // No chat notification on expiry to keep the request flow quiet — the
        // sender just discovers the timeout when they try to act on it again.
        notify.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        cancelOutgoing(uuid);
        Deque<Request> queue = incoming.remove(uuid);
        if (queue != null) {
            for (Request request : queue) {
                outgoing.remove(request.sender);
            }
        }
    }
}
