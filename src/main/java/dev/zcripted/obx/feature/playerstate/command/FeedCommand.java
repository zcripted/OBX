package dev.zcripted.obx.feature.playerstate.command;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.command.PlayerActionCommand;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class FeedCommand extends PlayerActionCommand {

    public FeedCommand(OBX plugin) {
        super(plugin, "obx.feed");
    }

    @Override
    protected void run(Player player, String[] args) {
        GameMode mode = player.getGameMode();
        if (mode == GameMode.SPECTATOR || mode == GameMode.CREATIVE) {
            languages.send(player, "utility.feed.invalid-gamemode");
            return;
        }

        player.setFoodLevel(20);
        player.setSaturation(20f);
        languages.send(player, "utility.feed.success");
    }
}
