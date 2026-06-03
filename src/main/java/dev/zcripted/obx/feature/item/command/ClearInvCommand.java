package dev.zcripted.obx.feature.item.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clears an inventory, optionally only a specific item up to a maximum count —
 * mirroring vanilla {@code /clear [targets] [item] [maxCount]} so the OBX command
 * can fully replace it (see {@code CommandOverrideListener}). The summary chat
 * message reports the quantity removed in parentheses and carries a hover tooltip
 * listing the total cleared plus the five most recently held item stacks.
 */
public class ClearInvCommand extends AbstractObxCommand implements TabCompleter {

    /** How many cleared item stacks to surface in the hover tooltip. */
    private static final int HOVER_RECENT_LIMIT = 5;


    public ClearInvCommand(OBX plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("obx.clearinv.others")) {
                languages.send(sender, "core.no-permission");
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                languages.send(sender, "teleport.tp.not-online", Placeholders.with("player", args[0]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                languages.send(sender, "core.player-only");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("obx.clearinv")) {
                languages.send(target, "core.no-permission");
                return true;
            }
        }

        // Optional item filter (arg 1) — accepts "dirt" or "minecraft:dirt".
        Material filter = null;
        if (args.length >= 2) {
            filter = Material.matchMaterial(args[1]);
            if (filter == null) {
                languages.send(sender, "item.unknown-material", Placeholders.with("material", args[1]));
                return true;
            }
        }
        // Optional max count (arg 2). -1 = remove all matching; 0 = count only (vanilla parity).
        int maxCount = -1;
        if (args.length >= 3) {
            try {
                maxCount = Math.max(0, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
                languages.send(sender, "item.give.invalid-amount", Placeholders.with("input", args[2]));
                return true;
            }
        }

        ClearResult result = clear(target.getInventory(), filter, maxCount);

        List<String> hover = buildHover(sender, result);
        boolean self = sender.equals(target);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(result.total));
        placeholders.put("player", target.getName());

        if (self) {
            sendWithHover(target, "inventory.clearinv.self", placeholders, hover);
        } else {
            sendWithHover(sender, "inventory.clearinv.other", placeholders, hover);
            // Notify the target too (no hover — it's the giver who wants the breakdown).
            languages.send(target, "inventory.clearinv.self", Placeholders.with("count", String.valueOf(result.total)));
        }
        return true;
    }

    /**
     * Removes items from {@code inv} and tallies what was taken. Scans slots in
     * reverse order so the tally's iteration order is "most recent first" (newer
     * items tend to occupy later hotbar / first-empty slots) — a best-effort proxy
     * for recency, documented as such since Bukkit exposes no real pickup order.
     */
    private ClearResult clear(PlayerInventory inv, Material filter, int maxCount) {
        ClearResult result = new ClearResult();
        ItemStack[] contents = inv.getContents();
        for (int i = contents.length - 1; i >= 0; i--) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
                continue;
            }
            if (filter != null && stack.getType() != filter) {
                continue;
            }

            int take;
            if (maxCount == 0) {
                // Count-only: tally the whole stack, remove nothing.
                take = 0;
                result.tally.merge(stack.getType(), stack.getAmount(), Integer::sum);
                result.total += stack.getAmount();
                continue;
            } else if (filter != null && maxCount > 0) {
                int remaining = maxCount - result.total;
                if (remaining <= 0) {
                    break;
                }
                take = Math.min(stack.getAmount(), remaining);
            } else {
                take = stack.getAmount();
            }

            if (take >= stack.getAmount()) {
                inv.setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - take);
                inv.setItem(i, stack);
            }
            result.total += take;
            result.tally.merge(filter != null ? filter : stack.getType(), take, Integer::sum);
        }
        return result;
    }

    private List<String> buildHover(CommandSender viewer, ClearResult result) {
        List<String> hover = new ArrayList<>();
        hover.add(languages.get(viewer, "inventory.clearinv.hover.title"));
        hover.add(languages.get(viewer, "core.divider-line"));
        Map<String, String> counts = new HashMap<>();
        counts.put("count", String.valueOf(result.total));
        counts.put("types", String.valueOf(result.tally.size()));
        hover.add(languages.get(viewer, "inventory.clearinv.hover.count", counts));
        if (result.tally.isEmpty()) {
            hover.add(languages.get(viewer, "inventory.clearinv.hover.empty"));
            return hover;
        }
        hover.add(languages.get(viewer, "inventory.clearinv.hover.recent"));
        int shown = 0;
        for (Map.Entry<Material, Integer> entry : result.tally.entrySet()) {
            if (shown >= HOVER_RECENT_LIMIT) {
                break;
            }
            Map<String, String> entryPlaceholders = new HashMap<>();
            entryPlaceholders.put("material", prettyName(entry.getKey()));
            entryPlaceholders.put("amount", String.valueOf(entry.getValue()));
            hover.add(languages.get(viewer, "inventory.clearinv.hover.entry", entryPlaceholders));
            shown++;
        }
        int remainingTypes = result.tally.size() - shown;
        if (remainingTypes > 0) {
            hover.add(languages.get(viewer, "inventory.clearinv.hover.more",
                    Placeholders.with("count", String.valueOf(remainingTypes))));
        }
        return hover;
    }

    private void sendWithHover(CommandSender recipient, String key, Map<String, String> placeholders, List<String> hover) {
        String message = languages.get(recipient, key, placeholders);
        ComponentMessenger.sendHoverMessage(recipient, message, hover, null);
    }

    /** Turns {@code DIAMOND_PICKAXE} into {@code Diamond Pickaxe} for the hover. */
    private static String prettyName(Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if (!sender.hasPermission("obx.clearinv.others")) {
                return Collections.emptyList();
            }
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
            List<String> counts = new ArrayList<>();
            String prefix = args[2];
            for (String suggestion : new String[]{"1", "16", "32", "64"}) {
                if (suggestion.startsWith(prefix)) counts.add(suggestion);
            }
            return counts;
        }
        return Collections.emptyList();
    }

    /** Accumulator for a clear operation: total quantity removed + per-type tally (recent-first). */
    private static final class ClearResult {
        private int total;
        private final LinkedHashMap<Material, Integer> tally = new LinkedHashMap<>();
    }
}
