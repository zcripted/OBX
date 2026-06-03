package dev.zcripted.obx.feature.mail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.mail.pm.gui.InboxMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /inbox} (alias {@code /inbound}) — opens the private-message inbox GUI.
 * Console has no inbox.
 */
public final class InboxCommand extends AbstractObxCommand {


    public InboxCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.message")) {
            plugin.getLanguageManager().send(sender, "core.no-permission");
            return true;
        }
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "inbox.console");
            return true;
        }
        InboxMenu.open(plugin, (Player) sender);
        return true;
    }
}
