package dev.zcripted.obx.api;

import dev.zcripted.obx.api.chat.ChatService;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.api.hub.HubApi;
import dev.zcripted.obx.api.hub.HubItemUseListener;
import dev.zcripted.obx.api.hub.HubKitApplier;
import dev.zcripted.obx.api.jail.JailApi;
import dev.zcripted.obx.api.moderation.ModerationApi;
import dev.zcripted.obx.api.playerinfo.JoinLeaveService;
import dev.zcripted.obx.api.playerstate.AfkService;
import dev.zcripted.obx.api.scoreboard.ScoreboardService;
import dev.zcripted.obx.api.staff.VanishApi;
import dev.zcripted.obx.api.tablist.TablistService;
import dev.zcripted.obx.api.teleport.TeleportManager;

/**
 * Public entry point for third-party plugins integrating with OBX.
 *
 * <p>The running OBX plugin instance implements this interface, so a consumer obtains it with:
 * <pre>{@code
 * if (Bukkit.getPluginManager().getPlugin("OBX") instanceof OBXApi obx) {
 *     EconomyService economy = obx.getEconomyService();
 * }
 * }</pre>
 *
 * <p>This is the <strong>only</strong> supported entry type — it lives in the kept {@code api}
 * package and exposes solely the public service contracts below. The plugin's internal accessors
 * are intentionally not part of this surface and are obfuscated in the shipped jar.
 */
public interface OBXApi {

    EconomyService getEconomyService();

    TeleportManager getTeleportManager();

    AfkService getAfkService();

    JoinLeaveService getJoinLeaveService();

    ChatService getChatService();

    TablistService getTablistService();

    ScoreboardService getScoreboardService();

    HubKitApplier getHubKitApplier();

    HubItemUseListener getHubItemUseListener();

    ModerationApi getModerationApi();

    VanishApi getVanishApi();

    JailApi getJailApi();

    HubApi getHubApi();
}