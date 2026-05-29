package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;

    public ItemCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.item")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "item.i.usage");
            return true;
        }
        Material material;
        try { material = Material.valueOf(args[0].toUpperCase()); }
        catch (IllegalArgumentException ignored) {
            languages.send(player, "item.unknown-material", Placeholders.with("material", args[0]));
            return true;
        }
        int amount = 1;
        if (args.length >= 2) {
            try { amount = Math.max(1, Integer.parseInt(args[1])); }
            catch (NumberFormatException ignored) {
                languages.send(player, "item.give.invalid-amount", Placeholders.with("input", args[1]));
                return true;
            }
        }
        ItemStack stack = new ItemStack(material, amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
        int dropped = 0;
        if (overflow != null && !overflow.isEmpty()) {
            for (ItemStack leftover : overflow.values()) {
                if (leftover == null) continue;
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                dropped += leftover.getAmount();
            }
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("material", material.name());
        languages.send(player, "item.i.received", placeholders);
        if (dropped > 0) {
            languages.send(player, "item.give.overflow", Placeholders.with("count", dropped));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> mats = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Material m : Material.values()) {
                if (m.name().toLowerCase().startsWith(prefix)) mats.add(m.name());
                if (mats.size() >= 30) break;
            }
            return mats;
        }
        return Collections.emptyList();
    }
}
