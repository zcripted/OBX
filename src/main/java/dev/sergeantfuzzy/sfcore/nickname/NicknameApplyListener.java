package dev.sergeantfuzzy.sfcore.nickname;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NicknameApplyListener implements Listener {

    private final NicknameService nicknames;

    public NicknameApplyListener(NicknameService nicknames) {
        this.nicknames = nicknames;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        nicknames.applyOnJoin(event.getPlayer());
    }
}
