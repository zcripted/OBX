package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand implements CommandExecutor {

    private final LanguageManager languages;

    public HealCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.heal")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        GameMode mode = player.getGameMode();
        if (mode == GameMode.SPECTATOR || mode == GameMode.CREATIVE) {
            languages.send(player, "utility.heal.invalid-gamemode");
            return true;
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
        return true;
    }
}
