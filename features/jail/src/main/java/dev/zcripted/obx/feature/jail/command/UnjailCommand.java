package dev.zcripted.obx.feature.jail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.jail.service.JailService;
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

    public UnjailCommand(ObxPlugin plugin) {
        super(plugin);
        this.jailService = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.jail.service.JailService.class);
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
            // Relocate the freed player out of the jail build — clearing state alone would
            // leave them standing inside it with the move-containment no longer pulling them back.
            jailService.teleportToRelease(target);
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
