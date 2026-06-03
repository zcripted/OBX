package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.control.AfkService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AfkCommand extends AbstractObxCommand implements TabCompleter {

    private final AfkService afkService;

    public AfkCommand(OBX plugin) {
        super(plugin);
        this.afkService = plugin.getAfkService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            if (!sender.hasPermission("obx.afk.others")) {
                languages.send(sender, "core.no-permission");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                languages.send(sender, "teleport.tp.not-online", Placeholders.with("player", args[0]));
                return true;
            }
            boolean nowAfk = !afkService.isAfk(target.getUniqueId());
            afkService.setAfk(target, nowAfk);
            languages.send(sender, nowAfk ? "afk.set-other-on" : "afk.set-other-off",
                    Placeholders.with("player", target.getName()));
            return true;
        }
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.afk")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        boolean nowAfk = !afkService.isAfk(player.getUniqueId());
        afkService.setAfk(player, nowAfk);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("obx.afk.others")) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
