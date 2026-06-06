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
        WARN("obx.moderation.warn", "player.moderation.usage.warn"),
        IPBAN("obx.moderation.ipban", "player.moderation.usage.ipban"),
        IPUNBAN("obx.moderation.ipunban", "player.moderation.usage.ipunban");

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
            case IPBAN:
                handleIpBan(sender, targetInput, reason);
                return true;
            case IPUNBAN:
                handleIpUnban(sender, targetInput, reason);
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
        if (rejectProtected(sender, target)) {
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
        if (rejectProtected(sender, target)) {
            return;
        }
        if (moderationService.hasActiveBan(target.getName())) {
            languages.send(sender, "player.moderation.tempban.already", Placeholders.with("target", target.getName()));
            return;
        }
        // Optional leading duration token: /tempban <player> [3d|2h30m|...] [reason]
        long durationMillis = 0L;
        String durationLabel = null;
        String effectiveReason = reason;
        if (reason != null && !reason.trim().isEmpty()) {
            String[] parts = reason.trim().split("\\s+", 2);
            long parsed = dev.zcripted.obx.feature.moderation.service.ModerationService.parseDurationMillis(parts[0]);
            if (parsed > 0L) {
                durationMillis = parsed;
                durationLabel = parts[0];
                effectiveReason = parts.length > 1 ? parts[1] : null;
            }
        }
        moderationService.tempBan(target, sender.getName(), effectiveReason, durationMillis, durationLabel);
        languages.send(
                sender,
                "player.moderation.tempban.success",
                Placeholders.with("target", target.getName(),
                        "duration", durationLabel != null ? durationLabel : moderationService.getDefaultTempBanDuration())
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
        if (rejectProtected(sender, target)) {
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
        if (rejectProtected(sender, target)) {
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
        if (rejectProtected(sender, target)) {
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

    private void handleIpBan(CommandSender sender, String targetInput, String reason) {
        String ip = moderationService.resolveIpForBan(targetInput);
        if (ip == null) {
            languages.send(sender, "player.moderation.ipban.unresolved", Placeholders.with("player", targetInput));
            return;
        }
        // Don't let a player ban the IP they're currently connected from.
        if (sender instanceof Player && ip.equals(addressOf((Player) sender))) {
            languages.send(sender, "player.moderation.self-target");
            return;
        }
        if (moderationService.isIpBanned(ip)) {
            languages.send(sender, "player.moderation.ipban.already", Placeholders.with("ip", ip));
            return;
        }
        moderationService.ipBan(ip, targetInput, sender.getName(), reason);
        languages.send(sender, "player.moderation.ipban.success", Placeholders.with("ip", ip, "reason", reason));
    }

    private void handleIpUnban(CommandSender sender, String targetInput, String reason) {
        // For unban the argument is normally the IP itself; fall back to treating
        // the raw input as the address if it isn't a currently-online player.
        String ip = moderationService.resolveIpForBan(targetInput);
        if (ip == null) {
            ip = targetInput;
        }
        if (!moderationService.ipUnban(ip, sender.getName(), reason)) {
            languages.send(sender, "player.moderation.ipunban.not-banned", Placeholders.with("ip", ip));
            return;
        }
        languages.send(sender, "player.moderation.ipunban.success", Placeholders.with("ip", ip));
    }

    private String addressOf(Player player) {
        if (player == null || player.getAddress() == null || player.getAddress().getAddress() == null) {
            return null;
        }
        return player.getAddress().getAddress().getHostAddress();
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

    /**
     * Whether {@code target} is shielded from punishment by an online {@code obx.moderation.exempt}
     * permission — used so a moderator can't ban/mute/kick/warn higher-ranked staff or the owner.
     * Console and holders of {@code obx.moderation.exempt.bypass} override the shield. Offline targets
     * can't be evaluated (Bukkit has no offline permission lookup), so the shield applies while online.
     */
    private boolean isProtectedTarget(CommandSender sender, ResolvedProfile target) {
        if (target == null || !(sender instanceof Player)) {
            return false; // console can act on anyone
        }
        if (sender.hasPermission("obx.moderation.exempt.bypass")) {
            return false;
        }
        Player online = target.isOnline() ? target.getPlayer() : null;
        return online != null && online.hasPermission("obx.moderation.exempt");
    }

    /** Sends the "target is exempt" feedback and returns true when {@code target} is protected. */
    private boolean rejectProtected(CommandSender sender, ResolvedProfile target) {
        if (isProtectedTarget(sender, target)) {
            languages.send(sender, "player.moderation.exempt-target", Placeholders.with("target", target.getName()));
            return true;
        }
        return false;
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
            case IPUNBAN:
                suggestions.addAll(moderationService.getBannedIps());
                break;
            case IPBAN:
                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
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
