package dev.zcripted.obx.api.staff;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Public vanish-state API — the cross-feature surface of the staff vanish system.
 * Implemented by {@code feature.staff.service.VanishManager}.
 */
public interface VanishApi {

    boolean isVanished(UUID uuid);

    boolean isVanished(Player player);
}