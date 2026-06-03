package dev.zcripted.obx.util.control;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkService implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> afkState = new ConcurrentHashMap<>();
    private SchedulerAdapter.CancellableTask ticker;

    public AfkService(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (ticker != null) {
            ticker.cancel();
        }
        ticker = plugin.getSchedulerAdapter().runRepeating(this::tick, 100L, 100L); // every 5s
    }

    public void stop() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        lastActivity.clear();
        afkState.clear();
    }

    public boolean isAfk(UUID uuid) {
        return uuid != null && Boolean.TRUE.equals(afkState.get(uuid));
    }

    public void setAfk(Player player, boolean afk) {
        if (player == null) return;
        Boolean previous = afkState.put(player.getUniqueId(), afk);
        if (previous != null && previous == afk) {
            return;
        }
        announce(player, afk);
        if (!afk) {
            lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    private void announce(Player player, boolean afk) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        String key = afk ? "afk.now-afk" : "afk.no-longer-afk";
        if (plugin.getConfig().getBoolean("afk.broadcast", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                plugin.getLanguageManager().send(online, key, placeholders);
            }
        } else {
            plugin.getLanguageManager().send(player, key, placeholders);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long idleSeconds = Math.max(30L, plugin.getConfig().getLong("afk.idle-seconds", 300L));
        long kickSeconds = plugin.getConfig().getLong("afk.kick-seconds", 0L);
        long idleMillis = idleSeconds * 1000L;
        long kickMillis = kickSeconds * 1000L;
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID uuid = online.getUniqueId();
            long last = lastActivity.getOrDefault(uuid, now);
            long sinceActivity = now - last;
            if (sinceActivity >= idleMillis && !isAfk(uuid)) {
                if (online.hasPermission("obx.afk.exempt")) continue;
                setAfk(online, true);
            }
            if (kickMillis > 0L && isAfk(uuid) && sinceActivity >= idleMillis + kickMillis) {
                if (online.hasPermission("obx.afk.exempt-kick")) continue;
                String reason = plugin.getLanguageManager().get(online, "afk.kick-reason",
                        Placeholders.with("player", online.getName()));
                online.kickPlayer(reason);
            }
        }
    }

    private void recordActivity(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());
        if (isAfk(uuid)) {
            setAfk(player, false);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        lastActivity.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastActivity.remove(uuid);
        afkState.remove(uuid);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        org.bukkit.Location from = event.getFrom();
        org.bukkit.Location to = event.getTo();
        if (from == null || to == null) {
            return;
        }
        // Any real position change OR looking around (yaw/pitch) counts as
        // activity, so a player clears AFK the moment they move or turn the
        // camera — not only when they cross a block boundary. PlayerMoveEvent
        // fires for rotation-only changes too.
        boolean moved = from.getX() != to.getX()
                || from.getY() != to.getY()
                || from.getZ() != to.getZ();
        boolean looked = from.getYaw() != to.getYaw()
                || from.getPitch() != to.getPitch();
        if (!moved && !looked) {
            return;
        }
        recordActivity(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        recordActivity(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message != null && (message.startsWith("/afk") || message.startsWith("/away"))) {
            return; // /afk itself shouldn't count as activity that toggles them out
        }
        recordActivity(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        recordActivity(event.getPlayer());
    }
}
