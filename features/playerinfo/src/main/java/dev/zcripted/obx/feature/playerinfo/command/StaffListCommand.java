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

/**
 * {@code /stafflist} (aliases {@code /onlinestaff}, {@code /staffonline}) — the player-list
 * box, filtered to online op/staff only. Names are click-to-message just like {@code /list}
 * (shared via {@link PlayerListRender}); the count lives in the header bar.
 */
public class StaffListCommand extends AbstractObxCommand implements TabCompleter {

    private final AfkService afkService;
    private final dev.zcripted.obx.api.staff.VanishApi vanishManager;

    public StaffListCommand(ObxPlugin plugin) {
        super(plugin);
        this.afkService = plugin.getAfkService();
        this.vanishManager = plugin.getServiceRegistry().get(dev.zcripted.obx.api.staff.VanishApi.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.stafflist")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        boolean canSeeVanished = sender.hasPermission("obx.list.vanished");
        List<PlayerListRender.Entry> staff = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!PlayerListRender.isStaff(online)) {
                continue; // op or obx.staff only
            }
            boolean vanished = vanishManager != null && vanishManager.isVanished(online.getUniqueId());
            if (vanished && !canSeeVanished) continue;
            String display = "&c" + online.getName();
            if (afkService != null && afkService.isAfk(online.getUniqueId())) {
                display = display + languages.get(sender, "info.list.afk-suffix");
            }
            if (vanished) {
                display = display + languages.get(sender, "info.list.vanish-suffix");
            }
            staff.add(new PlayerListRender.Entry(online.getName(), display));
        }
        staff.sort(PlayerListRender.byVisibleName());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(staff.size()));

        languages.send(sender, "info.stafflist.header", placeholders);
        PlayerListRender.sendNames(languages, sender, staff, "info.stafflist.none");
        languages.send(sender, "info.list.footer", placeholders);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}