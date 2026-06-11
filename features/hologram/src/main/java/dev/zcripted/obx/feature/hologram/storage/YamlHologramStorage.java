package dev.zcripted.obx.feature.hologram.storage;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.model.HologramId;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Default storage implementation — one file, {@code holograms.yml}, in the
 * plugin data folder. Loads on service activation, saves on every model
 * mutation routed through {@link #save(Hologram)} (atomic write via temp file
 * + rename so a crash mid-save can't corrupt the existing file).
 *
 * <p>Schema (forward-compatible; unknown keys are ignored on read):
 * <pre>
 * holograms:
 *   spawn_welcome:
 *     location: {world, x, y, z, yaw, pitch}
 *     settings: {...}
 *     lines:
 *       - {type: TEXT, value: "..."}
 *       - {type: ICON, material: DIAMOND, amount: 1}
 *       - {type: BLOCK, material: STONE}
 * </pre>
 */
public final class YamlHologramStorage implements HologramStorage {

    private final ObxPlugin plugin;
    private final File file;
    private final Object writeLock = new Object();

    public YamlHologramStorage(ObxPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "holograms.yml");
    }

    @Override
    public Collection<Hologram> loadAll() {
        if (!file.exists()) {
            return Collections.emptyList();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("holograms");
        if (root == null) {
            return Collections.emptyList();
        }
        List<Hologram> result = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Hologram hologram = HologramSerializer.read(key, section, plugin);
            if (hologram != null) {
                result.add(hologram);
            }
        }
        return result;
    }

    @Override
    public void save(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        synchronized (writeLock) {
            YamlConfiguration config = file.exists()
                    ? YamlConfiguration.loadConfiguration(file)
                    : new YamlConfiguration();
            String path = "holograms." + hologram.getId().value();
            config.set(path, null);
            ConfigurationSection section = config.createSection(path);
            HologramSerializer.write(hologram, section);
            writeAtomically(config);
        }
    }

    @Override
    public void saveAll(Collection<Hologram> holograms) {
        synchronized (writeLock) {
            YamlConfiguration config = new YamlConfiguration();
            ConfigurationSection root = config.createSection("holograms");
            if (holograms != null) {
                for (Hologram hologram : holograms) {
                    if (hologram == null) {
                        continue;
                    }
                    ConfigurationSection section = root.createSection(hologram.getId().value());
                    HologramSerializer.write(hologram, section);
                }
            }
            writeAtomically(config);
        }
    }

    @Override
    public void delete(HologramId id) {
        if (id == null) {
            return;
        }
        synchronized (writeLock) {
            if (!file.exists()) {
                return;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = config.getConfigurationSection("holograms");
            if (root == null) {
                return;
            }
            root.set(id.value(), null);
            writeAtomically(config);
        }
    }

    private void writeAtomically(YamlConfiguration config) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File temp = new File(file.getAbsolutePath() + ".tmp");
        try {
            config.save(temp);
            // Replace atomically. On POSIX renameTo is atomic; on Windows it
            // requires the target not to exist, so fall back to delete-then-rename.
            if (file.exists() && !temp.renameTo(file)) {
                if (file.delete() && temp.renameTo(file)) {
                    return;
                }
                // last-ditch — copy the temp over the target with a load+save.
                YamlConfiguration backup = YamlConfiguration.loadConfiguration(temp);
                backup.save(file);
                temp.delete();
            } else if (!file.exists()) {
                temp.renameTo(file);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("[Holograms] Failed to save holograms.yml: " + exception.getMessage());
        }
    }
}