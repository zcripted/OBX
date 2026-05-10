package dev.sergeantfuzzy.sfcore.command.admin;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.gui.admin.StaffMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * {@code /staff} — opens the staff overview GUI listing every online player
 * (excluding self) with hover-tooltip profile cards and a per-player
 * moderator action sub-menu.
 *
 * <p>Permission-gated by {@code sfcore.staff.menu} (op by default). The
 * tab-completer returns an empty list both for permission denial and for
 * the no-arguments case so non-permitted players can't even discover the
 * command's existence in tab-completion.
 */
public final class StaffCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "sfcore.staff.menu";

    private final Main plugin;

    public StaffCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(PERMISSION)) {
            plugin.getLanguageManager().send(player, "core.no-permission");
            return true;
        }
        StaffMenu.open(plugin, player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
