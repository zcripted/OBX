package dev.sergeantfuzzy.sfcore.command.admin;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.jail.Jail;
import dev.sergeantfuzzy.sfcore.jail.JailService;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger.InteractiveMessagePart;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JailCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final LanguageManager languages;
    private final JailService jailService;

    public JailCommand(Main plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
        this.jailService = plugin.getJailService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.jail")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "jail.usage");
            return true;
        }
        String targetName = args[0];
        UUID targetUuid = jailService.resolveOfflineUuid(targetName);
        if (targetUuid == null) {
            languages.send(sender, "info.unknown-player", Placeholders.with("player", targetName));
            return true;
        }
        String jailName = args[1];
        Jail jail = jailService.getJail(jailName);
        if (jail == null) {
            languages.send(sender, "jail.unknown-jail", Placeholders.with("jail", jailName));
            return true;
        }
        long durationSeconds = 0L;
        String reason = languages.get(sender, "jail.default-reason");
        if (args.length >= 3) {
            Long parsed = jailService.parseDuration(args[2]);
            if (parsed != null) {
                durationSeconds = parsed;
                if (args.length >= 4) {
                    reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                }
            } else {
                reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }
        }
        jailService.jail(targetUuid, jail.getName(), durationSeconds, reason);
        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null && target.isOnline()) {
            jailService.teleportToJail(target);
            Map<String, String> notify = new HashMap<>();
            notify.put("jail", jail.getName());
            notify.put("reason", reason);
            notify.put("duration", jailService.formatDuration(durationSeconds));
            languages.send(target, "jail.notify-target", notify);
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", targetName);
        placeholders.put("jail", jail.getName());
        placeholders.put("duration", jailService.formatDuration(durationSeconds));
        placeholders.put("reason", reason);
        // Adventure-driven success line with a clickable [Unjail] button.
        if (sender instanceof Player) {
            String prefix = languages.get(sender, "jail.jailed-prefix", placeholders);
            String unjailLabel = languages.get(sender, "jail.button.unjail");
            List<String> hover = languages.list(sender, "jail.button.unjail-hover",
                    Placeholders.with("player", targetName));
            List<InteractiveMessagePart> parts = new ArrayList<>();
            parts.add(InteractiveMessagePart.plain(prefix + " "));
            parts.add(InteractiveMessagePart.interactive(unjailLabel, hover, "/unjail " + targetName, true));
            ComponentMessenger.sendJoinedHoverMessages(sender, parts);
        } else {
            languages.send(sender, "jail.jailed-prefix", placeholders);
        }
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
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Jail jail : jailService.getJails()) {
                if (jail.getName().toLowerCase().startsWith(prefix)) names.add(jail.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
