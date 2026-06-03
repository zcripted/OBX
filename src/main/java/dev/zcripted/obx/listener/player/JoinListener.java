package dev.zcripted.obx.listener.player;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.tablist.format.TablistTeams;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final OBX plugin;

    public JoinListener(OBX plugin) {
        this.plugin = plugin;
    }

    /**
     * Colors the name shown above the player's head: OP names light red, regular
     * players light yellow. Runs independently of the tablist module so the
     * nameplate color always applies. One-tick delay so the join has settled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinNameplate(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        plugin.getSchedulerAdapter().runLater(new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    TablistTeams.assign(player, player.isOp());
                }
            }
        }, 1L);
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

