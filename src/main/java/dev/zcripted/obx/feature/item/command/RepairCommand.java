package dev.zcripted.obx.feature.item.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RepairCommand extends AbstractObxCommand implements TabCompleter {


    public RepairCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (args.length >= 1 && args[0].equalsIgnoreCase("all")) {
            if (!player.hasPermission("obx.repair.all")) {
                languages.send(player, "core.no-permission");
                return true;
            }
            int repaired = repairAll(player.getInventory());
            languages.send(player, "inventory.repair.all", Placeholders.with("count", repaired));
            return true;
        }
        if (!player.hasPermission("obx.repair")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            languages.send(player, "inventory.repair.empty-hand");
            return true;
        }
        if (!isRepairable(hand)) {
            languages.send(player, "inventory.repair.not-repairable",
                    Placeholders.with("material", hand.getType().name()));
            return true;
        }
        repair(hand);
        languages.send(player, "inventory.repair.single",
                Placeholders.with("material", hand.getType().name()));
        return true;
    }

    private int repairAll(PlayerInventory inventory) {
        int total = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && isRepairable(stack)) {
                repair(stack);
                total++;
            }
        }
        for (ItemStack stack : inventory.getArmorContents()) {
            if (stack != null && isRepairable(stack)) {
                repair(stack);
                total++;
            }
        }
        return total;
    }

    private boolean isRepairable(ItemStack stack) {
        return stack != null && stack.getType().getMaxDurability() > 0 && stack.getDurability() > 0;
    }

    @SuppressWarnings("deprecation")
    private void repair(ItemStack stack) {
        // setDurability((short)0) restores full durability on every version OBX targets.
        stack.setDurability((short) 0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return new java.util.ArrayList<>(Arrays.asList("all"));
        }
        return Collections.emptyList();
    }
}
