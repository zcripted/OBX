package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;

/**
 * Opens a virtual smithing table. The smithing table inventory type was added
 * in 1.16, and the dedicated {@code Player.openSmithingTable} API only landed
 * on Paper 1.19+, so this command tries (in order):
 *
 * <ol>
 *   <li>The Paper {@code Player.openSmithingTable(Location, boolean)} method.</li>
 *   <li>{@link Bukkit#createInventory(org.bukkit.inventory.InventoryHolder, InventoryType, String)}
 *       with {@link InventoryType#SMITHING}, opened directly.</li>
 *   <li>A "feature unavailable" language message on legacy servers.</li>
 * </ol>
 */
public class SmithCommand extends AbstractObxCommand {


    public SmithCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        if (!sender.hasPermission("obx.smith")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        Player player = (Player) sender;
        if (tryPaperOpen(player)) {
            languages.send(player, "utility.smith.opened");
            return true;
        }
        if (tryFakeInventoryOpen(player)) {
            languages.send(player, "utility.smith.opened");
            return true;
        }
        languages.send(player, "utility.smith.unsupported");
        return true;
    }

    private static boolean tryPaperOpen(Player player) {
        try {
            Method openSmithing = player.getClass().getMethod("openSmithingTable", Location.class, boolean.class);
            openSmithing.invoke(player, null, true);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryFakeInventoryOpen(Player player) {
        try {
            InventoryType type;
            try {
                type = InventoryType.valueOf("SMITHING");
            } catch (IllegalArgumentException missingType) {
                return false;
            }
            Inventory smithing = Bukkit.createInventory(player, type, "Smithing");
            player.openInventory(smithing);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
