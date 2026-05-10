package dev.sergeantfuzzy.sfcore.command.moderation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.moderation.ModerationService;
import dev.sergeantfuzzy.sfcore.moderation.ModerationService.ActionHistoryEntry;
import dev.sergeantfuzzy.sfcore.moderation.ModerationService.BanView;
import dev.sergeantfuzzy.sfcore.moderation.ModerationService.ModerationStatusProfile;
import dev.sergeantfuzzy.sfcore.moderation.ModerationService.MuteView;
import dev.sergeantfuzzy.sfcore.moderation.ModerationService.ResolvedProfile;
import dev.sergeantfuzzy.sfcore.moderation.ModerationService.WarningView;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ModerationStatusCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;
    private final ModerationService moderationService;

    public ModerationStatusCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.moderationService = plugin.getModerationService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.moderation.status")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(sender, "player.moderation.usage.status");
            return true;
        }

        ModerationStatusProfile profile = moderationService.getStatusProfile(args[0]);
        if (profile == null) {
            languages.send(sender, "player.moderation.target-not-found", Placeholders.with("player", args[0]));
            return true;
        }

        ResolvedProfile target = profile.getProfile();
        languages.send(sender, "core.divider");
        languages.send(sender, "player.moderation.status.header", Placeholders.with("target", target.getName()));
        languages.send(sender, "player.moderation.status.summary", placeholders(
                "uuid", target.getUniqueId() == null ? languages.get(sender, "player.moderation.status.value.unknown") : target.getUniqueId().toString(),
                "profileType", resolveProfileType(sender, target),
                "online", target.isOnline() ? languages.get(sender, "player.moderation.status.value.online") : languages.get(sender, "player.moderation.status.value.offline"),
                "updated", profile.getLastUpdated()
        ));

        BanView activeBan = profile.getActiveBan();
        if (activeBan == null) {
            languages.send(sender, "player.moderation.status.ban-clear");
        } else {
            languages.send(sender, "player.moderation.status.ban-active", placeholders(
                    "type", activeBan.getExpiresAt() == null
                            ? languages.get(sender, "player.moderation.status.value.permanent")
                            : languages.get(sender, "player.moderation.status.value.temporary"),
                    "actor", activeBan.getSource(),
                    "expires", moderationService.formatDate(activeBan.getExpiresAt())
            ));
            languages.send(sender, "player.moderation.status.ban-reason", Placeholders.with("reason", activeBan.getReason()));
        }

        MuteView activeMute = profile.getActiveMute();
        if (activeMute == null) {
            languages.send(sender, "player.moderation.status.mute-clear");
        } else {
            languages.send(sender, "player.moderation.status.mute-active", placeholders(
                    "actor", activeMute.getActor(),
                    "issuedAt", activeMute.getIssuedAt()
            ));
            languages.send(sender, "player.moderation.status.mute-reason", Placeholders.with("reason", activeMute.getReason()));
        }

        languages.send(sender, "player.moderation.status.counts-primary", placeholders(
                "bans", profile.getActionCount("ban"),
                "tempbans", profile.getActionCount("tempban"),
                "kicks", profile.getActionCount("kick"),
                "warnings", profile.getWarningCount()
        ));
        languages.send(sender, "player.moderation.status.counts-secondary", placeholders(
                "mutes", profile.getActionCount("mute"),
                "unmutes", profile.getActionCount("unmute"),
                "unbans", profile.getActionCount("unban")
        ));

        WarningView lastWarning = profile.getLastWarning();
        if (lastWarning == null) {
            languages.send(sender, "player.moderation.status.last-warning-none");
        } else {
            languages.send(sender, "player.moderation.status.last-warning", placeholders(
                    "issuedAt", lastWarning.getIssuedAt(),
                    "actor", lastWarning.getActor(),
                    "reason", lastWarning.getReason()
            ));
        }

        ActionHistoryEntry lastAction = profile.getLastAction();
        if (lastAction == null) {
            languages.send(sender, "player.moderation.status.last-action-none");
        } else {
            languages.send(sender, "player.moderation.status.last-action", placeholders(
                    "action", actionLabel(sender, lastAction.getAction()),
                    "issuedAt", lastAction.getIssuedAt(),
                    "actor", lastAction.getActor(),
                    "reason", lastAction.getReason()
            ));
        }

        List<ActionHistoryEntry> recentActions = profile.getRecentActions();
        languages.send(sender, "player.moderation.status.recent-header", Placeholders.with("count", recentActions.size()));
        if (recentActions.isEmpty()) {
            languages.send(sender, "player.moderation.status.recent-none");
        } else {
            for (ActionHistoryEntry entry : recentActions) {
                languages.send(sender, "player.moderation.status.recent-entry", placeholders(
                        "action", actionLabel(sender, entry.getAction()),
                        "issuedAt", entry.getIssuedAt(),
                        "actor", entry.getActor(),
                        "reason", entry.getReason()
                ));
            }
        }
        languages.send(sender, "core.divider");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String partial = args[0].toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (String name : moderationService.getStatusProfileNames()) {
            if (name.toLowerCase(Locale.ENGLISH).startsWith(partial)) {
                matches.add(name);
            }
        }
        Collections.sort(matches, String.CASE_INSENSITIVE_ORDER);
        return matches;
    }

    private String resolveProfileType(CommandSender sender, ResolvedProfile target) {
        if (target.isFakeProfile()) {
            return languages.get(sender, "player.moderation.status.value.profile-fake");
        }
        if (target.isOnline()) {
            return languages.get(sender, "player.moderation.status.value.profile-online");
        }
        return languages.get(sender, "player.moderation.status.value.profile-offline");
    }

    private String actionLabel(CommandSender sender, String action) {
        String key = "player.moderation.status.value.action-" + action.toLowerCase(Locale.ENGLISH);
        String translated = languages.get(sender, key);
        if (translated.contains(key)) {
            return action;
        }
        return translated;
    }

    private Map<String, String> placeholders(Object... pairs) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            values.put(String.valueOf(pairs[i]), pairs[i + 1] == null ? "" : String.valueOf(pairs[i + 1]));
        }
        return values;
    }
}
