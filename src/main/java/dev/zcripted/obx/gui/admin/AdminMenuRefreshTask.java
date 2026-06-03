package dev.zcripted.obx.gui.admin;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Once a second, re-renders the live status items of any open Server Control /
 * Server State / Player Access / Performance admin submenu so their
 * whitelist / join-lock / redstone / max-player readouts stay accurate while the
 * menu is open — including when a different admin (or a reload) changes the
 * state. On Folia the per-player refresh is dispatched onto each player's entity
 * region (inventory access must run there); elsewhere it runs inline on the main
 * thread the repeating task already executes on.
 */
public final class AdminMenuRefreshTask {

    // ~0.5s (10 ticks). Fast enough that the Staff Menu's online-player list and
    // the viewer head's live session/active timers update near-immediately, while
    // staying cheap — only players who actually have one of these menus open do
    // any work.
    private static final long PERIOD_TICKS = 10L;

    private final Main plugin;
    private SchedulerAdapter.CancellableTask task;

    public AdminMenuRefreshTask(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        cancel();
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        final boolean folia = scheduler.isFolia();
        Runnable tick = () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (folia) {
                    scheduler.runAtEntity(player, () -> refreshPlayer(player));
                } else {
                    refreshPlayer(player);
                }
            }
        };
        task = scheduler.runRepeating(tick, PERIOD_TICKS, PERIOD_TICKS);
    }

    private void refreshPlayer(Player player) {
        Inventory top;
        try {
            top = player.getOpenInventory().getTopInventory();
        } catch (Throwable ignored) {
            return;
        }
        if (top == null) {
            return;
        }
        InventoryHolder holder = top.getHolder();
        if (holder instanceof AdminSubMenu.Holder) {
            AdminSubMenu.refresh((AdminSubMenu.Holder) holder);
        } else if (holder instanceof StaffMenuHolder) {
            // Live online-player list + viewer head session/active timers.
            StaffMenu.refresh(plugin, player, (StaffMenuHolder) holder);
        } else {
            return;
        }
        try {
            player.updateInventory();
        } catch (Throwable ignored) {
            // updateInventory is deprecated/absent on some forks — non-fatal.
        }
    }

    public void cancel() {
        if (task != null) {
            try {
                task.cancel();
            } catch (Throwable ignored) {
                // Already cancelled — no-op.
            }
            task = null;
        }
    }
}
