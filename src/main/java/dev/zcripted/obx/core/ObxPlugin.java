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
 * saveResource()} calls keep working, and adds the OBX-specific accessors plus
 * the shared service registry and module manager.
 *
 * <p>Only services consumed <em>across</em> features (or by the public API) are
 * exposed here; feature-private services are resolved via
 * {@link #getServiceRegistry()}. The cross-feature return types below are
 * narrowed to {@code :api} interfaces in Phase 2b so {@code :core} never depends
 * on a feature module.
 */
public interface ObxPlugin extends Plugin {

    // ── core framework ──────────────────────────────────────────────────────
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

    // ── cross-feature / public-API services (narrowed to :api interfaces in Phase 2b) ──
    dev.zcripted.obx.api.economy.EconomyService getEconomyService();

    dev.zcripted.obx.feature.teleport.service.TeleportManager getTeleportManager();

    dev.zcripted.obx.feature.playerstate.service.AfkService getAfkService();

    dev.zcripted.obx.feature.staff.service.VanishManager getVanishManager();

    dev.zcripted.obx.feature.moderation.service.ModerationService getModerationService();

    dev.zcripted.obx.feature.playerinfo.service.JoinLeaveService getJoinLeaveService();

    dev.zcripted.obx.api.chat.ChatService getChatService();

    dev.zcripted.obx.feature.tablist.service.TablistService getTablistService();

    dev.zcripted.obx.feature.scoreboard.service.ScoreboardService getScoreboardService();

    dev.zcripted.obx.feature.jail.service.JailService getJailService();

    dev.zcripted.obx.feature.hub.service.HubService getHubService();

    dev.zcripted.obx.feature.hub.kit.HubKitApplier getHubKitApplier();

    dev.zcripted.obx.feature.hub.listener.HubItemUseListener getHubItemUseListener();

    dev.zcripted.obx.feature.hologram.service.HologramService getHologramService();

    // ── OBX-specific operations not on Bukkit's Plugin interface ──────────────
    /** {@link org.bukkit.plugin.java.JavaPlugin#getCommand(String)} — declared here since {@link Plugin} lacks it. */
    PluginCommand getCommand(String name);

    /** Writes a raw, ANSI-colorized line straight to the server console. */
    void writeConsoleLine(String line);

    /** Reloads every live component; returns component label -> elapsed nanos. */
    Map<String, Long> reloadPlugin();

    /** Reloads the enchant world-loot generation listener. */
    void reloadEnchantLoot();
}
