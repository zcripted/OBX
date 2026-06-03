package dev.zcripted.obx.feature.mail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.mail.mail.MailService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SocialSpyCommand extends AbstractObxCommand implements TabCompleter {

    private final MailService messageService;

    public SocialSpyCommand(OBX plugin) {
        super(plugin);
        this.messageService = plugin.getMailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.socialspy")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        boolean nowOn = messageService.toggleSocialSpy(player.getUniqueId());
        languages.send(player, nowOn ? "messaging.socialspy.on" : "messaging.socialspy.off");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
