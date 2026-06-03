package dev.zcripted.obx.feature.enchant.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.enchant.effect.EnchantState;
import dev.zcripted.obx.feature.enchant.service.EnchantService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /satchel} — opens the personal storage granted by the Satchel
 * enchantment on a worn helmet or chestplate. Contents are kept per session.
 */
public final class SatchelCommand extends AbstractObxCommand {

    private final EnchantState state;

    public SatchelCommand(ObxPlugin plugin, EnchantState state) {
        super(plugin);
        this.state = state;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EnchantService service = plugin.getEnchantService();
        if (!service.isEnabled()) {
            plugin.getLanguageManager().send(sender, "enchant.module-disabled");
            return true;
        }
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        int level = Math.max(
                service.getStorage().level(player.getInventory().getHelmet(), "satchel"),
                service.getStorage().level(player.getInventory().getChestplate(), "satchel"));
        if (level <= 0) {
            plugin.getLanguageManager().send(player, "enchant.satchel.none");
            return true;
        }
        int slots = service.getRegistry().get("satchel").levelInt(level, "slots", 9);
        player.openInventory(state.satchel(player, slots));
        return true;
    }
}
