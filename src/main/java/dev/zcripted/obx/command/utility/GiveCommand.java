package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GiveCommand extends AbstractObxCommand implements TabCompleter {


    public GiveCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.give")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "item.give.usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            languages.send(sender, "teleport.tp.not-online", Placeholders.with("player", args[0]));
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

        // Safeguard: only hand over what fits in the target's combined hotbar +
        // storage (the 36 main inventory slots). addItem() fills what fits and
        // returns the remainder, which we discard (never drop) and warn about.
        ItemStack stack = new ItemStack(material, amount);
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(stack);
        int notGiven = 0;
        if (overflow != null) {
            for (ItemStack leftover : overflow.values()) {
                if (leftover != null) notGiven += leftover.getAmount();
            }
        }
        int given = amount - notGiven;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("amount", String.valueOf(given));
        placeholders.put("requested", String.valueOf(amount));
        placeholders.put("notGiven", String.valueOf(notGiven));
        placeholders.put("material", material.name());

        if (given <= 0) {
            // Nothing fit — inventory is already full for this material.
            languages.send(sender, "item.give.full", placeholders);
            return true;
        }
        languages.send(sender, "item.give.sent", placeholders);
        if (notGiven > 0) {
            // Partial give: warn the giver that the rest didn't fit.
            languages.send(sender, "item.give.partial", placeholders);
        }
        return true;
    }

    /** Handy preset amounts offered for the [amount] argument; any value still works. */
    private static final String[] AMOUNT_SUGGESTIONS = {"1", "16", "32", "64"};

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
        if (args.length == 3) {
            List<String> amounts = new ArrayList<>();
            String prefix = args[2];
            for (String suggestion : AMOUNT_SUGGESTIONS) {
                if (suggestion.startsWith(prefix)) amounts.add(suggestion);
            }
            // Offer the item's own max stack size too (e.g. 16 for ender pearls).
            try {
                int max = Material.valueOf(args[1].toUpperCase()).getMaxStackSize();
                String maxStr = String.valueOf(max);
                if (maxStr.startsWith(prefix) && !amounts.contains(maxStr)) amounts.add(maxStr);
            } catch (IllegalArgumentException ignored) {
                // unknown material typed so far — just offer the presets
            }
            return amounts;
        }
        return Collections.emptyList();
    }
}
