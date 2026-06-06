package dev.zcripted.obx.feature.jail.listener;

import dev.zcripted.obx.feature.jail.service.JailService;
import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JailListener implements Listener {

    // Exact base commands a jailed player may still run (matched on the first
    // token only, so "/helpme" or "/obxescape" can't sneak through a prefix match).
    private static final Set<String> ALLOWED_COMMANDS = new HashSet<>(Arrays.asList(
            "/jailtime", "/obx", "/language", "/sprache", "/help", "/?"));

    /** Min millis between "you can't leave the jail" messages per player, so wall-pushing can't spam chat. */
    private static final long CONTAINMENT_MSG_COOLDOWN_MS = 3000L;

    private final ObxPlugin plugin;
    private final JailService jailService;
    private final LanguageManager languages;
    private final java.util.Map<java.util.UUID, Long> lastContainmentMsg = new java.util.concurrent.ConcurrentHashMap<>();

    public JailListener(ObxPlugin plugin) {
        this.plugin = plugin;
        this.jailService = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.jail.service.JailService.class);
        this.languages = plugin.getLanguageManager();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        jailService.evictCache(event.getPlayer().getUniqueId());
        lastContainmentMsg.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Load the jail state into the cache up front so the move handler never queries the DB.
        jailService.refreshCache(event.getPlayer().getUniqueId());
        if (!jailService.isJailed(event.getPlayer().getUniqueId())) return;
        plugin.getSchedulerAdapter().runLater(() -> jailService.teleportToJail(event.getPlayer()), 20L);
        JailService.JailState state = jailService.getState(event.getPlayer().getUniqueId());
        if (state != null) {
            languages.send(event.getPlayer(), "jail.on-join",
                    Placeholders.with("jail", state.getJailName(),
                            "remaining", jailService.formatDuration(state.getSecondsRemaining())));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // A jailed player who dies would otherwise respawn at their bed/world spawn, outside the
        // jail. PlayerRespawnEvent isn't a teleport, so onTeleport doesn't catch it — redirect the
        // respawn straight to the jail anchor (cache-backed; no DB on this path).
        if (!jailService.isJailed(event.getPlayer().getUniqueId())) {
            return;
        }
        org.bukkit.Location anchor = jailService.getJailAnchor(event.getPlayer().getUniqueId());
        if (anchor != null && anchor.getWorld() != null) {
            event.setRespawnLocation(anchor.clone());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        Location from = event.getFrom();
        // Only react to whole-block moves — head/look changes share the same block and are ignored
        // so this stays cheap on the hot move path.
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        // Cache-backed: online jailed players are loaded on join, so this never hits the DB.
        if (!jailService.isJailed(event.getPlayer().getUniqueId())) {
            return;
        }
        Location anchor = jailService.getJailAnchor(event.getPlayer().getUniqueId());
        if (anchor == null || anchor.getWorld() == null) {
            return;
        }
        double radius = jailService.getContainmentRadius();
        boolean strayed = to.getWorld() == null
                || !to.getWorld().equals(anchor.getWorld())
                || to.distanceSquared(anchor) > radius * radius;
        if (strayed) {
            // setTo (not teleport) keeps it cheap and avoids the teleport-block handler below.
            event.setTo(anchor.clone());
            // Throttle the message so holding into the boundary doesn't spam chat (the pull-back
            // still happens every time).
            java.util.UUID id = event.getPlayer().getUniqueId();
            long now = System.currentTimeMillis();
            Long last = lastContainmentMsg.get(id);
            if (last == null || now - last >= CONTAINMENT_MSG_COOLDOWN_MS) {
                lastContainmentMsg.put(id, now);
                languages.send(event.getPlayer(), "jail.containment");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        java.util.UUID id = event.getPlayer().getUniqueId();
        if (!jailService.isJailed(id)) {
            return;
        }
        // We no longer blanket-exempt PLUGIN teleports: the plugin's own /tp, /spawn, /home, /back,
        // /tpa-accept, RTP, hub-on-join, etc. are all PLUGIN-cause and previously let a jailed player
        // (or staff) leave the jail. Instead, allow only teleports whose destination stays INSIDE the
        // jail containment (covers the initial jail-in teleport to the anchor); redirect everything
        // else back to the anchor. Unjail clears the jail state first, so release teleports aren't hit.
        org.bukkit.Location to = event.getTo();
        org.bukkit.Location anchor = jailService.getJailAnchor(id);
        if (to == null || anchor == null || anchor.getWorld() == null) {
            return; // can't evaluate/contain — don't interfere
        }
        double radius = jailService.getContainmentRadius();
        boolean withinJail = to.getWorld() != null
                && to.getWorld().equals(anchor.getWorld())
                && to.distanceSquared(anchor) <= radius * radius;
        if (withinJail) {
            return;
        }
        event.setTo(anchor.clone());
        long now = System.currentTimeMillis();
        Long last = lastContainmentMsg.get(id);
        if (last == null || now - last >= CONTAINMENT_MSG_COOLDOWN_MS) {
            lastContainmentMsg.put(id, now);
            languages.send(event.getPlayer(), "jail.blocked-teleport");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!jailService.isJailed(event.getPlayer().getUniqueId())) return;
        String message = event.getMessage().trim();
        int space = message.indexOf(' ');
        String base = (space < 0 ? message : message.substring(0, space)).toLowerCase(java.util.Locale.ROOT);
        if (ALLOWED_COMMANDS.contains(base)) {
            return;
        }
        event.setCancelled(true);
        languages.send(event.getPlayer(), "jail.blocked-command");
    }
}
