package dev.zcripted.obx;

import dev.zcripted.obx.feature.playerstate.command.KillCommand;
import dev.zcripted.obx.core.command.PluginListCommand;
import dev.zcripted.obx.core.diagnostics.TpsCommand;
import dev.zcripted.obx.core.command.HelpGuiCommand;
import dev.zcripted.obx.core.command.ListCommand;
import dev.zcripted.obx.core.command.ObxCommand;
import dev.zcripted.obx.feature.teleport.command.BackCommand;
import dev.zcripted.obx.feature.teleport.command.DelHomeCommand;
import dev.zcripted.obx.feature.teleport.command.HomeCommand;
import dev.zcripted.obx.feature.teleport.command.HomesCommand;
import dev.zcripted.obx.feature.hub.command.HubCommand;
import dev.zcripted.obx.feature.teleport.command.SetHomeCommand;
import dev.zcripted.obx.feature.teleport.command.SpawnCommand;
import dev.zcripted.obx.feature.teleport.command.TopCommand;
import dev.zcripted.obx.feature.warp.command.WarpCommand;
import dev.zcripted.obx.feature.hub.service.HubService;
import dev.zcripted.obx.feature.hub.kit.HubKitApplier;
import dev.zcripted.obx.feature.hub.launchpad.LaunchpadCooldownManager;
import dev.zcripted.obx.feature.hub.messaging.BungeeMessenger;
import dev.zcripted.obx.feature.enchant.command.EnchantsBrowseCommand;
import dev.zcripted.obx.feature.enchant.command.RecallCommand;
import dev.zcripted.obx.feature.enchant.command.SatchelCommand;
import dev.zcripted.obx.feature.enchant.command.ObxEnchantCommand;
import dev.zcripted.obx.feature.enchant.effect.BoundMovement;
import dev.zcripted.obx.feature.enchant.effect.EnchantState;
import dev.zcripted.obx.feature.enchant.effect.EnchantTickTask;
import dev.zcripted.obx.feature.enchant.gui.EnchantAdminMenu;
import dev.zcripted.obx.feature.enchant.gui.EnchantMenuListener;
import dev.zcripted.obx.feature.enchant.item.EnchantItems;
import dev.zcripted.obx.feature.enchant.listener.CombatEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.CursedEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.DefenseEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.FarmingEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.MovementEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.ToolEnchantListener;
import dev.zcripted.obx.feature.enchant.listener.UtilityEnchantListener;
import dev.zcripted.obx.feature.enchant.loot.EnchantLoot;
import dev.zcripted.obx.feature.enchant.scroll.AnvilEnchantListener;
import dev.zcripted.obx.feature.enchant.scroll.ScrollApplyService;
import dev.zcripted.obx.feature.enchant.scroll.ScrollDragListener;
import dev.zcripted.obx.feature.enchant.service.EnchantFeedback;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.feature.hub.listener.HubFallDamageListener;
import dev.zcripted.obx.feature.hub.listener.HubFishingListener;
import dev.zcripted.obx.feature.hub.listener.HubItemProtectionListener;
import dev.zcripted.obx.feature.hub.listener.HubItemUseListener;
import dev.zcripted.obx.feature.hub.listener.HubJoinListener;
import dev.zcripted.obx.feature.hub.listener.HubLaunchpadListener;
import dev.zcripted.obx.feature.hub.gui.ServerSelectorListener;
import dev.zcripted.obx.core.language.LanguageCommand;
import dev.zcripted.obx.feature.item.command.AnvilCommand;
import dev.zcripted.obx.feature.item.command.CraftCommand;
import dev.zcripted.obx.feature.item.command.EnchantCommand;
import dev.zcripted.obx.feature.playerstate.command.FeedCommand;
import dev.zcripted.obx.feature.playerstate.command.GamemodeCommand;
import dev.zcripted.obx.feature.playerstate.command.GodCommand;
import dev.zcripted.obx.feature.playerstate.command.HealCommand;
import dev.zcripted.obx.feature.item.command.MapCommand;
import dev.zcripted.obx.feature.item.command.ResearchCommand;
import dev.zcripted.obx.feature.item.command.SmithCommand;
import dev.zcripted.obx.feature.item.command.VirtualStationCommand;
import dev.zcripted.obx.feature.playerstate.command.VitalCommand;
import dev.zcripted.obx.feature.staff.command.InvSeeCommand;
import dev.zcripted.obx.feature.staff.command.VanishCommand;
import dev.zcripted.obx.feature.chat.ChatModule;
import dev.zcripted.obx.feature.chat.service.ChatService;
import dev.zcripted.obx.feature.nickname.NicknameModule;
import dev.zcripted.obx.feature.kit.KitModule;
import dev.zcripted.obx.feature.jail.JailModule;
import dev.zcripted.obx.feature.moderation.ModerationModule;
import dev.zcripted.obx.feature.tablist.listener.TablistJoinListener;
import dev.zcripted.obx.feature.tablist.scheduler.TablistRefreshTask;
import dev.zcripted.obx.feature.tablist.service.TablistService;
import dev.zcripted.obx.feature.scoreboard.listener.ScoreboardJoinListener;
import dev.zcripted.obx.feature.scoreboard.scheduler.ScoreboardRefreshTask;
import dev.zcripted.obx.feature.scoreboard.service.ScoreboardService;
import dev.zcripted.obx.feature.scoreboard.ScoreboardModule;
import dev.zcripted.obx.feature.tablist.TablistModule;
import dev.zcripted.obx.feature.economy.EconomyModule;
import dev.zcripted.obx.feature.warp.gui.WarpMenuInputListener;
import dev.zcripted.obx.feature.teleport.listener.BackListener;
import dev.zcripted.obx.core.command.CommandOverrideListener;
import dev.zcripted.obx.feature.world.listener.JoinLockListener;
import dev.zcripted.obx.feature.playerinfo.listener.JoinListener;
import dev.zcripted.obx.feature.playerinfo.listener.JoinLeaveListener;
import dev.zcripted.obx.core.gui.help.HelpGuiListener;
import dev.zcripted.obx.core.gui.main.MainMenuListener;
import dev.zcripted.obx.feature.warp.gui.WarpMenuListener;
import dev.zcripted.obx.core.motd.MotdPingListener;
import dev.zcripted.obx.feature.world.listener.RedstoneControlListener;
import dev.zcripted.obx.core.platform.PlatformInfo;
import dev.zcripted.obx.core.platform.resourcepack.AutoResourcePackManager;
import dev.zcripted.obx.core.platform.resourcepack.ResourcePackListener;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.core.storage.DataService;
import dev.zcripted.obx.feature.playerinfo.service.JoinLeaveService;
import dev.zcripted.obx.core.motd.MotdService;
import dev.zcripted.obx.feature.warp.service.WarpService;
import dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.feature.moderation.service.ModerationService;
import dev.zcripted.obx.feature.playerstate.service.GodModeManager;
import dev.zcripted.obx.feature.playerstate.service.KillModeManager;
import dev.zcripted.obx.feature.staff.service.VanishManager;
import dev.zcripted.obx.core.diagnostics.TpsService;
import dev.zcripted.obx.feature.teleport.service.TeleportManager;
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
    private LanguageManager languageManager;
    private MotdService motdService;
    private AutoResourcePackManager resourcePackManager;
    private TpsService tpsService;
    private dev.zcripted.obx.core.storage.SqliteDataStore dataStore;
    private SchedulerAdapter scheduler;
    private PlatformInfo platformInfo;
    private dev.zcripted.obx.feature.hologram.service.HologramService hologramService;
    private dev.zcripted.obx.feature.hologram.gui.HologramEditorMenu hologramEditorMenu;
    private String releaseDate = "Unknown";
    private long lastLoadDurationMs;
    private final dev.zcripted.obx.core.service.ServiceRegistry serviceRegistry = new dev.zcripted.obx.core.service.ServiceRegistry();
    private final dev.zcripted.obx.core.module.ModuleManager moduleManager = new dev.zcripted.obx.core.module.ModuleManager(this);

    /** Shared service container; feature modules register here and the getters below read from it. */
    public dev.zcripted.obx.core.service.ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /** Drives feature module lifecycle (enable/disable/reload/runtime-toggle). */
    public dev.zcripted.obx.core.module.ModuleManager getModuleManager() {
        return moduleManager;
    }

    @Override
    public void onEnable() {
        long enableStart = System.nanoTime();
        saveDefaultConfig();
        loadBuildMetadata();

        platformInfo = PlatformInfo.get();
        scheduler = new SchedulerAdapter(this);
        dev.zcripted.obx.util.message.ConsoleLog.info(this, "Detected platform: " + platformInfo.summary());

        languageManager = new LanguageManager(this);
        dataStore = new dev.zcripted.obx.core.storage.SqliteDataStore(this);
        dataStore.open();
        dataService = new DataService(this);
        dataService.load();
        motdService = new MotdService(this);
        motdService.load();
        resourcePackManager = new AutoResourcePackManager(this);
        resourcePackManager.installBundledPack();
        resourcePackManager.prepareHosting();
        tpsService = new TpsService(this);

        // Holograms module. Dormant when systems/holograms.yml master enabled=false.
        // Renders via real display entities on 1.19.4+, armor-stand fallback below.
        hologramService = new dev.zcripted.obx.feature.hologram.service.HologramService(this);
        hologramService.load();
        hologramEditorMenu = new dev.zcripted.obx.feature.hologram.gui.HologramEditorMenu(this);

        registerModules();
        moduleManager.enableAll();

        registerCommands();
        registerListeners();

        tpsService.start();

        lastLoadDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - enableStart);
        printBanner(true);
    }

    @Override
    public void onDisable() {
        moduleManager.disableAll();
        if (tpsService != null) {
            tpsService.stop();
        }
        if (dataService != null) {
            dataService.save();
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
        return serviceRegistry.get(WarpService.class);
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public TeleportManager getTeleportManager() {
        return serviceRegistry.get(TeleportManager.class);
    }

    public dev.zcripted.obx.feature.teleport.service.TeleportRequestService getTeleportRequestService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.teleport.service.TeleportRequestService.class);
    }

    public dev.zcripted.obx.feature.mail.pm.PrivateMessageService getMessageService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.mail.pm.PrivateMessageService.class);
    }

    public GodModeManager getGodModeManager() {
        return serviceRegistry.get(GodModeManager.class);
    }

    public KillModeManager getKillModeManager() {
        return serviceRegistry.get(KillModeManager.class);
    }

    public VanishManager getVanishManager() {
        return serviceRegistry.get(VanishManager.class);
    }

    public dev.zcripted.obx.feature.staff.gui.InvSeeMenuManager getInvSeeMenuManager() {
        return serviceRegistry.get(dev.zcripted.obx.feature.staff.gui.InvSeeMenuManager.class);
    }

    public ModerationService getModerationService() {
        return serviceRegistry.get(ModerationService.class);
    }

    public MotdService getMotdService() {
        return motdService;
    }

    public JoinLeaveService getJoinLeaveService() {
        return serviceRegistry.get(JoinLeaveService.class);
    }

    public ChatService getChatService() {
        return serviceRegistry.get(ChatService.class);
    }

    public TablistService getTablistService() {
        return serviceRegistry.get(TablistService.class);
    }

    public ScoreboardService getScoreboardService() {
        return serviceRegistry.get(ScoreboardService.class);
    }

    public AutoResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public WarpMenuInputManager getWarpMenuInputManager() {
        return serviceRegistry.get(WarpMenuInputManager.class);
    }

    public dev.zcripted.obx.feature.staff.gui.StaffMenuInputManager getStaffMenuInputManager() {
        return serviceRegistry.get(dev.zcripted.obx.feature.staff.gui.StaffMenuInputManager.class);
    }

    public dev.zcripted.obx.feature.staff.service.StaffSessionTracker getStaffSessionTracker() {
        return serviceRegistry.get(dev.zcripted.obx.feature.staff.service.StaffSessionTracker.class);
    }

    public TpsService getTpsService() {
        return tpsService;
    }

    public dev.zcripted.obx.feature.teleport.service.TpaService getTpaService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.teleport.service.TpaService.class);
    }

    public dev.zcripted.obx.feature.mail.mail.MailService getMailService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.mail.mail.MailService.class);
    }

    public dev.zcripted.obx.feature.playerstate.service.AfkService getAfkService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.playerstate.service.AfkService.class);
    }

    public dev.zcripted.obx.feature.kit.service.KitService getKitService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.kit.service.KitService.class);
    }

    public dev.zcripted.obx.api.economy.EconomyService getEconomyService() {
        return serviceRegistry.get(dev.zcripted.obx.api.economy.EconomyService.class);
    }

    public dev.zcripted.obx.feature.economy.service.WorthService getWorthService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.economy.service.WorthService.class);
    }

    public dev.zcripted.obx.feature.playerinfo.service.PlaytimeService getPlaytimeService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.playerinfo.service.PlaytimeService.class);
    }

    public dev.zcripted.obx.core.storage.SqliteDataStore getDataStore() {
        return dataStore;
    }

    public dev.zcripted.obx.feature.playerstate.service.FlightStateService getFlightStateService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.playerstate.service.FlightStateService.class);
    }

    public dev.zcripted.obx.feature.staff.service.FreezeService getFreezeService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.staff.service.FreezeService.class);
    }

    public dev.zcripted.obx.feature.nickname.service.NicknameService getNicknameService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.nickname.service.NicknameService.class);
    }

    public dev.zcripted.obx.feature.world.service.PerPlayerTimeService getPerPlayerTimeService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.world.service.PerPlayerTimeService.class);
    }

    public dev.zcripted.obx.feature.jail.service.JailService getJailService() {
        return serviceRegistry.get(dev.zcripted.obx.feature.jail.service.JailService.class);
    }

    public SchedulerAdapter getSchedulerAdapter() {
        return scheduler;
    }

    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    public HubService getHubService() {
        return serviceRegistry.get(HubService.class);
    }

    public HubKitApplier getHubKitApplier() {
        return serviceRegistry.get(HubKitApplier.class);
    }

    public LaunchpadCooldownManager getLaunchpadCooldownManager() {
        return serviceRegistry.get(LaunchpadCooldownManager.class);
    }

    public BungeeMessenger getBungeeMessenger() {
        return serviceRegistry.get(BungeeMessenger.class);
    }

    public HubItemUseListener getHubItemUseListener() {
        return serviceRegistry.get(HubItemUseListener.class);
    }

    public EnchantService getEnchantService() {
        return serviceRegistry.get(EnchantService.class);
    }

    public EnchantItems getEnchantItems() {
        return serviceRegistry.get(EnchantItems.class);
    }

    public EnchantFeedback getEnchantFeedback() {
        return serviceRegistry.get(EnchantFeedback.class);
    }

    public EnchantAdminMenu getEnchantAdminMenu() {
        return serviceRegistry.get(EnchantAdminMenu.class);
    }

    public dev.zcripted.obx.feature.hologram.service.HologramService getHologramService() {
        return hologramService;
    }

    /**
     * Reloads the world-loot generation listener. The loot subsystem (Phase 4)
     * registers itself here once wired in; until then this is a safe no-op so the
     * {@code /obxench loot reload} command remains valid.
     */
    public void reloadEnchantLoot() {
        dev.zcripted.obx.feature.enchant.loot.EnchantLoot enchantLoot =
                serviceRegistry.get(dev.zcripted.obx.feature.enchant.loot.EnchantLoot.class);
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
        if (motdService != null) {
            s = System.nanoTime(); motdService.reload(); times.put("motd.yml", System.nanoTime() - s);
        }
        if (resourcePackManager != null) {
            s = System.nanoTime();
            resourcePackManager.refreshConfig();
            resourcePackManager.installBundledPack();
            resourcePackManager.prepareHosting();
            times.put("resource-pack", System.nanoTime() - s);
        }
        if (hologramService != null) {
            s = System.nanoTime(); hologramService.reload(); times.put("holograms.yml", System.nanoTime() - s);
        }
        s = System.nanoTime(); moduleManager.reloadAll(); times.put("modules", System.nanoTime() - s);
        return times;
    }

    /**
     * Registers every feature module with the {@link dev.zcripted.obx.core.module.ModuleManager}.
     * Modules are enabled in registration order (the manager's topological sort
     * preserves it for independent modules), which mirrors the historical
     * construction order. Features migrate out of {@link #registerCommands()} /
     * {@link #registerListeners()} into their module's {@code onEnable} over the
     * course of the package-by-feature restructure.
     */
    private void registerModules() {
        moduleManager.register(new ChatModule());
        moduleManager.register(new NicknameModule());
        moduleManager.register(new KitModule());
        moduleManager.register(new JailModule());
        moduleManager.register(new ModerationModule());
        moduleManager.register(new ScoreboardModule());
        moduleManager.register(new TablistModule());
        moduleManager.register(new EconomyModule());
        moduleManager.register(new dev.zcripted.obx.feature.item.ItemModule());
        moduleManager.register(new dev.zcripted.obx.feature.world.WorldModule());
        moduleManager.register(new dev.zcripted.obx.feature.playerstate.PlayerStateModule());
        moduleManager.register(new dev.zcripted.obx.feature.playerinfo.PlayerInfoModule());
        moduleManager.register(new dev.zcripted.obx.feature.teleport.TeleportModule());
        moduleManager.register(new dev.zcripted.obx.feature.mail.MailModule());
        moduleManager.register(new dev.zcripted.obx.feature.warp.WarpModule());
        moduleManager.register(new dev.zcripted.obx.feature.staff.StaffModule());
        moduleManager.register(new dev.zcripted.obx.feature.hub.HubModule());
        moduleManager.register(new dev.zcripted.obx.feature.enchant.EnchantModule());
    }

    private void registerCommands() {
        bind("obx", new ObxCommand(this, languageManager));
        bind("help", new HelpGuiCommand(this));
        bind("tps", new TpsCommand(this));
        bind("pl", new PluginListCommand(this));
        bind("language", new LanguageCommand(this));
        bind("sprache", new LanguageCommand(this));
        bind("holo", new dev.zcripted.obx.feature.hologram.command.HologramCommand(this, hologramService));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new CommandOverrideListener(this), this);
        getServer().getPluginManager().registerEvents(new MainMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new HelpGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this, resourcePackManager), this);
        MotdPingListener motdPingListener = new MotdPingListener(this);
        getServer().getPluginManager().registerEvents(motdPingListener, this);
        // Paper (and forks) fire PaperServerListPingEvent on its own HandlerList,
        // so register for it explicitly — without this the server-list hover
        // (player sample) never gets set on Paper. No-op on base Spigot.
        motdPingListener.registerPaperPingListener();


        // Holograms module listeners — re-shows holograms on join / respawn / world-change / resource-pack reload.
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.feature.hologram.listener.HologramJoinListener(this, hologramService), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.feature.hologram.listener.HologramResourcePackListener(this, hologramService), this);
        getServer().getPluginManager().registerEvents(new dev.zcripted.obx.feature.hologram.listener.HologramConnectionListener(this, hologramService), this);
        if (hologramEditorMenu != null) {
            getServer().getPluginManager().registerEvents(hologramEditorMenu, this);
        }
    }

    public dev.zcripted.obx.feature.hologram.gui.HologramEditorMenu getHologramEditorMenu() {
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