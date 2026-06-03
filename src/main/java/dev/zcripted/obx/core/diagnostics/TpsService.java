package dev.zcripted.obx.core.diagnostics;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 * Self-measured server performance probe. Schedules a tick-aligned task that records
 * a timestamp on every server tick so TPS values can be derived purely from elapsed
 * wall-clock time, independent of Paper / Spigot reflection differences.
 *
 * <p>Three rolling windows (1m / 5m / 15m) are exposed alongside an MSPT estimate
 * computed from the last 100 ticks. The implementation keeps a one-sample-per-second
 * deque to bound memory while staying accurate enough for diagnostic output.
 */
public final class TpsService {

    private static final long SAMPLE_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final long RETENTION_NANOS = TimeUnit.MINUTES.toNanos(15);
    private static final int RECENT_TICK_WINDOW = 100;

    private final ObxPlugin plugin;
    private final Deque<Sample> samples = new ArrayDeque<>();
    private final Deque<Long> recentTickDurationsNanos = new ArrayDeque<>();

    private long startNanos;
    private long tickCounter;
    private long lastTickNanos;
    private long lastSampleNanos;
    private dev.zcripted.obx.core.platform.scheduler.CancellableTask task;

    /**
     * Cached reflection probe for Paper's {@code Server.getAverageTickTime()}, which
     * returns the genuine tick processing time in milliseconds (vs. our inter-tick
     * interval, which is always ~50 ms on a healthy server because that's the tick
     * budget). Resolved lazily on first {@link #mspt()} call. {@link #PAPER_PROBED}
     * guards against repeated lookup attempts on non-Paper servers.
     */
    private static volatile Method PAPER_AVG_TICK_TIME;
    private static volatile boolean PAPER_PROBED;

    public TpsService(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            return;
        }
        startNanos = System.nanoTime();
        tickCounter = 0;
        lastTickNanos = 0;
        lastSampleNanos = 0;
        samples.clear();
        recentTickDurationsNanos.clear();
        task = plugin.getSchedulerAdapter().runRepeating(this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        long now = System.nanoTime();
        tickCounter++;
        if (lastTickNanos > 0) {
            long delta = now - lastTickNanos;
            recentTickDurationsNanos.offerLast(delta);
            while (recentTickDurationsNanos.size() > RECENT_TICK_WINDOW) {
                recentTickDurationsNanos.pollFirst();
            }
        }
        lastTickNanos = now;
        if (lastSampleNanos == 0 || now - lastSampleNanos >= SAMPLE_INTERVAL_NANOS) {
            samples.offerLast(new Sample(now, tickCounter));
            lastSampleNanos = now;
            long cutoff = now - RETENTION_NANOS;
            while (!samples.isEmpty() && samples.peekFirst().timeNanos < cutoff) {
                samples.pollFirst();
            }
        }
    }

    public double tpsForWindow(long durationNanos) {
        if (samples.size() < 2) {
            return 20.0;
        }
        long now = System.nanoTime();
        long target = now - durationNanos;
        Sample baseline = null;
        for (Sample sample : samples) {
            if (sample.timeNanos >= target) {
                baseline = sample;
                break;
            }
        }
        if (baseline == null) {
            baseline = samples.peekFirst();
        }
        Sample latest = samples.peekLast();
        if (baseline == null || latest == null || latest.timeNanos <= baseline.timeNanos) {
            return 20.0;
        }
        double seconds = (latest.timeNanos - baseline.timeNanos) / 1_000_000_000.0;
        if (seconds <= 0) {
            return 20.0;
        }
        double rate = (latest.tickCounter - baseline.tickCounter) / seconds;
        return Math.min(20.0, Math.max(0.0, rate));
    }

    /**
     * Returns the average milliseconds the server spends processing a single tick.
     *
     * <p>On Paper / Folia / PurPur the value comes straight from
     * {@code Server.getAverageTickTime()}, which measures true tick processing time
     * (typically 1–15 ms on a healthy server, climbing toward 50 ms when the server
     * starts to fall behind).
     *
     * <p>On Spigot / CraftBukkit (no such API) this falls back to the inter-tick
     * interval the OBX scheduler observes minus the 50 ms tick budget, giving the
     * <em>excess</em> time over the budget — i.e., how far behind schedule the server
     * is running. A healthy Spigot server reports near 0 ms; a lagging one reports a
     * positive offset that grows with the deficit. This is not the same value Paper
     * reports, but it carries the same semantics: smaller is better, 0 is healthy.
     */
    public double mspt() {
        Double paperValue = paperMspt();
        if (paperValue != null) {
            return paperValue;
        }
        if (recentTickDurationsNanos.isEmpty()) {
            return 0.0;
        }
        long sum = 0;
        for (long delta : recentTickDurationsNanos) {
            sum += delta;
        }
        double avgIntervalMs = (sum / (double) recentTickDurationsNanos.size()) / 1_000_000.0;
        // Subtract the 50 ms tick budget so a healthy server reports ~0 ms instead of
        // ~50 ms (which the user reasonably reads as "lagging"). Clamp to 0 on the low
        // side because nothing useful comes from negative numbers.
        return Math.max(0.0, avgIntervalMs - 50.0);
    }

    /** {@code true} when {@link #mspt()} returned a real Paper measurement, not the fallback. */
    public boolean isMsptFromTickProcessing() {
        return paperMspt() != null;
    }

    private Double paperMspt() {
        if (!PAPER_PROBED) {
            synchronized (TpsService.class) {
                if (!PAPER_PROBED) {
                    PAPER_PROBED = true;
                    try {
                        PAPER_AVG_TICK_TIME = Bukkit.getServer().getClass().getMethod("getAverageTickTime");
                    } catch (NoSuchMethodException ignored) {
                        PAPER_AVG_TICK_TIME = null;
                    } catch (Throwable ignored) {
                        PAPER_AVG_TICK_TIME = null;
                    }
                }
            }
        }
        Method method = PAPER_AVG_TICK_TIME;
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(Bukkit.getServer());
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Throwable ignored) {
            // Hand back null so callers fall through to the interval-based estimate.
        }
        return null;
    }

    public boolean isReady() {
        return samples.size() >= 2;
    }

    public long getStartNanos() {
        return startNanos;
    }

    public long getMeasuredUptimeMillis() {
        if (startNanos == 0) {
            return 0;
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static final class Sample {
        final long timeNanos;
        final long tickCounter;

        Sample(long timeNanos, long tickCounter) {
            this.timeNanos = timeNanos;
            this.tickCounter = tickCounter;
        }
    }
}
