package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens a virtual enchanting table at the player's current location. Uses the
 * universally-available {@link Player#openEnchanting(org.bukkit.Location, boolean)}
 * API. The {@code force} flag bypasses the bookshelf-power requirement so the
 * level-30 enchantment slot is always reachable.
 */
public class EnchantCommand implements CommandExecutor {

    private final LanguageManager languages;

    public EnchantCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        if (!sender.hasPermission("sfcore.enchant")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        Player player = (Player) sender;
        try {
            player.openEnchanting(null, true);
            languages.send(player, "utility.enchant.opened");
        } catch (Throwable throwable) {
            languages.send(player, "utility.enchant.unsupported");
        }
        return true;
    }
}
