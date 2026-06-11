package dev.zcripted.obx.api.hub;

import org.bukkit.entity.Player;

/** Public hub-kit application API. Implemented by {@code feature.hub.kit.HubKitApplierImpl}. */
public interface HubKitApplier {

    void apply(Player player);

    void applyForce(Player player);

    void applyToAllInHubWorlds();

    void revokeFlightInHubWorlds();
}