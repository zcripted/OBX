package dev.zcripted.obx.core.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.gui.help.HelpGuiMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Replaces Bukkit's built-in /help (and aliases /?, /bukkit:?, /bukkit:help) with a
 * paginated GUI listing every default-true command on the server. Console senders
 * fall back to the default Bukkit help via {@link CommandSender#sendMessage}.
 */
public final class HelpGuiCommand extends AbstractObxCommand {


    public HelpGuiCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Use /obx help for OBX help text. The graphical help requires a player.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.help.gui")) {
            plugin.getLanguageManager().send(player, "core.no-permission");
            return true;
        }
        int page = 1;
        String category = HelpGuiMenu.CATEGORY_ALL;
        for (String arg : args) {
            if (arg == null || arg.isEmpty()) {
                continue;
            }
            try {
                page = Integer.parseInt(arg);
            } catch (NumberFormatException ignored) {
                category = HelpGuiMenu.normalizeCategory(arg);
            }
        }
        HelpGuiMenu.open(plugin, player, page, category);
        return true;
    }
}
