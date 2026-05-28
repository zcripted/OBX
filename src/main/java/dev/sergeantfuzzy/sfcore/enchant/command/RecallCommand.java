package dev.sergeantfuzzy.sfcore.enchant.command;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.enchant.effect.EnchantState;
import dev.sergeantfuzzy.sfcore.enchant.model.CustomEnchant;
import dev.sergeantfuzzy.sfcore.enchant.util.Sounds;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;

/**
 * {@code /recall} — teleports to the location bound by the Beacon's Memory boots
 * enchantment (sneak + right-click to bind). Honors the per-level cooldown.
 */
public final class RecallCommand implements CommandExecutor {

    private final Main plugin;
    private final EnchantState state;

    public RecallCommand(Main plugin, EnchantState state) {
        this.plugin = plugin;
        this.state = state;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getEnchantService().isEnabled()) {
            plugin.getLanguageManager().send(sender, "enchant.module-disabled");
            return true;
        }
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        ItemStack boots = player.getInventory().getBoots();
        int level = plugin.getEnchantService().getStorage().level(boots, "beacons_memory");
        if (level <= 0) {
            plugin.getLanguageManager().send(player, "enchant.recall.no-enchant");
            return true;
        }
        Location target = state.latestRecall(player);
        if (target == null) {
            plugin.getLanguageManager().send(player, "enchant.recall.none");
            return true;
        }
        if (state.onCooldown(player, "recall")) {
            plugin.getLanguageManager().send(player, "enchant.recall.cooldown",
                    Collections.singletonMap("seconds", Long.toString(state.remainingSeconds(player, "recall"))));
            return true;
        }
        CustomEnchant enchant = plugin.getEnchantService().getRegistry().get("beacons_memory");
        player.teleport(target);
        state.setCooldown(player, "recall", enchant.levelInt(level, "cooldown_seconds", 600));
        plugin.getLanguageManager().send(player, "enchant.recall.teleported");
        Sounds.play(player, Sounds.BLINK, 0.6f, 1.0f);
        return true;
    }
}
