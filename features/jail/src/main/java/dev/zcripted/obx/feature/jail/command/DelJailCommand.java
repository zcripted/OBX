package dev.zcripted.obx.feature.jail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.jail.Jail;
import dev.zcripted.obx.feature.jail.service.JailService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DelJailCommand extends AbstractObxCommand implements TabCompleter {

    private final JailService jailService;

    public DelJailCommand(ObxPlugin plugin) {
        super(plugin);
        this.jailService = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.jail.service.JailService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.deljail")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "jail.deljail.usage");
            return true;
        }
        if (jailService.deleteJail(args[0])) {
            languages.send(sender, "jail.deljail.deleted", Placeholders.with("jail", args[0]));
        } else {
            languages.send(sender, "jail.unknown-jail", Placeholders.with("jail", args[0]));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Jail jail : jailService.getJails()) {
                if (jail.getName().toLowerCase().startsWith(prefix)) names.add(jail.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}