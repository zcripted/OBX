package dev.zcripted.obx.feature.item.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;

import java.util.Map;

/**
 * Gives the player a freshly-rendered map centered on their current position —
 * a "you are here" view of the surrounding terrain. The map is created with
 * {@link Bukkit#createMap(World)}, re-centered on the player at the closest
 * (1 block : 1 pixel) scale, then handed over. Binding the {@link MapView} to
 * the item is version-sensitive: modern servers use {@code MapMeta#setMapView}
 * /{@code #setMapId} (resolved reflectively, since neither exists in the 1.12.2
 * compile API), while legacy servers fall back to the map-id durability.
 *
 * <p>Note: tiles render client-side as the player holds the map and the
 * surrounding chunks are loaded, so a fresh map fills in over the first moment
 * it is held rather than instantly.
 */
public final class MapCommand extends AbstractObxCommand {


    public MapCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        if (!sender.hasPermission("obx.map")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        Player player = (Player) sender;
        try {
            World world = player.getWorld();
            MapView view = Bukkit.createMap(world);
            if (view == null) {
                languages.send(player, "utility.map.unsupported");
                return true;
            }
            view.setCenterX(player.getLocation().getBlockX());
            view.setCenterZ(player.getLocation().getBlockZ());
            try {
                view.setScale(MapView.Scale.CLOSEST);
            } catch (Throwable ignoredScale) {
                // Scale enum present on all supported versions; guard anyway.
            }
            openInHand(player, buildMapItem(view));
            languages.send(player, "utility.map.opened");
        } catch (Throwable throwable) {
            languages.send(player, "utility.map.unsupported");
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack buildMapItem(MapView view) {
        Material material = Material.matchMaterial("FILLED_MAP");
        if (material == null) {
            material = Material.matchMaterial("MAP");
        }
        ItemStack item = new ItemStack(material == null ? Material.MAP : material);
        int id = mapId(view);
        boolean bound = false;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            try {
                meta.getClass().getMethod("setMapView", MapView.class).invoke(meta, view);
                bound = true;
            } catch (Throwable noSetView) {
                // Pre-1.14 server — try the 1.13 map-id setter next.
            }
            if (!bound) {
                try {
                    meta.getClass().getMethod("setMapId", int.class).invoke(meta, id);
                    bound = true;
                } catch (Throwable noSetId) {
                    // Pre-1.13 server — fall through to the durability path.
                }
            }
            if (bound) {
                item.setItemMeta(meta);
            }
        }
        if (!bound) {
            item.setDurability((short) id);
        }
        return item;
    }

    private static int mapId(MapView view) {
        try {
            Object result = MapView.class.getMethod("getId").invoke(view);
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
        } catch (Throwable ignored) {
            // getId returns short on legacy and int on modern; reflection covers both.
        }
        return 0;
    }

    /**
     * "Opens" the map by placing it into the player's held hand so it renders on screen
     * immediately. Prefers the currently-held hand if empty, else an empty hotbar slot
     * (switching the selection to it), else stashes/drops it without destroying any item.
     */
    private static void openInHand(Player player, ItemStack item) {
        PlayerInventory inv = player.getInventory();
        int held = inv.getHeldItemSlot();
        ItemStack current = inv.getItem(held);
        if (current == null || current.getType() == Material.AIR) {
            inv.setItem(held, item);
            return;
        }
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack at = inv.getItem(slot);
            if (at == null || at.getType() == Material.AIR) {
                inv.setItem(slot, item);
                inv.setHeldItemSlot(slot);
                return;
            }
        }
        Map<Integer, ItemStack> overflow = inv.addItem(item);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
}