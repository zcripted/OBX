package dev.zcripted.obx.core.platform;

/**
 * Server capability + type detection seam. Implemented by {@link PlatformInfo}
 * (a single runtime probe), resolved via
 * {@link dev.zcripted.obx.core.bootstrap.PlatformResolver}. Lets feature code ask
 * "is this Folia / Paper / does it have Adventure?" without touching the concrete
 * probe, and lets the probe be swapped per build target later.
 */
public interface Platform {

    PlatformInfo.ServerType getServerType();

    String getServerName();

    String getMinecraftVersion();

    int getMajor();

    int getMinor();

    int getPatch();

    boolean isAtLeast(int major, int minor);

    boolean isAtLeast(int major, int minor, int patch);

    boolean isFolia();

    boolean isPaper();

    boolean isPurpur();

    boolean isSpigot();

    boolean hasFoliaScheduler();

    boolean hasAdventureApi();

    boolean hasPaperPluginLoaderApi();

    String summary();
}