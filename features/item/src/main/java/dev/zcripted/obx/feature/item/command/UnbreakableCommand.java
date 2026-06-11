package dev.zcripted.obx.feature.item.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;

public class UnbreakableCommand extends AbstractObxCommand implements TabCompleter {


    public UnbreakableCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.unbreakable")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            languages.send(player, "item.empty-hand");
            return true;
        }
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            languages.send(player, "item.no-meta");
            return true;
        }
        if (!isBreakable(hand.getType())) {
            languages.send(player, "item.unbreakable.not-breakable");
            return true;
        }
        boolean current = isUnbreakable(meta);
        boolean next = !current;
        if (!applyUnbreakable(meta, next)) {
            languages.send(player, "item.unbreakable.unsupported");
            return true;
        }
        hand.setItemMeta(meta);
        languages.send(player, next ? "item.unbreakable.enabled" : "item.unbreakable.disabled");
        return true;
    }

    private boolean isUnbreakable(ItemMeta meta) {
        try {
            // 1.11+ exposes ItemMeta#isUnbreakable() directly (present in the 1.12.2 compile API).
            return meta.isUnbreakable();
        } catch (NoSuchMethodError legacy) {
            // 1.8.8-1.10: the flag lives on ItemMeta.Spigot instead.
            try {
                return meta.spigot().isUnbreakable();
            } catch (Throwable unsupported) {
                return false;
            }
        }
    }

    private boolean applyUnbreakable(ItemMeta meta, boolean value) {
        try {
            meta.setUnbreakable(value);
            return true;
        } catch (NoSuchMethodError legacy) {
            try {
                meta.spigot().setUnbreakable(value);
                return true;
            } catch (Throwable unsupported) {
                return false;
            }
        }
    }

    private static boolean isBreakable(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || name.endsWith("_BOW")
                || name.endsWith("_CROSSBOW")
                || name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || name.endsWith("_TRIDENT")
                || name.endsWith("_MACE")
                || name.equals("SHIELD")
                || name.equals("ELYTRA")
                || name.equals("FISHING_ROD")
                || name.equals("SHEARS")
                || name.equals("FLINT_AND_STEEL");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}