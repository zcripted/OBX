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
        this.moderationService = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.moderation.service.ModerationService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.moderation.banlist")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        List<ModerationService.BanView> activeBans = moderationService.getActiveBans();
        Map<String, String> meta = Placeholders.with("count", activeBans.size());
        for (String line : languages.list(sender, "player.moderation.banlist.header", meta)) {
            sender.sendMessage(line);
        }
        if (activeBans.isEmpty()) {
            languages.send(sender, "player.moderation.banlist.none");
            sender.sendMessage("");
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
        for (String line : languages.list(sender, "player.moderation.banlist.footer", meta)) {
            sender.sendMessage(line);
        }
        return true;
    }
}