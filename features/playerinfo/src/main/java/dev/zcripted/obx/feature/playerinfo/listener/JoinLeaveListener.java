package dev.zcripted.obx.feature.playerinfo.listener;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.chat.ChatService;
import dev.zcripted.obx.api.playerinfo.JoinLeaveService;
import dev.zcripted.obx.util.message.AdventureMessageUtil;
import dev.zcripted.obx.util.message.ConsoleTimestamp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JoinLeaveListener implements Listener {

    private final ObxPlugin plugin;
    private final JoinLeaveService service;

    public JoinLeaveListener(ObxPlugin plugin, JoinLeaveService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore();

        if (service.isJoinLeaveEnabled()) {
            if (service.suppressVanillaJoinMessage()) {
                event.setJoinMessage(null);
            }
            String template = firstJoin && service.isFirstJoinMessageEnabled()
                    ? service.getFirstJoinMessage()
                    : service.getJoinMessage();
            if (template != null && !template.isEmpty()) {
                Map<String, String> placeholders = placeholders(player);
                AdventureMessageUtil.broadcast(plugin.getServer(), template, placeholders, false);
                writeConsoleMirror(template, placeholders);
            }
        }

        if (service.isJoinMotdEnabled()) {
            // Localize the MOTD to the player's chosen /language (defaults to EN).
            final String langCode = plugin.getLanguageManager() == null
                    ? "en"
                    : plugin.getLanguageManager().getLanguage(player.getUniqueId()).code();
            final List<String> lines = firstJoin && service.isFirstJoinMotdEnabled()
                    ? service.getFirstJoinMotdLines(langCode)
                    : service.getJoinMotdLines(langCode);
            if (lines != null && !lines.isEmpty()) {
                final Map<String, String> placeholders = placeholders(player);
                plugin.getSchedulerAdapter().runLater(() -> {
                    if (player.isOnline()) {
                        AdventureMessageUtil.sendLines(player, lines, placeholders);
                    }
                }, 10L);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        if (!service.isJoinLeaveEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (service.suppressVanillaJoinMessage()) {
            event.setQuitMessage(null);
        }
        String template = service.getLeaveMessage();
        if (template == null || template.isEmpty()) {
            return;
        }
        Map<String, String> placeholders = placeholders(player);
        AdventureMessageUtil.broadcast(plugin.getServer(), template, placeholders, false);
        writeConsoleMirror(template, placeholders);
    }

    private void writeConsoleMirror(String template, Map<String, String> placeholders) {
        String body = AdventureMessageUtil.renderAnsi(template, placeholders);
        ChatService chat = plugin.getChatService();
        boolean enabled = chat == null || chat.isConsoleTimestampEnabled();
        String pattern = chat == null ? null : chat.getConsoleTimestampFormat();
        plugin.writeConsoleLine(ConsoleTimestamp.prefix(plugin, enabled, pattern) + body);
    }

    private Map<String, String> placeholders(Player player) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("player", player.getName());
        // Neutralize tags: another plugin's display name could carry MiniMessage <...> that would
        // otherwise render in the broadcast (parity with the chat path).
        placeholders.put("displayname", dev.zcripted.obx.util.text.MessageSanitizer.neutralizeTags(player.getDisplayName()));
        placeholders.put("world", player.getWorld() == null ? "" : player.getWorld().getName());
        placeholders.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        placeholders.put("max", String.valueOf(Bukkit.getMaxPlayers()));
        placeholders.put("uuid", player.getUniqueId().toString());
        return placeholders;
    }
}
