package dev.sergeantfuzzy.sfcore.util.control;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.platform.scheduler.SchedulerAdapter;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeService implements Listener {

    private final Main plugin;
    private final LanguageManager languages;
    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();
    private SchedulerAdapter.CancellableTask reminderTask;

    public FreezeService(Main plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
    }

    public void start() {
        if (reminderTask != null) reminderTask.cancel();
        reminderTask = plugin.getSchedulerAdapter().runRepeating(this::sendReminders, 40L, 40L);
    }

    public void stop() {
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }
        frozen.clear();
    }

    public boolean isFrozen(UUID uuid) {
        return uuid != null && frozen.contains(uuid);
    }

    public boolean toggle(Player target) {
        if (target == null) return false;
        if (frozen.remove(target.getUniqueId())) {
            languages.send(target, "freeze.no-longer-frozen");
            return false;
        }
        frozen.add(target.getUniqueId());
        languages.send(target, "freeze.now-frozen");
        return true;
    }

    private void sendReminders() {
        for (UUID uuid : frozen) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            ComponentMessenger.sendActionBar(player, languages.get(player, "freeze.actionbar"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom() == null || event.getTo() == null) return;
        if (!frozen.contains(event.getPlayer().getUniqueId())) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // allow head rotation
        }
        event.setTo(event.getFrom());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Frozen state is in-memory and intentionally not persisted — clear on quit.
        frozen.remove(event.getPlayer().getUniqueId());
    }
}
