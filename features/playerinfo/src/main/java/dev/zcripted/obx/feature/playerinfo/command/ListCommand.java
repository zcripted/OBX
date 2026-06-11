package dev.zcripted.obx.feature.playerinfo.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.playerstate.AfkService;
import org.bukkit.Bukkit;
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
    private final dev.zcripted.obx.api.staff.VanishApi vanishManager;

    public ListCommand(ObxPlugin plugin) {
        super(plugin);
        this.afkService = plugin.getAfkService();
        this.vanishManager = plugin.getServiceRegistry().get(dev.zcripted.obx.api.staff.VanishApi.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /list is a default command — available to everyone, no permission required.
        // (obx.list.vanished still gates whether vanished players are shown.)
        boolean canSeeVanished = sender.hasPermission("obx.list.vanished");
        // Staff (op) and regular players are bucketed into separate sections.
        List<PlayerListRender.Entry> staff = new ArrayList<>();
        List<PlayerListRender.Entry> players = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            boolean vanished = vanishManager != null && vanishManager.isVanished(online.getUniqueId());
            if (vanished && !canSeeVanished) continue;
            // Staff (op or the obx.staff permission) render red, regular players yellow.
            boolean isStaff = PlayerListRender.isStaff(online);
            String display = (isStaff ? "&c" : "&e") + online.getName();
            if (afkService != null && afkService.isAfk(online.getUniqueId())) {
                display = display + languages.get(sender, "info.list.afk-suffix");
            }
            if (vanished) {
                display = display + languages.get(sender, "info.list.vanish-suffix");
            }
            (isStaff ? staff : players).add(new PlayerListRender.Entry(online.getName(), display));
        }
        staff.sort(PlayerListRender.byVisibleName());
        players.sort(PlayerListRender.byVisibleName());
        int total = staff.size() + players.size();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(total));
        placeholders.put("max", String.valueOf(Bukkit.getMaxPlayers()));
        placeholders.put("staff_count", String.valueOf(staff.size()));
        placeholders.put("player_count", String.valueOf(players.size()));

        // Boxed report styled like /pl and /obx about: the count lives in the header
        // bar; the body holds a Staff section and a Players section (each may read
        // "(none)"), separated by a blank line, between the top/bottom bars.
        languages.send(sender, "info.list.header", placeholders);
        if (total == 0) {
            languages.send(sender, "info.list.no-players", placeholders);
        } else {
            languages.send(sender, "info.list.section.staff", placeholders);
            PlayerListRender.sendNames(languages, sender, staff, "info.list.section-empty");
            languages.send(sender, "info.list.section.players", placeholders);
            PlayerListRender.sendNames(languages, sender, players, "info.list.section-empty");
        }
        languages.send(sender, "info.list.footer", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}