package dev.zcripted.obx.feature.hologram.interact;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Optional click / hover particle effect. Played at the hologram's origin
 * via Bukkit's {@code Player#spawnParticle}, so the effect is visible only
 * to the interacting viewer.
 */
public final class ParticleFx {

    private ParticleFx() {
    }

    public static void play(ObxPlugin plugin, Player viewer, Location location, String particleName, int count) {
        if (plugin == null || viewer == null || location == null || particleName == null) {
            return;
        }
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            viewer.spawnParticle(particle, location, Math.max(1, count), 0.2, 0.2, 0.2, 0.0);
        } catch (Throwable ignored) {
        }
    }
}
