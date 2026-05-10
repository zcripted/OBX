package dev.sergeantfuzzy.sfcore.gui.admin;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
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

    private final Main plugin;
    private final Map<UUID, Long> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingPage = new ConcurrentHashMap<>();

    public StaffMenuInputManager(Main plugin) {
        this.plugin = plugin;
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
        return uuid != null && pending.containsKey(uuid);
    }

    public void clear(UUID uuid) {
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
