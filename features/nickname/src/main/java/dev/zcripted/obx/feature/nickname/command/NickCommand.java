package dev.zcripted.obx.feature.nickname.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.nickname.service.NicknameService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NickCommand extends AbstractObxCommand implements TabCompleter {

    private final NicknameService nicknames;

    public NickCommand(ObxPlugin plugin) {
        super(plugin);
        this.nicknames = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.nickname.service.NicknameService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            languages.send(sender, "nickname.usage");
            return true;
        }
        // The optional target is the LAST argument, treated as a target only when it's an
        // online player and the sender may nick others; otherwise every argument forms the
        // (possibly multi-word) nickname. So /nick cool name → self nick "cool name", and
        // /nick cool name Steve → set Steve's nick to "cool name".
        Player target;
        String desired;
        Player maybeTarget = (args.length >= 2 && sender.hasPermission("obx.nick.others"))
                ? Bukkit.getPlayerExact(args[args.length - 1]) : null;
        if (maybeTarget != null && maybeTarget.isOnline()) {
            target = maybeTarget;
            desired = String.join(" ", Arrays.copyOfRange(args, 0, args.length - 1));
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("obx.nick")) {
                languages.send(target, "core.no-permission");
                return true;
            }
            desired = String.join(" ", args);
        }

        // Trim/collapse whitespace up front so a padded nick ("Notch ") can't slip past the
        // impersonation check by failing a literal equality against the real name.
        desired = nicknames.normalize(desired);

        if (desired.equalsIgnoreCase("off") || desired.equalsIgnoreCase("reset") || desired.equalsIgnoreCase("clear")) {
            nicknames.clearNickname(target);
            languages.send(target, "nickname.cleared-self");
            if (!sender.equals(target)) {
                languages.send(sender, "nickname.cleared-other", Placeholders.with("player", target.getName()));
            }
            return true;
        }

        boolean allowColor = sender.hasPermission("obx.nick.color");
        String stripped = nicknames.previewStripped(desired, allowColor);
        if (stripped.length() < 2 || stripped.length() > 32) {
            languages.send(sender, "nickname.length");
            return true;
        }
        // Reject characters outside the allowed set (default: ASCII letters/digits/underscore/space),
        // which blocks Unicode homoglyph impersonation.
        if (!nicknames.isAllowedNick(stripped)) {
            languages.send(sender, "nickname.invalid-chars");
            return true;
        }
        // Block impersonating another player's name or nickname.
        if (nicknames.isNameTaken(target.getUniqueId(), stripped)) {
            languages.send(sender, "nickname.taken", Placeholders.with("nickname", stripped));
            return true;
        }
        nicknames.setNickname(target, desired, allowColor);
        String shown = nicknames.getNickname(target.getUniqueId());
        if (shown == null) {
            shown = target.getName();
        }
        languages.send(target, "nickname.applied-self", Placeholders.with("nickname", shown));
        if (!sender.equals(target)) {
            languages.send(sender, "nickname.applied-other",
                    Placeholders.with("player", target.getName(), "nickname", shown));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("off");
            return options;
        }
        if (args.length == 2 && sender.hasPermission("obx.nick.others")) {
            List<String> names = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}