package dev.sergeantfuzzy.sfcore.command.messaging;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.messaging.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SocialSpyCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final MessageService messageService;

    public SocialSpyCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.messageService = plugin.getMailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.socialspy")) {
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
