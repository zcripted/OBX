package dev.zcripted.obx.feature.nickname;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.nickname.command.NickCommand;
import dev.zcripted.obx.feature.nickname.listener.NicknameApplyListener;
import dev.zcripted.obx.feature.nickname.service.NicknameService;

/**
 * Nickname feature: {@code /nick} backed by {@link NicknameService}, plus the
 * {@link NicknameApplyListener} that re-applies a player's nickname on join.
 */
public final class NicknameModule extends AbstractModule {

    @Override
    public String id() {
        return "nickname";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        NicknameService service = service(NicknameService.class, new NicknameService(plugin));
        service.load();
        command("nick", new NickCommand(plugin));
        listener(new NicknameApplyListener(service));
    }
}