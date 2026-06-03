package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class MoreCommand extends AbstractObxCommand implements TabCompleter {


    public MoreCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.more")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            languages.send(player, "inventory.more.empty-hand");
            return true;
        }
        int max = hand.getMaxStackSize();
        if (max <= 1) {
            languages.send(player, "inventory.more.unstackable",
                    Placeholders.with("material", hand.getType().name()));
            return true;
        }
        hand.setAmount(max);
        languages.send(player, "inventory.more.filled",
                Placeholders.with("material", hand.getType().name(), "amount", max));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
