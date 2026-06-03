package dev.zcripted.obx.listener.player;

import dev.zcripted.obx.language.LanguageManager;
import dev.zcripted.obx.util.control.ServerControlState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class JoinLockListener implements Listener {

    private final LanguageManager languages;

    public JoinLockListener(LanguageManager languages) {
        this.languages = languages;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (!ServerControlState.isJoinLocked()) {
            return;
        }
        if (event.getPlayer().isOp()) {
            return;
        }
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, languages.get(event.getPlayer(), "admin.joinlock.locked-kick"));
    }
}
