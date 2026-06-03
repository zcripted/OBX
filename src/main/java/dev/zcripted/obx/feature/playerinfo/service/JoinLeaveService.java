package dev.zcripted.obx.feature.playerinfo.service;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageRegistry;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * Centralised access to the join/leave broadcast and welcome MOTD configuration.
 *
 * <p>Two independent toggles live under {@code join-leave.*} and {@code join-motd.*} in
 * {@code config.yml}. Each can be flipped at runtime through the admin command without
 * a full plugin reload.
 *
 * <p>The toggle <em>booleans</em> stay in {@code config.yml}; the actual message
 * <em>text</em> (join/leave broadcasts and the welcome-MOTD line lists) lives in the
 * language system ({@code languages/language_en.yml} → {@code welcome.*}), so all
 * player-facing strings sit in one place. The text is read raw (no &amp;-code
 * colorizing) so its Adventure / MiniMessage markup survives to
 * {@link dev.zcripted.obx.util.message.AdventureMessageUtil}.
 *
 * <p>This service caches every value it exposes so listeners on the player-join hot
 * path read pre-resolved volatile fields instead of re-walking config/lang every
 * connection. The cached snapshot is rebuilt on {@link #reload()} and on each toggle.
 */
public final class JoinLeaveService {

    private final ObxPlugin plugin;

    private volatile boolean joinLeaveEnabled;
    private volatile boolean suppressVanilla;
    private volatile boolean firstJoinMessageEnabled;
    private volatile boolean joinMotdEnabled;
    private volatile boolean firstJoinMotdEnabled;

    private volatile String joinMessage = "";
    private volatile String leaveMessage = "";
    private volatile String firstJoinMessage = "";

    private volatile List<String> joinMotdLines = Collections.emptyList();
    private volatile List<String> firstJoinMotdLines = Collections.emptyList();

    public JoinLeaveService(ObxPlugin plugin) {
        this.plugin = plugin;
        cacheSnapshot();
    }

    public void reload() {
        plugin.reloadConfig();
        cacheSnapshot();
    }

    private void cacheSnapshot() {
        FileConfiguration config = plugin.getConfig();
        // Toggles stay in config.yml.
        joinLeaveEnabled = config.getBoolean("join-leave.enabled", true);
        suppressVanilla = config.getBoolean("join-leave.suppress-vanilla", true);
        firstJoinMessageEnabled = config.getBoolean("join-leave.first-join.enabled", true);
        joinMotdEnabled = config.getBoolean("join-motd.enabled", true);
        firstJoinMotdEnabled = config.getBoolean("join-motd.first-join.enabled", true);

        // Message TEXT comes from the language system (welcome.* keys), read raw so
        // its MiniMessage markup reaches AdventureMessageUtil intact. Server-wide
        // broadcasts/MOTD use the EN catalog; per-recipient placeholders are applied
        // later by JoinLeaveListener.
        dev.zcripted.obx.core.language.LanguageManager lang = plugin.getLanguageManager();
        if (lang == null) {
            joinMessage = "";
            leaveMessage = "";
            firstJoinMessage = "";
            joinMotdLines = Collections.emptyList();
            firstJoinMotdLines = Collections.emptyList();
            return;
        }
        joinMessage = lang.rawTemplate(LanguageRegistry.EN, "welcome.join-message");
        leaveMessage = lang.rawTemplate(LanguageRegistry.EN, "welcome.leave-message");
        firstJoinMessage = lang.rawTemplate(LanguageRegistry.EN, "welcome.first-join-message");

        // MOTD lists are structured (mixed plain-string + {text,hover,click} nodes);
        // resolveMotdLines reassembles each node into a renderable MiniMessage line.
        joinMotdLines = freezeList(lang.resolveMotdLines(LanguageRegistry.EN, "welcome.motd-lines"));
        List<String> firstLines = lang.resolveMotdLines(LanguageRegistry.EN, "welcome.motd-first-join-lines");
        firstJoinMotdLines = freezeList(firstLines.isEmpty() ? joinMotdLines : firstLines);
    }

    private static List<String> freezeList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new java.util.ArrayList<>(input));
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    // ---------------------------------------------------------------------
    // Join / leave broadcast
    // ---------------------------------------------------------------------

    public boolean isJoinLeaveEnabled() {
        return joinLeaveEnabled;
    }

    public void setJoinLeaveEnabled(boolean enabled) {
        plugin.getConfig().set("join-leave.enabled", enabled);
        plugin.saveConfig();
        joinLeaveEnabled = enabled;
    }

    public boolean suppressVanillaJoinMessage() {
        return suppressVanilla;
    }

    public boolean isFirstJoinMessageEnabled() {
        return firstJoinMessageEnabled;
    }

    public String getJoinMessage() {
        return joinMessage;
    }

    public String getLeaveMessage() {
        return leaveMessage;
    }

    public String getFirstJoinMessage() {
        return firstJoinMessage;
    }

    // ---------------------------------------------------------------------
    // In-game welcome MOTD
    // ---------------------------------------------------------------------

    public boolean isJoinMotdEnabled() {
        return joinMotdEnabled;
    }

    public void setJoinMotdEnabled(boolean enabled) {
        plugin.getConfig().set("join-motd.enabled", enabled);
        plugin.saveConfig();
        joinMotdEnabled = enabled;
    }

    public boolean isFirstJoinMotdEnabled() {
        return firstJoinMotdEnabled;
    }

    public List<String> getJoinMotdLines() {
        return joinMotdLines;
    }

    public List<String> getFirstJoinMotdLines() {
        return firstJoinMotdLines;
    }
}
