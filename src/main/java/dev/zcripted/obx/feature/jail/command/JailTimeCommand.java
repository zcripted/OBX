package dev.zcripted.obx.feature.jail.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.jail.service.JailService;
import dev.zcripted.obx.util.text.Placeholders;
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
import java.util.UUID;

public class JailTimeCommand extends AbstractObxCommand implements TabCompleter {

    private final JailService jailService;

    public JailTimeCommand(OBX plugin) {
        super(plugin);
        this.jailService = plugin.getJailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.jailtime")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        UUID target;
        String displayName;
        if (args.length >= 1) {
            target = jailService.resolveOfflineUuid(args[0]);
            displayName = args[0];
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "jail.jailtime.usage");
                return true;
            }
            Player player = (Player) sender;
            target = player.getUniqueId();
            displayName = player.getName();
        }
        JailService.JailState state = jailService.getState(target);
        if (state == null) {
            languages.send(sender, "jail.jailtime.not-jailed", Placeholders.with("player", displayName));
            return true;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", displayName);
        placeholders.put("jail", state.getJailName());
        placeholders.put("remaining", jailService.formatDuration(state.getSecondsRemaining()));
        placeholders.put("reason", state.getReason() == null ? "" : state.getReason());
        languages.send(sender, "jail.jailtime.line", placeholders);
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
