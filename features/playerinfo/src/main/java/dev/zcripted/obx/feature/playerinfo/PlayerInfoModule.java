package dev.zcripted.obx.feature.playerinfo;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.playerinfo.command.FirstSeenCommand;
import dev.zcripted.obx.feature.playerinfo.command.InfoCommand;
import dev.zcripted.obx.feature.playerinfo.command.ListCommand;
import dev.zcripted.obx.feature.playerinfo.command.NearCommand;
import dev.zcripted.obx.feature.playerinfo.command.PlaytimeCommand;
import dev.zcripted.obx.feature.playerinfo.command.TopPlaytimeCommand;
import dev.zcripted.obx.feature.playerinfo.command.RealnameCommand;
import dev.zcripted.obx.feature.playerinfo.command.SeenCommand;
import dev.zcripted.obx.feature.playerinfo.command.StaffListCommand;
import dev.zcripted.obx.feature.playerinfo.command.WhoisCommand;
import dev.zcripted.obx.feature.playerinfo.listener.JoinLeaveListener;
import dev.zcripted.obx.feature.playerinfo.listener.JoinListener;
import dev.zcripted.obx.api.playerinfo.JoinLeaveService;
import dev.zcripted.obx.feature.playerinfo.service.PlaytimeService;

/**
 * Player-presence / info feature: seen/firstseen/playtime/list/near/whois/
 * realname/info, the join + join-leave listeners, and the playtime + join-leave
 * services. Owns the canonical AFK/vanish-aware {@code /list}.
 */
public final class PlayerInfoModule extends AbstractModule {

    @Override
    public String id() {
        return "playerinfo";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        JoinLeaveService joinLeave = service(JoinLeaveService.class, new dev.zcripted.obx.feature.playerinfo.service.JoinLeaveServiceImpl(plugin));
        PlaytimeService playtime = service(PlaytimeService.class, new PlaytimeService(plugin));
        playtime.load();
        onDisable(playtime::save);

        // PlaytimeService is itself a Listener — its onJoin/onQuit record the playtime sessions.
        // Without this registration no playtime ever accumulates (the service was previously only
        // registered in the service registry, so its @EventHandlers never fired).
        listener(playtime);
        listener(new JoinListener(plugin));
        listener(new JoinLeaveListener(plugin, joinLeave));

        command("seen", new SeenCommand(plugin));
        command("firstseen", new FirstSeenCommand(plugin));
        command("playtime", new PlaytimeCommand(plugin));
        command("topplaytime", new TopPlaytimeCommand(plugin));
        command("list", new ListCommand(plugin));
        command("stafflist", new StaffListCommand(plugin));
        command("near", new NearCommand(plugin));
        command("whois", new WhoisCommand(plugin));
        command("realname", new RealnameCommand(plugin));
        command("info", new InfoCommand(plugin));
    }

    @Override
    public void reload(ObxPlugin plugin) {
        JoinLeaveService joinLeave = plugin.getJoinLeaveService();
        if (joinLeave != null) {
            joinLeave.reload();
        }
    }
}