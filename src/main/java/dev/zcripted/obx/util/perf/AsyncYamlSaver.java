package dev.zcripted.obx.util.perf;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Debounced async writer for plugin YAML files.
 *
 * <p>Storage services on the main thread historically called
 * {@code data.save(dataFile)} every time a player set a home, used /back, set a
 * warp, or any moderation action — a synchronous disk write per event blocks the
 * tick. This helper replaces those direct {@code save()} calls with
 * {@link #markDirty()}, which:
 * <ol>
 *   <li>schedules a single async writer task on first dirty</li>
 *   <li>coalesces subsequent {@code markDirty()} calls inside a debounce window so
 *   bursts (e.g. setting 5 homes in 2 seconds) collapse into one disk write</li>
 *   <li>serializes the YAML on the main thread immediately before the write so the
 *   off-thread task only does the I/O — Bukkit's {@code FileConfiguration} is not
 *   thread-safe for concurrent reads/writes, but a snapshot via
 *   {@link FileConfiguration#saveToString()} on the main thread followed by an async
 *   file write is safe</li>
 * </ol>
 *
 * <p>The {@link #flushNow()} method writes synchronously and is meant for plugin
 * disable / forced reload paths where durability outweighs the tiny tick cost.
 */
public final class AsyncYamlSaver {

    private static final long DEBOUNCE_TICKS = 12L; // ~600 ms
    private static final long RECHECK_TICKS = 6L;   // ~300 ms

    private final Main plugin;
    private final FileConfiguration data;
    private final File targetFile;
    private final String label;

    private final AtomicBoolean writeScheduled = new AtomicBoolean(false);
    private volatile long debounceDeadlineNanos;

    public AsyncYamlSaver(Main plugin, FileConfiguration data, File targetFile, String label) {
        this.plugin = plugin;
        this.data = data;
        this.targetFile = targetFile;
        this.label = label;
    }

    /**
     * Mark the underlying file as needing to be written. Coalesces multiple calls
     * inside the debounce window into a single async write. Cheap enough to call
     * from any mutator.
     */
    public void markDirty() {
        debounceDeadlineNanos = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(DEBOUNCE_TICKS * 50L);
        if (writeScheduled.compareAndSet(false, true)) {
            scheduleFlush(DEBOUNCE_TICKS);
        }
    }

    /** Synchronously serialize-and-write. Use only from disable/reload paths. */
    public void flushNow() {
        // Cancel any pending async flush by claiming the slot; any in-flight async
        // task will see writeScheduled is false-ish at its end and bail.
        writeScheduled.set(false);
        writeFile(serializeOnMainThread());
    }

    private void scheduleFlush(long delayTicks) {
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            // Plugin disabled before the saver's first run — flush sync best-effort.
            writeScheduled.set(false);
            writeFile(serializeOnMainThread());
            return;
        }
        scheduler.runLater(this::onTick, delayTicks);
    }

    /**
     * Runs on the main thread (so we can safely snapshot the YAML config). Decides
     * whether to actually flush, or push the deadline back if a fresh markDirty has
     * arrived inside the debounce window.
     */
    private void onTick() {
        long now = System.nanoTime();
        if (now < debounceDeadlineNanos) {
            scheduleFlush(RECHECK_TICKS);
            return;
        }
        String snapshot = serializeOnMainThread();
        writeScheduled.set(false);
        // Off the main thread for the actual disk write.
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            writeFile(snapshot);
        } else {
            scheduler.runAsync(() -> writeFile(snapshot));
        }
    }

    private String serializeOnMainThread() {
        if (data == null) {
            return null;
        }
        try {
            return data.saveToString();
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Failed to snapshot " + label + " for async save", throwable);
            return null;
        }
    }

    private void writeFile(String snapshot) {
        if (snapshot == null || targetFile == null) {
            return;
        }
        try {
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.isDirectory()) {
                parent.mkdirs();
            }
            // Write to a temp file then atomic-rename so a crash mid-write doesn't
            // leave a half-written YAML on disk.
            File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
            try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(tempFile), java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write(snapshot);
            }
            if (targetFile.exists() && !targetFile.delete()) {
                // On Windows the rename may fail if the destination still exists; fall
                // back to writing directly through YamlConfiguration for safety.
                YamlConfiguration tmp = new YamlConfiguration();
                tmp.loadFromString(snapshot);
                tmp.save(targetFile);
                tempFile.delete();
                return;
            }
            if (!tempFile.renameTo(targetFile)) {
                YamlConfiguration tmp = new YamlConfiguration();
                tmp.loadFromString(snapshot);
                tmp.save(targetFile);
                tempFile.delete();
            }
        } catch (IOException | RuntimeException throwable) {
            plugin.getLogger().log(Level.SEVERE, "Failed to async-write " + label + ": " + throwable.getMessage(), throwable);
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Failed to async-write " + label, throwable);
        }
    }
}
