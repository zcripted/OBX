package dev.zcripted.obx.command.teleportation;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TpAllCommand extends AbstractObxCommand implements TabCompleter {


    public TpAllCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player staff = (Player) sender;
        if (!staff.hasPermission("obx.tpall")) {
            languages.send(staff, "core.no-permission");
            return true;
        }
        int moved = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(staff.getUniqueId())) {
                continue;
            }
            plugin.getDataService().setBack(target.getUniqueId(), target.getLocation());
            plugin.getTeleportManager().teleportPlayer(target, staff.getLocation(),
                    "teleport.tphere.target-message", Placeholders.with("player", staff.getName()));
            moved++;
        }
        languages.send(staff, "teleport.tpall.success", Placeholders.with("count", moved));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
