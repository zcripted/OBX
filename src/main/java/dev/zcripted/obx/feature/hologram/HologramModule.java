package dev.zcripted.obx.feature.hologram;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.hologram.command.HologramCommand;
import dev.zcripted.obx.feature.hologram.gui.HologramEditorMenu;
import dev.zcripted.obx.feature.hologram.listener.HologramConnectionListener;
import dev.zcripted.obx.feature.hologram.listener.HologramJoinListener;
import dev.zcripted.obx.feature.hologram.listener.HologramResourcePackListener;
import dev.zcripted.obx.feature.hologram.service.HologramService;

/**
 * Holograms feature. Dormant internally when systems/holograms.yml has
 * {@code enabled: false}. Renders via display entities on 1.19.4+ with an
 * armor-stand fallback. Owns the {@code /holo} command tree, the editor GUI
 * (also a listener), and the re-show listeners.
 */
public final class HologramModule extends AbstractModule {

    @Override
    public String id() {
        return "hologram";
    }

    @Override
    protected void onEnable(OBX plugin) {
        HologramService holo = service(HologramService.class, new HologramService(plugin));
        holo.load();
        HologramEditorMenu editor = service(HologramEditorMenu.class, new HologramEditorMenu(plugin));

        command("holo", new HologramCommand(plugin, holo));

        listener(new HologramJoinListener(plugin, holo));
        listener(new HologramResourcePackListener(plugin, holo));
        listener(new HologramConnectionListener(plugin, holo));
        listener(editor);

        onDisable(() -> {
            holo.shutdown();
            holo.save();
        });
    }

    @Override
    public void reload(OBX plugin) {
        HologramService holo = plugin.getHologramService();
        if (holo != null) {
            holo.reload();
        }
    }
}
