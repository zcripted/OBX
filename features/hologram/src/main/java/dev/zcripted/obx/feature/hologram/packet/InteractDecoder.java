package dev.zcripted.obx.feature.hologram.packet;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflective decoder for {@code ServerboundInteractPacket} (modern) and
 * {@code PacketPlayInUseEntity} (legacy). Walks the packet class's declared
 * fields once and caches the result so subsequent decodes only do two
 * field reads.
 *
 * <p>Returned action types are flattened to a small enum so the dispatcher
 * doesn't need to know NMS internals.
 */
public final class InteractDecoder {

    public enum Action {
        ATTACK,        // left-click
        INTERACT,      // right-click (general)
        INTERACT_AT,   // right-click on a specific spot
        UNKNOWN
    }

    public static final class Decoded {
        public final int entityId;
        public final Action action;
        public final boolean sneaking;

        public Decoded(int entityId, Action action, boolean sneaking) {
            this.entityId = entityId;
            this.action = action;
            this.sneaking = sneaking;
        }
    }

    private static final Map<Class<?>, FieldHandles> CACHE = new HashMap<>();

    private InteractDecoder() {
    }

    public static Decoded decode(Object packet) {
        if (packet == null) {
            return null;
        }
        try {
            FieldHandles handles = handlesFor(packet.getClass());
            int entityId = handles.entityIdField == null ? -1
                    : ((Number) handles.entityIdField.get(packet)).intValue();
            String actionName = handles.actionField == null ? "UNKNOWN"
                    : String.valueOf(handles.actionField.get(packet));
            boolean sneaking = false;
            if (handles.sneakField != null) {
                Object value = handles.sneakField.get(packet);
                sneaking = value instanceof Boolean && (Boolean) value;
            }
            Action action;
            if (actionName == null) {
                action = Action.UNKNOWN;
            } else if (actionName.equalsIgnoreCase("ATTACK")) {
                action = Action.ATTACK;
            } else if (actionName.equalsIgnoreCase("INTERACT")) {
                action = Action.INTERACT;
            } else if (actionName.equalsIgnoreCase("INTERACT_AT")) {
                action = Action.INTERACT_AT;
            } else {
                action = Action.UNKNOWN;
            }
            return new Decoded(entityId, action, sneaking);
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static synchronized FieldHandles handlesFor(Class<?> packetClass) {
        FieldHandles cached = CACHE.get(packetClass);
        if (cached != null) {
            return cached;
        }
        FieldHandles handles = new FieldHandles();
        // entity id: an int field. Common Mojang-mapped name: 'entityId';
        // obfuscated names: 'a', 'b'. We pick the first int field declared.
        for (Field field : packetClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (handles.entityIdField == null && field.getType() == int.class) {
                handles.entityIdField = field;
                continue;
            }
            if (handles.actionField == null && (field.getType().isEnum()
                    || field.getType().getName().endsWith("Action"))) {
                handles.actionField = field;
                continue;
            }
            if (handles.sneakField == null && field.getType() == boolean.class) {
                handles.sneakField = field;
            }
        }
        CACHE.put(packetClass, handles);
        return handles;
    }

    private static final class FieldHandles {
        Field entityIdField;
        Field actionField;
        Field sneakField;
    }
}