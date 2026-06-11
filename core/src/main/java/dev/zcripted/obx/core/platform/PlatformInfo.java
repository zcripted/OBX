package dev.zcripted.obx.core.platform;

import org.bukkit.Bukkit;

import dev.zcripted.obx.util.ClassUtil;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime detection of the host server: which fork (Paper, Spigot, PurPur, Folia,
 * CraftBukkit) is loaded, which Minecraft version is being served, and which optional
 * APIs are present on the classpath. Detection is reflection-only so the plugin still
 * compiles to a single JAR against the Spigot 1.12 baseline yet boots cleanly on every
 * fork from 1.8.8 through 26.1.x.
 *
 * <p>Detection order:
 * <ol>
 *   <li>Folia is reported even when its class is also a Paper subclass — Folia ships the
 *   {@code RegionizedServer} marker so we look for that first.</li>
 *   <li>PurPur exposes {@code org.purpurmc.purpur.PurpurConfig} (older builds use the
 *   {@code net.pl3x.purpur} package, also probed).</li>
 *   <li>Paper is detected via {@code com.destroystokyo.paper.PaperConfig} or, on 1.19+,
 *   {@code io.papermc.paper.configuration.GlobalConfiguration}.</li>
 *   <li>Spigot is detected via {@code org.spigotmc.SpigotConfig}.</li>
 *   <li>Anything else falls back to vanilla CraftBukkit.</li>
 * </ol>
 *
 * <p>The Minecraft version is parsed from {@code Bukkit.getBukkitVersion()} (e.g.
 * {@code 1.21.11-R0.1-SNAPSHOT} -&gt; major 1, minor 21, patch 11).
 */
public final class PlatformInfo implements Platform {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private static volatile PlatformInfo cached;

    private final ServerType serverType;
    private final String serverName;
    private final String minecraftVersion;
    private final int major;
    private final int minor;
    private final int patch;
    private final boolean foliaScheduler;
    private final boolean adventureApi;
    private final boolean paperPluginLoaderApi;

    private PlatformInfo(ServerType serverType, String serverName, String minecraftVersion,
                         int major, int minor, int patch,
                         boolean foliaScheduler, boolean adventureApi, boolean paperPluginLoaderApi) {
        this.serverType = serverType;
        this.serverName = serverName;
        this.minecraftVersion = minecraftVersion;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.foliaScheduler = foliaScheduler;
        this.adventureApi = adventureApi;
        this.paperPluginLoaderApi = paperPluginLoaderApi;
    }

    public static PlatformInfo get() {
        PlatformInfo local = cached;
        if (local != null) {
            return local;
        }
        synchronized (PlatformInfo.class) {
            if (cached == null) {
                cached = detect();
            }
            return cached;
        }
    }

    public ServerType getServerType() {
        return serverType;
    }

    public String getServerName() {
        return serverName;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public boolean isAtLeast(int requestedMajor, int requestedMinor) {
        return isAtLeast(requestedMajor, requestedMinor, 0);
    }

    public boolean isAtLeast(int requestedMajor, int requestedMinor, int requestedPatch) {
        if (major != requestedMajor) {
            return major > requestedMajor;
        }
        if (minor != requestedMinor) {
            return minor > requestedMinor;
        }
        return patch >= requestedPatch;
    }

    public boolean isFolia() {
        return serverType == ServerType.FOLIA;
    }

    public boolean isPaper() {
        return serverType == ServerType.PAPER || serverType == ServerType.PURPUR || serverType == ServerType.FOLIA;
    }

    public boolean isPurpur() {
        return serverType == ServerType.PURPUR;
    }

    public boolean isSpigot() {
        return serverType == ServerType.SPIGOT || isPaper();
    }

    public boolean hasFoliaScheduler() {
        return foliaScheduler;
    }

    public boolean hasAdventureApi() {
        return adventureApi;
    }

    public boolean hasPaperPluginLoaderApi() {
        return paperPluginLoaderApi;
    }

    public String summary() {
        StringBuilder builder = new StringBuilder();
        builder.append(serverName).append(' ');
        builder.append(minecraftVersion);
        builder.append(" (api ").append(major).append('.').append(minor);
        if (patch > 0) {
            builder.append('.').append(patch);
        }
        builder.append(')');
        if (foliaScheduler) {
            builder.append(" [folia-scheduler]");
        }
        if (adventureApi) {
            builder.append(" [adventure]");
        }
        return builder.toString();
    }

    private static PlatformInfo detect() {
        ServerType type = detectServerType();
        String mcVersion = detectMcVersion();
        int[] parts = parseVersion(mcVersion);
        boolean foliaScheduler = hasClass("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
        boolean adventure = hasClass("net.kyori.adventure.text.minimessage.MiniMessage")
                && hasClass("net.kyori.adventure.text.Component")
                && hasClass("net.kyori.adventure.audience.Audience");
        boolean paperPluginLoader = hasClass("io.papermc.paper.plugin.loader.PluginLoader");
        String serverName;
        try {
            serverName = Bukkit.getServer() != null ? Bukkit.getServer().getName() : type.label();
        } catch (Throwable ignored) {
            serverName = type.label();
        }
        return new PlatformInfo(type, serverName, mcVersion, parts[0], parts[1], parts[2],
                foliaScheduler, adventure, paperPluginLoader);
    }

    private static ServerType detectServerType() {
        if (hasClass("io.papermc.paper.threadedregions.RegionizedServer")
                || hasClass("io.papermc.paper.threadedregions.scheduler.RegionScheduler")) {
            return ServerType.FOLIA;
        }
        if (hasClass("org.purpurmc.purpur.PurpurConfig")
                || hasClass("net.pl3x.purpur.PurpurConfig")
                || hasClass("net.pl3x.purpur.PurpurWorldConfig")) {
            return ServerType.PURPUR;
        }
        if (hasClass("io.papermc.paper.configuration.GlobalConfiguration")
                || hasClass("com.destroystokyo.paper.PaperConfig")
                || hasClass("com.destroystokyo.paper.VersionHistoryManager")) {
            return ServerType.PAPER;
        }
        if (hasClass("org.spigotmc.SpigotConfig")) {
            return ServerType.SPIGOT;
        }
        return ServerType.CRAFTBUKKIT;
    }

    private static String detectMcVersion() {
        try {
            String raw = Bukkit.getBukkitVersion();
            if (raw != null && !raw.isEmpty()) {
                return raw.toLowerCase(Locale.ENGLISH);
            }
        } catch (Throwable ignored) {
        }
        try {
            String raw = Bukkit.getVersion();
            if (raw != null && !raw.isEmpty()) {
                return raw.toLowerCase(Locale.ENGLISH);
            }
        } catch (Throwable ignored) {
        }
        return "unknown";
    }

    private static int[] parseVersion(String version) {
        int[] parts = new int[]{1, 8, 0};
        if (version == null) {
            return parts;
        }
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            try {
                parts[0] = Integer.parseInt(matcher.group(1));
                parts[1] = Integer.parseInt(matcher.group(2));
                String patch = matcher.group(3);
                parts[2] = patch == null ? 0 : Integer.parseInt(patch);
            } catch (NumberFormatException ignored) {
                // keep defaults on parse failure
            }
        }
        return parts;
    }

    private static boolean hasClass(String name) {
        return ClassUtil.hasClass(name);
    }

    public enum ServerType {
        CRAFTBUKKIT("CraftBukkit"),
        SPIGOT("Spigot"),
        PAPER("Paper"),
        PURPUR("PurPur"),
        FOLIA("Folia");

        private final String label;

        ServerType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}