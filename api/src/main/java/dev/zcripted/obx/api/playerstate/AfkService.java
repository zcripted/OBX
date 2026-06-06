package dev.zcripted.obx.api.playerstate;

import org.bukkit.entity.Player;

import java.util.UUID;

/** Public AFK-state API. Implemented by {@code feature.playerstate.service.AfkServiceImpl}. */
public interface AfkService {

    void start();

    void stop();

    boolean isAfk(UUID uuid);

    void setAfk(Player player, boolean afk);

    /** Master switch: whether the AFK system (auto-detection, timeout/kick, messages) is active. */
    boolean isEnabled();

    /** Enables/disables the entire AFK system and persists the choice to config.yml. */
    void setEnabled(boolean enabled);
}
