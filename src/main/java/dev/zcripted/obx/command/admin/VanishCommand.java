package dev.zcripted.obx.command.admin;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.control.VanishManager;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /vanish [player]} — staff-only invisibility toggle. Hides the executing
 * staff member (or a named target with the appropriate permission) from every
 * other player on the server, suppresses mob aggro, blocks passive damage
 * triggers, and silences pickup events. Toggles are mirrored to the console
 * with a clean ANSI-coloured staff-action log line.
 */
public class VanishCommand extends AbstractObxCommand implements TabCompleter {

    private final VanishManager vanishManager;
    private final SimpleDateFormat timestamp = new SimpleDateFormat("HH:mm:ss");

    public VanishCommand(OBX plugin) {
        super(plugin);
        this.vanishManager = plugin.getVanishManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.vanish")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("obx.vanish.others")) {
                languages.send(sender, "core.no-permission");
                return true;
            }
            target = resolvePlayer(args[0]);
            if (target == null) {
                languages.send(sender, "player.vanish.target-not-found", Placeholders.with("player", args[0]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            languages.send(sender, "player.vanish.usage-console");
            return true;
        }

        boolean nowVanished = vanishManager.toggle(target);
        boolean self = sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());

        if (self) {
            languages.send(target, nowVanished ? "player.vanish.enabled" : "player.vanish.disabled");
        } else {
            languages.send(sender,
                    nowVanished ? "player.vanish.enabled-other" : "player.vanish.disabled-other",
                    Placeholders.with("target", target.getName()));
            languages.send(target,
                    nowVanished ? "player.vanish.enabled-target" : "player.vanish.disabled-target",
                    Placeholders.with("sender", sender.getName()));
        }

        writeStaffLog(sender, target, nowVanished);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("obx.vanish.others")) {
            return Collections.emptyList();
        }
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ENGLISH);
        List<String> suggestions = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            String name = online.getName();
            if (name.toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }

    /**
     * Emits a single colour-coded staff-action line to the console. Every piece
     * of the line — template, state label, detail label — is sourced from the
     * language file so the format can be customised or translated without code
     * changes. Goes through {@link OBX#writeConsoleLine(String)} which
     * translates the legacy {@code §} codes to ANSI escapes for terminals.
     */
    private void writeStaffLog(CommandSender sender, Player target, boolean nowVanished) {
        boolean self = sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());
        String state = languages.formatConsole(
                nowVanished ? "player.vanish.console-state-on" : "player.vanish.console-state-off",
                Collections.<String, String>emptyMap());
        String detail = self
                ? languages.formatConsole("player.vanish.console-detail-self", Collections.<String, String>emptyMap())
                : languages.formatConsole("player.vanish.console-detail-other", Placeholders.with("sender", sender.getName()));
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("time", timestamp.format(new Date()));
        placeholders.put("target", target.getName());
        placeholders.put("state", state);
        placeholders.put("detail", detail);
        String line = languages.formatConsole("player.vanish.console-log", placeholders);
        plugin.writeConsoleLine(line);
    }

    private Player resolvePlayer(String name) {
        if (name == null) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return online;
            }
        }
        return null;
    }
}
