package dev.sergeantfuzzy.sfcore.jail;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JailListener implements Listener {

    private static final Set<String> ALLOWED_PREFIXES = new HashSet<>(Arrays.asList(
            "/jailtime", "/sf ", "/sf", "/language", "/sprache", "/help", "/?"));

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
        String message = event.getMessage().toLowerCase();
        for (String allowed : ALLOWED_PREFIXES) {
            if (message.startsWith(allowed)) return;
        }
        event.setCancelled(true);
        languages.send(event.getPlayer(), "jail.blocked-command");
    }
}
