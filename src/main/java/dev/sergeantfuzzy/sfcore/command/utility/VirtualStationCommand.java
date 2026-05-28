package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

/**
 * Opens a virtual workstation inventory — stonecutter, loom, grindstone, or
 * cartography table. These {@link InventoryType} constants were added in 1.14,
 * after this plugin's 1.12.2 compile target, so the type is resolved by name via
 * {@link InventoryType#valueOf(String)} and the menu opened directly with
 * {@link Bukkit#createInventory}. On servers older than 1.14 the constant is
 * absent and the player gets a "feature unavailable" message instead.
 */
public final class VirtualStationCommand implements CommandExecutor {

    /** One workstation: its inventory-type name, menu title, permission, and message-key prefix. */
    public enum Station {
        STONECUTTER("STONECUTTER", "Stonecutter", "sfcore.stonecut", "utility.stonecut"),
        LOOM("LOOM", "Loom", "sfcore.loom", "utility.loom"),
        GRINDSTONE("GRINDSTONE", "Grindstone", "sfcore.grindstone", "utility.grindstone"),
        CARTOGRAPHY("CARTOGRAPHY", "Cartography Table", "sfcore.cartography", "utility.cartography");

        private final String typeName;
        private final String title;
        private final String permission;
        private final String messageKey;

        Station(String typeName, String title, String permission, String messageKey) {
            this.typeName = typeName;
            this.title = title;
            this.permission = permission;
            this.messageKey = messageKey;
        }
    }

    private final LanguageManager languages;
    private final Station station;

    public VirtualStationCommand(Main plugin, Station station) {
        this.languages = plugin.getLanguageManager();
        this.station = station;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        if (!sender.hasPermission(station.permission)) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        Player player = (Player) sender;
        InventoryType type;
        try {
            type = InventoryType.valueOf(station.typeName);
        } catch (IllegalArgumentException missingType) {
            languages.send(player, station.messageKey + ".unsupported");
            return true;
        }
        try {
            Inventory inventory;
            try {
                inventory = Bukkit.createInventory(player, type, station.title);
            } catch (Throwable titledUnsupported) {
                // Some forks reject custom titles for these container types.
                inventory = Bukkit.createInventory(player, type);
            }
            player.openInventory(inventory);
            languages.send(player, station.messageKey + ".opened");
        } catch (Throwable throwable) {
            languages.send(player, station.messageKey + ".unsupported");
        }
        return true;
    }
}
