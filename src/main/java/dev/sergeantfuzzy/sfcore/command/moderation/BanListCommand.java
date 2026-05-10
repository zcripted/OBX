package dev.sergeantfuzzy.sfcore.command.moderation;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.moderation.ModerationService;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BanListCommand implements CommandExecutor {

    private final LanguageManager languages;
    private final ModerationService moderationService;

    public BanListCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
        this.moderationService = plugin.getModerationService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.moderation.banlist")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        List<ModerationService.BanView> activeBans = moderationService.getActiveBans();
        sender.sendMessage(" ");
        sender.sendMessage(languages.get(sender, "core.divider"));
        languages.send(sender, "player.moderation.banlist.header", Placeholders.with("count", activeBans.size()));
        if (activeBans.isEmpty()) {
            languages.send(sender, "player.moderation.banlist.none");
            sender.sendMessage(languages.get(sender, "core.divider"));
            return true;
        }

        for (ModerationService.BanView ban : activeBans) {
            Map<String, String> placeholders = new LinkedHashMap<>();
            placeholders.put("target", ban.getTarget());
            placeholders.put("reason", ban.getReason());
            placeholders.put("source", ban.getSource());
            placeholders.put("expires", ban.getExpiresAt() == null
                    ? languages.get(sender, "player.moderation.banlist.permanent")
                    : moderationService.formatDate(ban.getExpiresAt()));
            languages.send(sender, "player.moderation.banlist.entry", placeholders);
        }
        sender.sendMessage(languages.get(sender, "core.divider"));
        return true;
    }
}
