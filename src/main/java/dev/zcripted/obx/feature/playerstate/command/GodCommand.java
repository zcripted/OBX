package dev.zcripted.obx.feature.playerstate.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.playerstate.service.GodModeManager;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GodCommand extends AbstractObxCommand implements TabCompleter {

    private final GodModeManager godModeManager;

    public GodCommand(OBX plugin) {
        super(plugin);
        this.godModeManager = plugin.getGodModeManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.god")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        Player target = null;
        if (args.length > 0) {
            target = resolvePlayer(args[0]);
            if (target == null) {
                languages.send(sender, "player.god.target-not-found", Placeholders.with("player", args[0]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            languages.send(sender, "player.god.usage-console");
            return true;
        }

        GameMode mode = target.getGameMode();
        if (mode == GameMode.SPECTATOR || mode == GameMode.CREATIVE) {
            if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
                languages.send(sender, "player.god.invalid-gamemode");
            } else {
                languages.send(sender, "player.god.invalid-gamemode-other", Placeholders.with("target", target.getName()));
            }
            return true;
        }

        boolean enabled = godModeManager.toggle(target);
        boolean self = sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());
        Map<String, String> placeholders = Placeholders.with("target", target.getName(), "sender", sender.getName());
        if (self) {
            languages.send(target, enabled ? "player.god.enabled" : "player.god.disabled");
        } else {
            languages.send(sender, enabled ? "player.god.enabled-other" : "player.god.disabled-other", placeholders);
            languages.send(target, enabled ? "player.god.enabled-target" : "player.god.disabled-target", placeholders);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String current = args[0].toLowerCase(Locale.ENGLISH);
        List<String> suggestions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (name.toLowerCase(Locale.ENGLISH).startsWith(current)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }

    private Player resolvePlayer(String name) {
        if (name == null) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
}
