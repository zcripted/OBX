package dev.zcripted.obx.feature.hologram.service;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.hologram.backend.BackendSelector;
import dev.zcripted.obx.feature.hologram.backend.HologramBackend;
import dev.zcripted.obx.feature.hologram.interact.RaycastTargeter;
import dev.zcripted.obx.feature.hologram.packet.PacketAvailability;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.render.HologramRenderer;
import dev.zcripted.obx.feature.hologram.render.TickLoop;
import dev.zcripted.obx.feature.hologram.storage.HologramStorage;
import dev.zcripted.obx.feature.hologram.storage.YamlHologramStorage;
import dev.zcripted.obx.core.platform.PlatformInfo;
import dev.zcripted.obx.util.message.ConsoleLog;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Service entry point for the holograms module. Lifecycle, config gate, and
 * backend selection — mirrors {@link dev.zcripted.obx.feature.hub.service.HubService}
 * and {@link dev.zcripted.obx.feature.chat.service.ChatService}.
 *
 * <p>Dormant by default — the {@code enabled} flag in
 * {@code systems/holograms.yml} starts as {@code false}, and {@link #load()}
 * logs a one-line dormancy notice without selecting a backend, registering a
 * tick loop, or doing any work. Operators flip {@code enabled: true} (or use
 * {@code /holo enable} once the command tree lands in Phase 2) to activate.
 *
 * <p>When enabled, the service:
 * <ul>
 *   <li>Resolves the appropriate {@link HologramBackend} via
 *       {@link BackendSelector};</li>
 *   <li>Probes for the optional packet layer via
 *       {@link PacketAvailability#probe(OBX)} and logs the result;</li>
 *   <li>Owns the {@link HologramRegistry} — populated by storage in Phase 2;</li>
 *   <li>Defers tick / listener / command wiring to later phases (Phase 1
 *       brings the renderer + listeners, Phase 2 brings the command tree).</li>
 * </ul>
 *
 * <p>This service writes nothing to disk in Phase 0; {@link #save()} is a
 * no-op until persistence lands in Phase 2. {@link #reload()} re-reads
 * {@code holograms.yml} and re-runs backend selection.
 */
public final class HologramService {

    private static final String RESOURCE_PATH = "systems/holograms.yml";

    private final OBX plugin;
    private final File configFile;
    private final HologramRegistry registry = new HologramRegistry();
    private final HologramStorage storage;

    private volatile YamlConfiguration config;
    private volatile HologramBackend backend;
    private volatile HologramRenderer renderer;
    private volatile TickLoop tickLoop;
    private volatile RaycastTargeter raycastTargeter;
    private volatile boolean active;

    public HologramService(OBX plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), RESOURCE_PATH);
        this.storage = new YamlHologramStorage(plugin);
    }

    public void load() {
        ensureConfigOnDisk();
        config = YamlConfiguration.loadConfiguration(configFile);

        if (!isEnabled()) {
            active = false;
            backend = null;
            renderer = null;
            ConsoleLog.info(plugin, "Holograms", "Module dormant (systems/holograms.yml → enabled: false).");
            return;
        }

        PlatformInfo platform = plugin.getPlatformInfo();
        backend = BackendSelector.choose(plugin, platform);
        boolean packetAvailable = PacketAvailability.probe(plugin);
        renderer = new HologramRenderer(plugin, registry, backend);

        // Load persisted holograms (Phase 2). Each is registered into the
        // in-memory registry; entities are spawned lazily on the next tick.
        for (Hologram hologram : storage.loadAll()) {
            registry.register(hologram);
        }
        renderer.spawnAll();

        long period = config.getLong("view-update-ticks", 5L);
        tickLoop = new TickLoop(plugin, renderer, period);
        tickLoop.start();

        // Raycast targeter — always runs, but acts only on holograms with
        // interaction-enabled=true. Picks up clicks on platforms where the
        // Netty packet layer is unavailable or as a fast-path for
        // hover-color changes on every server.
        long raycastHz = Math.max(1L, config.getLong("interaction.raycast-hz", 4L));
        long raycastPeriod = Math.max(1L, 20L / raycastHz);
        raycastTargeter = new RaycastTargeter(plugin, this, raycastPeriod);
        raycastTargeter.start();

        active = true;

        ConsoleLog.info(plugin, "Holograms", backend.describe() + " selected.");
        if (packetAvailable) {
            ConsoleLog.info(plugin, "Holograms",
                    "Custom packet layer available — interaction events will use packet dispatch.");
        } else {
            ConsoleLog.info(plugin, "Holograms",
                    "Custom packet layer unavailable — interaction events will fall back to raycast. Reason: "
                            + PacketAvailability.describe());
        }
    }

    public void reload() {
        // Tear down live entities, stop the tick loop, then re-run load() so
        // a new backend can be chosen if the YAML changed.
        if (raycastTargeter != null) {
            raycastTargeter.stop();
            raycastTargeter = null;
        }
        if (tickLoop != null) {
            tickLoop.stop();
            tickLoop = null;
        }
        if (renderer != null) {
            renderer.destroyAll();
            renderer = null;
        } else if (backend != null) {
            for (dev.zcripted.obx.feature.hologram.model.Hologram hologram : registry.all()) {
                backend.destroy(hologram);
            }
        }
        active = false;
        backend = null;
        registry.clear();
        load();
        // Re-spawn anything storage repopulated (Phase 2+) — registry is empty in Phase 1
        // so this is a no-op until persistence lands.
        if (renderer != null) {
            renderer.spawnAll();
        }
    }

    /**
     * Synchronous flush. Phase 0 has no in-memory state to persist; Phase 2
     * wires hologram serialization through here. Always safe to call.
     */
    public void save() {
        if (config == null) {
            return;
        }
        try {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(configFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("[Holograms] Failed to save holograms.yml: " + exception.getMessage());
        }
    }

    public boolean isEnabled() {
        return config != null && config.getBoolean("enabled", false);
    }

    public boolean isActive() {
        return active;
    }

    public HologramBackend getBackend() {
        return backend;
    }

    public HologramRegistry getRegistry() {
        return registry;
    }

    public HologramRenderer getRenderer() {
        return renderer;
    }

    public HologramStorage getStorage() {
        return storage;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    /** Called from OBX#onDisable to tear down live entities cleanly. */
    public void shutdown() {
        if (raycastTargeter != null) {
            raycastTargeter.stop();
            raycastTargeter = null;
        }
        if (tickLoop != null) {
            tickLoop.stop();
            tickLoop = null;
        }
        if (renderer != null) {
            renderer.destroyAll();
            renderer = null;
        } else if (backend != null) {
            for (dev.zcripted.obx.feature.hologram.model.Hologram hologram : registry.all()) {
                backend.destroy(hologram);
            }
        }
        active = false;
    }

    private void ensureConfigOnDisk() {
        if (configFile.exists()) {
            return;
        }
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            plugin.saveResource(RESOURCE_PATH, false);
        } catch (IllegalArgumentException ignored) {
            // Resource not bundled — we'll fall back to an empty YamlConfiguration.
        }
    }
}
