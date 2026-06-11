package dev.zcripted.obx.api.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

/** Public teleport API (warmup + safe teleport). Implemented by {@code feature.teleport.service.TeleportManagerImpl}. */
public interface TeleportManager {

    void teleportPlayer(Player player, Location destination, String messagePath, Map<String, String> placeholders);

    void cancelAll();
}