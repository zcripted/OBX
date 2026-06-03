package dev.zcripted.obx.feature.teleport.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManagerImpl implements Listener, dev.zcripted.obx.api.teleport.TeleportManager {

    private final ObxPlugin plugin;
    private final LanguageManager languages;
    private final Map<UUID, dev.zcripted.obx.core.platform.scheduler.CancellableTask> pendingTeleports = new ConcurrentHashMap<>();

    public TeleportManagerImpl(ObxPlugin plugin, LanguageManager languages) {
        this.plugin = plugin;
        this.languages = languages;
    }

    public void teleportPlayer(final Player player, final Location destination, final String messagePath, Map<String, String> placeholders) {
        if (player == null || destination == null) {
            return;
        }
        final Map<String, String> effectivePlaceholders = placeholders == null
                ? Placeholders.with("seconds", plugin.getConfig().getInt("teleport.warmup-seconds", 0))
                : placeholders;
        int warmupSeconds = Math.max(0, plugin.getConfig().getInt("teleport.warmup-seconds", 0));
        final boolean sendMessages = plugin.getConfig().getBoolean("teleport.send-messages", true);
        if (warmupSeconds <= 0) {
            if (sendMessages && messagePath != null) {
                languages.send(player, messagePath, effectivePlaceholders);
            }
            player.teleport(destination);
            return;
        }
        final UUID uuid = player.getUniqueId();
        cancelExisting(uuid);
        if (sendMessages) {
            languages.send(player, "teleport.warmup.start", Placeholders.merge(effectivePlaceholders, "seconds", warmupSeconds));
        }
        dev.zcripted.obx.core.platform.scheduler.CancellableTask task = plugin.getSchedulerAdapter().runAtEntity(player, () -> {
            pendingTeleports.remove(uuid);
            player.teleport(destination);
            if (sendMessages && messagePath != null) {
                languages.send(player, messagePath, effectivePlaceholders);
            }
        }, () -> pendingTeleports.remove(uuid));
        // The above runs immediately; for the warmup we need a delayed entity-bound task.
        // Cancel the immediate scheduling and use the delayed path instead.
        if (task != null) {
            task.cancel();
        }
        dev.zcripted.obx.core.platform.scheduler.CancellableTask delayed = scheduleDelayedTeleport(player, destination, messagePath, effectivePlaceholders, warmupSeconds, sendMessages, uuid);
        if (delayed != null) {
            pendingTeleports.put(uuid, delayed);
        }
    }

    private dev.zcripted.obx.core.platform.scheduler.CancellableTask scheduleDelayedTeleport(final Player player, final Location destination,
                                                                     final String messagePath, final Map<String, String> placeholders,
                                                                     int warmupSeconds, final boolean sendMessages, final UUID uuid) {
        final long delayTicks = warmupSeconds * 20L;
        return plugin.getSchedulerAdapter().runLater(() -> {
            pendingTeleports.remove(uuid);
            // Re-validate the player is still online before teleporting; on Folia this runs on
            // the global region, so hop to the entity's region for the actual teleport.
            if (!player.isOnline()) {
                return;
            }
            plugin.getSchedulerAdapter().runAtEntity(player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                player.teleport(destination);
                if (sendMessages && messagePath != null) {
                    languages.send(player, messagePath, placeholders);
                }
            });
        }, delayTicks);
    }

    public void cancelAll() {
        for (dev.zcripted.obx.core.platform.scheduler.CancellableTask task : pendingTeleports.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        pendingTeleports.clear();
    }

    private void cancelExisting(UUID uuid) {
        dev.zcripted.obx.core.platform.scheduler.CancellableTask pending = pendingTeleports.remove(uuid);
        if (pending != null) {
            pending.cancel();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelExisting(event.getPlayer().getUniqueId());
    }
}
