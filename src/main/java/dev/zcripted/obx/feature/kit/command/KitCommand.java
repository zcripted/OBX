package dev.zcripted.obx.feature.kit.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.feature.kit.model.Kit;
import dev.zcripted.obx.feature.kit.service.KitService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KitCommand extends AbstractObxCommand implements TabCompleter {

    private final KitService kitService;

    public KitCommand(OBX plugin) {
        super(plugin);
        this.kitService = plugin.getKitService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("list")) return list(sender);
            if (sub.equals("info")) return info(sender, args);
            if (sub.equals("give")) return give(sender, args);
            if (sub.equals("reload")) return reload(sender);
        }
        if (!(sender instanceof Player)) {
            return list(sender);
        }
        Player player = (Player) sender;
        if (!player.hasPermission("obx.kit")) {
            languages.send(player, "core.no-permission");
            return true;
        }
        if (args.length == 0) {
            return list(player);
        }
        Kit kit = kitService.getKit(args[0]);
        if (kit == null) {
            languages.send(player, "kit.not-found", Placeholders.with("kit", args[0]));
            return true;
        }
        return claim(player, kit, false);
    }

    private boolean list(CommandSender sender) {
        if (!sender.hasPermission("obx.kit.list") && !sender.hasPermission("obx.kit")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        java.util.Collection<Kit> all = kitService.getKits();
        if (all.isEmpty()) {
            languages.send(sender, "kit.list.empty");
            return true;
        }
        List<String> names = new ArrayList<>();
        for (Kit kit : all) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission("obx.kit." + kit.getName().toLowerCase())
                        && !player.hasPermission("obx.kit.*")) {
                    continue;
                }
            }
            names.add(kit.getDisplayName());
        }
        if (names.isEmpty()) {
            languages.send(sender, "kit.list.empty");
            return true;
        }
        languages.send(sender, "kit.list.header", Placeholders.with("count", names.size()));
        languages.send(sender, "kit.list.entries", Placeholders.with("kits", String.join(", ", names)));
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (!sender.hasPermission("obx.kit.info")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 2) {
            languages.send(sender, "kit.info-usage");
            return true;
        }
        Kit kit = kitService.getKit(args[1]);
        if (kit == null) {
            languages.send(sender, "kit.not-found", Placeholders.with("kit", args[1]));
            return true;
        }
        languages.send(sender, "kit.info.header", Placeholders.with("kit", kit.getDisplayName()));
        languages.send(sender, "kit.info.cooldown", Placeholders.with("seconds", kit.getCooldownSeconds()));
        StringBuilder items = new StringBuilder();
        int i = 0;
        for (ItemStack stack : kit.getItems()) {
            if (i++ > 0) items.append(", ");
            items.append(stack.getAmount()).append("x ").append(stack.getType().name());
        }
        languages.send(sender, "kit.info.items", Placeholders.with("items", items.length() == 0 ? "—" : items.toString()));
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("obx.kit.give")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length < 3) {
            languages.send(sender, "kit.give-usage");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            languages.send(sender, "teleport.tp.not-online", Placeholders.with("player", args[1]));
            return true;
        }
        Kit kit = kitService.getKit(args[2]);
        if (kit == null) {
            languages.send(sender, "kit.not-found", Placeholders.with("kit", args[2]));
            return true;
        }
        boolean given = claim(target, kit, true);
        if (given) {
            languages.send(sender, "kit.given-staff",
                    Placeholders.with("kit", kit.getDisplayName(), "player", target.getName()));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("obx.kit.reload")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        kitService.reload();
        languages.send(sender, "kit.reloaded");
        return true;
    }

    private boolean claim(Player player, Kit kit, boolean bypassCooldown) {
        if (!bypassCooldown) {
            if (!player.hasPermission("obx.kit." + kit.getName().toLowerCase())
                    && !player.hasPermission("obx.kit.*")) {
                languages.send(player, "kit.no-permission", Placeholders.with("kit", kit.getDisplayName()));
                return false;
            }
            long remaining = kitService.getCooldownRemaining(player.getUniqueId(), kit);
            if (remaining > 0) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("kit", kit.getDisplayName());
                placeholders.put("seconds", String.valueOf(remaining));
                languages.send(player, "kit.cooldown", placeholders);
                return false;
            }
        }
        Map<String, Object> result = kitService.giveItems(player, kit);
        languages.send(player, "kit.claimed", Placeholders.with("kit", kit.getDisplayName()));
        Object dropped = result.get("droppedCount");
        if (dropped instanceof Integer && (Integer) dropped > 0) {
            languages.send(player, "kit.overflow", Placeholders.with("count", dropped));
        }
        if (!bypassCooldown) {
            kitService.markUsed(player.getUniqueId(), kit);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("list");
            if (sender.hasPermission("obx.kit.info")) options.add("info");
            if (sender.hasPermission("obx.kit.give")) options.add("give");
            if (sender.hasPermission("obx.kit.reload")) options.add("reload");
            for (Kit kit : kitService.getKits()) {
                if (sender instanceof Player && !((Player) sender).hasPermission("obx.kit." + kit.getName().toLowerCase())
                        && !((Player) sender).hasPermission("obx.kit.*")) continue;
                options.add(kit.getName());
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("give"))) {
            if (args[0].equalsIgnoreCase("info")) {
                List<String> kits = new ArrayList<>();
                for (Kit kit : kitService.getKits()) kits.add(kit.getName());
                return filter(kits, args[1]);
            }
            // give -> player
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) names.add(online.getName());
            return filter(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> kits = new ArrayList<>();
            for (Kit kit : kitService.getKits()) kits.add(kit.getName());
            return filter(kits, args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String value : options) {
            if (value.toLowerCase().startsWith(lower)) matches.add(value);
        }
        return matches;
    }
}
