package dev.zcripted.obx.feature.hub.launchpad;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hub.service.HubService;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks launchpad cooldowns per player and renders a live countdown in the
 * action bar (the slot directly above the hotbar). The countdown is only
 * visible while a cooldown is active — when the cooldown clears the action
 * bar is overwritten once with an empty string to avoid the vanilla "fade
 * over 3s" tail lingering at zero.
 *
 * <p>Refresh cadence is 4 ticks (200 ms) — fast enough that the visible
 * timer looks live (with sub-second resolution shown as one decimal) but
 * not so fast that legacy clients (1.8.x) flicker.
 */
public final class LaunchpadCooldownManager {

    private static final long REFRESH_PERIOD_TICKS = 4L;

    private final ObxPlugin plugin;
    private final HubService hubService;
    private final Map<UUID, Long> cooldownEndMillis = new ConcurrentHashMap<>();
    private final Map<UUID, Long> launchExpiryMillis = new ConcurrentHashMap<>();

    private dev.zcripted.obx.core.platform.scheduler.CancellableTask refreshTask;

    public LaunchpadCooldownManager(ObxPlugin plugin, HubService hubService) {
        this.plugin = plugin;
        this.hubService = hubService;
    }

    public void start() {
        if (refreshTask != null) {
            return;
        }
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            return;
        }
        refreshTask = scheduler.runRepeating(this::tick, REFRESH_PERIOD_TICKS, REFRESH_PERIOD_TICKS);
    }

    public void stop() {
        if (refreshTask != null) {
            try {
                refreshTask.cancel();
            } catch (Throwable ignored) {
            }
            refreshTask = null;
        }
        cooldownEndMillis.clear();
        launchExpiryMillis.clear();
    }

    /**
     * @return milliseconds remaining on the cooldown for {@code player}, or 0
     *         if no cooldown is active.
     */
    public long remainingMillis(Player player) {
        if (player == null) {
            return 0L;
        }
        Long end = cooldownEndMillis.get(player.getUniqueId());
        if (end == null) {
            return 0L;
        }
        long delta = end - System.currentTimeMillis();
        return delta <= 0L ? 0L : delta;
    }

    public boolean isOnCooldown(Player player) {
        return remainingMillis(player) > 0L;
    }

    /**
     * Starts the cooldown for {@code player} using the duration configured
     * in {@code hub.yml}. A zero-second config disables the cooldown.
     */
    public void start(Player player) {
        if (player == null) {
            return;
        }
        int seconds = hubService.launchpadCooldownSeconds();
        if (seconds <= 0) {
            return;
        }
        long end = System.currentTimeMillis() + (seconds * 1000L);
        cooldownEndMillis.put(player.getUniqueId(), end);
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        cooldownEndMillis.remove(player.getUniqueId());
        launchExpiryMillis.remove(player.getUniqueId());
    }

    /**
     * Marks a player as "currently launched" — used by the fall-damage
     * listener to know whether to cancel landing damage. The mark expires
     * 10 s after launch as a safety net in case the ground-hit detection
     * never fires (e.g. player logs out mid-flight).
     */
    public void markLaunched(Player player) {
        if (player == null) {
            return;
        }
        launchExpiryMillis.put(player.getUniqueId(), System.currentTimeMillis() + 10_000L);
    }

    public boolean isLaunched(Player player) {
        if (player == null) {
            return false;
        }
        Long expiry = launchExpiryMillis.get(player.getUniqueId());
        if (expiry == null) {
            return false;
        }
        if (expiry < System.currentTimeMillis()) {
            launchExpiryMillis.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void clearLaunched(Player player) {
        if (player == null) {
            return;
        }
        launchExpiryMillis.remove(player.getUniqueId());
    }

    private void tick() {
        if (cooldownEndMillis.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        // ConcurrentHashMap's iterator is weakly consistent, so a concurrent start() during
        // iteration is safe without snapshotting.
        Iterator<Map.Entry<UUID, Long>> it = cooldownEndMillis.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID uuid = entry.getKey();
            long end = entry.getValue();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }
            final long remaining = end - now;
            final Player target = player;
            if (remaining <= 0L) {
                it.remove();
                // Overwrite with empty so the stale countdown clears before fade.
                dispatchToPlayer(target, () -> ComponentMessenger.sendActionBar(target, ""));
                continue;
            }
            dispatchToPlayer(target, () -> sendCountdown(target, remaining));
        }
    }

    /** Runs player-touching work on the player's region thread (Folia) or inline (Bukkit). */
    private void dispatchToPlayer(Player player, Runnable task) {
        if (plugin.getSchedulerAdapter().isFolia()) {
            plugin.getSchedulerAdapter().runAtEntity(player, task);
        } else {
            task.run();
        }
    }

    private void sendCountdown(Player player, long remainingMillis) {
        LanguageManager languages = plugin.getLanguageManager();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("seconds", formatSeconds(remainingMillis));
        placeholders.put("bar", buildBar(remainingMillis));
        String line;
        if (languages != null) {
            line = languages.get(player, "hub.launchpad.cooldown", placeholders);
            if (line == null || line.isEmpty()) {
                line = fallbackCountdown(placeholders);
            }
        } else {
            line = fallbackCountdown(placeholders);
        }
        ComponentMessenger.sendActionBar(player, line);
    }

    private String fallbackCountdown(Map<String, String> placeholders) {
        return "§eLaunchpad cooldown §7» §f" + placeholders.get("seconds")
                + "s §8" + placeholders.get("bar");
    }

    private String formatSeconds(long millis) {
        double s = millis / 1000.0;
        return String.format(java.util.Locale.ENGLISH, "%.1f", s);
    }

    /** 10-segment unicode bar — fills proportional to the remaining cooldown. */
    private String buildBar(long remainingMillis) {
        int totalSeconds = hubService.launchpadCooldownSeconds();
        if (totalSeconds <= 0) {
            return "";
        }
        double totalMillis = totalSeconds * 1000.0;
        double ratio = Math.max(0.0, Math.min(1.0, remainingMillis / totalMillis));
        int filled = (int) Math.round(ratio * 10.0);
        StringBuilder sb = new StringBuilder();
        sb.append("§c");
        for (int i = 0; i < filled; i++) {
            sb.append('█');
        }
        sb.append("§8");
        for (int i = filled; i < 10; i++) {
            sb.append('█');
        }
        return sb.toString();
    }
}
