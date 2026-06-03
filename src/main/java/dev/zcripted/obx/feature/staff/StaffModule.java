package dev.zcripted.obx.feature.staff;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.staff.command.FreezeCommand;
import dev.zcripted.obx.feature.staff.command.InvSeeCommand;
import dev.zcripted.obx.feature.staff.command.StaffCommand;
import dev.zcripted.obx.feature.staff.command.VanishCommand;
import dev.zcripted.obx.feature.staff.gui.AdminMenuRefreshTask;
import dev.zcripted.obx.feature.staff.gui.InvSeeMenuListener;
import dev.zcripted.obx.feature.staff.gui.InvSeeMenuManager;
import dev.zcripted.obx.feature.staff.gui.StaffMenuInputListener;
import dev.zcripted.obx.feature.staff.gui.StaffMenuInputManager;
import dev.zcripted.obx.feature.staff.gui.StaffMenuListener;
import dev.zcripted.obx.feature.staff.service.FreezeService;
import dev.zcripted.obx.feature.staff.service.StaffSessionTracker;
import dev.zcripted.obx.feature.staff.service.VanishManager;

/**
 * Staff tools feature: vanish, freeze, invsee, staff menu + admin menu. Owns the
 * vanish manager (listener + start/stop), freeze + staff-session services, the
 * staff/invsee menu input managers and their listeners, and the admin-menu
 * refresh task.
 */
public final class StaffModule extends AbstractModule {

    @Override
    public String id() {
        return "staff";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        VanishManager vanish = service(VanishManager.class, new VanishManager(plugin));
        service(dev.zcripted.obx.api.staff.VanishApi.class, vanish);
        service(FreezeService.class, new FreezeService(plugin));
        StaffSessionTracker sessions = service(StaffSessionTracker.class, new StaffSessionTracker());
        StaffMenuInputManager staffInput = service(StaffMenuInputManager.class, new StaffMenuInputManager(plugin));
        InvSeeMenuManager invsee = service(InvSeeMenuManager.class, new InvSeeMenuManager(plugin));
        AdminMenuRefreshTask adminRefresh = new AdminMenuRefreshTask(plugin);

        listener(vanish);
        listener(sessions);
        listener(new StaffMenuListener(plugin));
        listener(new InvSeeMenuListener(plugin));
        listener(new StaffMenuInputListener(staffInput));

        vanish.start();
        invsee.start();
        adminRefresh.start();
        onDisable(invsee::stop);
        onDisable(vanish::stop);
        onDisable(adminRefresh::cancel);

        command("vanish", new VanishCommand(plugin));
        command("invsee", new InvSeeCommand(plugin));
        command("freeze", new FreezeCommand(plugin));
        command("staff", new StaffCommand(plugin));
    }
}
