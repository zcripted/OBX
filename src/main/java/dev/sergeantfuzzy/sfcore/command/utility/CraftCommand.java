package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens a virtual crafting table (3x3 workbench) at the player's current location
 * without requiring a real crafting table block. Uses the universal
 * {@link Player#openWorkbench(org.bukkit.Location, boolean)} API which is
 * available on every supported Spigot/Paper version.
 */
public class CraftCommand implements CommandExecutor {

    private final LanguageManager languages;

    public CraftCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        if (!sender.hasPermission("sfcore.craft")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        Player player = (Player) sender;
        try {
            player.openWorkbench(null, true);
            languages.send(player, "utility.craft.opened");
        } catch (Throwable throwable) {
            languages.send(player, "utility.craft.unsupported");
        }
        return true;
    }
}
