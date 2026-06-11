package dev.zcripted.obx.util;

/**
 * Shared utility for runtime class detection via reflection.
 * Consolidates the {@code hasClass} probes previously duplicated across
 * PlatformInfo, ComponentMessenger, AdventureMessageUtil, and BackendSelector.
 */
public final class ClassUtil {

    private ClassUtil() {
    }

    /**
     * Returns {@code true} if {@code name} can be loaded via
     * {@link Class#forName(String)}. Catches {@link ClassNotFoundException} and
     * any other linkage errors silently.
     */
    public static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}