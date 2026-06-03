package dev.zcripted.obx.enchant.service;

import dev.zcripted.obx.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player combat-FX preferences, surfaced through {@code /enchants settings}.
 *
 * <p>The global {@code combat_global} block ({@link CombatSettings}) is the server
 * owner's master switch; this is each player's personal override to silence the FX
 * <em>they</em> generate (their kill banners and combat action-bar feedback) without
 * affecting anyone else. Persisted as a flat UUID list in
 * {@code enchants/combat_prefs.yml} so a choice survives restarts.
 *
 * <p>Default is enabled — only players who opt out are stored.
 */
public final class CombatPrefs {

    private static final String RESOURCE_PATH = "enchants/combat_prefs.yml";

    private final Main plugin;
    private final File file;
    private final Set<UUID> disabled = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    public CombatPrefs(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), RESOURCE_PATH);
    }

    public void load() {
        disabled.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String raw : config.getStringList("fx_disabled")) {
            try {
                disabled.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                // skip a malformed entry
            }
        }
    }

    /** True unless the player has opted out — null/unknown players default to enabled. */
    public boolean fxEnabled(UUID id) {
        return id == null || !disabled.contains(id);
    }

    /** Flips the player's preference and persists; returns the new enabled state. */
    public boolean toggle(UUID id) {
        if (id == null) {
            return true;
        }
        boolean nowEnabled;
        if (disabled.remove(id)) {
            nowEnabled = true;
        } else {
            disabled.add(id);
            nowEnabled = false;
        }
        save();
        return nowEnabled;
    }

    public void setEnabled(UUID id, boolean enabled) {
        if (id == null) {
            return;
        }
        if (enabled) {
            disabled.remove(id);
        } else {
            disabled.add(id);
        }
        save();
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        List<String> list = new ArrayList<String>();
        for (UUID id : disabled) {
            list.add(id.toString());
        }
        config.set("fx_disabled", list);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException ignored) {
            // best effort — a failed save just means the opt-out won't survive restart
        }
    }
}
