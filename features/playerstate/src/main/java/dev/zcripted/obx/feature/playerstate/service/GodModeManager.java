package dev.zcripted.obx.feature.playerstate.service;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GodModeManager implements Listener {

    private final Set<UUID> enabled = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    public boolean toggle(Player player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        boolean nowEnabled;
        if (enabled.contains(uuid)) {
            enabled.remove(uuid);
            nowEnabled = false;
        } else {
            enabled.add(uuid);
            nowEnabled = true;
            player.setFireTicks(0);
            player.setFallDistance(0f);
        }
        return nowEnabled;
    }

    public boolean isEnabled(UUID uuid) {
        return uuid != null && enabled.contains(uuid);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (isEnabled(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (isEnabled(player.getUniqueId())) {
            event.setCancelled(true);
            player.setFireTicks(0);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        enabled.remove(event.getPlayer().getUniqueId());
    }
}