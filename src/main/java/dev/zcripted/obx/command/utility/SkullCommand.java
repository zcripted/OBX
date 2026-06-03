package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkullCommand extends AbstractObxCommand implements TabCompleter {


    public SkullCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.skull")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length < 1) {
            languages.send(player, "inventory.skull.usage");
            return true;
        }
        Material skullMaterial = resolveSkullMaterial();
        if (skullMaterial == null) {
            languages.send(player, "inventory.skull.unsupported");
            return true;
        }
        ItemStack skull = new ItemStack(skullMaterial, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            applyOwner(meta, args[0]);
            meta.setDisplayName("§e" + args[0] + "'s Head");
            skull.setItemMeta(meta);
        }
        player.getInventory().addItem(skull);
        languages.send(player, "inventory.skull.given", Placeholders.with("player", args[0]));
        return true;
    }

    private Material resolveSkullMaterial() {
        // 1.13+ has PLAYER_HEAD; 1.12 has SKULL_ITEM. Match by name to dodge the rename.
        try {
            return Material.valueOf("PLAYER_HEAD");
        } catch (IllegalArgumentException ignored) {
            try {
                return Material.valueOf("SKULL_ITEM");
            } catch (IllegalArgumentException ignored2) {
                return null;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void applyOwner(SkullMeta meta, String name) {
        try {
            // 1.13+ exposes setOwningPlayer(OfflinePlayer).
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            meta.getClass().getMethod("setOwningPlayer", OfflinePlayer.class).invoke(meta, offline);
        } catch (Exception modernMissing) {
            meta.setOwner(name);
        }
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
        return Collections.emptyList();
    }
}
