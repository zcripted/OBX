package dev.zcripted.obx.feature.staff.gui;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks open {@link InvSeeMenu} instances and refreshes each one against the
 * target player's live inventory at a fixed cadence so the viewer sees
 * inventory edits / drops / pickups as they happen on the target's side.
 *
 * <p>The refresh task runs every 5 server ticks (≈ 4 Hz, 250 ms cadence) — fast
 * enough to feel "live" to a human observer without spamming
 * {@link org.bukkit.inventory.Inventory#setItem} packets when nothing
 * changed (the per-slot diff in {@link InvSeeMenu#refreshFromTarget} skips
 * unchanged slots).
 *
 * <p>Folia is handled via {@link SchedulerAdapter#runRepeating} — the global
 * region task is fine here because the only Bukkit calls inside the task body
 * are read-only inventory snapshots plus per-slot writes into a chest
 * inventory that lives in the global tick anyway.
 */
public final class InvSeeMenuManager {

    /** Refresh cadence in server ticks. 5 ticks = ~250 ms = visually instant. */
    private static final long REFRESH_PERIOD_TICKS = 5L;

    private final ObxPlugin plugin;
    private final Map<UUID, InvSeeMenuHolder> openByViewer = new ConcurrentHashMap<UUID, InvSeeMenuHolder>();
    private dev.zcripted.obx.core.platform.scheduler.CancellableTask refreshTask;

    public InvSeeMenuManager(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (refreshTask != null) {
            return;
        }
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            return;
        }
        refreshTask = scheduler.runRepeating(this::refreshAll,
                REFRESH_PERIOD_TICKS, REFRESH_PERIOD_TICKS);
    }

    public void stop() {
        if (refreshTask != null) {
            try {
                refreshTask.cancel();
            } catch (Throwable ignored) {
            }
            refreshTask = null;
        }
        // Force-close any still-open mirrors so they don't outlive the
        // plugin instance (their slot maps and holders would be stale
        // references after a /reload).
        for (Map.Entry<UUID, InvSeeMenuHolder> entry : new HashMap<UUID, InvSeeMenuHolder>(openByViewer).entrySet()) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null) {
                try {
                    viewer.closeInventory();
                } catch (Throwable ignored) {
                }
            }
        }
        openByViewer.clear();
    }

    public void register(Player viewer, InvSeeMenuHolder holder) {
        if (viewer == null || holder == null) {
            return;
        }
        openByViewer.put(viewer.getUniqueId(), holder);
    }

    public void unregister(UUID viewerUuid) {
        if (viewerUuid != null) {
            openByViewer.remove(viewerUuid);
        }
    }

    public boolean isOpen(UUID viewerUuid) {
        return viewerUuid != null && openByViewer.containsKey(viewerUuid);
    }

    private void refreshAll() {
        if (openByViewer.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, InvSeeMenuHolder>> it = openByViewer.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, InvSeeMenuHolder> entry = it.next();
            UUID viewerUuid = entry.getKey();
            InvSeeMenuHolder holder = entry.getValue();
            Player viewer = Bukkit.getPlayer(viewerUuid);
            if (viewer == null || !viewer.isOnline()) {
                it.remove();
                continue;
            }
            Player target = Bukkit.getPlayer(holder.getTargetUuid());
            if (target == null || !target.isOnline()) {
                // Target logged off mid-view — close the GUI and drop the
                // entry so the viewer doesn't sit on a frozen snapshot.
                try {
                    viewer.closeInventory();
                } catch (Throwable ignored) {
                }
                it.remove();
                continue;
            }
            InvSeeMenu.refreshFromTarget(holder.getInventory(), target, holder.getSlotMap());
        }
    }
}