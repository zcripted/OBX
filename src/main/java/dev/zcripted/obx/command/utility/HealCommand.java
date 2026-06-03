package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.command.PlayerActionCommand;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class HealCommand extends PlayerActionCommand {

    public HealCommand(Main plugin) {
        super(plugin, "obx.heal");
    }

    @Override
    protected void run(Player player, String[] args) {
        GameMode mode = player.getGameMode();
        if (mode == GameMode.SPECTATOR || mode == GameMode.CREATIVE) {
            languages.send(player, "utility.heal.invalid-gamemode");
            return;
        }

        double maxHealth = player.getHealth();
        try {
            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attribute != null) {
                maxHealth = attribute.getValue();
            }
        } catch (NoSuchMethodError ignored) {
            maxHealth = player.getMaxHealth();
        }

        player.setHealth(maxHealth);
        player.setFireTicks(0);
        languages.send(player, "utility.heal.success");
    }
}
