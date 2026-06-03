package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;

public class ItemNameCommand extends AbstractObxCommand implements TabCompleter {


    public ItemNameCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.itemname")) {
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
        if (args.length < 1 || (args.length == 1 && (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("reset")))) {
            meta.setDisplayName(null);
            hand.setItemMeta(meta);
            languages.send(player, "item.itemname.cleared");
            return true;
        }
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) name.append(' ');
            name.append(args[i]);
        }
        String colored = ChatColor.translateAlternateColorCodes('&', name.toString());
        meta.setDisplayName(colored);
        hand.setItemMeta(meta);
        languages.send(player, "item.itemname.set", Placeholders.with("name", colored));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> matches = new java.util.ArrayList<>();
        for (String option : new String[]{"clear", "reset"}) {
            if (option.startsWith(prefix)) matches.add(option);
        }
        return matches;
    }
}
