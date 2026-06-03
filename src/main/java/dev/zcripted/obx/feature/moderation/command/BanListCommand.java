package dev.zcripted.obx.feature.moderation.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.moderation.service.ModerationService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BanListCommand extends AbstractObxCommand {

    private final ModerationService moderationService;

    public BanListCommand(ObxPlugin plugin) {
        super(plugin);
        this.moderationService = plugin.getModerationService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.moderation.banlist")) {
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
