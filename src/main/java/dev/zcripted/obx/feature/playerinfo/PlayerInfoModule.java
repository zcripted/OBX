package dev.zcripted.obx.feature.playerinfo;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.playerinfo.command.FirstSeenCommand;
import dev.zcripted.obx.feature.playerinfo.command.InfoCommand;
import dev.zcripted.obx.feature.playerinfo.command.ListCommand;
import dev.zcripted.obx.feature.playerinfo.command.NearCommand;
import dev.zcripted.obx.feature.playerinfo.command.PlaytimeCommand;
import dev.zcripted.obx.feature.playerinfo.command.RealnameCommand;
import dev.zcripted.obx.feature.playerinfo.command.SeenCommand;
import dev.zcripted.obx.feature.playerinfo.command.WhoisCommand;
import dev.zcripted.obx.feature.playerinfo.listener.JoinLeaveListener;
import dev.zcripted.obx.feature.playerinfo.listener.JoinListener;
import dev.zcripted.obx.feature.playerinfo.service.JoinLeaveService;
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
    protected void onEnable(OBX plugin) {
        JoinLeaveService joinLeave = service(JoinLeaveService.class, new JoinLeaveService(plugin));
        PlaytimeService playtime = service(PlaytimeService.class, new PlaytimeService(plugin));
        playtime.load();
        onDisable(playtime::save);

        listener(new JoinListener(plugin));
        listener(new JoinLeaveListener(plugin, joinLeave));

        command("seen", new SeenCommand(plugin));
        command("firstseen", new FirstSeenCommand(plugin));
        command("playtime", new PlaytimeCommand(plugin));
        command("list", new ListCommand(plugin));
        command("near", new NearCommand(plugin));
        command("whois", new WhoisCommand(plugin));
        command("realname", new RealnameCommand(plugin));
        command("info", new InfoCommand(plugin));
    }

    @Override
    public void reload(OBX plugin) {
        JoinLeaveService joinLeave = plugin.getJoinLeaveService();
        if (joinLeave != null) {
            joinLeave.reload();
        }
    }
}
