package dev.zcripted.obx.command.teleportation;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.util.text.ComponentMessenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code /pos} (alias {@code /position}) — shows the player's coordinates in a clean styled
 * report with a click-to-copy line, and starts a live action-bar readout that updates as
 * they move and fades after ~5 seconds. Default-allowed ({@code obx.position}); the
 * console has no in-world position.
 */
public final class PositionCommand extends AbstractObxCommand {

    private static final int ACTIONBAR_TICKS = 100; // ~5 seconds
    private static final long PERIOD = 4L;
    private static final String[] CARDINALS = {"South", "South-West", "West", "North-West", "North", "North-East", "East", "South-East"};

    private final Map<UUID, SchedulerAdapter.CancellableTask> trackers = new ConcurrentHashMap<UUID, SchedulerAdapter.CancellableTask>();

    public PositionCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.position")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (!(sender instanceof Player)) {
            languages.send(sender, "position.console");
            return true;
        }
        Player player = (Player) sender;
        Location loc = player.getLocation();
        String x = Integer.toString(loc.getBlockX());
        String y = Integer.toString(loc.getBlockY());
        String z = Integer.toString(loc.getBlockZ());
        String coords = x + " " + y + " " + z;

        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("x", x);
        ph.put("y", y);
        ph.put("z", z);
        ph.put("world", loc.getWorld() == null ? "world" : loc.getWorld().getName());
        ph.put("facing", facing(loc.getYaw()));

        player.sendMessage(languages.get(player, "position.header"));
        player.sendMessage(languages.get(player, "core.divider-line"));
        player.sendMessage(languages.get(player, "position.coords-line", ph));
        player.sendMessage(languages.get(player, "position.world-line", ph));
        // Click-to-copy line.
        String copyLabel = languages.get(player, "position.copy-label");
        String copyHover = languages.get(player, "position.copy-hover", Collections.singletonMap("coords", coords));
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<ComponentMessenger.InteractiveMessagePart>();
        parts.add(ComponentMessenger.InteractiveMessagePart.plain("  "));
        parts.add(ComponentMessenger.InteractiveMessagePart.copy(copyLabel, Collections.singletonList(copyHover), coords));
        ComponentMessenger.sendJoinedHoverMessages(player, parts);

        startTracker(player.getUniqueId());
        return true;
    }

    /** Starts (or refreshes) the live coordinate action bar for ~5 seconds. */
    private void startTracker(final UUID id) {
        SchedulerAdapter scheduler = plugin.getSchedulerAdapter();
        if (scheduler == null) {
            return;
        }
        SchedulerAdapter.CancellableTask existing = trackers.remove(id);
        if (existing != null) {
            existing.cancel();
        }
        final int[] elapsed = {0};
        final SchedulerAdapter.CancellableTask[] handle = new SchedulerAdapter.CancellableTask[1];
        handle[0] = scheduler.runRepeating(new Runnable() {
            @Override
            public void run() {
                try {
                    Player player = Bukkit.getPlayer(id);
                    if (player == null || !player.isOnline() || elapsed[0] >= ACTIONBAR_TICKS) {
                        stop(id, handle[0]);
                        return;
                    }
                    Location loc = player.getLocation();
                    Map<String, String> ph = new LinkedHashMap<String, String>();
                    ph.put("x", Integer.toString(loc.getBlockX()));
                    ph.put("y", Integer.toString(loc.getBlockY()));
                    ph.put("z", Integer.toString(loc.getBlockZ()));
                    ComponentMessenger.sendActionBar(player, languages.get(player, "position.actionbar", ph));
                    elapsed[0] += PERIOD;
                } catch (Throwable broken) {
                    stop(id, handle[0]);
                }
            }
        }, 1L, PERIOD);
        trackers.put(id, handle[0]);
    }

    private void stop(UUID id, SchedulerAdapter.CancellableTask task) {
        if (task != null) {
            task.cancel();
        }
        trackers.remove(id);
    }

    private static String facing(float yaw) {
        int index = Math.round(yaw / 45f) & 7;
        return CARDINALS[index];
    }
}
