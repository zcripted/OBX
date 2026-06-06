package dev.zcripted.obx;

import dev.zcripted.obx.core.command.PluginListCommand;
import dev.zcripted.obx.core.diagnostics.TpsCommand;
import dev.zcripted.obx.core.command.HelpGuiCommand;
import dev.zcripted.obx.core.command.ObxCommand;
import dev.zcripted.obx.feature.hub.service.HubService;
import dev.zcripted.obx.api.hub.HubKitApplier;
import dev.zcripted.obx.feature.hub.launchpad.LaunchpadCooldownManager;
import dev.zcripted.obx.feature.hub.messaging.BungeeMessenger;
import dev.zcripted.obx.feature.enchant.gui.EnchantAdminMenu;
import dev.zcripted.obx.feature.enchant.item.EnchantItems;
import dev.zcripted.obx.feature.enchant.loot.EnchantLoot;
import dev.zcripted.obx.feature.enchant.service.EnchantFeedback;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import dev.zcripted.obx.api.hub.HubItemUseListener;
import dev.zcripted.obx.core.language.LanguageCommand;
import dev.zcripted.obx.feature.chat.ChatModule;
import dev.zcripted.obx.api.chat.ChatService;
import dev.zcripted.obx.feature.nickname.NicknameModule;
import dev.zcripted.obx.feature.kit.KitModule;
import dev.zcripted.obx.feature.jail.JailModule;
import dev.zcripted.obx.feature.moderation.ModerationModule;
import dev.zcripted.obx.api.tablist.TablistService;
import dev.zcripted.obx.api.scoreboard.ScoreboardService;
import dev.zcripted.obx.feature.scoreboard.ScoreboardModule;
import dev.zcripted.obx.feature.tablist.TablistModule;
import dev.zcripted.obx.feature.economy.EconomyModule;
import dev.zcripted.obx.core.command.CommandOverrideListener;
import dev.zcripted.obx.core.gui.help.HelpGuiListener;
import dev.zcripted.obx.core.gui.main.MainMenuListener;
import dev.zcripted.obx.core.motd.MotdPingListener;
import dev.zcripted.obx.core.platform.PlatformInfo;
import dev.zcripted.obx.core.platform.resourcepack.AutoResourcePackManager;
import dev.zcripted.obx.core.platform.resourcepack.ResourcePackListener;
import dev.zcripted.obx.core.platform.scheduler.SchedulerAdapter;
import dev.zcripted.obx.core.storage.DataService;
import dev.zcripted.obx.api.playerinfo.JoinLeaveService;
import dev.zcripted.obx.core.motd.MotdService;
import dev.zcripted.obx.feature.warp.service.WarpService;
import dev.zcripted.obx.feature.warp.gui.WarpMenuInputManager;
import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.feature.moderation.service.ModerationService;
import dev.zcripted.obx.feature.playerstate.service.GodModeManager;
import dev.zcripted.obx.feature.playerstate.service.KillModeManager;
import dev.zcripted.obx.feature.staff.service.VanishManager;
import dev.zcripted.obx.core.diagnostics.TpsService;
import dev.zcripted.obx.api.teleport.TeleportManager;
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

public class OBX extends JavaPlugin implements dev.zcripted.obx.core.ObxPlugin {

    private DataService dataService;
    private LanguageManager languageManager;
    private MotdService motdService;
    private AutoResourcePackManager resourcePackManager;
    private TpsService tpsService;
    private dev.zcripted.obx.core.storage.SqliteDataStore dataStore;
    private dev.zcripted.obx.util.update.UpdateNotificationService updateNotificationService;
    private SchedulerAdapter scheduler;
    private PlatformInfo platformInfo;
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

        // Update notifications: registered before commands bind so ObxCommand can resolve the
        // shared instance; started after listeners so the startup-check result and any periodic
        // announces find a fully wired plugin. Holders of obx.updates.notify are notified by
        // default; /obx updates notify records a persisted opt-out.
        updateNotificationService = new dev.zcripted.obx.util.update.UpdateNotificationService(this);
        serviceRegistry.register(dev.zcripted.obx.util.update.UpdateNotificationService.class, updateNotificationService);

        registerModules();
        moduleManager.enableAll();

        registerCommands();
        registerListeners();

        tpsService.start();
        updateNotificationService.start();

        lastLoadDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - enableStart);
        printBanner(true);
    }

    @Override
    public void onDisable() {
        // Switch the store to inline writes BEFORE anything saves: the server cancels the
        // plugin's async pool on stop, so module/per-player final writes must run sync.
        if (dataStore != null) {
            dataStore.beginShutdown();
        }
        moduleManager.disableAll();
        if (updateNotificationService != null) {
            updateNotificationService.stop();
        }
        if (tpsService != null) {
            tpsService.stop();
        }
        if (dataService != null) {
            dataService.save();
        }
        // Checkpoint the WAL and close the connection so nothing is left only in obx.db-wal.
        if (dataStore != null) {
            dataStore.close();
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

    public dev.zcripted.obx.api.playerstate.AfkService getAfkService() {
        return serviceRegistry.get(dev.zcripted.obx.api.playerstate.AfkService.class);
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
        return serviceRegistry.get(dev.zcripted.obx.feature.hologram.service.HologramService.class);
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
        s = System.nanoTime(); moduleManager.reloadAll(); times.put("modules", System.nanoTime() - s);
        if (updateNotificationService != null) {
            // Re-reads updates.check-interval-minutes and reschedules the periodic check.
            s = System.nanoTime(); updateNotificationService.reload(); times.put("update-checker", System.nanoTime() - s);
        }
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
        moduleManager.register(new dev.zcripted.obx.feature.hologram.HologramModule());
        moduleManager.register(new dev.zcripted.obx.feature.deathdrop.DeathDropModule());
        moduleManager.register(new dev.zcripted.obx.feature.backpack.BackpackModule());
    }

    private void registerCommands() {
        bind("obx", new ObxCommand(this, languageManager));
        bind("help", new HelpGuiCommand(this));
        bind("tps", new TpsCommand(this));
        bind("health", new dev.zcripted.obx.core.diagnostics.HealthCommand(this));
        bind("pl", new PluginListCommand(this));
        bind("preview", new dev.zcripted.obx.core.command.PreviewCommand(this));
        bind("language", new LanguageCommand(this));
        bind("sprache", new LanguageCommand(this));
        bind("idioma", new LanguageCommand(this));
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
    }

    public dev.zcripted.obx.feature.hologram.gui.HologramEditorMenu getHologramEditorMenu() {
        return serviceRegistry.get(dev.zcripted.obx.feature.hologram.gui.HologramEditorMenu.class);
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
    // OBX logo palette — purple -> magenta gradient (#b794f6 -> #e879f9): the "obsidian"
    // identity used to tint the ASCII wordmark in the enable/disable banner.
    private static final int[] GRADIENT_START = {183, 148, 246};
    private static final int[] GRADIENT_END = {232, 121, 249};

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

    /**
     * Builds the styled enable/disable console banner: the purple→magenta gradient OBX
     * block wordmark (Unicode █ glyphs with a dimmed box-drawing shadow ╔ ═ ║ ╚ ╝ ╗) with
     * an aligned info column (bold near-white name + light-purple version, italic gray
     * tagline, author, environment line with gray labels / white values and {@code ·}
     * separators), then — on enable — soft-dependency &amp; storage status markers
     * (green {@code ✔} found · amber {@code ⚠} optional missing · red {@code ✗} required
     * missing) and the teal Docs/Support rows with light-blue URLs. Dividers are
     * dark-gray {@code ═} rules. The disable banner is the wordmark block only.
     */
    private List<String> buildBannerLines(boolean starting) {
        // console.banner-style gate: "compact" → two-line summary; anything else → full
        // block banner. Read from the live config at PRINT time, so editing config.yml and
        // running /obx reload applies the new style to the next banner (e.g. the disable
        // banner on stop, and the enable banner on the following start).
        String style = getConfig().getString("console.banner-style", "full");
        if (style != null && style.trim().equalsIgnoreCase("compact")) {
            return buildCompactBannerLines(starting);
        }
        String divider = ChatColor.DARK_GRAY + "══════════════════════════════════════════════════";
        List<String> authors = getDescription().getAuthors();
        String developer = authors == null || authors.isEmpty() ? "Unknown" : String.join(", ", authors);
        String version = getDescription().getVersion();

        // 6-line OBX wordmark in Unicode block/box-drawing characters (ANSI-shadow style),
        // space-padded to one width so the info column lines up. █ glyphs carry the full
        // gradient; the box-drawing outline renders as a dimmed shadow of the same hue.
        String[] art = {
                " ██████╗ ██████╗ ██╗  ██╗",
                "██╔═══██╗██╔══██╗╚██╗██╔╝",
                "██║   ██║██████╔╝ ╚███╔╝ ",
                "██║   ██║██╔══██╗ ██╔██╗ ",
                "╚██████╔╝██████╔╝██╔╝ ██╗",
                " ╚═════╝ ╚═════╝ ╚═╝  ╚═╝"
        };

        String name = "\u001B[1m" + ansiRgb(245, 245, 245) + "OBSIDIAN EXTENDED" + ChatColor.RESET
                + "  " + ansiRgb(199, 125, 255) + "v" + version;
        String tagline = "" + ChatColor.GRAY + ChatColor.ITALIC + "Forged from Obsidian";
        String author = ChatColor.GRAY + "by " + ChatColor.WHITE + developer;
        String platform = platformInfo == null ? "Unknown" : platformInfo.getServerName();
        String mcVersion = platformInfo == null ? "?" : platformInfo.getMinecraftVersion();
        String javaVersion = System.getProperty("java.specification.version", "?");
        String state = starting
                ? ChatColor.GRAY + "enabled in " + ChatColor.WHITE + formatLoadDuration(lastLoadDurationMs)
                : "" + ChatColor.RED + "disabling…";
        String environment = ChatColor.GRAY + platform + " " + ChatColor.WHITE + mcVersion
                + ChatColor.DARK_GRAY + " · " + ChatColor.GRAY + "Java " + ChatColor.WHITE + javaVersion
                + ChatColor.DARK_GRAY + " · " + state;

        List<String> lines = new ArrayList<>();
        lines.add(" ");
        lines.add(gradientArt(art[0], ""));
        lines.add(gradientArt(art[1], "   " + name));
        lines.add(gradientArt(art[2], "   " + tagline));
        lines.add(gradientArt(art[3], "   " + author));
        lines.add(gradientArt(art[4], "   " + environment));
        lines.add(gradientArt(art[5], ""));
        lines.add(divider);
        if (starting) {
            // Hook + storage status row(s). The hologram count only renders while the
            // hologram service is registered (it's gone by the time the disable banner prints).
            lines.add(" " + hookLabel("PlaceholderAPI") + "   " + hookLabel("Vault")
                    + "   " + hookLabel("ProtocolLib"));
            StringBuilder storageRow = new StringBuilder(" ").append(storageStatus());
            int holograms = hologramCount();
            if (holograms >= 0) {
                storageRow.append("   ").append("§a✔ §fLoaded §d").append(holograms)
                        .append(" §fhologram").append(holograms == 1 ? "" : "s");
            }
            lines.add(storageRow.toString());
            lines.add(divider);
            String wiki = getLinkOrDefault("links.wiki", "https://github.com/zcripted/OBX");
            String discord = getLinkOrDefault("links.discord", "https://discord.gg/UxktSyT9Ag");
            // OSC 8 hyperlinks: the rows display the clean scheme-less text but CLICK
            // through to the full URL on terminals that support it.
            lines.add(ChatColor.DARK_AQUA + " Docs     " + ChatColor.AQUA + hyperlink(wiki, stripScheme(wiki)));
            lines.add(ChatColor.DARK_AQUA + " Support  " + ChatColor.AQUA + hyperlink(discord, stripScheme(discord)));
        }
        lines.add(" ");
        return lines;
    }

    /**
     * Compact banner ({@code console.banner-style: compact}): a gradient OBX word with the
     * same live values as the full banner — version, platform + MC version, Java version,
     * measured load time — plus one status line with the real hook/storage/hologram checks.
     */
    private List<String> buildCompactBannerLines(boolean starting) {
        String version = getDescription().getVersion();
        String platform = platformInfo == null ? "Unknown" : platformInfo.getServerName();
        String mcVersion = platformInfo == null ? "?" : platformInfo.getMinecraftVersion();
        String javaVersion = System.getProperty("java.specification.version", "?");
        String dot = ChatColor.DARK_GRAY + " · ";
        String state = starting
                ? ChatColor.GRAY + "enabled in " + ChatColor.WHITE + formatLoadDuration(lastLoadDurationMs)
                : "" + ChatColor.RED + "disabling…";
        List<String> lines = new ArrayList<>();
        lines.add(" ");
        lines.add(" " + gradientText("OBX") + " " + ansiRgb(199, 125, 255) + "v" + version + dot
                + ChatColor.GRAY + platform + " " + ChatColor.WHITE + mcVersion + dot
                + ChatColor.GRAY + "Java " + ChatColor.WHITE + javaVersion + dot + state);
        if (starting) {
            StringBuilder status = new StringBuilder(" ")
                    .append(hookLabel("PlaceholderAPI")).append(dot)
                    .append(hookLabel("Vault")).append(dot)
                    .append(hookLabel("ProtocolLib")).append(dot)
                    .append(storageStatus());
            int holograms = hologramCount();
            if (holograms >= 0) {
                status.append(dot).append("§a✔ §f").append(holograms)
                        .append(" hologram").append(holograms == 1 ? "" : "s");
            }
            lines.add(status.toString());
        }
        lines.add(" ");
        return lines;
    }

    /**
     * Three-state soft-dependency marker, reflection-probed live at print time:
     * <ul>
     *   <li>green {@code ✔} — plugin enabled AND its integration point is actually usable
     *       (Vault: an Economy provider is registered with the services manager;
     *       PlaceholderAPI / ProtocolLib: the API class resolves);</li>
     *   <li>amber {@code ⚠ (note)} — plugin installed but the integration is NOT usable
     *       (failed to enable · no economy provider · api unreachable);</li>
     *   <li>amber {@code ⚠ (optional)} — plugin not installed.</li>
     * </ul>
     * plugin.yml declares these as {@code softdepend}, so OBX enables after them — by
     * banner time their enable state and service registrations are final, not racing.
     */
    private String hookLabel(String pluginName) {
        org.bukkit.plugin.Plugin hook = getServer().getPluginManager().getPlugin(pluginName);
        if (hook == null) {
            return "§e⚠ §7" + pluginName + " §8(optional)";
        }
        String problem = hookProblem(pluginName, hook);
        if (problem == null) {
            return "§a✔ §f" + pluginName;
        }
        return "§e⚠ §7" + pluginName + " §8(" + problem + ")";
    }

    /**
     * Reflection-based functional probe for one hook (OBX never compiles against these
     * plugins): returns a short problem note, or {@code null} when the integration is
     * actually usable right now.
     */
    private String hookProblem(String pluginName, org.bukkit.plugin.Plugin hook) {
        if (!hook.isEnabled()) {
            // Loaded but not running — with softdepend in place this means it FAILED to
            // enable, not that it simply hasn't been reached yet.
            return "failed to enable";
        }
        try {
            if ("Vault".equals(pluginName)) {
                // Usable = an Economy provider is registered with the services manager
                // (normally OBX's own VaultEconomyProvider, registered during enable).
                Class<?> economy = Class.forName("net.milkbowl.vault.economy.Economy");
                return getServer().getServicesManager().getRegistration(economy) == null
                        ? "no economy provider" : null;
            }
            if ("PlaceholderAPI".equals(pluginName)) {
                Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                return null;
            }
            if ("ProtocolLib".equals(pluginName)) {
                Class.forName("com.comphenix.protocol.ProtocolLibrary");
                return null;
            }
            // Unknown hook name: enabled is the best signal available.
            return null;
        } catch (Throwable apiUnreachable) {
            return "api unreachable";
        }
    }

    /** SQLite store status — red ✗ when unavailable (the one dependency OBX truly requires). */
    private String storageStatus() {
        boolean available = dataStore != null && dataStore.isAvailable();
        if (available) {
            return "§a✔ §fStorage §dSQLite";
        }
        return "§c✗ §7Storage SQLite §8(unavailable)";
    }

    /** Loaded hologram count, or {@code -1} when the hologram service isn't up. */
    private int hologramCount() {
        try {
            dev.zcripted.obx.feature.hologram.service.HologramService holograms = getHologramService();
            return holograms == null || holograms.getRegistry() == null ? -1 : holograms.getRegistry().size();
        } catch (Throwable unavailable) {
            return -1;
        }
    }

    /** {@code https://github.com/x} → {@code github.com/x} for the banner's link rows. */
    private static String stripScheme(String url) {
        return url == null ? "" : url.replaceFirst("^https?://(www\\.)?", "");
    }

    /**
     * Wraps {@code text} in an OSC 8 terminal hyperlink ({@code ESC ] 8 ; ; url ESC \}) so it
     * is clickable in consoles that support it — Windows Terminal, iTerm2, and most modern
     * emulators / hosting web consoles. Terminals without support consume the escape sequence
     * and render just the text. Only emitted on the direct-stdout path; {@link #stripAnsi}
     * removes the wrapper for the plain-logger fallback.
     */
    private static String hyperlink(String url, String text) {
        char esc = (char) 27;
        String terminator = "" + esc + '\\';
        return esc + "]8;;" + url + terminator + text + esc + "]8;;" + terminator;
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

    /**
     * Tints one wordmark row with the horizontal logo gradient and appends {@code suffix}
     * (the aligned info column) after a color reset. Solid █ glyphs get the full gradient
     * color for their column; every other glyph (the ╔ ═ ║ ╚ ╝ ╗ outline) gets the same
     * hue dimmed to ~55% so the wordmark reads with a shadowed, two-tone depth.
     */
    private String gradientArt(String label, String suffix) {
        StringBuilder builder = new StringBuilder();
        int len = label.length();
        for (int i = 0; i < len; i++) {
            char c = label.charAt(i);
            if (c == ' ') {
                builder.append(' ');
                continue;
            }
            double ratio = len <= 1 ? 0 : (double) i / (len - 1);
            builder.append(ansiForRatio(ratio, c == '█' ? 1.0 : 0.55)).append(c);
        }
        return builder.append(ChatColor.RESET).append(suffix).toString();
    }

    /** Tints plain text with the full-brightness logo gradient (compact banner wordmark). */
    private String gradientText(String label) {
        StringBuilder builder = new StringBuilder();
        int len = label.length();
        for (int i = 0; i < len; i++) {
            double ratio = len <= 1 ? 0 : (double) i / (len - 1);
            builder.append(ansiForRatio(ratio, 1.0)).append(label.charAt(i));
        }
        return builder.append(ChatColor.RESET).toString();
    }

    /** Gradient color at {@code ratio} (0..1 across the wordmark), scaled by {@code brightness}. */
    private String ansiForRatio(double ratio, double brightness) {
        ratio = Math.max(0, Math.min(1, ratio));
        int r = (int) Math.round((GRADIENT_START[0] + (GRADIENT_END[0] - GRADIENT_START[0]) * ratio) * brightness);
        int g = (int) Math.round((GRADIENT_START[1] + (GRADIENT_END[1] - GRADIENT_START[1]) * ratio) * brightness);
        int b = (int) Math.round((GRADIENT_START[2] + (GRADIENT_END[2] - GRADIENT_START[2]) * ratio) * brightness);
        return ansiRgb(r, g, b);
    }

    /** 24-bit (truecolor) ANSI foreground escape. */
    private static String ansiRgb(int r, int g, int b) {
        return (char) 27 + "[38;2;" + r + ";" + g + ";" + b + "m";
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
        return value
                // OSC 8 hyperlink wrappers (ESC ] 8 ; ; url ESC \) — drop, keep the display text.
                .replaceAll("\\x1B\\]8;;[^\\x1B]*\\x1B\\\\", "")
                // CSI color/style sequences (ESC [ ... m).
                .replaceAll("\\x1B\\[[;\\d]*m", "")
                .replace(ChatColor.COLOR_CHAR, '&');
    }
}
