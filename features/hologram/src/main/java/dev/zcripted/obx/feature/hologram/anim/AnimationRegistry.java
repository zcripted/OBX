package dev.zcripted.obx.feature.hologram.anim;

import java.util.HashMap;
import java.util.Map;

/**
 * Lookup from animation type name → factory. New animation types added in
 * future phases register a factory here without touching the renderer or
 * the serializer — both call {@link #build(AnimationConfig)} and the
 * registry handles dispatch.
 */
public final class AnimationRegistry {

    private static final Map<String, Factory> FACTORIES = new HashMap<>();

    static {
        FACTORIES.put("fade", FadeAnimation::new);
        FACTORIES.put("rotate", RotateAnimation::new);
        FACTORIES.put("bob", IconBobAnimation::new);
        FACTORIES.put("iconbob", IconBobAnimation::new);
    }

    private AnimationRegistry() {
    }

    public static Animation build(AnimationConfig config) {
        if (config == null || config.getType() == null || config.getType().isEmpty()) {
            return null;
        }
        Factory factory = FACTORIES.get(config.getType().toLowerCase());
        return factory == null ? null : factory.create(config);
    }

    public static boolean isKnown(String typeName) {
        return typeName != null && FACTORIES.containsKey(typeName.toLowerCase());
    }

    @FunctionalInterface
    public interface Factory {
        Animation create(AnimationConfig config);
    }
}