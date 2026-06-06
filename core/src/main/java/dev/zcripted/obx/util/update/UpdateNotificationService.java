package dev.zcripted.obx.util.update;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.platform.scheduler.CancellableTask;
import dev.zcripted.obx.core.storage.SqliteDataStore;
import dev.zcripted.obx.util.message.ConsoleLog;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns OBX's update-notification lifecycle on top of {@link UpdateChecker}:
 *
 * <ul>
 *   <li><b>Startup check</b> — one async check during enable; the result is printed to the
 *       console (new release, up to date, or check failed). Gated by
 *       {@code updates.check-on-startup}.</li>
 *   <li><b>Periodic re-check</b> — re-checks every {@code updates.check-interval-minutes}
 *       minutes while the server runs and announces each new release <em>once</em>
 *       (console + every online player eligible per {@link #notificationsEnabledFor}).
 *       {@code 0} disables the repeating check.</li>
 *   <li><b>Default-on, persisted opt-OUT</b> — players holding {@code obx.updates.notify}
 *       (default: op) receive join/periodic notifications <em>by default</em>;
 *       {@code /obx updates notify} toggles them off/on per player. Opt-outs are stored in
 *       SQLite ({@code obx_update_notify_optout}) so the choice survives restarts. When the
 *       store is unavailable the toggle still works for the current session (in-memory).</li>
 * </ul>
 *
 * <p>Threading: all network I/O happens off-thread inside {@link UpdateChecker#checkAsync};
 * results (and therefore every chat/console announce here) are delivered on the main/global
 * thread, which is safe on Bukkit/Spigot/Paper/Purpur and Folia alike. The opt-out and
 * announced-version sets are concurrent because the async toggle persistence and main-thread
 * reads may interleave.
 */
public final class UpdateNotificationService {

    /** Permission that receives notifications by default (granted to ops via plugin.yml). */
    public static final String NOTIFY_PERMISSION = "obx.updates.notify";

    /** Opt-out row key used for the console sender (players use their UUID string). */
    private static final String CONSOLE_KEY = "CONSOLE";

    private static final String TABLE = "obx_update_notify_optout";
    private static final long DEFAULT_INTERVAL_MINUTES = 15L;
    private static final String CONSOLE_TAG = "Updates";

    private final ObxPlugin plugin;
    private final UpdateChecker checker;
    /** UUID strings (or {@link #CONSOLE_KEY}) that opted OUT via {@code /obx updates notify}. */
    private final Set<String> optOuts = ConcurrentHashMap.newKeySet();
    /** Versions already announced this runtime, so the periodic check never repeats itself. */
    private final Set<String> announcedVersions = ConcurrentHashMap.newKeySet();
    private CancellableTask periodicTask;

    public UpdateNotificationService(ObxPlugin plugin) {
        this.plugin = plugin;
        this.checker = new UpdateChecker(plugin);
    }

    /** The underlying checker, for on-demand checks ({@code /obx updates}). */
    public UpdateChecker checker() {
        return checker;
    }

    /**
     * Loads persisted opt-outs, runs the startup check (config-gated) and schedules the
     * periodic re-check. Call once from {@code onEnable}.
     */
    public void start() {
        loadOptOuts();
        if (plugin.getConfig().getBoolean("updates.check-on-startup", true)) {
            checker.checkAsync(result -> handleResult(result, true));
        }
        schedulePeriodic();
    }

    /** Re-reads {@code updates.*} config and reschedules the periodic check. */
    public void reload() {
        cancelPeriodic();
        schedulePeriodic();
    }

    /** Cancels the periodic check. Call from {@code onDisable}. */
    public void stop() {
        cancelPeriodic();
    }

    /**
     * Whether {@code sender} currently receives update notifications: the master config
     * switch is on, they hold {@link #NOTIFY_PERMISSION}, and they have not opted out.
     */
    public boolean notificationsEnabledFor(CommandSender sender) {
        return plugin.getConfig().getBoolean("updates.notify-players", true)
                && sender.hasPermission(NOTIFY_PERMISSION)
                && !optOuts.contains(keyFor(sender));
    }

    /**
     * Flips {@code sender}'s notification preference (persisted) and returns the NEW state:
     * {@code true} when notifications are now enabled for them.
     */
    public boolean toggle(CommandSender sender) {
        String key = keyFor(sender);
        if (optOuts.remove(key)) {
            deleteOptOut(key);
            return true;
        }
        optOuts.add(key);
        saveOptOut(key);
        return false;
    }

    /**
     * Join-time notification: if {@code player} is eligible, checks for a newer release and
     * tells them when one exists. Network runs async; the messages send on the main thread.
     */
    public void notifyOnJoin(Player player) {
        if (!notificationsEnabledFor(player)) {
            return;
        }
        checker.checkAsync(result -> {
            if (result.status() != UpdateChecker.Status.UPDATE_AVAILABLE || !player.isOnline()) {
                return;
            }
            sendAvailableMessages(player, result);
        });
    }

    // ── check handling ───────────────────────────────────────────────────────

    /**
     * Routes a check result: startup checks always report to console (any outcome);
     * a newly seen release is announced once to console + eligible online players.
     */
    private void handleResult(UpdateChecker.Result result, boolean startup) {
        switch (result.status()) {
            case UPDATE_AVAILABLE:
                announceOnce(result);
                break;
            case UP_TO_DATE:
                if (startup) {
                    ConsoleLog.info(plugin, CONSOLE_TAG, "Running the latest version (§f"
                            + result.currentVersion() + "§7).");
                }
                break;
            case FAILED:
            default:
                if (startup) {
                    ConsoleLog.info(plugin, CONSOLE_TAG,
                            "Update check failed (offline or rate-limited) — will retry on the next scheduled check.");
                }
                break;
        }
    }

    /**
     * Announces {@code result}'s release to the console and every eligible online player —
     * once per version per runtime, no matter how many checks see it.
     */
    private void announceOnce(UpdateChecker.Result result) {
        String latest = result.latestVersion() == null ? "?" : result.latestVersion();
        if (!announcedVersions.add(latest)) {
            return;
        }
        ConsoleLog.info(plugin, CONSOLE_TAG, "§eA new OBX release is available§7: §f" + latest
                + " §7(running §f" + result.currentVersion() + "§7)");
        ConsoleLog.info(plugin, CONSOLE_TAG, "Download§8: §f" + UpdateChecker.DOWNLOAD_URL);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (notificationsEnabledFor(player)) {
                sendAvailableMessages(player, result);
            }
        }
    }

    /**
     * Sends the localized "update available" box, closed by recipient-appropriate links:
     * <ul>
     *   <li><b>Players</b> — a clickable button row: {@code [Download]} opens the BuiltByBit
     *       resource page and {@code [Release Notes]} opens the GitHub latest-release page,
     *       each with a hover tooltip.</li>
     *   <li><b>Console</b> — the same two destinations as plain text field rows (the server
     *       console has no hover/click transport), via the
     *       {@code commands.obx.updates.available-link} text block.</li>
     * </ul>
     */
    public void sendAvailableMessages(CommandSender target, UpdateChecker.Result result) {
        plugin.getLanguageManager().send(target, "commands.obx.updates.available",
                linkPlaceholders(result));
        sendLinkActions(target, result);
    }

    /**
     * Sends just the Download / Release-Notes actions that close an updates box:
     * a clickable {@code [Download]} (BuiltByBit) + {@code [Release Notes]} (GitHub
     * latest release) button row with hover tooltips for players, or the same two
     * destinations as plain text field rows for the console. Also used by the
     * {@code /obx updates} / {@code /obx updates check} up-to-date and failed
     * outcomes so a manual check always offers the links.
     */
    public void sendLinkActions(CommandSender target, UpdateChecker.Result result) {
        Map<String, String> placeholders = linkPlaceholders(result);
        if (target instanceof Player) {
            java.util.List<dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart> buttons =
                    new java.util.ArrayList<>();
            buttons.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart.plain("  "));
            buttons.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart.openUrl(
                    plugin.getLanguageManager().get(target, "commands.obx.updates.button.download", placeholders),
                    plugin.getLanguageManager().list(target, "commands.obx.updates.button.download.hover", placeholders),
                    UpdateChecker.DOWNLOAD_URL));
            buttons.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart.plain("  "));
            buttons.add(dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart.openUrl(
                    plugin.getLanguageManager().get(target, "commands.obx.updates.button.notes", placeholders),
                    plugin.getLanguageManager().list(target, "commands.obx.updates.button.notes.hover", placeholders),
                    UpdateChecker.RELEASE_NOTES_URL));
            dev.zcripted.obx.util.text.ComponentMessenger.sendJoinedHoverMessages(target, buttons);
            target.sendMessage(" ");
            return;
        }
        plugin.getLanguageManager().send(target, "commands.obx.updates.available-link", placeholders);
    }

    private static Map<String, String> linkPlaceholders(UpdateChecker.Result result) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("current", result.currentVersion());
        placeholders.put("version", result.currentVersion());
        placeholders.put("latest", result.latestVersion() == null ? "?" : result.latestVersion());
        placeholders.put("url", UpdateChecker.DOWNLOAD_URL);
        placeholders.put("notes", UpdateChecker.RELEASE_NOTES_URL);
        return placeholders;
    }

    // ── scheduling ───────────────────────────────────────────────────────────

    private void schedulePeriodic() {
        long minutes = plugin.getConfig().getLong("updates.check-interval-minutes", DEFAULT_INTERVAL_MINUTES);
        if (minutes <= 0) {
            return;
        }
        long ticks = minutes * 60L * 20L;
        // The repeating trigger is cheap (it only dispatches checkAsync, which does its
        // network I/O off-thread), so a main/global-thread repeating task is fine.
        periodicTask = plugin.getSchedulerAdapter().runRepeating(
                () -> checker.checkAsync(result -> handleResult(result, false)), ticks, ticks);
    }

    private void cancelPeriodic() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
    }

    // ── persistence ──────────────────────────────────────────────────────────

    /** Key by UUID (not name) so the preference survives a name change; console uses a fixed key. */
    private static String keyFor(CommandSender sender) {
        return sender instanceof Player ? ((Player) sender).getUniqueId().toString() : CONSOLE_KEY;
    }

    private void loadOptOuts() {
        SqliteDataStore store = plugin.getDataStore();
        if (store == null || !store.isAvailable()) {
            return; // degrade to in-memory toggles for this session
        }
        store.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (id TEXT PRIMARY KEY)");
        optOuts.addAll(store.queryAll("SELECT id FROM " + TABLE, rs -> rs.getString("id")));
    }

    private void saveOptOut(String key) {
        SqliteDataStore store = plugin.getDataStore();
        if (store != null && store.isAvailable()) {
            store.executeUpdateAsync("INSERT OR IGNORE INTO " + TABLE + " (id) VALUES (?)", key);
        }
    }

    private void deleteOptOut(String key) {
        SqliteDataStore store = plugin.getDataStore();
        if (store != null && store.isAvailable()) {
            store.executeUpdateAsync("DELETE FROM " + TABLE + " WHERE id = ?", key);
        }
    }
}
