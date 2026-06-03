package dev.zcripted.obx.feature.hologram.packet;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.hologram.service.HologramService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the Netty {@link Channel} backing a player's connection and
 * inserts the OBX hologram handler ahead of the vanilla
 * {@code packet_handler} sink. Removal mirrors the insertion path so quit
 * cleans up reliably.
 *
 * <p>The reflection chain (plan §5.1):
 * {@code CraftPlayer → getHandle() → ServerPlayer → connection field →
 * ServerGamePacketListenerImpl → connection field → Connection → channel field
 * → io.netty.channel.Channel}.
 *
 * <p>Field names changed across Mojang's remapping history; each step tries a
 * short ordered list of candidate names. The chosen name per class is cached
 * forever; future probes are reflection-light.
 */
public final class PacketChannelInjector {

    private static final String HANDLER_NAME = "obx-holograms";
    private static final String ANCHOR_NAME = "packet_handler";

    // Cached resolved field names per class — populated lazily after first success.
    private static volatile String connectionFieldName;
    private static volatile String networkManagerFieldName;
    private static volatile String channelFieldName;

    /** Track injected channels so we don't double-install on respawn-loop quirks. */
    private static final WeakHashMap<Channel, Boolean> INJECTED = new WeakHashMap<>();
    private static final ConcurrentHashMap<Object, Object> FAILED_PROBE_GUARD = new ConcurrentHashMap<>();

    private PacketChannelInjector() {
    }

    public static void inject(OBX plugin, HologramService service, Player player) {
        if (player == null || service == null || !PacketAvailability.isAvailable()) {
            return;
        }
        try {
            Channel channel = resolveChannel(player);
            if (channel == null) {
                return;
            }
            final ChannelPipeline pipeline = channel.pipeline();
            synchronized (INJECTED) {
                if (Boolean.TRUE.equals(INJECTED.get(channel))) {
                    return;
                }
                INJECTED.put(channel, Boolean.TRUE);
            }
            final HologramPacketHandler handler = new HologramPacketHandler(plugin, service, player);
            // Install on the channel's event loop to avoid CME with the
            // packet-handler thread.
            channel.eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (pipeline.get(HANDLER_NAME) != null) {
                            pipeline.remove(HANDLER_NAME);
                        }
                        if (pipeline.get(ANCHOR_NAME) != null) {
                            pipeline.addBefore(ANCHOR_NAME, HANDLER_NAME, handler);
                        } else {
                            pipeline.addLast(HANDLER_NAME, handler);
                        }
                    } catch (Throwable throwable) {
                        PacketAvailability.noteFailure(plugin, throwable);
                    }
                }
            });
        } catch (Throwable throwable) {
            PacketAvailability.noteFailure(plugin, throwable);
        }
    }

    public static void eject(OBX plugin, Player player) {
        if (player == null) {
            return;
        }
        try {
            Channel channel = resolveChannel(player);
            if (channel == null) {
                return;
            }
            final ChannelPipeline pipeline = channel.pipeline();
            synchronized (INJECTED) {
                INJECTED.remove(channel);
            }
            channel.eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (pipeline.get(HANDLER_NAME) != null) {
                            pipeline.remove(HANDLER_NAME);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable throwable) {
            PacketAvailability.noteFailure(plugin, throwable);
        }
    }

    private static Channel resolveChannel(Player player) throws Throwable {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = readField(handle, connectionFieldName, CONNECTION_CANDIDATES, (s) -> connectionFieldName = s);
        if (connection == null) {
            return null;
        }
        Object networkManager = readField(connection, networkManagerFieldName, NETWORK_MANAGER_CANDIDATES, (s) -> networkManagerFieldName = s);
        if (networkManager == null) {
            return null;
        }
        Object channel = readField(networkManager, channelFieldName, CHANNEL_CANDIDATES, (s) -> channelFieldName = s);
        return channel instanceof Channel ? (Channel) channel : null;
    }

    private static final List<String> CONNECTION_CANDIDATES = Arrays.asList(
            "connection", "playerConnection", "f", "c", "b");
    private static final List<String> NETWORK_MANAGER_CANDIDATES = Arrays.asList(
            "connection", "networkManager", "h", "c", "a");
    private static final List<String> CHANNEL_CANDIDATES = Arrays.asList(
            "channel", "k", "m", "f");

    private interface NameSetter {
        void set(String name);
    }

    private static Object readField(Object target, String chosen, List<String> candidates, NameSetter onSuccess) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        if (chosen != null) {
            Object value = tryField(target, type, chosen);
            if (value != null) {
                return value;
            }
        }
        for (String name : candidates) {
            Object value = tryField(target, type, name);
            if (value != null) {
                onSuccess.set(name);
                return value;
            }
        }
        // Inherited fields — walk the superclass chain.
        Class<?> walker = type.getSuperclass();
        while (walker != null && walker != Object.class) {
            for (String name : candidates) {
                Object value = tryField(target, walker, name);
                if (value != null) {
                    onSuccess.set(name);
                    return value;
                }
            }
            walker = walker.getSuperclass();
        }
        return null;
    }

    private static Object tryField(Object target, Class<?> declaring, String name) {
        try {
            Field field = declaring.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
