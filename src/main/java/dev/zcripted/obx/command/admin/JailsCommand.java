package dev.zcripted.obx.command.admin;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.jail.Jail;
import dev.zcripted.obx.jail.JailService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class JailsCommand extends AbstractObxCommand implements TabCompleter {

    private final JailService jailService;

    public JailsCommand(OBX plugin) {
        super(plugin);
        this.jailService = plugin.getJailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.jails")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        java.util.Collection<Jail> all = jailService.getJails();
        if (all.isEmpty()) {
            languages.send(sender, "jail.jails.empty");
            return true;
        }
        StringBuilder names = new StringBuilder();
        int i = 0;
        for (Jail jail : all) {
            if (i++ > 0) names.append(", ");
            names.append(jail.getName());
        }
        languages.send(sender, "jail.jails.list",
                Placeholders.with("count", all.size(), "names", names.toString()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
