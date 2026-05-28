package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.List;

public class HatCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;

    public HatCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.hat")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack hand = inventory.getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            languages.send(player, "inventory.hat.empty-hand");
            return true;
        }
        ItemStack currentHelmet = inventory.getHelmet();
        inventory.setHelmet(hand.clone());
        inventory.setItemInMainHand(currentHelmet == null ? new ItemStack(Material.AIR) : currentHelmet);
        languages.send(player, "inventory.hat.applied");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
