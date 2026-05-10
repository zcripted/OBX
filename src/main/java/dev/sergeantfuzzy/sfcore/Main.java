package dev.sergeantfuzzy.sfcore;

import dev.sergeantfuzzy.sfcore.command.admin.KillCommand;
import dev.sergeantfuzzy.sfcore.command.admin.PluginListCommand;
import dev.sergeantfuzzy.sfcore.command.admin.TpsCommand;
import dev.sergeantfuzzy.sfcore.command.core.HelpGuiCommand;
import dev.sergeantfuzzy.sfcore.command.core.SFCoreCommand;
import dev.sergeantfuzzy.sfcore.command.moderation.BanListCommand;
import dev.sergeantfuzzy.sfcore.command.moderation.ModerationCommand;
import dev.sergeantfuzzy.sfcore.command.moderation.ModerationStatusCommand;
import dev.sergeantfuzzy.sfcore.command.teleportation.BackCommand;
import dev.sergeantfuzzy.sfcore.command.teleportation.DelHomeCommand;
import dev.sergeantfuzzy.sfcore.command.teleportation.HomeCommand;
import dev.sergeantfuzzy.sfcore.command.teleportation.HomesCommand;
import dev.sergeantfuzzy.sfcore.command.teleportation.SetHomeCommand;
import dev.sergeantfuzzy.sfcore.command.teleportation.SpawnCommand;
import dev.sergeantfuzzy.sfcore.command.teleportation.TopCommand;
import dev.sergeantfuzzy.sfcore.command.teleportation.WarpCommand;
import dev.sergeantfuzzy.sfcore.command.language.LanguageCommand;
import dev.sergeantfuzzy.sfcore.command.utility.AnvilCommand;
import dev.sergeantfuzzy.sfcore.command.utility.CraftCommand;
import dev.sergeantfuzzy.sfcore.command.utility.EnchantCommand;
import dev.sergeantfuzzy.sfcore.command.utility.FeedCommand;
import dev.sergeantfuzzy.sfcore.command.utility.GamemodeCommand;
import dev.sergeantfuzzy.sfcore.command.utility.GodCommand;
import dev.sergeantfuzzy.sfcore.command.utility.HealCommand;
import dev.sergeantfuzzy.sfcore.command.utility.ResearchCommand;
import dev.sergeantfuzzy.sfcore.command.utility.SmithCommand;
import dev.sergeantfuzzy.sfcore.command.utility.VitalCommand;
import dev.sergeantfuzzy.sfcore.command.admin.InvSeeCommand;
import dev.sergeantfuzzy.sfcore.command.admin.VanishCommand;
import dev.sergeantfuzzy.sfcore.chat.listener.ChatManagementListener;
import dev.sergeantfuzzy.sfcore.chat.service.ChatService;
import dev.sergeantfuzzy.sfcore.tablist.listener.TablistJoinListener;
import dev.sergeantfuzzy.sfcore.tablist.scheduler.TablistRefreshTask;
import dev.sergeantfuzzy.sfcore.tablist.service.TablistService;
import dev.sergeantfuzzy.sfcore.listener.chat.WarpMenuInputListener;
import dev.sergeantfuzzy.sfcore.listener.teleport.BackListener;
import dev.sergeantfuzzy.sfcore.listener.player.CommandOverrideListener;
import dev.sergeantfuzzy.sfcore.listener.player.JoinLockListener;
import dev.sergeantfuzzy.sfcore.listener.player.JoinListener;
import dev.sergeantfuzzy.sfcore.listener.player.JoinLeaveListener;
import dev.sergeantfuzzy.sfcore.listener.menu.HelpGuiListener;
import dev.sergeantfuzzy.sfcore.listener.menu.MainMenuListener;
import dev.sergeantfuzzy.sfcore.listener.menu.WarpMenuListener;
import dev.sergeantfuzzy.sfcore.listener.server.MotdPingListener;
import dev.sergeantfuzzy.sfcore.listener.world.RedstoneControlListener;
import dev.sergeantfuzzy.sfcore.platform.PlatformInfo;
import dev.sergeantfuzzy.sfcore.platform.bukkit.resourcepack.AutoResourcePackManager;
import dev.sergeantfuzzy.sfcore.platform.bukkit.resourcepack.ResourcePackListener;
import dev.sergeantfuzzy.sfcore.platform.scheduler.SchedulerAdapter;
import dev.sergeantfuzzy.sfcore.storage.DataService;
import dev.sergeantfuzzy.sfcore.storage.JoinLeaveService;
import dev.sergeantfuzzy.sfcore.storage.MotdService;
import dev.sergeantfuzzy.sfcore.storage.WarpService;
import dev.sergeantfuzzy.sfcore.gui.player.WarpMenuInputManager;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.moderation.ModerationService;
import dev.sergeantfuzzy.sfcore.util.control.GodModeManager;
import dev.sergeantfuzzy.sfcore.util.control.KillModeManager;
import dev.sergeantfuzzy.sfcore.util.control.VanishManager;
import dev.sergeantfuzzy.sfcore.util.perf.TpsService;
import dev.sergeantfuzzy.sfcore.util.teleport.TeleportManager;
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

public class Main extends JavaPlugin {

    private DataService dataService;
    private WarpService warpService;
    private LanguageManager languageManager;
    private TeleportManager teleportManager;
    private GodModeManager godModeManager;
    private KillModeManager killModeManager;
    private VanishManager vanishManager;
    private ModerationService moderationService;
    private MotdService motdService;
    private JoinLeaveService joinLeaveService;
    private ChatService chatService;
    private TablistService tablistService;
    private TablistRefreshTask tablistRefreshTask;
    private AutoResourcePackManager resourcePackManager;
    private WarpMenuInputManager warpMenuInputManager;
    private dev.sergeantfuzzy.sfcore.gui.admin.StaffMenuInputManager staffMenuInputManager;
    private dev.sergeantfuzzy.sfcore.gui.admin.InvSeeMenuManager invSeeMenuManager;
    private dev.sergeantfuzzy.sfcore.util.control.StaffSessionTracker staffSessionTracker;
    private TpsService tpsService;
    private SchedulerAdapter scheduler;
    private PlatformInfo platformInfo;
    private String releaseDate = "Unknown";
    private long lastLoadDurationMs;

    @Override
    public void onEnable() {
        long enableStart = System.nanoTime();
        saveDefaultConfig();
        loadBuildMetadata();

        platformInfo = PlatformInfo.get();
        scheduler = new SchedulerAdapter(this);
        getLogger().info("[SF-Core] Detected platform: " + platformInfo.summary());

        languageManager = new LanguageManager(this);
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
        warpMenuInputManager = new WarpMenuInputManager(this);
        staffMenuInputManager = new dev.sergeantfuzzy.sfcore.gui.admin.StaffMenuInputManager(this);
        invSeeMenuManager = new dev.sergeantfuzzy.sfcore.gui.admin.InvSeeMenuManager(this);
        staffSessionTracker = new dev.sergeantfuzzy.sfcore.util.control.StaffSessionTracker();
        teleportManager = new TeleportManager(this, languageManager);
        godModeManager = new GodModeManager();
        killModeManager = new KillModeManager(this);
        vanishManager = new VanishManager(this);
        resourcePackManager = new AutoResourcePackManager(this);
        resourcePackManager.installBundledPack();
        resourcePackManager.prepareHosting();
        tpsService = new TpsService(this);

        registerCommands();
        registerListeners();

        if (tablistRefreshTask != null) {
            tablistRefreshTask.start();
        }
        tpsService.start();
        if (vanishManager != null) {
            vanishManager.start();
        }
        if (invSeeMenuManager != null) {
            invSeeMenuManager.start();
        }

        lastLoadDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - enableStart);
        printBanner(true);
    }

    @Override
    public void onDisable() {
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
        if (teleportManager != null) {
            teleportManager.cancelAll();
        }
        if (dataService != null) {
            dataService.save();
        }
        if (moderationService != null) {
            moderationService.save();
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

    public GodModeManager getGodModeManager() {
        return godModeManager;
    }

    public KillModeManager getKillModeManager() {
        return killModeManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public dev.sergeantfuzzy.sfcore.gui.admin.InvSeeMenuManager getInvSeeMenuManager() {
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

    public AutoResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public WarpMenuInputManager getWarpMenuInputManager() {
        return warpMenuInputManager;
    }

    public dev.sergeantfuzzy.sfcore.gui.admin.StaffMenuInputManager getStaffMenuInputManager() {
        return staffMenuInputManager;
    }

    public dev.sergeantfuzzy.sfcore.util.control.StaffSessionTracker getStaffSessionTracker() {
        return staffSessionTracker;
    }

    public TpsService getTpsService() {
        return tpsService;
    }

    public SchedulerAdapter getSchedulerAdapter() {
        return scheduler;
    }

    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    public void reloadPlugin() {
        reloadConfig();
        languageManager.reload();
        dataService.reload();
        warpService.load();
        if (moderationService != null) {
            moderationService.reload();
        }
        if (motdService != null) {
            motdService.reload();
        }
        if (joinLeaveService != null) {
            joinLeaveService.reload();
        }
        if (chatService != null) {
            chatService.reload();
        }
        if (tablistService != null) {
            tablistService.reload();
        }
        if (tablistRefreshTask != null) {
            tablistRefreshTask.start();
        }
        if (resourcePackManager != null) {
            resourcePackManager.refreshConfig();
            resourcePackManager.installBundledPack();
            resourcePackManager.prepareHosting();
        }
    }

    private void registerCommands() {
        SpawnCommand spawnCommand = new SpawnCommand(this);
        WarpCommand warpCommand = new WarpCommand(this);
        bind("sf", new SFCoreCommand(this, languageManager));
        bind("help", new HelpGuiCommand(this));
        bind("home", new HomeCommand(this));
        bind("sethome", new SetHomeCommand(this));
        bind("delhome", new DelHomeCommand(this));
        bind("homes", new HomesCommand(this));
        bind("spawn", spawnCommand);
        bind("setspawn", spawnCommand);
        bind("warp", warpCommand);
        bind("gamemode", new GamemodeCommand(this));
        bind("back", new BackCommand(this));
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
        bind("language", new LanguageCommand(this));
        bind("sprache", new LanguageCommand(this));
        bind("ban", new ModerationCommand(this, ModerationCommand.Action.BAN));
        bind("unban", new ModerationCommand(this, ModerationCommand.Action.UNBAN));
        bind("kick", new ModerationCommand(this, ModerationCommand.Action.KICK));
        bind("mute", new ModerationCommand(this, ModerationCommand.Action.MUTE));
        bind("unmute", new ModerationCommand(this, ModerationCommand.Action.UNMUTE));
        bind("tempban", new ModerationCommand(this, ModerationCommand.Action.TEMPBAN));
        bind("warn", new ModerationCommand(this, ModerationCommand.Action.WARN));
        bind("banlist", new BanListCommand(this));
        bind("status", new ModerationStatusCommand(this));
        bind("staff", new dev.sergeantfuzzy.sfcore.command.admin.StaffCommand(this));
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
        getServer().getPluginManager().registerEvents(new dev.sergeantfuzzy.sfcore.listener.menu.StaffMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.sergeantfuzzy.sfcore.listener.menu.InvSeeMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new dev.sergeantfuzzy.sfcore.listener.chat.StaffMenuInputListener(staffMenuInputManager), this);
        getServer().getPluginManager().registerEvents(new ChatManagementListener(this, chatService), this);
        getServer().getPluginManager().registerEvents(new TablistJoinListener(this, tablistService), this);
        getServer().getPluginManager().registerEvents(new MotdPingListener(this), this);
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
    private static final int[] GRADIENT_START = {255, 170, 0};
    private static final int[] GRADIENT_END = {255, 255, 85};

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
        String divider = ChatColor.GOLD.toString() + ChatColor.STRIKETHROUGH + "----------------------------------------------------";
        String pluginName = getDescription().getName();
        List<String> authors = getDescription().getAuthors();
        String developer = authors == null || authors.isEmpty() ? "Unknown" : String.join(", ", authors);
        String version = getDescription().getVersion();
        String release = releaseDate != null ? releaseDate : "Unknown";
        String builtByBit = getLinkOrDefault("links.builtbybit", "https://builtbybit.com/resources/sf-core/");
        String wiki = getLinkOrDefault("links.wiki", "https://github.com/SergeantFuzzy/SF-Core/wiki");
        String discord = getLinkOrDefault("links.discord", "https://discord.gg/sergeantfuzzy");
        String loadTime = formatLoadDuration(lastLoadDurationMs);

        List<String> lines = new ArrayList<>();
        lines.add(" ");
        lines.add(divider);
        lines.add(gradient(" SF-Core", starting ? ChatColor.GREEN + ": Enabled" : ChatColor.RED + ": Disabling..."));
        lines.add(gradient(" Developer:", " " + ChatColor.YELLOW + developer));
        lines.add(gradient(" Version:", " " + ChatColor.YELLOW + version + ChatColor.GOLD + " (Released " + release + ")"));
        lines.add(gradient(" Load Time:", " " + ChatColor.YELLOW + loadTime));
        lines.add(gradient(" BuiltByBit:", " " + ChatColor.AQUA + builtByBit));
        lines.add(gradient(" Wiki:", " " + ChatColor.AQUA + wiki));
        lines.add(gradient(" Discord:", " " + ChatColor.AQUA + discord));
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



