package dev.zcripted.obx.feature.moderation;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.moderation.command.BanListCommand;
import dev.zcripted.obx.feature.moderation.command.ModerationCommand;
import dev.zcripted.obx.feature.moderation.command.ModerationStatusCommand;
import dev.zcripted.obx.feature.moderation.listener.BanLoginListener;
import dev.zcripted.obx.feature.moderation.listener.MuteCommandListener;
import dev.zcripted.obx.feature.moderation.service.ModerationService;

/**
 * Moderation feature: ban/unban/kick/mute/unmute/tempban/warn + banlist/status,
 * backed by {@link ModerationService}.
 */
public final class ModerationModule extends AbstractModule {

    @Override
    public String id() {
        return "moderation";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        ModerationService service = service(ModerationService.class, new ModerationService(plugin));
        service(dev.zcripted.obx.api.moderation.ModerationApi.class, service);
        service.load();
        command("ban", new ModerationCommand(plugin, ModerationCommand.Action.BAN));
        command("unban", new ModerationCommand(plugin, ModerationCommand.Action.UNBAN));
        command("kick", new ModerationCommand(plugin, ModerationCommand.Action.KICK));
        command("mute", new ModerationCommand(plugin, ModerationCommand.Action.MUTE));
        command("unmute", new ModerationCommand(plugin, ModerationCommand.Action.UNMUTE));
        command("tempban", new ModerationCommand(plugin, ModerationCommand.Action.TEMPBAN));
        command("warn", new ModerationCommand(plugin, ModerationCommand.Action.WARN));
        command("ipban", new ModerationCommand(plugin, ModerationCommand.Action.IPBAN));
        command("ipunban", new ModerationCommand(plugin, ModerationCommand.Action.IPUNBAN));
        command("banlist", new BanListCommand(plugin));
        command("status", new ModerationStatusCommand(plugin));
        listener(new MuteCommandListener(plugin, service));
        // Enforce UUID bans at login so a name change can't bypass a ban on < 1.20.1.
        listener(new BanLoginListener(plugin, service));
        onDisable(service::save);
    }

    @Override
    public void reload(ObxPlugin plugin) {
        ModerationService service = plugin.getServiceRegistry().get(dev.zcripted.obx.feature.moderation.service.ModerationService.class);
        if (service != null) {
            service.reload();
        }
    }
}
