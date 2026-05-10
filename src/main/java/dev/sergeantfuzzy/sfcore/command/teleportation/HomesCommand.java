package dev.sergeantfuzzy.sfcore.command.teleportation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.StringJoiner;

public class HomesCommand implements CommandExecutor {

    private final DataService dataService;
    private final LanguageManager languages;

    public HomesCommand(Main plugin) {
        this.dataService = plugin.getDataService();
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.home.list")) {
            languages.send(player, "core.no-permission");
            return true;
        }

        Set<String> homes = dataService.getHomes(player.getUniqueId());
        StringJoiner joiner = new StringJoiner(", ");
        for (String home : homes) {
            joiner.add(home);
        }
        String list = joiner.length() == 0 ? languages.get(player, "teleport.homes.none") : joiner.toString();
        languages.send(player, "teleport.homes.list", Placeholders.with("homes", list));
        return true;
    }
}
