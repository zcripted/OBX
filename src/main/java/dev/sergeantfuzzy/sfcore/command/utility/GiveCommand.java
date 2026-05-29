package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
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

public class GiveCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;

    public GiveCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.give")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "item.give.usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            languages.send(sender, "tpa.target-not-online", Placeholders.with("player", args[0]));
            return true;
        }
        Material material;
        try { material = Material.valueOf(args[1].toUpperCase()); }
        catch (IllegalArgumentException ignored) {
            languages.send(sender, "item.unknown-material", Placeholders.with("material", args[1]));
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            try { amount = Math.max(1, Integer.parseInt(args[2])); }
            catch (NumberFormatException ignored) {
                languages.send(sender, "item.give.invalid-amount", Placeholders.with("input", args[2]));
                return true;
            }
        }
        ItemStack stack = new ItemStack(material, amount);
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(stack);
        int dropped = 0;
        if (overflow != null && !overflow.isEmpty()) {
            for (ItemStack leftover : overflow.values()) {
                if (leftover == null) continue;
                target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                dropped += leftover.getAmount();
            }
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("material", material.name());
        languages.send(sender, "item.give.sent", placeholders);
        if (dropped > 0) {
            languages.send(target, "item.give.overflow", Placeholders.with("count", dropped));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        if (args.length == 2) {
            List<String> mats = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Material m : Material.values()) {
                if (m.name().toLowerCase().startsWith(prefix)) mats.add(m.name());
                if (mats.size() >= 30) break;
            }
            return mats;
        }
        return Collections.emptyList();
    }
}
