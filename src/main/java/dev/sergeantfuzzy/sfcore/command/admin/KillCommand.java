package dev.sergeantfuzzy.sfcore.command.admin;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KillCommand implements CommandExecutor {

    private final Main plugin;
    private final LanguageManager languages;

    public KillCommand(Main plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.kill")) {
            languages.send(player, "core.no-permission");
            return true;
        }

        boolean enabled = plugin.getKillModeManager().toggle(player);
        languages.send(player, enabled ? "admin.kill.mode.enabled" : "admin.kill.mode.disabled");
        return true;
    }
}
