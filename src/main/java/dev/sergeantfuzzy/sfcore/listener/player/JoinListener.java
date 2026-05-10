package dev.sergeantfuzzy.sfcore.listener.player;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final Main plugin;

    public JoinListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location spawn = plugin.getDataService().getSpawn();
        if (spawn == null) {
            return;
        }

        boolean firstJoin = !player.hasPlayedBefore();
        boolean teleportOnFirst = plugin.getConfig().getBoolean("spawn.teleport-on-first-join", false);
        boolean teleportOnJoin = plugin.getConfig().getBoolean("spawn.teleport-on-join", false);

        if (firstJoin && teleportOnFirst) {
            plugin.getTeleportManager().teleportPlayer(player, spawn, "teleport.spawn.teleporting", null);
        } else if (teleportOnJoin) {
            plugin.getTeleportManager().teleportPlayer(player, spawn, "teleport.spawn.teleporting", null);
        }
    }
}

