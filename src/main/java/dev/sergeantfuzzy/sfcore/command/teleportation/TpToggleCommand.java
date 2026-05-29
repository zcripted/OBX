package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.teleport.TpaService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TpToggleCommand implements CommandExecutor, TabCompleter {

    private final TpaService tpaService;
    private final LanguageManager languages;

    public TpToggleCommand(Main plugin) {
        this.tpaService = plugin.getTpaService();
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.tptoggle")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        boolean nowAccepting = tpaService.toggle(player.getUniqueId());
        languages.send(player, nowAccepting ? "tpa.toggle-on" : "tpa.toggle-off");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
