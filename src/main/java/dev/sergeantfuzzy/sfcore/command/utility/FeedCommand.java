package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FeedCommand implements CommandExecutor {

    private final LanguageManager languages;

    public FeedCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.feed")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        GameMode mode = player.getGameMode();
        if (mode == GameMode.SPECTATOR || mode == GameMode.CREATIVE) {
            languages.send(player, "utility.feed.invalid-gamemode");
            return true;
        }

        player.setFoodLevel(20);
        player.setSaturation(20f);
        languages.send(player, "utility.feed.success");
        return true;
    }
}
