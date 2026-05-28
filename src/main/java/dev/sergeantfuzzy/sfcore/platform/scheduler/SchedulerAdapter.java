package dev.sergeantfuzzy.sfcore.platform.scheduler;

import dev.sergeantfuzzy.sfcore.platform.PlatformInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Folia-aware scheduler abstraction. On non-Folia servers (CraftBukkit, Spigot, Paper,
 * PurPur) every method routes to {@link BukkitScheduler}. On Folia, calls are dispatched
 * via reflection to the appropriate region-aware scheduler:
 * <ul>
 *   <li>Global / non-region work: {@code GlobalRegionScheduler}</li>
 *   <li>Location-bound work: {@code RegionScheduler}</li>
 *   <li>Entity-bound work: {@code EntityScheduler}</li>
 *   <li>Async work: {@code AsyncScheduler}</li>
 * </ul>
 *
 * <p>All Folia classes are referenced exclusively through reflection so SF-Core still
 * compiles to a single JAR against the Spigot 1.12 baseline. The same JAR boots cleanly
 * on Spigot 1.8.8 and on Folia 1.21.x because the reflection probes degrade silently
 * to the Bukkit fallback when the Folia classes are absent.
 *
 * <p>The returned {@link CancellableTask} is intentionally minimal so callers don't have
 * to import {@link BukkitTask} (which doesn't exist on Folia for these methods).
 */
public final class SchedulerAdapter {

    private final Plugin plugin;
    private final boolean folia;
    private final FoliaSchedulers foliaSchedulers;

    public SchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.folia = PlatformInfo.get().hasFoliaScheduler();
        this.foliaSchedulers = folia ? FoliaSchedulers.tryInit() : null;
    }

    public boolean isFolia() {
        return folia && foliaSchedulers != null;
    }

    public CancellableTask runNow(Runnable task) {
        if (isFolia()) {
            Object handle = foliaSchedulers.run(plugin, task);
            return wrapFolia(handle);
        }
        return wrapBukkit(Bukkit.getScheduler().runTask(plugin, task));
    }

    public CancellableTask runLater(Runnable task, long delayTicks) {
        long delay = Math.max(1L, delayTicks);
        if (isFolia()) {
            Object handle = foliaSchedulers.runDelayed(plugin, task, delay);
            return wrapFolia(handle);
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    public CancellableTask runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
        long initial = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        if (isFolia()) {
            Object handle = foliaSchedulers.runAtFixedRate(plugin, task, initial, period);
            return wrapFolia(handle);
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks));
    }

    public CancellableTask runAsync(Runnable task) {
        if (isFolia()) {
            Object handle = foliaSchedulers.runAsync(plugin, task);
            return wrapFolia(handle);
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    public CancellableTask runAsyncLater(Runnable task, long delayTicks) {
        long delayMillis = Math.max(50L, delayTicks * 50L);
        if (isFolia()) {
            Object handle = foliaSchedulers.runAsyncDelayed(plugin, task, delayMillis);
            return wrapFolia(handle);
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks));
    }

    public CancellableTask runAtLocation(Location location, Runnable task) {
        if (isFolia() && location != null) {
            Object handle = foliaSchedulers.runAtRegion(plugin, location, task);
            return wrapFolia(handle);
        }
        return runNow(task);
    }

    public CancellableTask runAtEntity(Entity entity, Runnable task, Runnable retired) {
        if (isFolia() && entity != null) {
            Object handle = foliaSchedulers.runAtEntity(plugin, entity, task, retired);
            return wrapFolia(handle);
        }
        return runNow(task);
    }

    public CancellableTask runAtEntity(Entity entity, Runnable task) {
        return runAtEntity(entity, task, null);
    }

    /**
     * Runs {@code task} after a delay on the entity's region (Folia) or the main
     * thread (otherwise). {@code retired} runs instead if the entity is removed
     * before the delay elapses. Used for safely despawning short-lived holograms.
     */
    public CancellableTask runAtEntityLater(Entity entity, Runnable task, Runnable retired, long delayTicks) {
        long delay = Math.max(1L, delayTicks);
        if (isFolia() && entity != null) {
            Object handle = foliaSchedulers.runAtEntityDelayed(plugin, entity, task, retired, delay);
            return wrapFolia(handle);
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskLater(plugin, task, delay));
    }

    public void cancelAll() {
        if (isFolia()) {
            foliaSchedulers.cancelTasks(plugin);
            return;
        }
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    private static CancellableTask wrapBukkit(BukkitTask task) {
        return new BukkitCancellableTask(task);
    }

    private static CancellableTask wrapFolia(Object foliaTask) {
        return new FoliaCancellableTask(foliaTask);
    }

    /**
     * Minimal task handle returned by all scheduler methods. The Bukkit and Folia task
     * types share no common interface, so this wrapper exposes the operations SF-Core
     * actually needs (cancel + isCancelled).
     */
    public interface CancellableTask {
        void cancel();

        boolean isCancelled();
    }

    private static final class BukkitCancellableTask implements CancellableTask {
        private final BukkitTask delegate;
        private volatile boolean cancelled;

        BukkitCancellableTask(BukkitTask delegate) {
            this.delegate = delegate;
        }

        @Override
        public void cancel() {
            if (delegate != null) {
                try {
                    delegate.cancel();
                } catch (Throwable ignored) {
                    // task already finished
                }
            }
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            if (cancelled) {
                return true;
            }
            try {
                return delegate != null && delegate.isCancelled();
            } catch (Throwable ignored) {
                return cancelled;
            }
        }
    }

    private static final class FoliaCancellableTask implements CancellableTask {

        /**
         * The public {@code ScheduledTask} interface. Folia's scheduler returns a
         * <em>non-public</em> implementation class, so reflecting {@code cancel} off
         * {@code delegate.getClass()} and invoking it throws {@link IllegalAccessException}
         * (the declaring class isn't accessible) — which would silently swallow the
         * cancel and leave repeating tasks running forever. Resolving the method from
         * the public interface (plus {@code setAccessible(true)}) makes cancel reliable.
         */
        private static final Class<?> SCHEDULED_TASK = resolveScheduledTask();

        private static Class<?> resolveScheduledTask() {
            try {
                return Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            } catch (Throwable ignored) {
                return null;
            }
        }

        private final Object delegate;
        private volatile boolean cancelled;

        FoliaCancellableTask(Object delegate) {
            this.delegate = delegate;
        }

        private Method resolve(String name) throws NoSuchMethodException {
            if (SCHEDULED_TASK != null && SCHEDULED_TASK.isInstance(delegate)) {
                Method method = SCHEDULED_TASK.getMethod(name);
                method.setAccessible(true);
                return method;
            }
            Method method = delegate.getClass().getMethod(name);
            method.setAccessible(true);
            return method;
        }

        @Override
        public void cancel() {
            if (delegate != null) {
                try {
                    resolve("cancel").invoke(delegate);
                } catch (Throwable ignored) {
                    // already cancelled or unavailable
                }
            }
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            if (cancelled) {
                return true;
            }
            if (delegate == null) {
                return true;
            }
            try {
                Object value = resolve("isCancelled").invoke(delegate);
                return value instanceof Boolean && (Boolean) value;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    /**
     * Reflection-only adapter to Folia's region/global/async/entity schedulers. Each
     * scheduler instance is resolved once on construction; method handles are cached so
     * dispatch is reflection-light at runtime. All probe failures fall back to throwing
     * an {@link IllegalStateException} so the caller can short-circuit to Bukkit.
     */
    private static final class FoliaSchedulers {
        private final Object globalScheduler;
        private final Object asyncScheduler;
        private final Object regionScheduler;
        private final Method globalRun;
        private final Method globalRunDelayed;
        private final Method globalRunAtFixedRate;
        private final Method asyncRunNow;
        private final Method asyncRunDelayed;
        private final Method regionExecute;
        private final Method entityRun;
        private final Method entityRunDelayed;
        private final Method cancelTasks;

        private FoliaSchedulers(Object globalScheduler,
                                Object asyncScheduler,
                                Object regionScheduler,
                                Method globalRun,
                                Method globalRunDelayed,
                                Method globalRunAtFixedRate,
                                Method asyncRunNow,
                                Method asyncRunDelayed,
                                Method regionExecute,
                                Method entityRun,
                                Method entityRunDelayed,
                                Method cancelTasks) {
            this.globalScheduler = globalScheduler;
            this.asyncScheduler = asyncScheduler;
            this.regionScheduler = regionScheduler;
            this.globalRun = globalRun;
            this.globalRunDelayed = globalRunDelayed;
            this.globalRunAtFixedRate = globalRunAtFixedRate;
            this.asyncRunNow = asyncRunNow;
            this.asyncRunDelayed = asyncRunDelayed;
            this.regionExecute = regionExecute;
            this.entityRun = entityRun;
            this.entityRunDelayed = entityRunDelayed;
            this.cancelTasks = cancelTasks;
        }

        static FoliaSchedulers tryInit() {
            try {
                Class<?> serverClass = Bukkit.getServer().getClass();
                Object server = Bukkit.getServer();

                Object global = serverClass.getMethod("getGlobalRegionScheduler").invoke(server);
                Object async = serverClass.getMethod("getAsyncScheduler").invoke(server);
                Object region = serverClass.getMethod("getRegionScheduler").invoke(server);

                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Class<?> scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");

                Method globalRun = global.getClass().getMethod("run", Plugin.class, consumerClass);
                Method globalRunDelayed = global.getClass().getMethod("runDelayed", Plugin.class, consumerClass, long.class);
                Method globalRunAtFixedRate = global.getClass().getMethod("runAtFixedRate", Plugin.class, consumerClass, long.class, long.class);
                Method asyncRunNow = async.getClass().getMethod("runNow", Plugin.class, consumerClass);
                Method asyncRunDelayed = async.getClass().getMethod("runDelayed", Plugin.class, consumerClass, long.class, TimeUnit.class);
                Method regionExecute = region.getClass().getMethod("execute", Plugin.class, World.class, int.class, int.class, Runnable.class);
                Method entityRun = null;
                Method entityRunDelayed = null;
                try {
                    Class<?> entityClass = Class.forName("org.bukkit.entity.Entity");
                    Method getScheduler = entityClass.getMethod("getScheduler");
                    Class<?> entitySchedulerClass = getScheduler.getReturnType();
                    entityRun = entitySchedulerClass.getMethod("run", Plugin.class, consumerClass, Runnable.class);
                    entityRunDelayed = entitySchedulerClass.getMethod("runDelayed", Plugin.class, consumerClass, Runnable.class, long.class);
                } catch (Throwable ignored) {
                    // entity scheduler unavailable - fall back to global region
                }

                Method cancelTasks;
                try {
                    cancelTasks = global.getClass().getMethod("cancelTasks", Plugin.class);
                } catch (NoSuchMethodException ignored) {
                    cancelTasks = null;
                }

                // sanity check that the returned task class is what we expect
                scheduledTaskClass.getMethods();
                return new FoliaSchedulers(global, async, region,
                        globalRun, globalRunDelayed, globalRunAtFixedRate,
                        asyncRunNow, asyncRunDelayed, regionExecute,
                        entityRun, entityRunDelayed, cancelTasks);
            } catch (Throwable throwable) {
                return null;
            }
        }

        Object run(Plugin plugin, Runnable task) {
            try {
                return globalRun.invoke(globalScheduler, plugin, asConsumer(task));
            } catch (Throwable throwable) {
                rethrow(throwable);
                return null;
            }
        }

        Object runDelayed(Plugin plugin, Runnable task, long delayTicks) {
            try {
                return globalRunDelayed.invoke(globalScheduler, plugin, asConsumer(task), delayTicks);
            } catch (Throwable throwable) {
                rethrow(throwable);
                return null;
            }
        }

        Object runAtFixedRate(Plugin plugin, Runnable task, long initialDelayTicks, long periodTicks) {
            try {
                return globalRunAtFixedRate.invoke(globalScheduler, plugin, asConsumer(task), initialDelayTicks, periodTicks);
            } catch (Throwable throwable) {
                rethrow(throwable);
                return null;
            }
        }

        Object runAsync(Plugin plugin, Runnable task) {
            try {
                return asyncRunNow.invoke(asyncScheduler, plugin, asConsumer(task));
            } catch (Throwable throwable) {
                rethrow(throwable);
                return null;
            }
        }

        Object runAsyncDelayed(Plugin plugin, Runnable task, long delayMillis) {
            try {
                return asyncRunDelayed.invoke(asyncScheduler, plugin, asConsumer(task), delayMillis, TimeUnit.MILLISECONDS);
            } catch (Throwable throwable) {
                rethrow(throwable);
                return null;
            }
        }

        Object runAtRegion(Plugin plugin, Location location, Runnable task) {
            try {
                int chunkX = location.getBlockX() >> 4;
                int chunkZ = location.getBlockZ() >> 4;
                regionExecute.invoke(regionScheduler, plugin, location.getWorld(), chunkX, chunkZ, task);
                return null; // RegionScheduler.execute returns void
            } catch (Throwable throwable) {
                rethrow(throwable);
                return null;
            }
        }

        Object runAtEntity(Plugin plugin, Entity entity, Runnable task, Runnable retired) {
            if (entityRun == null) {
                return run(plugin, task);
            }
            try {
                Method getScheduler = entity.getClass().getMethod("getScheduler");
                Object entityScheduler = getScheduler.invoke(entity);
                return entityRun.invoke(entityScheduler, plugin, asConsumer(task), retired);
            } catch (Throwable throwable) {
                rethrow(throwable);
                return null;
            }
        }

        Object runAtEntityDelayed(Plugin plugin, Entity entity, Runnable task, Runnable retired, long delayTicks) {
            if (entityRunDelayed == null) {
                return runDelayed(plugin, task, delayTicks);
            }
            try {
                Method getScheduler = entity.getClass().getMethod("getScheduler");
                Object entityScheduler = getScheduler.invoke(entity);
                return entityRunDelayed.invoke(entityScheduler, plugin, asConsumer(task), retired, delayTicks);
            } catch (Throwable throwable) {
                rethrow(throwable);
                return null;
            }
        }

        void cancelTasks(Plugin plugin) {
            if (cancelTasks == null) {
                return;
            }
            try {
                cancelTasks.invoke(globalScheduler, plugin);
            } catch (Throwable ignored) {
                // best effort
            }
        }

        private static Object asConsumer(Runnable task) {
            return (java.util.function.Consumer<Object>) ignored -> {
                if (task != null) {
                    task.run();
                }
            };
        }

        private static void rethrow(Throwable throwable) {
            Throwable cause = throwable.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new IllegalStateException(throwable);
        }
    }
}
