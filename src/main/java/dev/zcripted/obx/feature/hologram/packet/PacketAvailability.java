package dev.zcripted.obx.feature.hologram.packet;

import dev.zcripted.obx.OBX;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Cached probe result + failure-rate-limited logger for the Netty channel
 * injector (plan §5). Returns {@code true} when the
 * {@code CraftPlayer / ServerPlayer / Connection / Channel} reflection chain
 * resolves without exception on the host server; subsequent calls return
 * the cached result.
 *
 * <p>Any reflection failure flips the cache to {@code false} once and logs
 * a single informational line. After that, the renderer falls back to
 * raycast targeting (plan §5 / §10.4) and the system keeps working.
 */
public final class PacketAvailability {

    private static volatile Boolean cached;
    private static volatile String reason = "not probed";
    private static final AtomicLong lastFailureLogNanos = new AtomicLong(0L);

    private PacketAvailability() {
    }

    public static synchronized boolean probe(OBX plugin) {
        if (cached != null) {
            return cached;
        }
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            int dot = pkg.lastIndexOf('.');
            if (dot < 0) {
                reason = "Unrecognized server package: " + pkg;
                cached = Boolean.FALSE;
                return false;
            }
            String craftPlayerName = pkg + ".entity.CraftPlayer";
            Class.forName(craftPlayerName);
            Class.forName("io.netty.channel.Channel");
            Class.forName("io.netty.channel.ChannelDuplexHandler");
        } catch (Throwable throwable) {
            reason = "Reflection probe failed: " + throwable.getClass().getSimpleName()
                    + " (" + throwable.getMessage() + ")";
            cached = Boolean.FALSE;
            return false;
        }
        reason = "Netty channel injector available.";
        cached = Boolean.TRUE;
        return true;
    }

    public static boolean isAvailable() {
        return Boolean.TRUE.equals(cached);
    }

    public static String describe() {
        return reason;
    }

    public static synchronized void reset() {
        cached = null;
        reason = "not probed";
    }

    /**
     * Rate-limited failure logger — emits at most one warning every 60 seconds
     * even under packet storms, so a single misbehaving entity can't flood
     * the log.
     */
    public static void noteFailure(OBX plugin, Throwable throwable) {
        if (plugin == null || throwable == null) {
            return;
        }
        long now = System.nanoTime();
        long last = lastFailureLogNanos.get();
        if (now - last < 60_000_000_000L) {
            return;
        }
        if (!lastFailureLogNanos.compareAndSet(last, now)) {
            return;
        }
        plugin.getLogger().warning("[Holograms] Packet layer error: "
                + throwable.getClass().getSimpleName() + " — " + throwable.getMessage());
    }
}
