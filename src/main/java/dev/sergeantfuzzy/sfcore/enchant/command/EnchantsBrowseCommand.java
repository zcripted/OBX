package dev.sergeantfuzzy.sfcore.enchant.command;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /enchants} (alias {@code /scrolls}) — opens the read-only browse-mode
 * Arcanum menu so players can learn what enchantments exist without obtaining
 * one. Backed by {@link dev.sergeantfuzzy.sfcore.enchant.gui.EnchantAdminMenu}
 * with give/apply actions suppressed.
 *
 * <p>{@code /enchants settings} toggles the caller's personal combat-FX preference
 * (kill banners + combat action-bar feedback), persisted per player.
 */
public final class EnchantsBrowseCommand implements CommandExecutor {

    private final Main plugin;

    public EnchantsBrowseCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Per-player FX toggle — available to any player regardless of the module state.
        if (args.length > 0 && args[0].equalsIgnoreCase("settings")) {
            if (!(sender instanceof Player)) {
                plugin.getLanguageManager().send(sender, "core.player-only");
                return true;
            }
            Player player = (Player) sender;
            boolean enabled = plugin.getEnchantService().getCombatPrefs().toggle(player.getUniqueId());
            plugin.getLanguageManager().send(player, enabled ? "enchant.settings.enabled" : "enchant.settings.disabled");
            return true;
        }
        if (!plugin.getEnchantService().isEnabled()) {
            plugin.getLanguageManager().send(sender, "enchant.module-disabled");
            return true;
        }
        if (!sender.hasPermission("sfcore.enchants.browse")) {
            plugin.getLanguageManager().send(sender, "core.no-permission");
            return true;
        }
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "core.player-only");
            return true;
        }
        plugin.getEnchantAdminMenu().open((Player) sender, true);
        return true;
    }
}
