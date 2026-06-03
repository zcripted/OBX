package dev.zcripted.obx.command.admin;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.jail.Jail;
import dev.zcripted.obx.jail.JailService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DelJailCommand extends AbstractObxCommand implements TabCompleter {

    private final JailService jailService;

    public DelJailCommand(OBX plugin) {
        super(plugin);
        this.jailService = plugin.getJailService();
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
