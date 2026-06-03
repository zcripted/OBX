package dev.zcripted.obx.feature.moderation.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.moderation.service.ModerationService;
import dev.zcripted.obx.feature.moderation.service.ModerationService.ResolvedProfile;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ModerationCommand extends AbstractObxCommand implements TabCompleter {

    public enum Action {
        BAN("obx.moderation.ban", "player.moderation.usage.ban"),
        UNBAN("obx.moderation.unban", "player.moderation.usage.unban"),
        KICK("obx.moderation.kick", "player.moderation.usage.kick"),
        MUTE("obx.moderation.mute", "player.moderation.usage.mute"),
        UNMUTE("obx.moderation.unmute", "player.moderation.usage.unmute"),
        TEMPBAN("obx.moderation.tempban", "player.moderation.usage.tempban"),
        WARN("obx.moderation.warn", "player.moderation.usage.warn");

        private final String permission;
        private final String usageKey;

        Action(String permission, String usageKey) {
            this.permission = permission;
            this.usageKey = usageKey;
        }

        public String permission() {
            return permission;
        }

        public String usageKey() {
            return usageKey;
        }
    }

    private final ModerationService moderationService;
    private final Action action;

    public ModerationCommand(ObxPlugin plugin, Action action) {
        super(plugin);
        this.moderationService = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.moderation.service.ModerationService.class);
        this.action = action;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(action.permission())) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, action.usageKey());
            return true;
        }

        String targetInput = args[0];
        String reason = args.length > 1 ? joinReason(args) : moderationService.getDefaultReason();

        switch (action) {
            case BAN:
                handleBan(sender, targetInput, reason);
                return true;
            case UNBAN:
                handleUnban(sender, targetInput, reason);
                return true;
            case KICK:
                handleKick(sender, targetInput, reason);
                return true;
            case MUTE:
                handleMute(sender, targetInput, reason);
                return true;
            case UNMUTE:
                handleUnmute(sender, targetInput, reason);
                return true;
            case TEMPBAN:
                handleTempBan(sender, targetInput, reason);
                return true;
            case WARN:
                handleWarn(sender, targetInput, reason);
                return true;
            default:
                return true;
        }
    }

    private void handleBan(CommandSender sender, String targetInput, String reason) {
        ResolvedProfile target = moderationService.resolvePunishmentProfile(targetInput);
        if (target == null) {
            languages.send(sender, "player.moderation.target-not-found", Placeholders.with("player", targetInput));
            return;
        }
        if (isSelfTarget(sender, target)) {
            languages.send(sender, "player.moderation.self-target");
            return;
        }
        if (moderationService.hasActiveBan(target.getName())) {
            languages.send(sender, "player.moderation.ban.already", Placeholders.with("target", target.getName()));
            return;
        }
        moderationService.ban(target, sender.getName(), reason);
        languages.send(sender, "player.moderation.ban.success", Placeholders.with("target", target.getName(), "reason", reason));
    }

    private void handleTempBan(CommandSender sender, String targetInput, String reason) {
        ResolvedProfile target = moderationService.resolvePunishmentProfile(targetInput);
        if (target == null) {
            languages.send(sender, "player.moderation.target-not-found", Placeholders.with("player", targetInput));
            return;
        }
        if (isSelfTarget(sender, target)) {
            languages.send(sender, "player.moderation.self-target");
            return;
        }
        if (moderationService.hasActiveBan(target.getName())) {
            languages.send(sender, "player.moderation.tempban.already", Placeholders.with("target", target.getName()));
            return;
        }
        moderationService.tempBan(target, sender.getName(), reason);
        languages.send(
                sender,
                "player.moderation.tempban.success",
                Placeholders.with("target", target.getName(), "duration", moderationService.getDefaultTempBanDuration())
        );
    }

    private void handleUnban(CommandSender sender, String targetInput, String reason) {
        ResolvedProfile resolved = moderationService.resolvePunishmentProfile(targetInput);
        String canonicalName = resolved == null ? targetInput : resolved.getName();
        if (!moderationService.unban(canonicalName, sender.getName(), reason)) {
            languages.send(sender, "player.moderation.unban.not-banned", Placeholders.with("target", targetInput));
            return;
        }
        languages.send(sender, "player.moderation.unban.success", Placeholders.with("target", canonicalName));
    }

    private void handleKick(CommandSender sender, String targetInput, String reason) {
        ResolvedProfile target = moderationService.resolveKnownProfile(targetInput);
        if (target == null) {
            languages.send(sender, "player.moderation.target-not-found", Placeholders.with("player", targetInput));
            return;
        }
        if (!target.isOnline() && !target.isFakeProfile()) {
            languages.send(sender, "player.moderation.target-not-found", Placeholders.with("player", targetInput));
            return;
        }
        if (isSelfTarget(sender, target)) {
            languages.send(sender, "player.moderation.self-target");
            return;
        }
        moderationService.kick(target, sender.getName(), reason);
        languages.send(sender, "player.moderation.kick.success", Placeholders.with("target", target.getName(), "reason", reason));
    }

    private void handleMute(CommandSender sender, String targetInput, String reason) {
        ResolvedProfile target = moderationService.resolveKnownProfile(targetInput);
        if (target == null) {
            languages.send(sender, "player.moderation.target-not-found", Placeholders.with("player", targetInput));
            return;
        }
        if (isSelfTarget(sender, target)) {
            languages.send(sender, "player.moderation.self-target");
            return;
        }
        if (moderationService.isMuted(target.getName())) {
            languages.send(sender, "player.moderation.mute.already", Placeholders.with("target", target.getName()));
            return;
        }
        moderationService.mute(target, sender.getName(), reason);
        languages.send(sender, "player.moderation.mute.success", Placeholders.with("target", target.getName(), "reason", reason));
        if (target.isOnline()) {
            languages.send(target.getPlayer(), "player.moderation.mute.target", Placeholders.with("reason", reason, "sender", sender.getName()));
        }
    }

    private void handleUnmute(CommandSender sender, String targetInput, String reason) {
        ResolvedProfile target = moderationService.resolveKnownProfile(targetInput);
        if (target == null) {
            languages.send(sender, "player.moderation.target-not-found", Placeholders.with("player", targetInput));
            return;
        }
        if (!moderationService.unmute(target, sender.getName(), reason)) {
            languages.send(sender, "player.moderation.unmute.not-muted", Placeholders.with("target", target.getName()));
            return;
        }
        languages.send(sender, "player.moderation.unmute.success", Placeholders.with("target", target.getName()));
        if (target.isOnline()) {
            languages.send(target.getPlayer(), "player.moderation.unmute.target", Placeholders.with("sender", sender.getName()));
        }
    }

    private void handleWarn(CommandSender sender, String targetInput, String reason) {
        ResolvedProfile target = moderationService.resolveKnownProfile(targetInput);
        if (target == null) {
            languages.send(sender, "player.moderation.target-not-found", Placeholders.with("player", targetInput));
            return;
        }
        if (isSelfTarget(sender, target)) {
            languages.send(sender, "player.moderation.self-target");
            return;
        }
        int warningCount = moderationService.warn(target, sender.getName(), reason);
        languages.send(
                sender,
                "player.moderation.warn.success",
                Placeholders.with("target", target.getName(), "count", warningCount)
        );
        if (target.isOnline()) {
            languages.send(
                    target.getPlayer(),
                    "player.moderation.warn.target",
                    Placeholders.with("sender", sender.getName(), "reason", reason)
            );
        }
    }

    private boolean isSelfTarget(CommandSender sender, ResolvedProfile target) {
        if (!(sender instanceof Player) || target == null) {
            return false;
        }
        Player player = (Player) sender;
        if (target.getUniqueId() != null) {
            return target.getUniqueId().equals(player.getUniqueId());
        }
        return player.getName().equalsIgnoreCase(target.getName());
    }

    private Player findOnlinePlayer(String input) {
        Player exact = Bukkit.getPlayerExact(input);
        if (exact != null) {
            return exact;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(input)) {
                return player;
            }
        }
        return null;
    }

    private String joinReason(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        String joined = builder.toString().trim();
        return joined.isEmpty() ? moderationService.getDefaultReason() : joined;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String partial = args[0].toLowerCase(Locale.ENGLISH);
        Set<String> suggestions = new LinkedHashSet<>();
        switch (action) {
            case KICK:
                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
                suggestions.addAll(moderationService.getFakeProfileNames());
                break;
            case UNBAN:
                for (ModerationService.BanView ban : moderationService.getActiveBans()) {
                    suggestions.add(ban.getTarget());
                }
                suggestions.addAll(moderationService.getFakeProfileNames());
                break;
            case UNMUTE:
                suggestions.addAll(moderationService.getMutedProfileNames());
                suggestions.addAll(moderationService.getFakeProfileNames());
                break;
            case BAN:
            case TEMPBAN:
            case MUTE:
            case WARN:
            default:
                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
                suggestions.addAll(moderationService.getFakeProfileNames());
                break;
        }

        List<String> filtered = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase(Locale.ENGLISH).startsWith(partial)) {
                filtered.add(suggestion);
            }
        }
        Collections.sort(filtered, String.CASE_INSENSITIVE_ORDER);
        return filtered;
    }
}
