package dev.zcripted.obx.feature.playerstate.command;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.command.PlayerActionCommand;
import org.bukkit.entity.Player;

public class KillCommand extends PlayerActionCommand {

    public KillCommand(OBX plugin) {
        super(plugin, "obx.kill");
    }

    @Override
    protected void run(Player player, String[] args) {
        boolean enabled = plugin.getKillModeManager().toggle(player);
        languages.send(player, enabled ? "admin.kill.mode.enabled" : "admin.kill.mode.disabled");
    }
}
