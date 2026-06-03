package dev.zcripted.obx.core;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * The plugin contract that features and the core framework program against,
 * instead of the concrete {@code dev.zcripted.obx.OBX} bootstrap class.
 *
 * <p>This is the seam that lets each feature live in its own module: features
 * depend on this interface (in {@code :core}) plus {@code :api}, never on the
 * concrete {@code OBX} that lives in {@code :plugin}. It extends Bukkit's
 * {@link Plugin} so the usual {@code getConfig()/getDataFolder()/getLogger()/
 * saveResource()} calls keep working, and adds the OBX-specific service
 * accessors plus the shared service registry and module manager.
 *
 * <p>Return types that currently point at feature-resident services are
 * progressively narrowed to {@code :api} interfaces as those are extracted; the
 * accessor names are stable.
 */
public interface ObxPlugin extends Plugin {

    dev.zcripted.obx.core.service.ServiceRegistry getServiceRegistry();

    dev.zcripted.obx.core.module.ModuleManager getModuleManager();

    dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter getSchedulerAdapter();

    dev.zcripted.obx.core.platform.PlatformInfo getPlatformInfo();

    dev.zcripted.obx.core.storage.DataService getDataService();

    dev.zcripted.obx.core.storage.SqliteDataStore getDataStore();

    dev.zcripted.obx.core.language.LanguageManager getLanguageManager();

    dev.zcripted.obx.core.motd.MotdService getMotdService();

    dev.zcripted.obx.core.diagnostics.TpsService getTpsService();

    dev.zcripted.obx.core.platform.resourcepack.AutoResourcePackManager getResourcePackManager();

    dev.zcripted.obx.api.economy.EconomyService getEconomyService();

    dev.zcripted.obx.feature.economy.service.WorthService getWorthService();

    dev.zcripted.obx.feature.warp.service.WarpService getWarpService();

    dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager getWarpMenuInputManager();

    dev.zcripted.obx.feature.teleport.service.TeleportManager getTeleportManager();

    dev.zcripted.obx.feature.teleport.service.TeleportRequestService getTeleportRequestService();

    dev.zcripted.obx.feature.teleport.service.TpaService getTpaService();

    dev.zcripted.obx.feature.mail.pm.PrivateMessageService getMessageService();

    dev.zcripted.obx.feature.mail.mail.MailService getMailService();

    dev.zcripted.obx.feature.playerstate.service.GodModeManager getGodModeManager();

    dev.zcripted.obx.feature.playerstate.service.KillModeManager getKillModeManager();

    dev.zcripted.obx.feature.playerstate.service.AfkService getAfkService();

    dev.zcripted.obx.feature.playerstate.service.FlightStateService getFlightStateService();

    dev.zcripted.obx.feature.staff.service.VanishManager getVanishManager();

    dev.zcripted.obx.feature.staff.service.FreezeService getFreezeService();

    dev.zcripted.obx.feature.staff.service.StaffSessionTracker getStaffSessionTracker();

    dev.zcripted.obx.feature.staff.gui.InvSeeMenuManager getInvSeeMenuManager();

    dev.zcripted.obx.feature.staff.gui.StaffMenuInputManager getStaffMenuInputManager();

    dev.zcripted.obx.feature.moderation.service.ModerationService getModerationService();

    dev.zcripted.obx.feature.playerinfo.service.JoinLeaveService getJoinLeaveService();

    dev.zcripted.obx.feature.playerinfo.service.PlaytimeService getPlaytimeService();

    dev.zcripted.obx.feature.chat.service.ChatService getChatService();

    dev.zcripted.obx.feature.tablist.service.TablistService getTablistService();

    dev.zcripted.obx.feature.scoreboard.service.ScoreboardService getScoreboardService();

    dev.zcripted.obx.feature.kit.service.KitService getKitService();

    dev.zcripted.obx.feature.nickname.service.NicknameService getNicknameService();

    dev.zcripted.obx.feature.world.service.PerPlayerTimeService getPerPlayerTimeService();

    dev.zcripted.obx.feature.jail.service.JailService getJailService();

    dev.zcripted.obx.feature.hub.service.HubService getHubService();

    dev.zcripted.obx.feature.hub.kit.HubKitApplier getHubKitApplier();

    dev.zcripted.obx.feature.hub.launchpad.LaunchpadCooldownManager getLaunchpadCooldownManager();

    dev.zcripted.obx.feature.hub.messaging.BungeeMessenger getBungeeMessenger();

    dev.zcripted.obx.feature.hub.listener.HubItemUseListener getHubItemUseListener();

    dev.zcripted.obx.feature.enchant.service.EnchantService getEnchantService();

    dev.zcripted.obx.feature.enchant.item.EnchantItems getEnchantItems();

    dev.zcripted.obx.feature.enchant.service.EnchantFeedback getEnchantFeedback();

    dev.zcripted.obx.feature.enchant.gui.EnchantAdminMenu getEnchantAdminMenu();

    dev.zcripted.obx.feature.hologram.service.HologramService getHologramService();

    dev.zcripted.obx.feature.hologram.gui.HologramEditorMenu getHologramEditorMenu();

    /** {@link org.bukkit.plugin.java.JavaPlugin#getCommand(String)} — declared here since {@link Plugin} lacks it. */
    PluginCommand getCommand(String name);

    /** Writes a raw, ANSI-colorized line straight to the server console. */
    void writeConsoleLine(String line);

    /** Reloads every live component; returns component label -> elapsed nanos. */
    Map<String, Long> reloadPlugin();

    /** Reloads the enchant world-loot generation listener. */
    void reloadEnchantLoot();
}
