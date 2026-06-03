package dev.zcripted.obx.feature.hologram.render;

import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.model.HologramSettings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Decides whether a given player should currently see a given hologram.
 * Used by the tick loop to drive {@code backend.updateVisibility(...)}.
 *
 * <p>Decision factors (in order):
 * <ol>
 *   <li>Same world as the hologram.</li>
 *   <li>Within {@link HologramSettings#getShowRange()} blocks (squared).</li>
 *   <li>If {@code doubleSided == false}, the player must be on the front
 *       side of the hologram plane — derived from the dot product between
 *       the player's location vector relative to the hologram and the
 *       hologram's normal (computed from {@code Location.getYaw()}).</li>
 *   <li>Player is not in the hologram's {@code personallyHidden} set (Phase 6).</li>
 *   <li>Player has the view-permission (Phase 6); ignored when unset.</li>
 * </ol>
 *
 * <p>Hide-behind-walls (Phase 6) is layered on top of this in
 * {@code WallOcclusionCheck}; this tracker stays pure-distance to keep the
 * tick loop cheap.
 */
public final class ViewerTracker {

    private ViewerTracker() {
    }

    public static boolean shouldSee(Hologram hologram, Player player) {
        if (hologram == null || player == null) {
            return false;
        }
        Location holoLoc = hologram.getLocation();
        Location playerLoc = player.getLocation();
        if (holoLoc.getWorld() == null || playerLoc.getWorld() == null) {
            return false;
        }
        if (!holoLoc.getWorld().equals(playerLoc.getWorld())) {
            return false;
        }
        HologramSettings settings = hologram.getSettings();
        double dx = playerLoc.getX() - holoLoc.getX();
        double dy = playerLoc.getY() - holoLoc.getY();
        double dz = playerLoc.getZ() - holoLoc.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        double showRange = settings.getShowRange();
        if (distSq > showRange * showRange) {
            return false;
        }
        if (hologram.getPersonallyHidden().contains(player.getUniqueId())) {
            return false;
        }
        String perm = settings.getViewPermission();
        if (perm != null && !player.hasPermission(perm)) {
            return false;
        }
        if (!settings.isDoubleSided()) {
            // Compute the hologram normal from its yaw.
            float yaw = (float) Math.toRadians(holoLoc.getYaw());
            Vector normal = new Vector(-Math.sin(yaw), 0.0, Math.cos(yaw));
            Vector toPlayer = new Vector(dx, 0.0, dz);
            if (toPlayer.dot(normal) < 0.0) {
                return false;
            }
        }
        if (settings.isHideBehindWalls()) {
            if (!WallOcclusionCheck.canSee(player, hologram, 5)) {
                return false;
            }
        }
        return true;
    }
}
