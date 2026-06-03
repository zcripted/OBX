package dev.zcripted.obx.command.info;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.util.control.AfkService;
import dev.zcripted.obx.util.control.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListCommand extends AbstractObxCommand implements TabCompleter {

    private final AfkService afkService;
    private final VanishManager vanishManager;

    public ListCommand(Main plugin) {
        super(plugin);
        this.afkService = plugin.getAfkService();
        this.vanishManager = plugin.getVanishManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.list")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        boolean canSeeVanished = sender.hasPermission("obx.list.vanished");
        List<String> visibleNames = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            boolean vanished = vanishManager != null && vanishManager.isVanished(online.getUniqueId());
            if (vanished && !canSeeVanished) continue;
            // OP / staff names render red, regular players yellow (matches the
            // tablist + scoreboard name-team convention, which also keys on isOp()).
            String color = online.isOp() ? "&c" : "&e";
            String name = color + online.getName();
            if (afkService != null && afkService.isAfk(online.getUniqueId())) {
                name = name + languages.get(sender, "info.list.afk-suffix");
            }
            if (vanished) {
                name = name + languages.get(sender, "info.list.vanish-suffix");
            }
            visibleNames.add(name);
        }
        // Sort by the visible name (ignoring the &-color prefix so colors don't
        // group the list by op status).
        visibleNames.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', a)),
                ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', b))));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(visibleNames.size()));
        placeholders.put("max", String.valueOf(Bukkit.getMaxPlayers()));
        placeholders.put("names", String.join("&7, ", visibleNames));

        // Boxed report styled like /pl and /obx about: top bar, summary line,
        // colored player names (or an empty notice), bottom bar.
        languages.send(sender, "info.list.header", placeholders);
        languages.send(sender, "info.list.summary", placeholders);
        if (visibleNames.isEmpty()) {
            languages.send(sender, "info.list.no-players", placeholders);
        } else {
            languages.send(sender, "info.list.entries", placeholders);
        }
        languages.send(sender, "info.list.footer", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
