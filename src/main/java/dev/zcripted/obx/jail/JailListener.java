package dev.zcripted.obx.jail;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.language.LanguageManager;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JailListener implements Listener {

    // Exact base commands a jailed player may still run (matched on the first
    // token only, so "/helpme" or "/obxescape" can't sneak through a prefix match).
    private static final Set<String> ALLOWED_COMMANDS = new HashSet<>(Arrays.asList(
            "/jailtime", "/obx", "/language", "/sprache", "/help", "/?"));

    private final Main plugin;
    private final JailService jailService;
    private final LanguageManager languages;

    public JailListener(Main plugin) {
        this.plugin = plugin;
        this.jailService = plugin.getJailService();
        this.languages = plugin.getLanguageManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!jailService.isJailed(event.getPlayer().getUniqueId())) return;
        plugin.getSchedulerAdapter().runLater(() -> jailService.teleportToJail(event.getPlayer()), 20L);
        JailService.JailState state = jailService.getState(event.getPlayer().getUniqueId());
        if (state != null) {
            languages.send(event.getPlayer(), "jail.on-join",
                    Placeholders.with("jail", state.getJailName(),
                            "remaining", jailService.formatDuration(state.getSecondsRemaining())));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) return;
        if (!jailService.isJailed(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        languages.send(event.getPlayer(), "jail.blocked-teleport");
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
