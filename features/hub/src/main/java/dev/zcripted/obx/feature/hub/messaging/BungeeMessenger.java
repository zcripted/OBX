package dev.zcripted.obx.feature.hub.messaging;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hub.service.HubService;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BungeeCord / Velocity plugin-message integration for the hub. Handles two
 * subchannels:
 * <ul>
 *   <li><b>Connect</b> — sends a player to another proxied server.</li>
 *   <li><b>PlayerCount</b> — requests the online count for a target server;
 *       the response is cached for {@link HubService#selectorPingCacheSeconds()}
 *       seconds to avoid flooding the proxy.</li>
 * </ul>
 *
 * <p>The plugin channel is registered as {@code BungeeCord} (the legacy alias
 * Velocity recognises when {@code bungee-plugin-message-channel: true} is
 * enabled on the proxy). The channel ID is shared across both messengers.
 */
public final class BungeeMessenger implements PluginMessageListener, Listener {

    /** BungeeCord / Velocity-compatible plugin-message channel. */
    public static final String CHANNEL = "BungeeCord";

    private final ObxPlugin plugin;
    private final HubService hubService;

    /** Latest known online count keyed by lowercase server id. -1 means unknown. */
    private final Map<String, Integer> onlineCounts = new ConcurrentHashMap<>();

    /** When the cached online count was last refreshed (epoch millis). */
    private final Map<String, Long> onlineCountsTimestamp = new ConcurrentHashMap<>();

    /** UUID of the last player we used to send a PlayerCount request, kept for
     *  diagnostic logging. Not load-bearing. */
    private volatile UUID lastQueryUuid;

    private boolean registered;

    public BungeeMessenger(ObxPlugin plugin, HubService hubService) {
        this.plugin = plugin;
        this.hubService = hubService;
    }

    public void register() {
        if (registered) {
            return;
        }
        try {
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
            registered = true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[Hub] Could not register BungeeCord channel: " + throwable.getMessage());
        }
    }

    public void unregister() {
        if (!registered) {
            return;
        }
        try {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        } catch (Throwable ignored) {
        }
        registered = false;
    }

    public boolean isRegistered() {
        return registered;
    }

    // ── Connect ────────────────────────────────────────────────────────

    /**
     * Sends {@code player} to the proxied server {@code serverId}. No-op if
     * the channel isn't registered.
     */
    public void connect(Player player, String serverId) {
        if (player == null || serverId == null || serverId.isEmpty() || !registered) {
            return;
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF("Connect");
            out.writeUTF(serverId);
            player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
        } catch (IOException exception) {
            plugin.getLogger().warning("[Hub] Connect message failed for " + player.getName()
                    + " → " + serverId + ": " + exception.getMessage());
        }
    }

    // ── PlayerCount cache ──────────────────────────────────────────────

    public int cachedOnline(String serverId) {
        if (serverId == null) {
            return -1;
        }
        Integer value = onlineCounts.get(serverId.toLowerCase(java.util.Locale.ENGLISH));
        return value == null ? -1 : value;
    }

    public boolean isCacheFresh(String serverId) {
        if (serverId == null) {
            return false;
        }
        Long ts = onlineCountsTimestamp.get(serverId.toLowerCase(java.util.Locale.ENGLISH));
        if (ts == null) {
            return false;
        }
        long ageMillis = System.currentTimeMillis() - ts;
        return ageMillis < hubService.selectorPingCacheSeconds() * 1000L;
    }

    /**
     * Issues a PlayerCount request for each server, using {@code requester} as
     * the carrier. Plugin messages need at least one online player on this
     * server to leave the JVM — Bukkit refuses to flush messages from an
     * empty sender list.
     *
     * <p>Cached responses younger than the configured TTL are reused.
     */
    public void requestCounts(Player requester, Collection<String> serverIds) {
        if (!registered || requester == null || serverIds == null || serverIds.isEmpty()) {
            return;
        }
        lastQueryUuid = requester.getUniqueId();
        for (String serverId : serverIds) {
            if (serverId == null || serverId.isEmpty()) {
                continue;
            }
            if (isCacheFresh(serverId)) {
                continue;
            }
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                 DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeUTF("PlayerCount");
                out.writeUTF(serverId);
                requester.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
            } catch (IOException exception) {
                plugin.getLogger().fine("[Hub] PlayerCount request failed for " + serverId + ": "
                        + exception.getMessage());
            }
        }
    }

    /**
     * Schedules a {@link Runnable} to run after {@code delayTicks} so callers
     * can open the selector inventory once the proxy responses have a chance
     * to arrive. Convenience wrapper around {@link SchedulerAdapter#runLater}.
     */
    public void scheduleAfter(long delayTicks, Runnable task) {
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            task.run();
            return;
        }
        scheduler.runLater(task, delayTicks);
    }

    // ── Incoming response handling ─────────────────────────────────────

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(message);
             DataInputStream in = new DataInputStream(bytes)) {
            String subChannel = in.readUTF();
            if ("PlayerCount".equals(subChannel)) {
                String serverId = in.readUTF();
                int count = in.readInt();
                String key = serverId.toLowerCase(java.util.Locale.ENGLISH);
                onlineCounts.put(key, count);
                onlineCountsTimestamp.put(key, System.currentTimeMillis());
            }
            // Other subchannels (GetServer, GetServers, etc.) intentionally
            // ignored here — the hub doesn't depend on them yet.
        } catch (IOException ignored) {
            // Malformed payloads are dropped silently; the proxy may send
            // unrelated traffic on the same channel.
        }
    }

    /** Diagnostic helper used by /obx diagnostics integrations. */
    public Map<String, Integer> snapshotCounts() {
        return new HashMap<>(onlineCounts);
    }
}
