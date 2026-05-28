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
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class UnbreakableCommand implements CommandExecutor, TabCompleter {

    private final LanguageManager languages;

    public UnbreakableCommand(Main plugin) {
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("sfcore.unbreakable")) {
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
            // 1.11+ exposes ItemMeta#isUnbreakable() directly.
            Method modern = meta.getClass().getMethod("isUnbreakable");
            Object result = modern.invoke(meta);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception ignored) {
            try {
                Object spigot = meta.getClass().getMethod("spigot").invoke(meta);
                Object result = spigot.getClass().getMethod("isUnbreakable").invoke(spigot);
                return result instanceof Boolean && (Boolean) result;
            } catch (Exception alsoIgnored) {
                return false;
            }
        }
    }

    private boolean applyUnbreakable(ItemMeta meta, boolean value) {
        try {
            Method modern = meta.getClass().getMethod("setUnbreakable", boolean.class);
            modern.invoke(meta, value);
            return true;
        } catch (Exception ignored) {
            try {
                Object spigot = meta.getClass().getMethod("spigot").invoke(meta);
                spigot.getClass().getMethod("setUnbreakable", boolean.class).invoke(spigot, value);
                return true;
            } catch (Exception alsoIgnored) {
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
