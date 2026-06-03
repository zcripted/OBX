package dev.zcripted.obx.feature.jail;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.jail.command.DelJailCommand;
import dev.zcripted.obx.feature.jail.command.JailCommand;
import dev.zcripted.obx.feature.jail.command.JailTimeCommand;
import dev.zcripted.obx.feature.jail.command.JailsCommand;
import dev.zcripted.obx.feature.jail.command.SetJailCommand;
import dev.zcripted.obx.feature.jail.command.UnjailCommand;
import dev.zcripted.obx.feature.jail.service.JailService;

/**
 * Jail feature: {@code /jail}, {@code /unjail}, {@code /jails}, {@code /setjail},
 * {@code /deljail}, {@code /jailtime} backed by {@link JailService}.
 *
 * <p>Note: the legacy {@code JailListener} (jail confinement) was never registered
 * by the previous bootstrap, so it is intentionally left unwired here to preserve
 * existing behavior.
 */
public final class JailModule extends AbstractModule {

    @Override
    public String id() {
        return "jail";
    }

    @Override
    protected void onEnable(OBX plugin) {
        JailService service = service(JailService.class, new JailService(plugin));
        service.load();
        command("jail", new JailCommand(plugin));
        command("unjail", new UnjailCommand(plugin));
        command("jails", new JailsCommand(plugin));
        command("setjail", new SetJailCommand(plugin));
        command("deljail", new DelJailCommand(plugin));
        command("jailtime", new JailTimeCommand(plugin));
    }
}
