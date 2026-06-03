package dev.zcripted.obx.command.admin;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.jail.JailService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class UnjailCommand extends AbstractObxCommand implements TabCompleter {

    private final JailService jailService;

    public UnjailCommand(OBX plugin) {
        super(plugin);
        this.jailService = plugin.getJailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.unjail")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "jail.unjail.usage");
            return true;
        }
        UUID uuid = jailService.resolveOfflineUuid(args[0]);
        if (uuid == null || !jailService.isJailed(uuid)) {
            languages.send(sender, "jail.unjail.not-jailed", Placeholders.with("player", args[0]));
            return true;
        }
        jailService.clearState(uuid);
        Player target = Bukkit.getPlayer(uuid);
        if (target != null && target.isOnline()) {
            languages.send(target, "jail.unjail.released-target");
        }
        languages.send(sender, "jail.unjail.released", Placeholders.with("player", args[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
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
