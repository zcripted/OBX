package dev.zcripted.obx.command.admin;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.language.LanguageManager;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Replaces the vanilla {@code /pl} and {@code /plugins} commands with a styled, grouped
 * report. Plugins are bucketed by detected platform (Bukkit, Paper, Purpur, Folia) so an
 * admin can tell at a glance which loader each plugin is using. Status colors:
 * <ul>
 *   <li>Light orange (&amp;6) - enabled / working</li>
 *   <li>Light red (&amp;c) - broken (jar present in plugins folder but not loaded)</li>
 *   <li>Light gray (&amp;7) - disabled (loaded but currently disabled)</li>
 * </ul>
 *
 * <p>The console output uses the same legacy color codes; OBX's existing console
 * writer translates them to truecolor ANSI so terminals display the in-game palette.
 */
public final class PluginListCommand extends AbstractObxCommand {

    private static final String PLATFORM_BUKKIT = "Bukkit";
    private static final String PLATFORM_PAPER = "Paper";
    private static final String PLATFORM_PURPUR = "Purpur";
    private static final String PLATFORM_FOLIA = "Folia";

    private static final List<String> PLATFORM_ORDER = Collections.unmodifiableList(Arrays.asList(
            PLATFORM_BUKKIT, PLATFORM_PAPER, PLATFORM_PURPUR, PLATFORM_FOLIA
    ));


    public PluginListCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager languages = plugin.getLanguageManager();
        if (!sender.hasPermission("obx.pl")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        Map<String, List<PluginEntry>> grouped = collectGroupedPlugins();
        int totalLoaded = 0;
        int enabledCount = 0;
        int disabledCount = 0;
        int brokenCount = 0;
        for (List<PluginEntry> entries : grouped.values()) {
            for (PluginEntry entry : entries) {
                totalLoaded++;
                switch (entry.status) {
                    case ENABLED:
                        enabledCount++;
                        break;
                    case DISABLED:
                        disabledCount++;
                        break;
                    case BROKEN:
                        brokenCount++;
                        break;
                    default:
                        break;
                }
            }
        }

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("total", String.valueOf(totalLoaded));
        placeholders.put("enabled", String.valueOf(enabledCount));
        placeholders.put("disabled", String.valueOf(disabledCount));
        placeholders.put("broken", String.valueOf(brokenCount));

        for (String headerLine : languages.list(sender, "commands.pl.header", placeholders)) {
            sendPlain(sender, headerLine);
        }
        boolean anySection = false;
        for (String platform : PLATFORM_ORDER) {
            List<PluginEntry> entries = grouped.get(platform);
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            anySection = true;
            Map<String, String> sectionPlaceholders = new LinkedHashMap<>(placeholders);
            sectionPlaceholders.put("platform", platform);
            sectionPlaceholders.put("count", String.valueOf(entries.size()));
            sendPlain(sender, languages.get(sender, "commands.pl.section", sectionPlaceholders));
            sendPluginLine(languages, sender, entries);
        }
        if (!anySection) {
            sendPlain(sender, languages.get(sender, "commands.pl.empty", placeholders));
        }
        for (String footerLine : languages.list(sender, "commands.pl.footer", placeholders)) {
            sendPlain(sender, footerLine);
        }
        return true;
    }

    /** Sends an already-colorized line straight to the console writer or the recipient. */
    private void sendPlain(CommandSender sender, String line) {
        if (sender instanceof ConsoleCommandSender) {
            plugin.writeConsoleLine(line);
        } else {
            sender.sendMessage(line);
        }
    }

    /**
     * Sends one platform's plugin list as a single chat line where each plugin name
     * is its own component carrying a hover tooltip (name, version, author, software,
     * status). Console senders fall back to the joined colored names.
     */
    private void sendPluginLine(LanguageManager languages, CommandSender sender, List<PluginEntry> entries) {
        List<PluginEntry> sorted = new ArrayList<>(entries);
        Collections.sort(sorted, (a, b) -> a.name.compareToIgnoreCase(b.name));
        List<InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(InteractiveMessagePart.plain("    ")); // matches the commands.pl.line indent
        String separator = ChatColor.translateAlternateColorCodes('&', "&8, ");
        for (int i = 0; i < sorted.size(); i++) {
            PluginEntry entry = sorted.get(i);
            if (i > 0) {
                parts.add(InteractiveMessagePart.plain(separator));
            }
            String name = ChatColor.translateAlternateColorCodes('&', statusColor(entry.status) + entry.name);
            parts.add(InteractiveMessagePart.interactive(name, buildPluginHover(languages, sender, entry), null, false));
        }
        ComponentMessenger.sendJoinedHoverMessages(sender, parts);
    }

    private List<String> buildPluginHover(LanguageManager languages, CommandSender sender, PluginEntry entry) {
        List<String> hover = new ArrayList<>();
        hover.add(languages.get(sender, "commands.pl.hover.title", Placeholders.with("name", entry.name)));
        hover.add(languages.get(sender, "core.divider-line"));
        hover.add(languages.get(sender, "commands.pl.hover.version",
                Placeholders.with("version", orUnknown(languages, sender, entry.version))));
        hover.add(languages.get(sender, "commands.pl.hover.author",
                Placeholders.with("author", orUnknown(languages, sender, entry.authors))));
        hover.add(languages.get(sender, "commands.pl.hover.software",
                Placeholders.with("platform", entry.platform)));
        if (entry.apiVersion != null && !entry.apiVersion.isEmpty()) {
            hover.add(languages.get(sender, "commands.pl.hover.api", Placeholders.with("api", entry.apiVersion)));
        }
        hover.add(languages.get(sender, "commands.pl.hover.status",
                Placeholders.with("status", languages.get(sender, "commands.pl.status." + entry.status.name().toLowerCase(Locale.ENGLISH)))));
        return hover;
    }

    private static String orUnknown(LanguageManager languages, CommandSender sender, String value) {
        return value == null || value.isEmpty() ? languages.get(sender, "commands.pl.hover.unknown") : value;
    }

    private static String statusColor(Status status) {
        switch (status) {
            case ENABLED:
                return "&5";
            case BROKEN:
                return "&c";
            case DISABLED:
            default:
                return "&7";
        }
    }

    private Map<String, List<PluginEntry>> collectGroupedPlugins() {
        Map<String, List<PluginEntry>> grouped = new LinkedHashMap<>();
        for (String platform : PLATFORM_ORDER) {
            grouped.put(platform, new ArrayList<>());
        }

        Set<String> loadedNamesLowercase = new HashSet<>();
        Plugin[] loaded = Bukkit.getPluginManager().getPlugins();
        Map<File, Plugin> jarToPlugin = new LinkedHashMap<>();
        for (Plugin loadedPlugin : loaded) {
            if (loadedPlugin == null || loadedPlugin.getName() == null) {
                continue;
            }
            loadedNamesLowercase.add(loadedPlugin.getName().toLowerCase(Locale.ENGLISH));
            File jar = getPluginFile(loadedPlugin);
            if (jar != null) {
                jarToPlugin.put(jar, loadedPlugin);
            }

            PluginEntry entry = new PluginEntry();
            entry.name = loadedPlugin.getName();
            entry.status = loadedPlugin.isEnabled() ? Status.ENABLED : Status.DISABLED;
            entry.platform = detectPlatform(loadedPlugin, jar);
            PluginDescriptionFile description = loadedPlugin.getDescription();
            if (description != null) {
                entry.version = description.getVersion();
                List<String> authors = description.getAuthors();
                entry.authors = (authors == null || authors.isEmpty()) ? null : String.join(", ", authors);
                entry.apiVersion = readApiVersion(description);
            }
            grouped.computeIfAbsent(entry.platform, key -> new ArrayList<>()).add(entry);
        }

        for (PluginEntry broken : findBrokenPlugins(loadedNamesLowercase)) {
            grouped.computeIfAbsent(broken.platform, key -> new ArrayList<>()).add(broken);
        }

        return grouped;
    }

    private List<PluginEntry> findBrokenPlugins(Set<String> loadedNamesLowercase) {
        List<PluginEntry> broken = new ArrayList<>();
        File pluginsDir = plugin.getDataFolder().getParentFile();
        if (pluginsDir == null || !pluginsDir.isDirectory()) {
            return broken;
        }
        File[] jars = pluginsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name != null && name.toLowerCase(Locale.ENGLISH).endsWith(".jar");
            }
        });
        if (jars == null) {
            return broken;
        }
        for (File jar : jars) {
            JarMetadata metadata = readJarMetadata(jar);
            if (metadata == null || metadata.name == null) {
                continue;
            }
            if (loadedNamesLowercase.contains(metadata.name.toLowerCase(Locale.ENGLISH))) {
                continue;
            }
            PluginEntry entry = new PluginEntry();
            entry.name = metadata.name;
            entry.status = Status.BROKEN;
            entry.platform = metadata.platform != null ? metadata.platform : PLATFORM_BUKKIT;
            broken.add(entry);
        }
        return broken;
    }

    private String detectPlatform(Plugin loadedPlugin, File jarFile) {
        if (jarFile != null) {
            JarMetadata metadata = readJarMetadata(jarFile);
            if (metadata != null && metadata.platform != null) {
                return metadata.platform;
            }
        }
        PluginDescriptionFile description = loadedPlugin.getDescription();
        if (description != null) {
            if (mentionsPurpur(description.getDepend()) || mentionsPurpur(description.getSoftDepend())) {
                return PLATFORM_PURPUR;
            }
        }
        return PLATFORM_BUKKIT;
    }

    private static boolean mentionsPurpur(Collection<String> values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ENGLISH).contains("purpur")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines the plugin's <em>primary</em> target platform from the descriptors the
     * author shipped inside the JAR. The decision is intentionally based on developer
     * intent (which loader format the plugin declares) rather than bytecode scanning,
     * because a plugin that includes one optional Folia-adapter class still has Bukkit
     * as its primary target — having "any reference" to a platform's API is not the
     * same as targeting it primarily.
     *
     * <p>Decision order, most specific first:
     * <ol>
     *   <li><strong>Folia</strong> — {@code paper-plugin.yml} present with
     *   {@code folia-supported: true} AND no fallback {@code plugin.yml}. This means the
     *   plugin can only load on the Paper plugin loader and explicitly opts into
     *   Folia's threading model, so it's effectively Folia-only.</li>
     *   <li><strong>PurPur</strong> — the descriptor declares Purpur as a hard or soft
     *   dependency.</li>
     *   <li><strong>Paper</strong> — {@code paper-plugin.yml} present (the new Paper
     *   plugin loader format that doesn't exist on Spigot or CraftBukkit).</li>
     *   <li><strong>Bukkit</strong> — anything else, including big plugins like
     *   WorldGuard / WorldEdit that ship a regular {@code plugin.yml} and merely
     *   include optional Paper / Folia adapter classes.</li>
     * </ol>
     *
     * <p>Note: {@code folia-supported: true} alone is intentionally <em>not</em> enough
     * to promote a plugin to the Folia section. Folia rejects every loaded plugin
     * without that flag, so on a Folia server <em>every</em> running plugin carries
     * it regardless of whether Folia is the plugin's actual primary target.
     */
    private JarMetadata readJarMetadata(File jar) {
        if (jar == null || !jar.isFile()) {
            return null;
        }
        JarMetadata metadata = new JarMetadata();
        boolean hasPluginYml = false;
        boolean hasPaperPluginYml = false;
        boolean paperLoaderFoliaOptIn = false;
        boolean purpurDependency = false;
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry paperEntry = jarFile.getJarEntry("paper-plugin.yml");
            if (paperEntry != null) {
                hasPaperPluginYml = true;
                ParsedYml parsed = parseSimpleYml(jarFile, paperEntry);
                if (parsed != null) {
                    if (metadata.name == null && parsed.name != null) {
                        metadata.name = parsed.name;
                    }
                    if (parsed.dependsOnPurpur) {
                        purpurDependency = true;
                    }
                    if (parsed.foliaSupported) {
                        paperLoaderFoliaOptIn = true;
                    }
                }
            }
            JarEntry pluginEntry = jarFile.getJarEntry("plugin.yml");
            if (pluginEntry != null) {
                hasPluginYml = true;
                ParsedYml parsed = parseSimpleYml(jarFile, pluginEntry);
                if (parsed != null) {
                    if (metadata.name == null && parsed.name != null) {
                        metadata.name = parsed.name;
                    }
                    if (parsed.dependsOnPurpur) {
                        purpurDependency = true;
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        if (hasPaperPluginYml && paperLoaderFoliaOptIn && !hasPluginYml) {
            metadata.platform = PLATFORM_FOLIA;
        } else if (purpurDependency) {
            metadata.platform = PLATFORM_PURPUR;
        } else if (hasPaperPluginYml) {
            metadata.platform = PLATFORM_PAPER;
        } else {
            metadata.platform = PLATFORM_BUKKIT;
        }
        return metadata;
    }

    private ParsedYml parseSimpleYml(JarFile jarFile, JarEntry entry) {
        try (InputStream in = jarFile.getInputStream(entry);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            ParsedYml parsed = new ParsedYml();
            String line;
            String section = null;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int leadingSpaces = countLeadingSpaces(line);
                if (leadingSpaces == 0) {
                    section = null;
                    if (trimmed.startsWith("name:")) {
                        parsed.name = stripValue(trimmed.substring(5));
                    } else if (trimmed.startsWith("folia-supported:")) {
                        parsed.foliaSupported = parseBoolean(stripValue(trimmed.substring("folia-supported:".length())));
                    } else if (trimmed.startsWith("depend:") || trimmed.startsWith("softdepend:") || trimmed.startsWith("loadbefore:")) {
                        section = trimmed.substring(0, trimmed.indexOf(':'));
                        String inline = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                        if (inline.startsWith("[") && inline.endsWith("]")) {
                            String inner = inline.substring(1, inline.length() - 1);
                            for (String token : inner.split(",")) {
                                if (containsPurpur(token)) {
                                    parsed.dependsOnPurpur = true;
                                }
                            }
                            section = null;
                        } else if (!inline.isEmpty() && !inline.equals("|") && !inline.equals(">")) {
                            if (containsPurpur(inline)) {
                                parsed.dependsOnPurpur = true;
                            }
                            section = null;
                        }
                    }
                } else if (section != null && trimmed.startsWith("- ")) {
                    String value = stripValue(trimmed.substring(2));
                    if (containsPurpur(value)) {
                        parsed.dependsOnPurpur = true;
                    }
                }
            }
            return parsed;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int countLeadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static boolean containsPurpur(String value) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains("purpur");
    }

    private static String stripValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }

    private static boolean parseBoolean(String raw) {
        if (raw == null) {
            return false;
        }
        return raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("yes") || raw.equals("1");
    }

    /**
     * Best-effort lookup for the JAR backing a loaded plugin. Uses reflection on
     * {@link JavaPlugin#getFile()} since it is package-private. Returns {@code null}
     * if the plugin was loaded by a non-JavaPlugin loader.
     */
    private File getPluginFile(Plugin loadedPlugin) {
        if (!(loadedPlugin instanceof JavaPlugin)) {
            return null;
        }
        try {
            Method method = JavaPlugin.class.getDeclaredMethod("getFile");
            method.setAccessible(true);
            Object value = method.invoke(loadedPlugin);
            return value instanceof File ? (File) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Reads {@code api-version} from the descriptor reflectively — the
     * {@code PluginDescriptionFile#getAPIVersion()} method only exists on 1.13+, so
     * a direct call would break the 1.12 target. Returns {@code null} when absent.
     */
    private static String readApiVersion(PluginDescriptionFile description) {
        try {
            Method method = PluginDescriptionFile.class.getMethod("getAPIVersion");
            Object value = method.invoke(description);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private enum Status {
        ENABLED, DISABLED, BROKEN
    }

    private static final class PluginEntry {
        String name;
        Status status;
        String platform;
        String version;
        String authors;
        String apiVersion;
    }

    private static final class JarMetadata {
        String name;
        String platform;
    }

    private static final class ParsedYml {
        String name;
        boolean foliaSupported;
        boolean dependsOnPurpur;
    }
}
