package dev.zcripted.obx.feature.deathdrop;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.deathdrop.listener.DeathDropListener;

/**
 * Death-item grouping feature.
 *
 * <p>When enabled, a dying player's entire set of dropped items (hotbar +
 * inventory + armor that would normally scatter) is combined into a single
 * "carry-all" {@link org.bukkit.entity.Item} entity — rendered as a chest
 * (1.13–1.16.5) or a bundle (1.17+), always falling back to a chest — that
 * floats with a holographic {@code ×<count>} name. Walking over it restores the
 * stored items to the player's inventory; anything that doesn't fit stays inside
 * the entity (the hologram updates) until there's room.
 *
 * <p>The module is opt-in ({@link #enabledByDefault()} is {@code false}) and is
 * toggled via the Module Toggles GUI, {@code /obx deathdrop on|off}, console, or
 * the {@code modules.deathdrop} flag in {@code config.yml}. Disabling it
 * unregisters the listener (via {@link AbstractModule}) so death behaviour
 * reverts to vanilla immediately.
 */
public final class DeathDropModule extends AbstractModule {

    @Override
    public String id() {
        return "deathdrop";
    }

    @Override
    public String displayName() {
        return "Death Grouping";
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        DeathDropListener listener = new DeathDropListener(plugin);
        listener(listener);
        // Start the per-tick hologram-follow loop (and sweep any holograms orphaned by a crash).
        listener.start();
        // Cancel the loop, remove every floating hologram, and drop the in-memory tracking on
        // disable so nothing leaks across reloads.
        onDisable(listener::shutdown);
    }
}
