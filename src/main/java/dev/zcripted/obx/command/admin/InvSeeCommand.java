package dev.zcripted.obx.command.admin;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.gui.admin.InvSeeMenu;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /invsee <player>} — opens a viewer of the target player's inventory.
 *
 * <p>Two permission tiers gate access:
 * <ul>
 *   <li>{@code obx.invsee.basic} (level 1): allowed to view inventories of
 *   non-op players that don't themselves have any {@code obx.invsee.*} permission.</li>
 *   <li>{@code obx.invsee.full} (level 2): allowed to view <em>any</em>
 *   player's inventory, including operators and other staff.</li>
 * </ul>
 * The full permission supersedes the basic permission. A staff member with only
 * the basic permission attempting to view a privileged player receives a clear
 * "no permission for this target" message rather than a generic refusal.
 */
public class InvSeeCommand extends AbstractObxCommand implements TabCompleter {

    public static final String PERMISSION_BASIC = "obx.invsee.basic";
    public static final String PERMISSION_FULL = "obx.invsee.full";

    private final SimpleDateFormat timestamp = new SimpleDateFormat("HH:mm:ss");

    public InvSeeCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "player.invsee.usage-console");
            return true;
        }
        Player viewer = (Player) sender;

        boolean hasFull = viewer.hasPermission(PERMISSION_FULL);
        boolean hasBasic = hasFull || viewer.hasPermission(PERMISSION_BASIC);
        if (!hasBasic) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        if (args.length < 1) {
            languages.send(sender, "player.invsee.usage");
            return true;
        }

        Player target = resolvePlayer(args[0]);
        if (target == null) {
            languages.send(sender, "player.invsee.target-not-found", Placeholders.with("player", args[0]));
            return true;
        }
        boolean self = target.getUniqueId().equals(viewer.getUniqueId());
        if (self && !hasFull) {
            // Basic-tier viewers can't /invsee themselves; the privileged-target
            // check below would catch it anyway (basic-tier holders are
            // privileged), but a dedicated message is clearer.
            languages.send(sender, "player.invsee.cannot-view-self");
            return true;
        }
        if (!self && !hasFull && isPrivilegedTarget(target)) {
            // Basic permission can only view non-op, non-staff targets.
            languages.send(sender, "player.invsee.no-permission-target",
                    Placeholders.with("player", target.getName()));
            return true;
        }

        try {
            // Live-mirror chest GUI — see InvSeeMenu javadoc for the layout.
            // Replaces the legacy openInventory(target.getInventory()) call,
            // which couldn't carry a custom title and exposed the target's
            // raw layout with no separator between hotbar and main storage.
            InvSeeMenu.open(plugin, viewer, target);
        } catch (Throwable failure) {
            languages.send(sender, "player.invsee.open-failed",
                    Placeholders.with("player", target.getName()));
            return true;
        }

        languages.send(sender,
                self ? "player.invsee.opened-self" : "player.invsee.opened",
                Placeholders.with("player", target.getName()));
        writeStaffLog(viewer, target, hasFull);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        Player viewer = (Player) sender;
        boolean hasFull = viewer.hasPermission(PERMISSION_FULL);
        if (!hasFull && !viewer.hasPermission(PERMISSION_BASIC)) {
            return Collections.emptyList();
        }
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ENGLISH);
        List<String> suggestions = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            if (!hasFull && isPrivilegedTarget(online)) {
                continue;
            }
            String name = online.getName();
            if (name.toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }

    /**
     * A target is "privileged" — and therefore off-limits to the basic tier — if
     * they're an operator OR they hold any of the {@code obx.invsee.*}
     * permissions themselves. The latter check prevents a basic-tier staff
     * member from spying on full-tier staff who haven't been opped.
     */
    private boolean isPrivilegedTarget(Player target) {
        return target.isOp()
                || target.hasPermission(PERMISSION_BASIC)
                || target.hasPermission(PERMISSION_FULL);
    }

    private void writeStaffLog(Player viewer, Player target, boolean fullTier) {
        String tier = languages.formatConsole(
                fullTier ? "player.invsee.console-tier-full" : "player.invsee.console-tier-basic",
                Collections.<String, String>emptyMap());
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("time", timestamp.format(new Date()));
        placeholders.put("tier", tier);
        placeholders.put("viewer", viewer.getName());
        placeholders.put("target", target.getName());
        plugin.writeConsoleLine(languages.formatConsole("player.invsee.console-log", placeholders));
    }

    private Player resolvePlayer(String name) {
        if (name == null) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return online;
            }
        }
        return null;
    }
}
