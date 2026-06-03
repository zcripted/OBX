package dev.zcripted.obx.feature.teleport.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pending {@code /tpa} teleport requests. A request is keyed by the <em>target</em>
 * (the player who decides). The target gets a styled message with clickable
 * <b>Accept</b> / <b>Deny</b> words; either click runs {@code /tpaccept} or {@code /tpdeny},
 * which route back here. Requests expire after 60 seconds.
 */
public final class TeleportRequestService {

    private static final long EXPIRE_MS = 60_000L;

    private final ObxPlugin plugin;
    private final LanguageManager languages;
    /** target UUID → pending request from a requester. Most-recent wins. */
    private final Map<UUID, Request> pending = new ConcurrentHashMap<UUID, Request>();

    public TeleportRequestService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
    }

    private static final class Request {
        private final UUID requester;
        private final String requesterName;
        private final long at;

        Request(UUID requester, String requesterName, long at) {
            this.requester = requester;
            this.requesterName = requesterName;
            this.at = at;
        }
    }

    /** Sends a teleport request from {@code requester} to {@code target}. */
    public void send(Player requester, Player target) {
        if (requester.equals(target)) {
            languages.send(requester, "teleport.request.self");
            return;
        }
        final long stamp = System.currentTimeMillis();
        pending.put(target.getUniqueId(), new Request(requester.getUniqueId(), requester.getName(), stamp));

        languages.send(requester, "teleport.request.sent", Collections.singletonMap("player", target.getName()));

        // Line 1: who is requesting.
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("player", requester.getName());
        languages.send(target, "teleport.request.received", ph);

        // Line 2: "Choose to [Accept] or [Deny] this request." on its own line.
        // The [Accept]/[Deny] buttons are structured interactive messages
        // (teleport.request.accept / .deny); {requester} fills the click command.
        java.util.Map<String, String> btnPh = dev.zcripted.obx.util.text.Placeholders.with("requester", requester.getName());
        String lead = languages.get(target, "teleport.request.choose-lead");
        String or = languages.get(target, "teleport.request.choose-or");
        String tail = languages.get(target, "teleport.request.choose-tail");
        java.util.List<ComponentMessenger.InteractiveMessagePart> parts = new java.util.ArrayList<ComponentMessenger.InteractiveMessagePart>();
        parts.add(ComponentMessenger.InteractiveMessagePart.plain(lead + " "));
        parts.add(languages.getInteractivePart(target, "teleport.request.accept", btnPh));
        parts.add(ComponentMessenger.InteractiveMessagePart.plain(" " + or + " "));
        parts.add(languages.getInteractivePart(target, "teleport.request.deny", btnPh));
        parts.add(ComponentMessenger.InteractiveMessagePart.plain(" " + tail));
        ComponentMessenger.sendJoinedHoverMessages(target, parts);

        // Expire after 60s if untouched.
        final UUID targetId = target.getUniqueId();
        plugin.getSchedulerAdapter().runLater(new Runnable() {
            @Override
            public void run() {
                Request current = pending.get(targetId);
                if (current != null && current.at == stamp) {
                    pending.remove(targetId);
                    Player req = Bukkit.getPlayer(current.requester);
                    if (req != null && req.isOnline()) {
                        languages.send(req, "teleport.request.expired", Collections.singletonMap("player", nameOf(targetId)));
                    }
                }
            }
        }, EXPIRE_MS / 50L);
    }

    /** Target accepts: teleport the requester to the target. */
    public void accept(Player target) {
        Request request = pending.remove(target.getUniqueId());
        if (request == null) {
            languages.send(target, "teleport.request.none");
            return;
        }
        Player requester = Bukkit.getPlayer(request.requester);
        if (requester == null || !requester.isOnline()) {
            languages.send(target, "teleport.request.requester-offline");
            return;
        }
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("player", target.getName());
        languages.send(target, "teleport.request.accepted-target", Collections.singletonMap("player", requester.getName()));
        plugin.getDataService().setBack(requester.getUniqueId(), requester.getLocation());
        plugin.getTeleportManager().teleportPlayer(requester, target.getLocation(), "teleport.request.accepted", ph);
    }

    /** Target denies the pending request. */
    public void deny(Player target) {
        Request request = pending.remove(target.getUniqueId());
        if (request == null) {
            languages.send(target, "teleport.request.none");
            return;
        }
        languages.send(target, "teleport.request.denied-target", Collections.singletonMap("player", request.requesterName));
        Player requester = Bukkit.getPlayer(request.requester);
        if (requester != null && requester.isOnline()) {
            languages.send(requester, "teleport.request.denied", Collections.singletonMap("player", target.getName()));
        }
    }

    public void clear(UUID id) {
        pending.remove(id);
    }

    private String nameOf(UUID id) {
        Player player = Bukkit.getPlayer(id);
        return player != null ? player.getName() : "player";
    }
}
