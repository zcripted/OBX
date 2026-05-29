package dev.sergeantfuzzy.sfcore.command.info;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.control.AfkService;
import dev.sergeantfuzzy.sfcore.util.control.VanishManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final AfkService afkService;
    private final VanishManager vanishManager;

    public ListCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.afkService = plugin.getAfkService();
        this.vanishManager = plugin.getVanishManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.list")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        boolean canSeeVanished = sender.hasPermission("sfcore.list.vanished");
        List<String> visibleNames = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            boolean vanished = vanishManager != null && vanishManager.isVanished(online.getUniqueId());
            if (vanished && !canSeeVanished) continue;
            String name = online.getName();
            if (afkService != null && afkService.isAfk(online.getUniqueId())) {
                name = name + languages.get(sender, "info.list.afk-suffix");
            }
            if (vanished) {
                name = name + languages.get(sender, "info.list.vanish-suffix");
            }
            visibleNames.add(name);
        }
        Collections.sort(visibleNames, String.CASE_INSENSITIVE_ORDER);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(visibleNames.size()));
        placeholders.put("max", String.valueOf(Bukkit.getMaxPlayers()));
        placeholders.put("names", visibleNames.isEmpty()
                ? languages.get(sender, "info.list.no-players")
                : String.join(", ", visibleNames));
        languages.send(sender, "info.list.header", placeholders);
        languages.send(sender, "info.list.entries", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
