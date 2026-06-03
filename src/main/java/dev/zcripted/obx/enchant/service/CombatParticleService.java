package dev.zcripted.obx.enchant.service;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.enchant.util.Particles;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Shared combat particle helpers: trails, rings, spirals, shockwaves, and auras.
 * All geometry is rendered through {@link Particles} (which guards
 * {@code spawnParticle} and resolves constants by name), so it degrades to a
 * no-op on servers that can't render a given particle. Timed effects (shockwave,
 * aura) run on the Folia-aware {@link SchedulerAdapter}.
 *
 * <p>Density honors the {@code combat_global.particle_intensity} setting:
 * {@code off} skips everything, {@code reduced} roughly halves point counts.
 */
public final class CombatParticleService {

    private final Main plugin;
    /** Live timed-effect tasks, tracked so a flaky cancel can never leak them past disable. */
    private final java.util.Set<SchedulerAdapter.CancellableTask> active =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<SchedulerAdapter.CancellableTask, Boolean>());

    public CombatParticleService(Main plugin) {
        this.plugin = plugin;
    }

    private void track(SchedulerAdapter.CancellableTask task) {
        if (task != null) {
            active.add(task);
        }
    }

    /** Cancels and forgets a finished timed effect. Safe to call repeatedly. */
    private void stop(SchedulerAdapter.CancellableTask task) {
        if (task != null) {
            task.cancel();
            active.remove(task);
        }
    }

    /** Cancels every live timed effect (call on plugin disable). */
    public void clear() {
        for (SchedulerAdapter.CancellableTask task : active) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
                // best effort
            }
        }
        active.clear();
    }

    private CombatSettings settings() {
        return plugin.getEnchantService() == null ? null : plugin.getEnchantService().getCombatSettings();
    }

    private boolean off() {
        CombatSettings s = settings();
        return s != null && s.particlesOff();
    }

    private int scale(int count) {
        CombatSettings s = settings();
        if (s != null && s.particlesReduced()) {
            return Math.max(1, count / 2);
        }
        return Math.max(1, count);
    }

    private static final String BLOOD_BLOCK = "REDSTONE_BLOCK";

    /** A small spray of red blood shards (bleed tick / on-hit). */
    public void spawnBlood(Location loc) {
        if (off() || loc == null) {
            return;
        }
        Particles.block(loc, BLOOD_BLOCK, scale(8), 0.25);
    }

    /** A large blood splatter (on death) — body burst + a low ground spray (+20% size/volume). */
    public void spawnBloodBurst(Location loc) {
        if (off() || loc == null) {
            return;
        }
        Particles.block(loc, BLOOD_BLOCK, scale(48), 0.6);
        Particles.block(loc.clone().subtract(0, 0.8, 0), BLOOD_BLOCK, scale(36), 0.84);
    }

    /** A line of particles from {@code from} to {@code to}. */
    public void spawnTrail(Location from, Location to, String[] particle, int density) {
        if (off() || from == null || to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }
        int points = scale(Math.max(1, density));
        double dx = (to.getX() - from.getX()) / points;
        double dy = (to.getY() - from.getY()) / points;
        double dz = (to.getZ() - from.getZ()) / points;
        Location cursor = from.clone();
        for (int i = 0; i <= points; i++) {
            Particles.at(cursor, particle, 1, 0.0);
            cursor.add(dx, dy, dz);
        }
    }

    /** A horizontal ring of particles around {@code center}. */
    public void spawnRing(Location center, double radius, String[] particle, int points) {
        if (off() || center == null || center.getWorld() == null) {
            return;
        }
        int n = scale(Math.max(4, points));
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius);
            Particles.at(point, particle, 1, 0.0);
        }
    }

    /** A rising spiral around the player (single frame). */
    public void spawnSpiral(Player player, double radius, double height, String[] particle) {
        if (off() || player == null) {
            return;
        }
        Location base = player.getLocation();
        int steps = scale(24);
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            double angle = t * Math.PI * 4;
            Location point = base.clone().add(Math.cos(angle) * radius, t * height, Math.sin(angle) * radius);
            Particles.at(point, particle, 1, 0.0);
        }
    }

    /** An expanding shockwave ring over {@code durationTicks}. */
    public void spawnShockwave(final Location center, final double maxRadius, final String[] particle, final int durationTicks) {
        if (off() || center == null || center.getWorld() == null) {
            return;
        }
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            spawnRing(center, maxRadius, particle, 24);
            return;
        }
        final int ticks = Math.max(2, durationTicks);
        final int[] elapsed = {0};
        final SchedulerAdapter.CancellableTask[] handle = new SchedulerAdapter.CancellableTask[1];
        handle[0] = scheduler.runRepeating(new Runnable() {
            @Override
            public void run() {
                try {
                    // Stop FIRST, before spawning, so a flaky cancel can never keep
                    // emitting particles past the effect's lifetime.
                    if (elapsed[0] > ticks) {
                        stop(handle[0]);
                        return;
                    }
                    double radius = maxRadius * (elapsed[0] / (double) ticks);
                    spawnRing(center, Math.max(0.1, radius), particle, 16 + (int) (radius * 4));
                    elapsed[0]++;
                } catch (Throwable broken) {
                    // Never let a thrown body keep a repeating task alive.
                    stop(handle[0]);
                }
            }
        }, 1L, 1L);
        track(handle[0]);
    }

    /** A subtle particle aura that follows the player for {@code durationTicks}. */
    public void spawnAura(final Player player, final String[] particle, final int durationTicks) {
        if (off() || player == null) {
            return;
        }
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            spawnRing(player.getLocation().add(0, 1, 0), 0.6, particle, 8);
            return;
        }
        final int ticks = Math.max(2, durationTicks);
        final int[] elapsed = {0};
        final SchedulerAdapter.CancellableTask[] handle = new SchedulerAdapter.CancellableTask[1];
        handle[0] = scheduler.runRepeating(new Runnable() {
            @Override
            public void run() {
                try {
                    // Stop FIRST: once the aura's lifetime is up (or the player left),
                    // never emit again — even if the underlying cancel is unreliable.
                    if (elapsed[0] > ticks || !player.isOnline()) {
                        stop(handle[0]);
                        return;
                    }
                    elapsed[0] += 4;
                    spawnRing(player.getLocation().add(0, 1, 0), 0.6, particle, 6);
                } catch (Throwable broken) {
                    stop(handle[0]);
                }
            }
        }, 1L, 4L);
        track(handle[0]);
    }
}
