package dev.zcripted.obx.gui.admin;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks staff members who are currently being prompted for a player-search
 * string after clicking the search head in the {@link StaffMenu}. The chat
 * listener {@code StaffMenuInputListener} consults this manager when an
 * async chat event fires; if the speaker is in the map, the chat event is
 * cancelled and routed here.
 */
public final class StaffMenuInputManager {

    private final OBX plugin;
    private final Map<UUID, Long> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingPage = new ConcurrentHashMap<>();
    private final Map<UUID, PendingAction> pendingActions = new ConcurrentHashMap<>();

    public StaffMenuInputManager(OBX plugin) {
        this.plugin = plugin;
    }

    /** A staff member's queued moderation action awaiting a typed reason. */
    private static final class PendingAction {
        private final String action;     // warn | mute | kick | tempban | ban
        private final UUID targetUuid;
        private final String targetName;

        private PendingAction(String action, UUID targetUuid, String targetName) {
            this.action = action;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
        }
    }

    /**
     * Prompts {@code staff} to type a reason in chat for a moderation
     * {@code action} (warn/mute/kick/tempban/ban) on {@code targetName}. The next
     * chat line is captured and the matching command is executed, reusing its
     * existing box-style response + permission checks.
     */
    public void promptAction(Player staff, UUID targetUuid, String targetName, String action) {
        pendingActions.put(staff.getUniqueId(), new PendingAction(action, targetUuid, targetName));
        plugin.getLanguageManager().send(staff, "admin.staff.action.prompt",
                Placeholders.with("action", action, "player", targetName == null ? "?" : targetName));
    }

    public void promptSearch(Player staff) {
        promptSearch(staff, 0);
    }

    /**
     * Same as {@link #promptSearch(Player)} but remembers {@code returnPage}
     * so a cancelled / empty / not-found input reopens the staff menu at
     * the page the operator was on when they invoked search.
     */
    public void promptSearch(Player staff, int returnPage) {
        pending.put(staff.getUniqueId(), System.currentTimeMillis());
        pendingPage.put(staff.getUniqueId(), Math.max(0, returnPage));
        plugin.getLanguageManager().send(staff, "admin.staff.search.prompt");
    }

    public boolean isPending(UUID uuid) {
        return uuid != null && (pending.containsKey(uuid) || pendingActions.containsKey(uuid));
    }

    public void clear(UUID uuid) {
        pendingActions.remove(uuid);
        pending.remove(uuid);
        pendingPage.remove(uuid);
    }

    /**
     * Routes a chat input back onto the staff member's region thread so the
     * subsequent {@link Player#openInventory(org.bukkit.inventory.Inventory)}
     * call (which Folia requires to run on the entity's region) lands in
     * the right scheduler context. Sync-platform servers behave identically
     * because {@code SchedulerAdapter.runAtEntity} just runs the task next
     * tick on the main thread.
     */
    public void handleInput(Player staff, String message) {
        UUID uuid = staff.getUniqueId();
        if (!pending.containsKey(uuid)) {
            return;
        }
        plugin.getSchedulerAdapter().runAtEntity(staff, () -> processInput(staff, message));
    }

    private void processInput(Player staff, String raw) {
        UUID uuid = staff.getUniqueId();
        // A queued moderation action takes priority over a search prompt.
        PendingAction queued = pendingActions.remove(uuid);
        if (queued != null) {
            processActionInput(staff, queued, raw);
            return;
        }
        pending.remove(uuid);
        Integer storedPage = pendingPage.remove(uuid);
        int returnPage = storedPage == null ? 0 : storedPage;
        String message = raw == null ? "" : raw.trim();

        if (message.isEmpty()) {
            plugin.getLanguageManager().send(staff, "admin.staff.search.empty");
            StaffMenu.open(plugin, staff, returnPage);
            return;
        }
        if (message.equalsIgnoreCase("cancel")) {
            plugin.getLanguageManager().send(staff, "admin.staff.search.cancelled");
            StaffMenu.open(plugin, staff, returnPage);
            return;
        }

        Player target = matchOnlinePlayer(message, staff);
        if (target == null) {
            plugin.getLanguageManager().send(staff, "admin.staff.search.not-online",
                    Placeholders.with("player", message));
            StaffMenu.open(plugin, staff, returnPage);
            return;
        }
        if (target.getUniqueId().equals(staff.getUniqueId())) {
            // The /staff GUI hides the viewer's own head and the search flow
            // mirrors that — staff can't open their own action menu.
            plugin.getLanguageManager().send(staff, "admin.staff.search.self",
                    Placeholders.with("player", target.getName()));
            StaffMenu.open(plugin, staff, returnPage);
            return;
        }

        plugin.getLanguageManager().send(staff, "admin.staff.search.success",
                Placeholders.with("player", target.getName()));
        StaffActionMenu.open(plugin, staff, target.getUniqueId(), target.getName());
    }

    /**
     * Runs the queued moderation command with the typed reason. Empty input or
     * "cancel" aborts (and reopens the action menu). The actual command
     * ({@code /warn|/mute|/kick|/tempban|/ban}) is executed as the staff member
     * so its existing box-style response, permission checks, and logging all fire.
     */
    private void processActionInput(Player staff, PendingAction req, String raw) {
        String message = raw == null ? "" : raw.trim();
        String targetName = req.targetName != null ? req.targetName : "?";
        if (message.isEmpty() || message.equalsIgnoreCase("cancel")) {
            plugin.getLanguageManager().send(staff, "admin.staff.action.cancelled");
            StaffActionMenu.open(plugin, staff, req.targetUuid, req.targetName);
            return;
        }
        staff.performCommand(req.action + " " + targetName + " " + message);
        // Reopen the action menu so the operator can take further action.
        StaffActionMenu.open(plugin, staff, req.targetUuid, req.targetName);
    }

    /**
     * Resolves an online player by exact name first, then prefix
     * match (case-insensitive). The viewer is excluded from prefix matches
     * so a partial name doesn't accidentally resolve to themselves. Returns
     * null when nothing matches.
     */
    private Player matchOnlinePlayer(String input, Player viewer) {
        Player exact = Bukkit.getPlayerExact(input);
        if (exact != null) {
            return exact;
        }
        String lower = input.toLowerCase(Locale.ROOT);
        Player best = null;
        int bestLen = Integer.MAX_VALUE;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || online.getName() == null) {
                continue;
            }
            if (viewer != null && online.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            String name = online.getName().toLowerCase(Locale.ROOT);
            if (name.startsWith(lower) && name.length() < bestLen) {
                best = online;
                bestLen = name.length();
            }
        }
        return best;
    }
}
