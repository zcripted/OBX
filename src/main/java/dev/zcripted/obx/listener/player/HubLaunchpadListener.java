package dev.zcripted.obx.listener.player;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.hub.HubService;
import dev.zcripted.obx.hub.item.HubItems;
import dev.zcripted.obx.hub.launchpad.LaunchpadCooldownManager;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Launchpad — the "hold item and double-tap space" trick used by every
 * commercial hub plugin (Hub, BedWars, Bungee lobbies).
 *
 * <h3>How it works</h3>
 * <p>The kit applier grants {@code allowFlight=true} to every launchpad-eligible
 * player (launchpad item enabled + {@code obx.hub.launchpad}). When the player
 * presses space a second time in mid-air, Minecraft requests flight from the
 * server → {@link PlayerToggleFlightEvent} fires with {@code isFlying() == true}.
 * We cancel the request, and — only while the launchpad is actually held — apply
 * an outward velocity and start the cooldown.
 *
 * <p>The flight request is cancelled for <em>every</em> survival/adventure
 * double-tap by an eligible player, even when the launchpad isn't held. That is
 * deliberate: the {@code allowFlight} grant exists solely for the launch, so
 * letting any other double-tap turn into real flight would be a free-fly exploit.
 *
 * <p>Fall damage on landing is cancelled by {@link HubFallDamageListener}
 * via {@link LaunchpadCooldownManager#isLaunched(Player)}.
 */
public final class HubLaunchpadListener implements Listener {

    private final OBX plugin;
    private final HubService hub;
    private final LaunchpadCooldownManager cooldownManager;

    public HubLaunchpadListener(OBX plugin, HubService hub, LaunchpadCooldownManager cooldownManager) {
        this.plugin = plugin;
        this.hub = hub;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!hub.isEnabled() || !event.isFlying()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (!hub.isInHubWorld(player)) {
            return;
        }
        // The hub only hands survival/adventure flight to launchpad-eligible
        // players. If this player isn't one of them, we didn't grant the flight,
        // so we must NOT interfere with whatever other source toggled it.
        if (!hub.isItemEnabled(HubItems.ID_LAUNCHPAD) || !player.hasPermission("obx.hub.launchpad")) {
            return;
        }

        // We own this flight grant. A survival/adventure double-tap must never
        // become real creative-style flight (that would be a free-fly exploit),
        // so cancel it unconditionally and re-affirm the grant + clear the flying
        // state so the next double-tap reliably fires another event.
        event.setCancelled(true);
        player.setAllowFlight(true);
        player.setFlying(false);

        // Launch only fires while the launchpad is actually held — matches the
        // item lore ("Hold and double-tap SPACE to launch"). Holding any other
        // hotbar item simply blocks the flight (handled above) without launching.
        ItemStack held;
        try {
            held = player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError legacy) {
            held = player.getItemInHand();
        }
        if (!HubItems.ID_LAUNCHPAD.equals(HubItems.getId(plugin, held))) {
            return;
        }

        // Apply the launch (cooldown-guarded inside). Shared with the
        // right-click path in HubItemUseListener so the launchpad works whether
        // the player double-taps space or simply right-clicks the item.
        launch(hub, cooldownManager, player);
    }

    /**
     * Applies the launch impulse: an upward + forward boost from the player's
     * look direction, marks the player as launched (so {@link HubFallDamageListener}
     * cancels the landing fall damage), and starts the cooldown. No-op while the
     * player is still on cooldown. Used by both the double-jump path (above) and
     * the right-click path ({@code HubItemUseListener}).
     */
    public static void launch(HubService hub, LaunchpadCooldownManager cooldownManager, Player player) {
        if (player == null || hub == null) {
            return;
        }
        if (cooldownManager != null && cooldownManager.isOnCooldown(player)) {
            return;
        }
        Vector velocity = player.getLocation().getDirection().multiply(hub.launchpadForwardPower());
        velocity.setY(hub.launchpadUpPower());
        player.setVelocity(velocity);
        if (cooldownManager != null) {
            cooldownManager.start(player);
            cooldownManager.markLaunched(player);
        }
        // Play a launch sound using a cross-version probe — modern enum
        // names differ from 1.8.
        playLaunchSound(player);
    }

    private static void playLaunchSound(Player player) {
        String[] candidates = new String[]{
                "ENTITY_FIREWORK_ROCKET_LAUNCH",
                "ENTITY_FIREWORK_LAUNCH",
                "FIREWORK_LAUNCH"
        };
        for (String name : candidates) {
            try {
                Sound sound = Sound.valueOf(name);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                return;
            } catch (IllegalArgumentException ignored) {
                // Try the next candidate.
            } catch (Throwable ignored) {
                return;
            }
        }
    }
}
