package dev.zcripted.obx.feature.playerstate.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.command.PlayerActionCommand;
import dev.zcripted.obx.feature.playerstate.service.KillModeManager;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code /kill} — with no argument, toggles "kill mode" (left-click the entity in your crosshair to
 * execute it). With a player argument, {@code /kill <player>} kills that online player directly.
 */
public class KillCommand extends PlayerActionCommand implements TabCompleter {

    public KillCommand(ObxPlugin plugin) {
        super(plugin, "obx.kill");
    }

    @Override
    protected void run(Player player, String[] args) {
        if (args.length >= 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                languages.send(player, "admin.kill.not-online", Placeholders.with("player", args[0]));
                return;
            }
            killSafe(target);
            languages.send(player, "admin.kill.player-killed", Placeholders.with("target", target.getName()));
            return;
        }
        boolean enabled = plugin.getServiceRegistry().get(KillModeManager.class).toggle(player);
        languages.send(player, enabled ? "admin.kill.mode.enabled" : "admin.kill.mode.disabled");
    }

    /** Sets the target's health to 0 on the target's own region thread (Folia-safe) or inline. */
    private void killSafe(final Player target) {
        if (plugin.getSchedulerAdapter().isFolia()) {
            plugin.getSchedulerAdapter().runAtEntity(target, () -> target.setHealth(0.0));
        } else {
            target.setHealth(0.0);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) {
                    names.add(online.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
