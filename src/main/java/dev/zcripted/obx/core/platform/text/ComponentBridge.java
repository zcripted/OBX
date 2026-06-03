package dev.zcripted.obx.core.platform.text;

import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Text-rendering seam: bridges OBX's MiniMessage/legacy markup to whatever the
 * running platform supports (native Adventure on Paper, legacy {@code §} sections
 * on Spigot). Resolved via {@link dev.zcripted.obx.core.bootstrap.PlatformResolver};
 * the default implementation, {@link AdventureComponentBridge}, delegates to the
 * existing renderer and degrades to legacy when Adventure is absent.
 *
 * <p>Feature code can program against this instead of calling the static text
 * utilities directly; migrating the existing call sites is incremental.
 */
public interface ComponentBridge {

    /** Renders {@code raw} (MiniMessage/legacy) and sends it to {@code player}. */
    void send(Player player, String raw, Map<String, String> placeholders);

    /** Renders {@code raw} down to a legacy {@code §}-coded string. */
    String renderLegacy(String raw);
}
