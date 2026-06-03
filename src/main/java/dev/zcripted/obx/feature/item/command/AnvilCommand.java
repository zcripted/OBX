package dev.zcripted.obx.feature.item.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;

/**
 * Opens a virtual anvil. Prefers the public {@code Player.openAnvil(Location, boolean)}
 * API added in Spigot 1.14, falling back to a {@link InventoryType#ANVIL} created
 * inventory on older platforms. The fake-inventory fallback won't perform real
 * repair calculations on legacy servers — operators on 1.13 and below get a
 * "feature unavailable" message instead of a non-functional UI.
 */
public class AnvilCommand extends AbstractObxCommand {


    public AnvilCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        if (!sender.hasPermission("obx.anvil")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        Player player = (Player) sender;
        if (tryNativeOpen(player)) {
            languages.send(player, "utility.anvil.opened");
            return true;
        }
        try {
            Inventory anvil = Bukkit.createInventory(player, InventoryType.ANVIL, "Anvil");
            player.openInventory(anvil);
            languages.send(player, "utility.anvil.opened");
        } catch (Throwable throwable) {
            languages.send(player, "utility.anvil.unsupported");
        }
        return true;
    }

    private static boolean tryNativeOpen(Player player) {
        try {
            Method openAnvil = player.getClass().getMethod("openAnvil", Location.class, boolean.class);
            openAnvil.invoke(player, null, true);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (Throwable invocationFailure) {
            return false;
        }
    }
}
