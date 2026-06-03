package dev.zcripted.obx.hologram.anim;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializable configuration for a single animation attached to a hologram.
 * Stored on {@link dev.zcripted.obx.hologram.model.HologramSettings}
 * and (re)hydrated into a live {@link Animation} via {@link AnimationRegistry}.
 *
 * <p>Forward-compatible — unknown keys in {@link #params} are preserved
 * across save / load so new params can be added in later phases without
 * breaking older configs.
 */
public final class AnimationConfig {

    private final String type;
    private final Map<String, Object> params;
    /**
     * Whether this animation is currently active. A disabled config stays in
     * the hologram's persisted list (so the operator can toggle it back on
     * without re-entering parameters) but is skipped when building the live
     * animation list — see {@code Hologram#rebuildAnimations}.
     */
    private volatile boolean enabled = true;

    public AnimationConfig(String type, Map<String, Object> params) {
        this.type = type == null ? "" : type;
        this.params = params == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<>(params);
    }

    public AnimationConfig(String type, Map<String, Object> params, boolean enabled) {
        this(type, params);
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getDouble(String key, double fallback) {
        Object raw = params.get(key);
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        if (raw != null) {
            try {
                return Double.parseDouble(raw.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    public long getLong(String key, long fallback) {
        Object raw = params.get(key);
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw != null) {
            try {
                return Long.parseLong(raw.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }
}
