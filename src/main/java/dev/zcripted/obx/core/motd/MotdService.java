package dev.zcripted.obx.core.motd;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.message.MotdMessageUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads and serves the MOTD configuration. Server-list pings hit this service on every
 * ping, including under botnet flood conditions, so anything that doesn't depend on the
 * live online/max counts is parsed once on {@link #load()} / {@link #reload()} and held
 * as immutable state — pings then read pre-resolved fields and only run the small
 * placeholder substitution per call.
 */
public class MotdService {

    private final OBX plugin;
    private final File motdFile;

    private volatile YamlConfiguration config;

    // Cached snapshot of motd.yml — refreshed only on load()/reload(). Pings read
    // these fields directly so config-list parsing happens at most once per reload,
    // not once per ping.
    private volatile boolean enabled;
    private volatile boolean hoverEnabled;
    private volatile boolean useProfileNames;
    private volatile HoverDisplayMode hoverDisplayMode;
    private volatile String motdLine1;
    private volatile String motdLine2;
    private volatile int centerPixels;
    private volatile int configuredFakeOnline;
    private volatile boolean useFakeOnline;
    private volatile int configuredMaxPlayers;
    private volatile List<String> hoverLineTemplates = Collections.emptyList();
    private volatile List<String> sampleProfileNames = Collections.emptyList();

    public MotdService(OBX plugin) {
        this.plugin = plugin;
        this.motdFile = new File(plugin.getDataFolder(), "motd.yml");
    }

    public void load() {
        try {
            if (!motdFile.exists()) {
                if (motdFile.getParentFile() != null) {
                    motdFile.getParentFile().mkdirs();
                }
                try {
                    plugin.saveResource("motd.yml", false);
                } catch (IllegalArgumentException ignored) {
                    motdFile.createNewFile();
                }
            }
            config = YamlConfiguration.loadConfiguration(motdFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to load motd.yml: " + exception.getMessage());
        }
        cacheSnapshot();
    }

    public void reload() {
        load();
    }

    private void cacheSnapshot() {
        YamlConfiguration cfg = config;
        if (cfg == null) {
            enabled = true;
            hoverEnabled = true;
            useProfileNames = true;
            hoverDisplayMode = HoverDisplayMode.PROFILES;
            motdLine1 = "";
            motdLine2 = "";
            centerPixels = MotdMessageUtil.DEFAULT_MOTD_CENTER_PX;
            useFakeOnline = false;
            configuredFakeOnline = 0;
            configuredMaxPlayers = 0;
            hoverLineTemplates = Collections.emptyList();
            sampleProfileNames = Collections.emptyList();
            return;
        }
        enabled = cfg.getBoolean("enabled", true);
        hoverEnabled = cfg.getBoolean("player-count.hover.enabled", true);
        if (cfg.contains("player-count.hover.use-profile-names")) {
            useProfileNames = cfg.getBoolean("player-count.hover.use-profile-names", true);
            hoverDisplayMode = useProfileNames ? HoverDisplayMode.PROFILES : HoverDisplayMode.LINES;
        } else {
            hoverDisplayMode = HoverDisplayMode.fromConfig(cfg.getString("player-count.hover.display-mode", "profiles"));
            useProfileNames = hoverDisplayMode == HoverDisplayMode.PROFILES;
        }
        motdLine1 = cfg.getString("motd.line-1", "");
        motdLine2 = cfg.getString("motd.line-2", "");
        int configuredCenter = cfg.getInt("motd.center-pixels", MotdMessageUtil.DEFAULT_MOTD_CENTER_PX);
        centerPixels = configuredCenter == 123 ? MotdMessageUtil.DEFAULT_MOTD_CENTER_PX : configuredCenter;
        String onlineMode = cfg.getString("player-count.online.mode", "real");
        useFakeOnline = onlineMode != null && "fake".equalsIgnoreCase(onlineMode.trim());
        configuredFakeOnline = cfg.getInt("player-count.online.fake", 0);
        configuredMaxPlayers = cfg.getInt("player-count.max", 0);
        hoverLineTemplates = cacheList(cfg.getStringList("player-count.hover.lines"));
        sampleProfileNames = cacheList(cfg.getStringList("player-count.hover.sample-profiles"));
    }

    private static List<String> cacheList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> copy = new ArrayList<>(raw.size());
        for (String entry : raw) {
            if (entry == null) {
                continue;
            }
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                copy.add(trimmed);
            }
        }
        return copy.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(copy);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String buildMotd(int realOnline, int realMax) {
        Map<String, String> placeholders = buildPlaceholders(realOnline, realMax);
        String line1 = MotdMessageUtil.formatMotdLine(motdLine1 == null ? "" : motdLine1, placeholders, centerPixels);
        String line2 = MotdMessageUtil.formatMotdLine(motdLine2 == null ? "" : motdLine2, placeholders, centerPixels);
        return line1 + "\n" + line2;
    }

    public int getDisplayedOnline(int realOnline) {
        if (useFakeOnline) {
            return Math.max(0, configuredFakeOnline);
        }
        return Math.max(0, realOnline);
    }

    public int getDisplayedMax(int realMax) {
        int configured = configuredMaxPlayers;
        return Math.max(0, configured <= 0 ? realMax : configured);
    }

    public List<String> getHoverLines(int realOnline, int realMax) {
        if (!hoverEnabled || hoverLineTemplates.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> placeholders = buildPlaceholders(realOnline, realMax);
        List<String> lines = new ArrayList<>(hoverLineTemplates.size());
        for (int i = 0; i < hoverLineTemplates.size(); i++) {
            lines.add(MotdMessageUtil.formatText(hoverLineTemplates.get(i), placeholders));
        }
        return lines;
    }

    public List<String> getSampleProfiles() {
        if (!hoverEnabled) {
            return Collections.emptyList();
        }
        return sampleProfileNames;
    }

    public boolean useProfileNamesForHover() {
        return hoverEnabled && useProfileNames;
    }

    public HoverDisplayMode getHoverDisplayMode() {
        return hoverDisplayMode == null ? HoverDisplayMode.PROFILES : hoverDisplayMode;
    }

    public enum HoverDisplayMode {
        PROFILES,
        LINES;

        public static HoverDisplayMode fromConfig(String input) {
            if (input == null) {
                return PROFILES;
            }
            String normalized = input.trim().toLowerCase(Locale.ENGLISH);
            if ("lines".equals(normalized) || "text".equals(normalized) || "count-lines".equals(normalized)) {
                return LINES;
            }
            return PROFILES;
        }
    }

    private Map<String, String> buildPlaceholders(int realOnline, int realMax) {
        int displayedOnline = getDisplayedOnline(realOnline);
        int displayedMax = Math.max(displayedOnline, getDisplayedMax(realMax));
        Map<String, String> placeholders = new LinkedHashMap<>(4);
        placeholders.put("online", Integer.toString(displayedOnline));
        placeholders.put("max", Integer.toString(displayedMax));
        placeholders.put("real_online", Integer.toString(Math.max(0, realOnline)));
        placeholders.put("real_max", Integer.toString(Math.max(0, realMax)));
        return placeholders;
    }
}
