package dev.zcripted.obx.listener.player;

import dev.zcripted.obx.hub.launchpad.LaunchpadCooldownManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Cancels fall damage for players who were recently launched by the
 * launchpad. The "launched" mark is set by
 * {@link HubLaunchpadListener} and expires automatically 10 s later as a
 * safety net (handled in {@link LaunchpadCooldownManager#isLaunched}).
 *
 * <p>Only fall damage is cancelled — every other damage cause passes
 * through normally so combat / void / fire damage in the hub world still
 * behaves vanilla.
 */
public final class HubFallDamageListener implements Listener {

    private final LaunchpadCooldownManager cooldownManager;

    public HubFallDamageListener(LaunchpadCooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (!cooldownManager.isLaunched(player)) {
            return;
        }
        event.setCancelled(true);
        // First ground touch ends the "launched" state so the next legit
        // fall (e.g. running off a ledge) deals damage normally.
        cooldownManager.clearLaunched(player);
        player.setFallDistance(0f);
    }
}
