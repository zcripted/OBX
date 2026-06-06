package dev.zcripted.obx.feature.mail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.mail.staffchat.StaffChatService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * {@code /staffchat} (aliases {@code /sc}, {@code /achat}). The bare command toggles
 * persistent staff-chat mode for the player (their normal chat then routes into staff
 * chat) and replies with a box-style status; {@code /staffchat <message>} dispatches a
 * single staff-chat line, working whether or not toggle mode is on. The dispatch + toggle
 * state live in {@link StaffChatService}.
 */
public class StaffChatCommand extends AbstractObxCommand implements TabCompleter {

    public StaffChatCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.staffchat")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        StaffChatService staffChat = plugin.getServiceRegistry().get(StaffChatService.class);

        // Bare /sc → toggle staff-chat mode.
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            Player player = (Player) sender;
            boolean enabled = staffChat.toggle(player);
            languages.send(player, enabled ? "messaging.staffchat.toggle.enabled" : "messaging.staffchat.toggle.disabled");
            return true;
        }

        // /sc <message> → send a single staff-chat line.
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) body.append(' ');
            body.append(args[i]);
        }
        staffChat.dispatch(sender, body.toString());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
