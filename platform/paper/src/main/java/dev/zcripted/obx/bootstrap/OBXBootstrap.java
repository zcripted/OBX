package dev.zcripted.obx.bootstrap;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.jetbrains.annotations.NotNull;

/**
 * Paper-native bootstrap entry point (referenced by paper-plugin.yml's
 * {@code bootstrapper:}). Runs once, before the server world loads, on Paper
 * 1.20+. OBX's real lifecycle stays in {@link dev.zcripted.obx.OBX#onEnable()};
 * this hook only logs, so behavior is identical to the plugin.yml path on older
 * Paper/Spigot.
 */
public class OBXBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        context.getLogger().info("OBX (Obsidian eXtended) bootstrapping on Paper.");
    }
}
