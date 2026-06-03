package dev.zcripted.obx.feature.jail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.jail.service.JailService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SetJailCommand extends AbstractObxCommand implements TabCompleter {

    private final JailService jailService;

    public SetJailCommand(ObxPlugin plugin) {
        super(plugin);
        this.jailService = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.jail.service.JailService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.setjail")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "jail.setjail.usage");
            return true;
        }
        if (jailService.createJail(args[0], player.getLocation())) {
            languages.send(player, "jail.setjail.created", Placeholders.with("jail", args[0]));
        } else {
            languages.send(player, "jail.setjail.failed", Placeholders.with("jail", args[0]));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> matches = new java.util.ArrayList<>();
        for (dev.zcripted.obx.api.jail.Jail jail : jailService.getJails()) {
            if (jail.getName().toLowerCase().startsWith(prefix)) matches.add(jail.getName());
        }
        return matches;
    }
}
