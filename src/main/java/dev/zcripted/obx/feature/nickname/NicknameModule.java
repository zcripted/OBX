package dev.zcripted.obx.feature.nickname;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.nickname.command.NickCommand;
import dev.zcripted.obx.feature.nickname.service.NicknameService;

/** Nickname feature: {@code /nick} backed by {@link NicknameService}. */
public final class NicknameModule extends AbstractModule {

    @Override
    public String id() {
        return "nickname";
    }

    @Override
    protected void onEnable(OBX plugin) {
        NicknameService service = service(NicknameService.class, new NicknameService(plugin));
        service.load();
        command("nick", new NickCommand(plugin));
    }
}
