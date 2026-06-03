package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemLoreCommand extends AbstractObxCommand implements TabCompleter {


    public ItemLoreCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.itemlore")) {
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
        if (args.length < 1) {
            languages.send(player, "item.itemlore.usage");
            return true;
        }
        String action = args[0].toLowerCase();
        if (action.equals("clear") || action.equals("reset")) {
            meta.setLore(null);
            hand.setItemMeta(meta);
            languages.send(player, "item.itemlore.cleared");
            return true;
        }
        if (action.equals("add")) {
            if (args.length < 2) {
                languages.send(player, "item.itemlore.usage");
                return true;
            }
            String text = ChatColor.translateAlternateColorCodes('&',
                    String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add(text);
            meta.setLore(lore);
            hand.setItemMeta(meta);
            languages.send(player, "item.itemlore.added", Placeholders.with("line", text));
            return true;
        }
        if (action.equals("set")) {
            if (args.length < 3) {
                languages.send(player, "item.itemlore.usage");
                return true;
            }
            int index;
            try { index = Integer.parseInt(args[1]) - 1; }
            catch (NumberFormatException ignored) {
                languages.send(player, "item.itemlore.usage");
                return true;
            }
            String text = ChatColor.translateAlternateColorCodes('&',
                    String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            while (lore.size() <= index) lore.add("");
            lore.set(index, text);
            meta.setLore(lore);
            hand.setItemMeta(meta);
            languages.send(player, "item.itemlore.set",
                    Placeholders.with("index", index + 1, "line", text));
            return true;
        }
        // bare /itemlore <text> appends
        String text = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));
        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        lore.add(text);
        meta.setLore(lore);
        hand.setItemMeta(meta);
        languages.send(player, "item.itemlore.added", Placeholders.with("line", text));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("add", "set", "clear"));
            return options;
        }
        return Collections.emptyList();
    }
}
