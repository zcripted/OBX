package dev.zcripted.obx.feature.backpack.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.command.AbstractObxCommand;
import dev.zcripted.obx.feature.backpack.service.BackpackService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /backpack} (aliases {@code /bp}, {@code /pack}) — opens and manages the player's
 * portable 3-row storage:
 *
 * <ul>
 *   <li>{@code /backpack} — opens the backpack (virtual mode), or requires the physical
 *       item to be carried (physical mode).</li>
 *   <li>{@code /backpack convert} — turns the virtual backpack into a physical item.</li>
 *   <li>{@code /backpack respawn} — re-issues a lost/destroyed physical backpack; rotates
 *       the instance token so every older copy becomes void (dupe-guard).</li>
 *   <li>{@code /backpack virtual} — converts back to command-opened virtual storage.</li>
 * </ul>
 */
public class BackpackCommand extends AbstractObxCommand implements TabCompleter {

    private final BackpackService backpacks;

    public BackpackCommand(ObxPlugin plugin) {
        super(plugin);
        this.backpacks = plugin.getServiceRegistry().get(BackpackService.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!requirePermission(player, "obx.backpack.use")) {
            return true;
        }
        if (!backpacks.isStorageAvailable()) {
            // Without the database the dupe-guard and persistence can't function —
            // refuse rather than risk losing whatever the player stores.
            languages.send(player, "backpack.storage-unavailable");
            return true;
        }
        String sub = args.length == 0 ? "open" : args[0].toLowerCase(Locale.ENGLISH);
        switch (sub) {
            case "open":
                handleOpen(player);
                return true;
            case "convert":
                handleConvert(player);
                return true;
            case "respawn":
                handleRespawn(player);
                return true;
            case "virtual":
            case "store":
                handleVirtual(player);
                return true;
            default:
                languages.send(player, "backpack.usage");
                return true;
        }
    }

    private void handleOpen(Player player) {
        if (backpacks.isPhysical(player.getUniqueId())) {
            // Physical mode: the item is the key. Opening still works via right-click;
            // the command only opens when the valid item is actually carried.
            if (backpacks.hasValidItem(player)) {
                backpacks.open(player);
            } else {
                languages.send(player, "backpack.physical-hint");
            }
            return;
        }
        backpacks.open(player);
    }

    private void handleConvert(Player player) {
        if (!requirePermission(player, "obx.backpack.convert")) {
            return;
        }
        if (backpacks.isPhysical(player.getUniqueId())) {
            languages.send(player, "backpack.already-physical");
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            languages.send(player, "backpack.inventory-full");
            return;
        }
        backpacks.setPhysical(player.getUniqueId(), true);
        String token = backpacks.rotateToken(player.getUniqueId());
        player.getInventory().addItem(backpacks.createItem(player, token));
        languages.send(player, "backpack.converted");
    }

    private void handleRespawn(Player player) {
        if (!requirePermission(player, "obx.backpack.respawn")) {
            return;
        }
        if (!backpacks.isPhysical(player.getUniqueId())) {
            languages.send(player, "backpack.already-virtual");
            return;
        }
        // Space check before any mutation: a carried (old) copy will be purged below and
        // frees its own slot, so space is only required when no copy is carried at all.
        boolean carriesAnyCopy = backpacks.hasValidItem(player) || carriesOwnTaggedItem(player);
        if (!carriesAnyCopy && player.getInventory().firstEmpty() == -1) {
            languages.send(player, "backpack.inventory-full");
            return;
        }
        // Rotate FIRST so every existing copy (carried, stashed, duped) is void, then purge
        // the now-stale copies from this inventory and hand over the single fresh item.
        String token = backpacks.rotateToken(player.getUniqueId());
        backpacks.removeStaleItems(player);
        player.getInventory().addItem(backpacks.createItem(player, token));
        languages.send(player, "backpack.respawned");
    }

    private void handleVirtual(Player player) {
        if (!requirePermission(player, "obx.backpack.convert")) {
            return;
        }
        if (!backpacks.isPhysical(player.getUniqueId())) {
            languages.send(player, "backpack.already-virtual");
            return;
        }
        backpacks.setPhysical(player.getUniqueId(), false);
        // Rotating voids every physical copy; the purge then deletes carried ones.
        backpacks.rotateToken(player.getUniqueId());
        backpacks.removeStaleItems(player);
        languages.send(player, "backpack.virtualized");
    }

    private boolean carriesOwnTaggedItem(Player player) {
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && player.getUniqueId().equals(backpacks.itemOwner(item))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<>();
        for (String option : Arrays.asList("open", "convert", "respawn", "virtual")) {
            if (option.startsWith(args[0].toLowerCase(Locale.ENGLISH))) {
                matches.add(option);
            }
        }
        return matches;
    }
}
