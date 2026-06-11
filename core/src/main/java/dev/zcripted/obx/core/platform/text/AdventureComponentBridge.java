package dev.zcripted.obx.core.platform.text;

import dev.zcripted.obx.util.message.AdventureMessageUtil;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Default {@link ComponentBridge} — delegates to {@link AdventureMessageUtil},
 * which renders natively via Adventure on Paper and falls back to legacy
 * {@code §} sections on Spigot. Used on every current platform; a Paper-native
 * bridge can replace it via the resolver without touching callers.
 */
public final class AdventureComponentBridge implements ComponentBridge {

    @Override
    public void send(Player player, String raw, Map<String, String> placeholders) {
        AdventureMessageUtil.send(player, raw, placeholders);
    }

    @Override
    public String renderLegacy(String raw) {
        return AdventureMessageUtil.renderLegacy(raw);
    }
}