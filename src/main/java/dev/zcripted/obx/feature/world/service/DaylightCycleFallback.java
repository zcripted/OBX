package dev.zcripted.obx.feature.world.service;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DaylightCycleFallback {

    private static final Map<UUID, Long> frozenTimes = new ConcurrentHashMap<>();
    private static volatile SchedulerAdapter.CancellableTask task;

    private DaylightCycleFallback() {
    }

    public static boolean isFrozen(World world) {
        return world != null && frozenTimes.containsKey(world.getUID());
    }

    public static void setFrozen(OBX plugin, World world, boolean frozen) {
        if (plugin == null || world == null) {
            return;
        }
        if (frozen) {
            frozenTimes.put(world.getUID(), world.getTime());
            ensureTask(plugin);
            return;
        }
        frozenTimes.remove(world.getUID());
        stopTaskIfIdle();
    }

    private static void ensureTask(OBX plugin) {
        if (task != null && !task.isCancelled()) {
            return;
        }
        task = plugin.getSchedulerAdapter().runRepeating(() -> {
            if (frozenTimes.isEmpty()) {
                stopTaskIfIdle();
                return;
            }
            for (Map.Entry<UUID, Long> entry : frozenTimes.entrySet()) {
                World world = Bukkit.getWorld(entry.getKey());
                if (world == null) {
                    frozenTimes.remove(entry.getKey());
                    continue;
                }
                world.setTime(entry.getValue());
            }
        }, 1L, 1L);
    }

    private static void stopTaskIfIdle() {
        if (frozenTimes.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }
}
