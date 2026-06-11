package dev.zcripted.obx.feature.hologram.backend;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.platform.PlatformInfo;
import dev.zcripted.obx.util.ClassUtil;

/**
 * Picks the appropriate {@link HologramBackend} at service load time.
 *
 * <p>Selection rules (plan §2.1 / §7.1):
 * <ol>
 *   <li>If {@link PlatformInfo#isAtLeast(int, int, int)} reports MC ≥ 1.19.4
 *       <em>and</em> the {@code TextDisplay} class resolves at runtime →
 *       {@link DisplayEntityBackend}.</li>
 *   <li>Otherwise → {@link ArmorStandBackend}.</li>
 * </ol>
 *
 * <p>The selector returns the chosen backend; logging is the caller's
 * responsibility (so the service can format a single startup line in its own
 * style via {@code ConsoleLog}).
 */
public final class BackendSelector {

    private BackendSelector() {
    }

    public static HologramBackend choose(ObxPlugin plugin, PlatformInfo platform) {
        if (platform != null && platform.isAtLeast(1, 19, 4) && hasClass("org.bukkit.entity.TextDisplay")) {
            String label = "Display-entity backend (TextDisplay, " + platform.getServerName()
                    + " " + platform.getMinecraftVersion() + ")";
            return new DisplayEntityBackend(plugin, label);
        }
        String label = "Armor-stand backend ("
                + (platform != null ? platform.getServerName() + " " + platform.getMinecraftVersion() : "legacy")
                + ")";
        return new ArmorStandBackend(plugin, label);
    }

    private static boolean hasClass(String name) {
        return ClassUtil.hasClass(name);
    }
}