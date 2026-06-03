package dev.zcripted.obx.hologram.api;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.hologram.model.Hologram;
import dev.zcripted.obx.hologram.model.HologramId;
import dev.zcripted.obx.hologram.model.HologramLine;
import dev.zcripted.obx.hologram.service.HologramService;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

/**
 * Stable public API for plugins that want to drive OBX holograms
 * programmatically. The facade is the only surface third-party code should
 * touch — internal classes ({@code HologramService}, backends, packet layer)
 * remain free to refactor without breaking integrators.
 *
 * <p>Resolution: {@code HologramFacade.get()} fetches the running facade
 * from the loaded OBX instance. Use the early-exit
 * {@link #isAvailable()} check before calling other methods so plugins
 * gracefully degrade when OBX is absent / disabled.
 */
public final class HologramFacade {

    private final OBX plugin;
    private final HologramService service;

    private HologramFacade(OBX plugin, HologramService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public static HologramFacade get() {
        JavaPlugin loaded = (JavaPlugin) org.bukkit.Bukkit.getPluginManager().getPlugin("OBX");
        if (!(loaded instanceof OBX)) {
            return null;
        }
        OBX main = (OBX) loaded;
        HologramService svc = main.getHologramService();
        if (svc == null) {
            return null;
        }
        return new HologramFacade(main, svc);
    }

    public boolean isAvailable() {
        return plugin != null && service != null && service.isActive();
    }

    public Collection<Hologram> all() {
        return service.getRegistry().all();
    }

    public Hologram find(String id) {
        HologramId hid = HologramId.parse(id);
        return hid == null ? null : service.getRegistry().get(hid);
    }

    public Hologram create(String id, Location location) {
        HologramId hid = HologramId.parse(id);
        if (hid == null || service.getRegistry().contains(hid)) {
            return null;
        }
        Hologram hologram = new Hologram(hid, location);
        service.getRegistry().register(hologram);
        service.getStorage().save(hologram);
        service.getBackend().spawn(hologram, java.util.Collections.<org.bukkit.entity.Player>emptyList());
        service.getRegistry().rebuildEntityIndex(hologram);
        plugin.getServer().getPluginManager().callEvent(new HologramSpawnEvent(hologram));
        return hologram;
    }

    public void addLine(Hologram hologram, HologramLine line) {
        if (hologram == null || line == null) {
            return;
        }
        hologram.addLine(line);
        service.getStorage().save(hologram);
        service.getBackend().applyMutations(hologram);
        service.getRegistry().rebuildEntityIndex(hologram);
    }

    public void delete(Hologram hologram) {
        if (hologram == null) {
            return;
        }
        service.getBackend().destroy(hologram);
        service.getRegistry().unregister(hologram.getId());
        service.getStorage().delete(hologram.getId());
    }
}
