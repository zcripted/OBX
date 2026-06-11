package dev.zcripted.obx.feature.item.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BookCommand extends AbstractObxCommand implements TabCompleter {


    public BookCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.book")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        String sub = args.length == 0 ? "new" : args[0].toLowerCase();
        switch (sub) {
            case "new":
                return newBook(player);
            case "unsign":
                return unsign(player);
            case "copy":
                return copy(player);
            default:
                languages.send(player, "item.book.usage");
                return true;
        }
    }

    private boolean newBook(Player player) {
        Material writable = resolveWritable();
        if (writable == null) {
            languages.send(player, "item.book.unsupported");
            return true;
        }
        player.getInventory().addItem(new ItemStack(writable, 1));
        languages.send(player, "item.book.received");
        return true;
    }

    private boolean unsign(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || !isWrittenBook(hand.getType())) {
            languages.send(player, "item.book.not-signed");
            return true;
        }
        Material writable = resolveWritable();
        if (writable == null) {
            languages.send(player, "item.book.unsupported");
            return true;
        }
        BookMeta source = (BookMeta) hand.getItemMeta();
        ItemStack replacement = new ItemStack(writable, hand.getAmount());
        BookMeta meta = (BookMeta) replacement.getItemMeta();
        if (meta != null && source != null) {
            meta.setPages(source.getPages());
            meta.setTitle(source.getTitle());
            meta.setAuthor(source.getAuthor());
            replacement.setItemMeta(meta);
        }
        player.getInventory().setItemInMainHand(replacement);
        languages.send(player, "item.book.unsigned");
        return true;
    }

    private boolean copy(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR
                || !(hand.getItemMeta() instanceof BookMeta)) {
            languages.send(player, "item.book.not-book");
            return true;
        }
        ItemStack copy = hand.clone();
        copy.setAmount(1);
        player.getInventory().addItem(copy);
        languages.send(player, "item.book.copied");
        return true;
    }

    private Material resolveWritable() {
        try { return Material.valueOf("WRITABLE_BOOK"); }
        catch (IllegalArgumentException ignored) {
            try { return Material.valueOf("BOOK_AND_QUILL"); }
            catch (IllegalArgumentException ignored2) { return null; }
        }
    }

    private boolean isWrittenBook(Material material) {
        if (material == null) return false;
        try { return material == Material.valueOf("WRITTEN_BOOK"); }
        catch (IllegalArgumentException ignored) { return false; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(Arrays.asList("new", "unsign", "copy"));
        }
        return Collections.emptyList();
    }
}