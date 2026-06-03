package dev.zcripted.obx;

import dev.zcripted.obx.command.admin.KillCommand;
import dev.zcripted.obx.command.admin.PluginListCommand;
import dev.zcripted.obx.command.admin.TpsCommand;
import dev.zcripted.obx.command.core.HelpGuiCommand;
import dev.zcripted.obx.command.core.ListCommand;
import dev.zcripted.obx.command.core.ObxCommand;
import dev.zcripted.obx.command.moderation.BanListCommand;
import dev.zcripted.obx.command.moderation.ModerationCommand;
import dev.zcripted.obx.command.moderation.ModerationStatusCommand;
import dev.zcripted.obx.command.teleportation.BackCommand;
import dev.zcripted.obx.command.teleportation.DelHomeCommand;
import dev.zcripted.obx.command.teleportation.HomeCommand;
import dev.zcripted.obx.command.teleportation.HomesCommand;
import dev.zcripted.obx.command.teleportation.HubCommand;
import dev.zcripted.obx.command.teleportation.SetHomeCommand;
import dev.zcripted.obx.command.teleportation.SpawnCommand;
import dev.zcripted.obx.command.teleportation.TopCommand;
import dev.zcripted.obx.command.teleportation.WarpCommand;
import dev.zcripted.obx.hub.HubService;
import dev.zcripted.obx.hub.kit.HubKitApplier;
import dev.zcripted.obx.hub.launchpad.LaunchpadCooldownManager;
import dev.zcripted.obx.hub.messaging.BungeeMessenger;
import dev.zcripted.obx.enchant.command.EnchantsBrowseCommand;
import dev.zcripted.obx.enchant.command.RecallCommand;
import dev.zcripted.obx.enchant.command.SatchelCommand;
import dev.zcripted.obx.enchant.command.ObxEnchantCommand;
import dev.zcripted.obx.enchant.effect.BoundMovement;
import dev.zcripted.obx.enchant.effect.EnchantState;
import dev.zcripted.obx.enchant.effect.EnchantTickTask;
import dev.zcripted.obx.enchant.gui.EnchantAdminMenu;
import dev.zcripted.obx.enchant.gui.EnchantMenuListener;
import dev.zcripted.obx.enchant.item.EnchantItems;
import dev.zcripted.obx.enchant.listener.CombatEnchantListener;
import dev.zcripted.obx.enchant.listener.CursedEnchantListener;
import dev.zcripted.obx.enchant.listener.DefenseEnchantListener;
import dev.zcripted.obx.enchant.listener.FarmingEnchantListener;
import dev.zcripted.obx.enchant.listener.MovementEnchantListener;
import dev.zcripted.obx.enchant.listener.ToolEnchantListener;
import dev.zcripted.obx.enchant.listener.UtilityEnchantListener;
import dev.zcripted.obx.enchant.loot.EnchantLoot;
import dev.zcripted.obx.enchant.scroll.AnvilEnchantListener;
import dev.zcripted.obx.enchant.scroll.ScrollApplyService;
import dev.zcripted.obx.enchant.scroll.ScrollDragListener;
import dev.zcripted.obx.enchant.service.EnchantFeedback;
import dev.zcripted.obx.enchant.service.EnchantService;
import dev.zcripted.obx.listener.player.HubFallDamageListener;
import dev.zcripted.obx.listener.player.HubFishingListener;
import dev.zcripted.obx.listener.player.HubItemProtectionListener;
import dev.zcripted.obx.listener.player.HubItemUseListener;
import dev.zcripted.obx.listener.player.HubJoinListener;
import dev.zcripted.obx.listener.player.HubLaunchpadListener;
import dev.zcripted.obx.listener.menu.ServerSelectorListener;
import dev.zcripted.obx.command.language.LanguageCommand;
import dev.zcripted.obx.command.utility.AnvilCommand;
import dev.zcripted.obx.command.utility.CraftCommand;
import dev.zcripted.obx.command.utility.EnchantCommand;
import dev.zcripted.obx.command.utility.FeedCommand;
import dev.zcripted.obx.command.utility.GamemodeCommand;
import dev.zcripted.obx.command.utility.GodCommand;
import dev.zcripted.obx.command.utility.HealCommand;
import dev.zcripted.obx.command.utility.MapCommand;
import dev.zcripted.obx.command.utility.ResearchCommand;
import dev.zcripted.obx.command.utility.SmithCommand;
import dev.zcripted.obx.command.utility.VirtualStationCommand;
import dev.zcripted.obx.command.utility.VitalCommand;
import dev.zcripted.obx.command.admin.InvSeeCommand;
import dev.zcripted.obx.command.admin.VanishCommand;
import dev.zcripted.obx.chat.listener.ChatManagementListener;
import dev.zcripted.obx.chat.service.ChatService;
import dev.zcripted.obx.tablist.listener.TablistJoinListener;
import dev.zcripted.obx.tablist.scheduler.TablistRefreshTask;
import dev.zcripted.obx.tablist.service.TablistService;
import dev.zcripted.obx.scoreboard.listener.ScoreboardJoinListener;
import dev.zcripted.obx.scoreboard.scheduler.ScoreboardRefreshTask;
import dev.zcripted.obx.scoreboard.service.ScoreboardService;
import dev.zcripted.obx.listener.chat.WarpMenuInputListener;
import dev.zcripted.obx.listener.teleport.BackListener;
import dev.zcripted.obx.listener.player.CommandOverrideListener;
import dev.zcripted.obx.listener.player.JoinLockListener;
import dev.zcripted.obx.listener.player.JoinListener;
import dev.zcripted.obx.listener.player.JoinLeaveListener;
import dev.zcripted.obx.listener.menu.HelpGuiListener;
import dev.zcripted.obx.listener.menu.MainMenuListener;
import dev.zcripted.obx.listener.menu.WarpMenuListener;
import dev.zcripted.obx.listener.server.MotdPingListener;
import dev.zcripted.obx.listener.world.RedstoneControlListener;
import dev.zcripted.obx.platform.PlatformInfo;
import dev.zcripted.obx.platform.bukkit.resourcepack.AutoResourcePackManager;
import dev.zcripted.obx.platform.bukkit.resourcepack.ResourcePackListener;
import dev.zcripted.obx.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.storage.DataService;
import dev.zcripted.obx.storage.JoinLeaveService;
import dev.zcripted.obx.storage.MotdService;
import dev.zcripted.obx.storage.WarpService;
import dev.zcripted.obx.gui.player.WarpMenuInputManager;
import dev.zcripted.obx.language.LanguageManager;
import dev.zcripted.obx.moderation.ModerationService;
import dev.zcripted.obx.util.control.GodModeManager;
import dev.zcripted.obx.util.control.KillModeManager;
import dev.zcripted.obx.util.control.VanishManager;
import dev.zcripted.obx.util.perf.TpsService;
import dev.zcripted.obx.util.teleport.TeleportManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class OBX extends JavaPlugin {

    private DataService dataService;
    private WarpService warpService;
    private LanguageManager languageManager;
    private TeleportManager teleportManager;
    private dev.zcripted.obx.util.teleport.TeleportRequestService teleportRequestService;
    private dev.zcripted.obx.message.MessageService messageService;
    private GodModeManager godModeManager;
    private KillModeManager killModeManager;
    private VanishManager vanishManager;
    private ModerationService moderationService;
    private MotdService motdService;
    private JoinLeaveService joinLeaveService;
    private ChatService chatService;
    private TablistService tablistService;
    private ScoreboardService scoreboardService;
    private ScoreboardRefreshTask scoreboardRefreshTask;
    private TablistRefreshTask tablistRefreshTask;
    private dev.zcripted.obx.gui.admin.AdminMenuRefreshTask adminMenuRefreshTask;
    private AutoResourcePackManager resourcePackManager;
    private WarpMenuInputManager warpMenuInputManager;
    private dev.zcripted.obx.gui.admin.StaffMenuInputManager staffMenuInputManager;
    private dev.zcripted.obx.gui.admin.InvSeeMenuManager invSeeMenuManager;
    private dev.zcripted.obx.util.control.StaffSessionTracker staffSessionTracker;
    private TpsService tpsService;
    private dev.zcripted.obx.util.teleport.TpaService tpaService;
    private dev.zcripted.obx.messaging.MessageService mailService;
    private dev.zcripted.obx.util.control.AfkService afkService;
    private dev.zcripted.obx.kit.KitService kitService;
    private dev.zcripted.obx.economy.EconomyService economyService;
    private dev.zcripted.obx.economy.WorthService worthService;
    private dev.zcripted.obx.util.perf.PlaytimeService playtimeService;
    private dev.zcripted.obx.storage.SqliteDataStore dataStore;
    private dev.zcripted.obx.util.control.FlightStateService flightStateService;
    private dev.zcripted.obx.util.control.FreezeService freezeService;
    private dev.zcripted.obx.nickname.NicknameService nicknameService;
    private dev.zcripted.obx.util.control.PerPlayerTimeService perPlayerTimeService;
    private dev.zcripted.obx.jail.JailService jailService;
    private SchedulerAdapter scheduler;
    private PlatformInfo platformInfo;
    private HubService hubService;
    private HubKitApplier hubKitApplier;
    private LaunchpadCooldownManager launchpadCooldownManager;
    private BungeeMessenger bungeeMessenger;
    private HubItemUseListener hubItemUseListener;
    private EnchantService enchantService;
    private EnchantItems enchantItems;
    private EnchantFeedback enchantFeedback;
    private EnchantAdminMenu enchantAdminMenu;
    private ScrollApplyService scrollApplyService;
    private EnchantLoot enchantLoot;
    private EnchantState enchantState;
    private EnchantTickTask enchantTickTask;
    private BoundMovement enchantBoundMovement;
    private dev.zcripted.obx.enchant.effect.CombatState combatState;
    private dev.zcripted.obx.enchant.service.CombatParticleService combatParticles;
    private dev.zcripted.obx.enchant.service.HoloFXService holoFX;
    private dev.zcripted.obx.enchant.service.ReactiveSpecialsService reactiveSpecials;
    private dev.zcripted.obx.enchant.service.CombatHudService combatHud;
    private dev.zcripted.obx.hologram.service.HologramService hologramService;
    private dev.zcripted.obx.hologram.gui.HologramEditorMenu hologramEditorMenu;
    private String releaseDate = "Unknown";
    private long lastLoadDurationMs;

    @Override
    public void onEnable() {
        long enableStart = System.nanoTime();
        saveDefaultConfig();
        loadBuildMetadata();

        platformInfo = PlatformInfo.get();
        scheduler = new SchedulerAdapter(this);
        dev.zcripted.obx.util.message.ConsoleLog.info(this, "Detected platform: " + platformInfo.summary());

        languageManager = new LanguageManager(this);
        dataStore = new dev.zcripted.obx.storage.SqliteDataStore(this);
        dataStore.open();
        dataService = new DataService(this);
        dataService.load();
        warpService = new WarpService(this);
        warpService.load();
        moderationService = new ModerationService(this);
        moderationService.load();
        motdService = new MotdService(this);
        motdService.load();
        joinLeaveService = new JoinLeaveService(this);
        chatService = new ChatService(this);
        chatService.load();
        tablistService = new TablistService(this);
        tablistService.load();
        tablistRefreshTask = new TablistRefreshTask(this, tablistService);
        scoreboardService = new ScoreboardService(this);
        scoreboardService.load();
        scoreboardRefreshTask = new ScoreboardRefreshTask(this, scoreboardService);
        adminMenuRefreshTask = new dev.zcripted.obx.gui.admin.AdminMenuRefreshTask(this);
        warpMenuInputManager = new WarpMenuInputManager(this);
        staffMenuInputManager = new dev.zcripted.obx.gui.admin.StaffMenuInputManager(this);
        invSeeMenuManager = new dev.zcripted.obx.gui.admin.InvSeeMenuManager(this);
        staffSessionTracker = new dev.zcripted.obx.util.control.StaffSessionTracker();
        teleportManager = new TeleportManager(this, languageManager);
        teleportRequestService = new dev.zcripted.obx.util.teleport.TeleportRequestService(this);
        dev.zcripted.obx.message.MessageStore messageStore = new dev.zcripted.obx.message.MessageStore(this);
        messageStore.load();
        messageService = new dev.zcripted.obx.message.MessageService(this, messageStore);
        godModeManager = new GodModeManager();
        killModeManager = new KillModeManager(this);
        vanishManager = new VanishManager(this);
        resourcePackManager = new AutoResourcePackManager(this);
        resourcePackManager.installBundledPack();
        resourcePackManager.prepareHosting();
        tpsService = new TpsService(this);
        tpaService = new dev.zcripted.obx.util.teleport.TpaService(this);
        mailService = new dev.zcripted.obx.messaging.MessageService(this);
        mailService.load();
        afkService = new dev.zcripted.obx.util.control.AfkService(this);
        kitService = new dev.zcripted.obx.kit.KitService(this);
        kitService.load();
        economyService = new dev.zcripted.obx.economy.EconomyService(this);
        economyService.load();
        worthService = new dev.zcripted.obx.economy.WorthService(this);
        worthService.load();
        playtimeService = new dev.zcripted.obx.util.perf.PlaytimeService(this);
        playtimeService.load();
        flightStateService = new dev.zcripted.obx.util.control.FlightStateService(this);
        flightStateService.load();
        freezeService = new dev.zcripted.obx.util.control.FreezeService(this);
        nicknameService = new dev.zcripted.obx.nickname.NicknameService(this);
        nicknameService.load();
        perPlayerTimeService = new dev.zcripted.obx.util.control.PerPlayerTimeService(this);
        perPlayerTimeService.load();
        jailService = new dev.zcripted.obx.jail.JailService(this);
        jailService.load();

        // Hub / lobby system — service must exist before listeners or
        // command registration since they reference it. Dormant when the
        // master `enabled: false` flag in systems/hub.yml is left at its
        // default.
        hubService = new HubService(this);
        hubService.load();
        hubKitApplier = new HubKitApplier(this, hubService);
        launchpadCooldownManager = new LaunchpadCooldownManager(this, hubService);
        bungeeMessenger = new BungeeMessenger(this, hubService);
        bungeeMessenger.register();

        // Arcanum custom-enchantment module. The service loads the roster and
        // config; the GUI/items/feedback depend on it being constructed first.
        enchantService = new EnchantService(this);
        enchantService.load();
        enchantItems = new EnchantItems(enchantService);
        enchantFeedback = new EnchantFeedback(this);
        enchantAdminMenu = new EnchantAdminMenu(this);
        scrollApplyService = new ScrollApplyService(this);
        enchantLoot = new EnchantLoot(this);
        enchantLoot.register();
        enchantState = new EnchantState(this);
        combatState = new dev.zcripted.obx.enchant.effect.CombatState();
        combatParticles = new dev.zcripted.obx.enchant.service.CombatParticleService(this);
        holoFX = new dev.zcripted.obx.enchant.service.HoloFXService(this);
        reactiveSpecials = new dev.zcripted.obx.enchant.service.ReactiveSpecialsService(this, combatState, combatParticles);
        combatHud = new dev.zcripted.obx.enchant.service.CombatHudService(this);
        combatHud.start();
        enchantBoundMovement = new BoundMovement(enchantService.getStorage());
        enchantTickTask = new EnchantTickTask(this, enchantBoundMovement);

        // Holograms module. Dormant when systems/holograms.yml master enabled=false.
        // Renders via real display entities on 1.19.4+, armor-stand fallback below.
        hologramService = new dev.zcripted.obx.hologram.service.HologramService(this);
        hologramService.load();
        hologramEditorMenu = new dev.zcripted.obx.hologram.gui.HologramEditorMenu(this);

        registerCommands();
        registerListeners();

        dev.zcripted.obx.economy.VaultEconomyProvider.register(this, economyService);

        if (tablistRefreshTask != null) {
            tablistRefreshTask.start();
        }
        if (adminMenuRefreshTask != null) {
            adminMenuRefreshTask.start();
        }
        if (scoreboardRefreshTask != null) {
            scoreboardRefreshTask.start();
        }
        tpsService.start();
        if (afkService != null) {
            afkService.start();
        }
        if (vanishManager != null) {
            vanishManager.start();
        }
        if (invSeeMenuManager != null) {
            invSeeMenuManager.start();
        }
        if (launchpadCooldownManager != null) {
            launchpadCooldownManager.start();
        }
        if (enchantTickTask != null) {
            enchantTickTask.start();
        }

        lastLoadDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - enableStart);
        printBanner(true);
    }

    @Override
    public void onDisable() {
        if (enchantTickTask != null) {
            enchantTickTask.stop();
        }
        if (enchantState != null) {
            enchantState.saveAll();
        }
        if (holoFX != null) {
            holoFX.clear();
        }
        if (combatParticles != null) {
            combatParticles.clear();
        }
        if (combatHud != null) {
            combatHud.clear();
        }
        // Clear Curse of the Bound toughness modifiers and speed throttle so nothing persists past unload.
        for (org.bukkit.entity.Player online : getServer().getOnlinePlayers()) {
            dev.zcripted.obx.enchant.effect.EffectUtil.setBoundToughness(online, 0.0);
            if (enchantBoundMovement != null) {
                enchantBoundMovement.restore(online);
            }
        }
        if (launchpadCooldownManager != null) {
            launchpadCooldownManager.stop();
        }
        if (bungeeMessenger != null) {
            bungeeMessenger.unregister();
        }
        if (hubService != null) {
            hubService.save();
        }
        if (invSeeMenuManager != null) {
            invSeeMenuManager.stop();
        }
        if (vanishManager != null) {
            vanishManager.stop();
        }
        if (tpsService != null) {
            tpsService.stop();
        }
        if (tablistRefreshTask != null) {
            tablistRefreshTask.cancel();
        }
        if (adminMenuRefreshTask != null) {
            adminMenuRefreshTask.cancel();
        }
        if (scoreboardRefreshTask != null) {
            scoreboardRefreshTask.cancel();
        }
        // Remove the OBX tablist staff/players sort teams from the main
        // scoreboard so they don't linger after the plugin unloads.
        dev.zcripted.obx.tablist.format.TablistTeams.reset();
        if (teleportManager != null) {
            teleportManager.cancelAll();
        }
        if (tpaService != null) {
            tpaService.stop();
        }
        if (afkService != null) {
            afkService.stop();
        }
        if (playtimeService != null) {
            playtimeService.save();
        }
        if (dataService != null) {
            dataService.save();
        }
        if (moderationService != null) {
            moderationService.save();
        }
        if (hologramService != null) {
            hologramService.shutdown();
            hologramService.save();
        }
        printBanner(false);
    }

    public DataService getDataService() {
        return dataService;
    }

    public WarpService getWarpService() {
        return warpService;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public dev.zcripted.obx.util.teleport.TeleportRequestService getTeleportRequestService() {
        return teleportRequestService;
    }

    public dev.zcripted.obx.message.MessageService getMessageService() {
        return messageService;
    }

    public GodModeManager getGodModeManager() {
        return godModeManager;
    }

    public KillModeManager getKillModeManager() {
        return killModeManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public dev.zcripted.obx.gui.admin.InvSeeMenuManager getInvSeeMenuManager() {
        return invSeeMenuManager;
    }

    public ModerationService getModerationService() {
        return moderationService;
    }

    public MotdService getMotdService() {
        return motdService;
    }

    public JoinLeaveService getJoinLeaveService() {
        return joinLeaveService;
    }

    public ChatService getChatService() {
        return chatService;
    }

    public TablistService getTablistService() {
        return tablistService;
    }

    public ScoreboardService getScoreboardService() {
        return scoreboardService;
    }

    public AutoResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public WarpMenuInputManager getWarpMenuInputManager() {
        return warpMenuInputManager;
    }

    public dev.zcripted.obx.gui.admin.StaffMenuInputManager getStaffMenuInputManager() {
        return staffMenuInputManager;
    }

    public dev.zcripted.obx.util.control.StaffSessionTracker getStaffSessionTracker() {
        return staffSessionTracker;
    }

    public TpsService getTpsService() {
        return tpsService;
    }

    public dev.zcripted.obx.util.teleport.TpaService getTpaService() {
        return tpaService;
    }

    public dev.zcripted.obx.messaging.MessageService getMailService() {
        return mailService;
    }

    public dev.zcripted.obx.util.control.AfkService getAfkService() {
        return afkService;
    }

    public dev.zcripted.obx.kit.KitService getKitService() {
        return kitService;
    }

    public dev.zcripted.obx.economy.EconomyService getEconomyService() {
        return economyService;
    }

    public dev.zcripted.obx.economy.WorthService getWorthService() {
        return worthService;
    }

    public dev.zcripted.obx.util.perf.PlaytimeService getPlaytimeService() {
        return playtimeService;
    }

    public dev.zcripted.obx.storage.SqliteDataStore getDataStore() {
        return dataStore;
    }

    public dev.zcripted.obx.util.control.FlightStateService getFlightStateService() {
        return flightStateService;
    }

    public dev.zcripted.obx.util.control.FreezeService getFreezeService() {
        return freezeService;
    }

    public dev.zcripted.obx.nickname.NicknameService getNicknameService() {
        return nicknameService;
    }

    public dev.zcripted.obx.util.control.PerPlayerTimeService getPerPlayerTimeService() {
        return perPlayerTimeService;
    }

    public dev.zcripted.obx.jail.JailService getJailService() {
        return jailService;
    }

    public SchedulerAdapter getSchedulerAdapter() {
        return scheduler;
    }

    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    public HubService getHubService() {
        return hubService;
    }

    public HubKitApplier getHubKitApplier() {
        return hubKitApplier;
    }

    public LaunchpadCooldownManager getLaunchpadCooldownManager() {
        return launchpadCooldownManager;
    }

    public BungeeMessenger getBungeeMessenger() {
        return bungeeMessenger;
    }

    public HubItemUseListener getHubItemUseListener() {
        return hubItemUseListener;
    }

    public EnchantService getEnchantService() {
        return enchantService;
    }

    public EnchantItems getEnchantItems() {
        return enchantItems;
    }

    public EnchantFeedback getEnchantFeedback() {
        return enchantFeedback;
    }

    public EnchantAdminMenu getEnchantAdminMenu() {
        return enchantAdminMenu;
    }

    public dev.zcripted.obx.hologram.service.HologramService getHologramService() {
        return hologramService;
    }

    /**
     * Reloads the world-loot generation listener. The loot subsystem (Phase 4)
     * registers itself here once wired in; until then this is a safe no-op so the
     * {@code /obxench loot reload} command remains valid.
     */
    public void reloadEnchantLoot() {
        if (enchantLoot != null) {
            enchantLoot.reload();
        }
    }

    /**
     * Reloads every live component and returns an ordered map of component label →
     * elapsed nanoseconds, so callers can report per-file/per-service load times.
     */
    public java.util.Map<String, Long> reloadPlugin() {
        java.util.LinkedHashMap<String, Long> times = new java.util.LinkedHashMap<String, Long>();
        long s;
        s = System.nanoTime(); reloadConfig(); times.put("config.yml", System.nanoTime() - s);
        s = System.nanoTime(); languageManager.reload(); times.put("languages", System.nanoTime() - s);
        s = System.nanoTime(); dataService.reload(); times.put("data.yml", System.nanoTime() - s);
        s = System.nanoTime(); warpService.load(); times.put("warps.yml", System.nanoTime() - s);
        if (moderationService != null) {
            s = System.nanoTime(); moderationService.reload(); times.put("moderation.yml", System.nanoTime() - s);
        }
        if (motdService != null) {
            s = System.nanoTime(); motdService.reload(); times.put("motd.yml", System.nanoTime() - s);
        }
        if (joinLeaveService != null) {
            s = System.nanoTime(); joinLeaveService.reload(); times.put("join-leave", System.nanoTime() - s);
        }
        if (chatService != null) {
            s = System.nanoTime(); chatService.reload(); times.put("chat.yml", System.nanoTime() - s);
        }
        if (tablistService != null) {
            s = System.nanoTime();
            tablistService.reload();
            if (tablistRefreshTask != null) {
                tablistRefreshTask.start();
            }
            times.put("tablist.yml", System.nanoTime() - s);
        }
        if (scoreboardService != null) {
            s = System.nanoTime();
            scoreboardService.reload();
            if (scoreboardRefreshTask != null) {
                scoreboardRefreshTask.start();
            }
            times.put("scoreboard.yml", System.nanoTime() - s);
        }
        if (resourcePackManager != null) {
            s = System.nanoTime();
            resourcePackManager.refreshConfig();
            resourcePackManager.installBundledPack();
            resourcePackManager.prepareHosting();
            times.put("resource-pack", System.nanoTime() - s);
        }
        if (hubService != null) {
            s = System.nanoTime(); hubService.reload(); times.put("hub.yml", System.nanoTime() - s);
        }
        if (enchantService != null) {
            s = System.nanoTime(); enchantService.reload(); times.put("enchants", System.nanoTime() - s);
        }
        if (enchantLoot != null) {
            s = System.nanoTime(); enchantLoot.reload(); times.put("enchant-loot", System.nanoTime() - s);
        }
        if (hologramService != null) {
            s = System.nanoTime(); hologramService.reload(); times.put("holograms.yml", System.nanoTime() - s);
        }
        return times;
    }

    private void registerCommands() {
        SpawnCommand spawnCommand = new SpawnCommand(this);
        WarpCommand warpCommand = new WarpCommand(this);
        bind("obx", new ObxCommand(this, languageManager));
        bind("help", new HelpGuiCommand(this));
        bind("list", new ListCommand(this));
        bind("home", new HomeCommand(this));
        bind("sethome", new SetHomeCommand(this));
        bind("delhome", new DelHomeCommand(this));
        bind("homes", new HomesCommand(this));
        bind("spawn", spawnCommand);
        bind("setspawn", spawnCommand);
        getServer().getPluginManager().registerEvents(spawnCommand, this);
        getServer().getPluginManager().registerEvents(messageService, this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.listener.menu.InboxMenuListener(this), this);
        bind("warp", warpCommand);
        bind("gamemode", new GamemodeCommand(this));
        bind("back", new BackCommand(this));
        bind("tp", new dev.zcripted.obx.command.teleportation.TeleportCommand(this, dev.zcripted.obx.command.teleportation.TeleportCommand.Mode.TO));
        bind("tphere", new dev.zcripted.obx.command.teleportation.TeleportCommand(this, dev.zcripted.obx.command.teleportation.TeleportCommand.Mode.HERE));
        bind("tpa", new dev.zcripted.obx.command.teleportation.TpaCommand(this, dev.zcripted.obx.command.teleportation.TpaCommand.Mode.REQUEST));
        bind("tpaccept", new dev.zcripted.obx.command.teleportation.TpaCommand(this, dev.zcripted.obx.command.teleportation.TpaCommand.Mode.ACCEPT));
        bind("tpdeny", new dev.zcripted.obx.command.teleportation.TpaCommand(this, dev.zcripted.obx.command.teleportation.TpaCommand.Mode.DENY));
        bind("pos", new dev.zcripted.obx.command.teleportation.PositionCommand(this));
        bind("msg", new dev.zcripted.obx.command.message.MsgCommand(this));
        bind("rply", new dev.zcripted.obx.command.message.ReplyCommand(this));
        bind("inbox", new dev.zcripted.obx.command.message.InboxCommand(this));
        bind("kill", new KillCommand(this));
        bind("tps", new TpsCommand(this));
        bind("pl", new PluginListCommand(this));
        bind("top", new TopCommand(this));
        bind("heal", new HealCommand(this));
        bind("feed", new FeedCommand(this));
        bind("vital", new VitalCommand(this));
        bind("god", new GodCommand(this));
        bind("vanish", new VanishCommand(this));
        bind("invsee", new InvSeeCommand(this));
        bind("craft", new CraftCommand(this));
        bind("research", new ResearchCommand(this));
        bind("anvil", new AnvilCommand(this));
        bind("enchant", new EnchantCommand(this));
        bind("smith", new SmithCommand(this));
        bind("stonecut", new VirtualStationCommand(this, VirtualStationCommand.Station.STONECUTTER));
        bind("loom", new VirtualStationCommand(this, VirtualStationCommand.Station.LOOM));
        bind("grindstone", new VirtualStationCommand(this, VirtualStationCommand.Station.GRINDSTONE));
        bind("cartography", new VirtualStationCommand(this, VirtualStationCommand.Station.CARTOGRAPHY));
        bind("map", new MapCommand(this));
        bind("language", new LanguageCommand(this));
        bind("sprache", new LanguageCommand(this));
        bind("tpcancel", new dev.zcripted.obx.command.teleportation.TpCancelCommand(this));
        bind("tptoggle", new dev.zcripted.obx.command.teleportation.TpToggleCommand(this));
        bind("tppos", new dev.zcripted.obx.command.teleportation.TpPosCommand(this));
        bind("tpall", new dev.zcripted.obx.command.teleportation.TpAllCommand(this));
        bind("ignore", new dev.zcripted.obx.command.messaging.IgnoreCommand(this));
        bind("socialspy", new dev.zcripted.obx.command.messaging.SocialSpyCommand(this));
        bind("mail", new dev.zcripted.obx.command.messaging.MailCommand(this));
        bind("me", new dev.zcripted.obx.command.messaging.MeCommand(this));
        bind("broadcast", new dev.zcripted.obx.command.messaging.BroadcastCommand(this));
        bind("staffchat", new dev.zcripted.obx.command.messaging.StaffChatCommand(this));
        bind("afk", new dev.zcripted.obx.command.utility.AfkCommand(this));
        bind("kit", new dev.zcripted.obx.command.utility.KitCommand(this));
        bind("balance", new dev.zcripted.obx.command.economy.BalanceCommand(this));
        bind("baltop", new dev.zcripted.obx.command.economy.BalTopCommand(this));
        bind("pay", new dev.zcripted.obx.command.economy.PayCommand(this));
        bind("eco", new dev.zcripted.obx.command.economy.EcoCommand(this));
        bind("worth", new dev.zcripted.obx.command.economy.WorthCommand(this));
        bind("sell", new dev.zcripted.obx.command.economy.SellCommand(this));
        bind("sellall", new dev.zcripted.obx.command.economy.SellAllCommand(this));
        bind("seen", new dev.zcripted.obx.command.info.SeenCommand(this));
        bind("firstseen", new dev.zcripted.obx.command.info.FirstSeenCommand(this));
        bind("playtime", new dev.zcripted.obx.command.info.PlaytimeCommand(this));
        bind("list", new dev.zcripted.obx.command.info.ListCommand(this));
        bind("near", new dev.zcripted.obx.command.info.NearCommand(this));
        bind("whois", new dev.zcripted.obx.command.info.WhoisCommand(this));
        bind("realname", new dev.zcripted.obx.command.info.RealnameCommand(this));
        bind("info", new dev.zcripted.obx.command.info.InfoCommand(this));
        bind("fly", new dev.zcripted.obx.command.utility.FlyCommand(this));
        bind("flyspeed", new dev.zcripted.obx.command.utility.FlySpeedCommand(this));
        bind("walkspeed", new dev.zcripted.obx.command.utility.WalkSpeedCommand(this));
        bind("freeze", new dev.zcripted.obx.command.admin.FreezeCommand(this));
        bind("enderchest", new dev.zcripted.obx.command.utility.EnderchestCommand(this));
        bind("disposal", new dev.zcripted.obx.command.utility.DisposalCommand(this));
        bind("hat", new dev.zcripted.obx.command.utility.HatCommand(this));
        bind("clearinv", new dev.zcripted.obx.command.utility.ClearInvCommand(this));
        bind("repair", new dev.zcripted.obx.command.utility.RepairCommand(this));
        bind("more", new dev.zcripted.obx.command.utility.MoreCommand(this));
        bind("skull", new dev.zcripted.obx.command.utility.SkullCommand(this));
        bind("itemname", new dev.zcripted.obx.command.utility.ItemNameCommand(this));
        bind("itemlore", new dev.zcripted.obx.command.utility.ItemLoreCommand(this));
        bind("unbreakable", new dev.zcripted.obx.command.utility.UnbreakableCommand(this));
        bind("give", new dev.zcripted.obx.command.utility.GiveCommand(this));
        bind("i", new dev.zcripted.obx.command.utility.ItemCommand(this));
        bind("book", new dev.zcripted.obx.command.utility.BookCommand(this));
        bind("nick", new dev.zcripted.obx.command.utility.NickCommand(this));
        bind("time", new dev.zcripted.obx.command.world.TimeCommand(this));
        bind("day", new dev.zcripted.obx.command.world.DayCommand(this, 1000L));
        bind("night", new dev.zcripted.obx.command.world.DayCommand(this, 13000L));
        bind("sun", new dev.zcripted.obx.command.world.DayCommand(this, 6000L));
        bind("weather", new dev.zcripted.obx.command.world.WeatherCommand(this));
        bind("ptime", new dev.zcripted.obx.command.world.PTimeCommand(this));
        bind("pweather", new dev.zcripted.obx.command.world.PWeatherCommand(this));
        bind("jail", new dev.zcripted.obx.command.admin.JailCommand(this));
        bind("unjail", new dev.zcripted.obx.command.admin.UnjailCommand(this));
        bind("jails", new dev.zcripted.obx.command.admin.JailsCommand(this));
        bind("setjail", new dev.zcripted.obx.command.admin.SetJailCommand(this));
        bind("deljail", new dev.zcripted.obx.command.admin.DelJailCommand(this));
        bind("jailtime", new dev.zcripted.obx.command.admin.JailTimeCommand(this));
        bind("butcher", new dev.zcripted.obx.command.admin.ButcherCommand(this));
        bind("spawnmob", new dev.zcripted.obx.command.admin.SpawnMobCommand(this));
        bind("spawner", new dev.zcripted.obx.command.admin.SpawnerCommand(this));
        bind("smite", new dev.zcripted.obx.command.admin.SmiteCommand(this));
        bind("tree", new dev.zcripted.obx.command.admin.TreeCommand(this));
        bind("ban", new ModerationCommand(this, ModerationCommand.Action.BAN));
        bind("unban", new ModerationCommand(this, ModerationCommand.Action.UNBAN));
        bind("kick", new ModerationCommand(this, ModerationCommand.Action.KICK));
        bind("mute", new ModerationCommand(this, ModerationCommand.Action.MUTE));
        bind("unmute", new ModerationCommand(this, ModerationCommand.Action.UNMUTE));
        bind("tempban", new ModerationCommand(this, ModerationCommand.Action.TEMPBAN));
        bind("warn", new ModerationCommand(this, ModerationCommand.Action.WARN));
        bind("banlist", new BanListCommand(this));
        bind("status", new ModerationStatusCommand(this));
        bind("staff", new dev.zcripted.obx.command.admin.StaffCommand(this));
        bind("hub", new HubCommand(this, hubService, hubKitApplier));
        bind("obxench", new ObxEnchantCommand(this));
        bind("enchants", new EnchantsBrowseCommand(this));
        bind("recall", new RecallCommand(this, enchantState));
        bind("satchel", new SatchelCommand(this, enchantState));
        bind("holo", new dev.zcripted.obx.hologram.command.HologramCommand(this, hologramService));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(teleportManager, this);
        getServer().getPluginManager().registerEvents(godModeManager, this);
        getServer().getPluginManager().registerEvents(killModeManager, this);
        getServer().getPluginManager().registerEvents(vanishManager, this);
        getServer().getPluginManager().registerEvents(new BackListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this, joinLeaveService), this);
        getServer().getPluginManager().registerEvents(new CommandOverrideListener(this), this);
        getServer().getPluginManager().registerEvents(new MainMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new HelpGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new WarpMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinLockListener(languageManager), this);
        getServer().getPluginManager().registerEvents(new RedstoneControlListener(), this);
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this, resourcePackManager), this);
        getServer().getPluginManager().registerEvents(new WarpMenuInputListener(warpMenuInputManager), this);
        getServer().getPluginManager().registerEvents(staffSessionTracker, this);
        if (afkService != null) {
            getServer().getPluginManager().registerEvents(afkService, this);
        }
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.listener.menu.StaffMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.listener.menu.InvSeeMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.listener.chat.StaffMenuInputListener(staffMenuInputManager), this);
        getServer().getPluginManager().registerEvents(new ChatManagementListener(this, chatService), this);
        getServer().getPluginManager().registerEvents(new TablistJoinListener(this, tablistService), this);
        getServer().getPluginManager().registerEvents(new ScoreboardJoinListener(this, scoreboardService), this);
        MotdPingListener motdPingListener = new MotdPingListener(this);
        getServer().getPluginManager().registerEvents(motdPingListener, this);
        // Paper (and forks) fire PaperServerListPingEvent on its own HandlerList,
        // so register for it explicitly — without this the server-list hover
        // (player sample) never gets set on Paper. No-op on base Spigot.
        motdPingListener.registerPaperPingListener();

        // Hub / Lobby system listeners (early-exit when hub-mode is off,
        // so registration cost is one ConcurrentHashMap read per event in
        // the dormant state).
        getServer().getPluginManager().registerEvents(new HubJoinListener(this, hubService, hubKitApplier), this);
        hubItemUseListener = new HubItemUseListener(this, hubService);
        getServer().getPluginManager().registerEvents(hubItemUseListener, this);
        getServer().getPluginManager().registerEvents(new HubItemProtectionListener(this, hubService), this);
        getServer().getPluginManager().registerEvents(new HubFishingListener(this, hubService), this);
        getServer().getPluginManager().registerEvents(new HubLaunchpadListener(this, hubService, launchpadCooldownManager), this);
        getServer().getPluginManager().registerEvents(new HubFallDamageListener(launchpadCooldownManager), this);
        getServer().getPluginManager().registerEvents(new ServerSelectorListener(this), this);

        // Arcanum enchantment module listeners (combat effects + GUI routing).
        getServer().getPluginManager().registerEvents(new CombatEnchantListener(this, enchantService, combatHud), this);
        getServer().getPluginManager().registerEvents(new EnchantMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilEnchantListener(this, scrollApplyService), this);
        getServer().getPluginManager().registerEvents(new ScrollDragListener(this, scrollApplyService), this);
        getServer().getPluginManager().registerEvents(new DefenseEnchantListener(this, enchantState), this);
        getServer().getPluginManager().registerEvents(new ToolEnchantListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmingEnchantListener(this), this);
        getServer().getPluginManager().registerEvents(new UtilityEnchantListener(this), this);
        getServer().getPluginManager().registerEvents(new MovementEnchantListener(this, enchantState), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.enchant.effect.SatchelCloseListener(enchantState), this);
        getServer().getPluginManager().registerEvents(new CursedEnchantListener(this, enchantBoundMovement), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.enchant.listener.EnchantLoreListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.enchant.listener.EnchantBookUseListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.enchant.listener.OnHitDamageListener(this, combatState, combatParticles, holoFX, combatHud), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.enchant.listener.OnKillListener(this, combatState, combatParticles, holoFX), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.enchant.listener.OnHitProcListener(this, combatState, combatParticles, combatHud), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.enchant.listener.OnDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.enchant.listener.RangedListener(this, combatState, combatParticles, reactiveSpecials, combatHud), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.enchant.listener.ReactiveSpecialsListener(this, combatState, reactiveSpecials), this);

        // Holograms module listeners — re-shows holograms on join / respawn / world-change / resource-pack reload.
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.hologram.listener.HologramJoinListener(this, hologramService), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.hologram.listener.HologramResourcePackListener(this, hologramService), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.hologram.listener.HologramConnectionListener(this, hologramService), this);
        if (hologramEditorMenu != null) {
            getServer().getPluginManager().registerEvents(hologramEditorMenu, this);
        }
    }

    public dev.zcripted.obx.hologram.gui.HologramEditorMenu getHologramEditorMenu() {
        return hologramEditorMenu;
    }

    private void bind(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command " + name + " was not found in plugin.yml");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter) {
            command.setTabCompleter((TabCompleter) executor);
        }
    }

    private void loadBuildMetadata() {
        try (InputStream inputStream = getResource("build-info.properties")) {
            if (inputStream == null) {
                return;
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            releaseDate = properties.getProperty("releaseDate", releaseDate);
        } catch (IOException exception) {
            getLogger().warning("Unable to read build-info.properties: " + exception.getMessage());
        }
    }

    private static final PrintWriter DIRECT_CONSOLE = createDirectConsole();
    private static final Map<Character, String> ANSI_CODES = createAnsiMap();
    // OBX obsidian palette: deep violet (#2A0A45) -> magenta fleck (#6A1B9A). "Forged from Obsidian."
    private static final int[] GRADIENT_START = {42, 10, 69};
    private static final int[] GRADIENT_END = {106, 27, 154};

    private void printBanner(boolean starting) {
        for (String line : buildBannerLines(starting)) {
            writeRawConsoleLine(line);
        }
    }

    public void writeConsoleLine(String line) {
        writeRawConsoleLine(line);
    }

    private void writeRawConsoleLine(String line) {
        String colored = applyAnsi(line);
        if (DIRECT_CONSOLE != null) {
            DIRECT_CONSOLE.print(colored);
            DIRECT_CONSOLE.print(System.lineSeparator());
            DIRECT_CONSOLE.flush();
        } else {
            getLogger().info(stripAnsi(colored));
        }
    }

    private static PrintWriter createDirectConsole() {
        try {
            return new PrintWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.out), StandardCharsets.UTF_8), true);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> buildBannerLines(boolean starting) {
        String divider = ChatColor.DARK_PURPLE.toString() + ChatColor.STRIKETHROUGH + "----------------------------------------------------";
        String pluginName = getDescription().getName();
        List<String> authors = getDescription().getAuthors();
        String developer = authors == null || authors.isEmpty() ? "Unknown" : String.join(", ", authors);
        String version = getDescription().getVersion();
        String release = releaseDate != null ? releaseDate : "Unknown";
        String builtByBit = getLinkOrDefault("links.builtbybit", "https://builtbybit.com/resources/OBX");
        String wiki = getLinkOrDefault("links.wiki", "https://github.com/zcripted/OBX");
        String discord = getLinkOrDefault("links.discord", "https://discord.gg/zcripted");
        String loadTime = formatLoadDuration(lastLoadDurationMs);

        List<String> lines = new ArrayList<>();
        lines.add(" ");
        lines.add(divider);
        lines.add(gradient(" OBX", starting ? ChatColor.GREEN + ": Enabled" : ChatColor.RED + ": Disabling..."));
        lines.add(gradient(" Obsidian eXtended", ChatColor.DARK_GRAY + " — " + ChatColor.GRAY + "Forged from Obsidian."));
        lines.add(gradient(" Developer:", " " + ChatColor.YELLOW + developer));
        lines.add(gradient(" Version:", " " + ChatColor.YELLOW + version + ChatColor.DARK_PURPLE + " (Released " + release + ")"));
        lines.add(gradient(" Load Time:", " " + ChatColor.YELLOW + loadTime));
        lines.add(gradient(" BuiltByBit:", " " + ChatColor.LIGHT_PURPLE + builtByBit));
        lines.add(gradient(" Wiki:", " " + ChatColor.LIGHT_PURPLE + wiki));
        lines.add(gradient(" Discord:", " " + ChatColor.LIGHT_PURPLE + discord));
        lines.add(divider);
        lines.add(" ");
        return lines;
    }

    private String formatLoadDuration(long millis) {
        double seconds = millis / 1000.0;
        return new DecimalFormat("0.00s").format(seconds);
    }

    private String getLinkOrDefault(String path, String fallback) {
        String value = getConfig().getString(path);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    private String gradient(String label, String suffix) {
        StringBuilder builder = new StringBuilder();
        int len = label.length();
        for (int i = 0; i < len; i++) {
            char c = label.charAt(i);
            if (c == ' ') {
                builder.append(' ');
                continue;
            }
            double ratio = len <= 1 ? 0 : (double) i / (len - 1);
            builder.append(ansiForRatio(ratio)).append(c);
        }
        return builder.append(ChatColor.RESET).append(suffix).toString();
    }

    private String ansiForRatio(double ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        int r = (int) Math.round(GRADIENT_START[0] + (GRADIENT_END[0] - GRADIENT_START[0]) * ratio);
        int g = (int) Math.round(GRADIENT_START[1] + (GRADIENT_END[1] - GRADIENT_START[1]) * ratio);
        int b = (int) Math.round(GRADIENT_START[2] + (GRADIENT_END[2] - GRADIENT_START[2]) * ratio);
        return "\u001B[38;2;" + r + ";" + g + ";" + b + "m";
    }

    private static Map<Character, String> createAnsiMap() {
        Map<Character, String> map = new HashMap<>();
        map.put('0', "\u001B[30m");
        map.put('1', "\u001B[34m");
        map.put('2', "\u001B[32m");
        map.put('3', "\u001B[36m");
        map.put('4', "\u001B[31m");
        map.put('5', "\u001B[35m");
        map.put('6', "\u001B[33m");
        map.put('7', "\u001B[37m");
        map.put('8', "\u001B[90m");
        map.put('9', "\u001B[94m");
        map.put('a', "\u001B[92m");
        map.put('b', "\u001B[96m");
        map.put('c', "\u001B[91m");
        map.put('d', "\u001B[95m");
        map.put('e', "\u001B[93m");
        map.put('f', "\u001B[97m");
        map.put('l', "\u001B[1m");
        map.put('n', "\u001B[4m");
        map.put('o', "\u001B[3m");
        map.put('m', "\u001B[9m");
        map.put('r', "\u001B[0m");
        return map;
    }

    private String applyAnsi(String input) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ChatColor.COLOR_CHAR && i + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(++i));
                String ansi = ANSI_CODES.get(code);
                if (ansi != null) {
                    builder.append(ansi);
                    continue;
                }
            }
            builder.append(c);
        }
        builder.append("\u001B[0m");
        return builder.toString();
    }

    private String stripAnsi(String value) {
        return value.replaceAll("\u001B\\[[;\\d]*m", "").replace(ChatColor.COLOR_CHAR, '&');
    }
}