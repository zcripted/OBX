package dev.zcripted.obx.command.utility;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VitalCommand extends AbstractObxCommand implements TabCompleter {


    public VitalCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.vital")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        Player target = null;
        if (args.length > 0) {
            target = resolvePlayer(args[0]);
            if (target == null) {
                languages.send(sender, "player.vital.target-not-found", Placeholders.with("player", args[0]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            languages.send(sender, "player.vital.usage-console");
            return true;
        }

        GameMode mode = target.getGameMode();
        if (mode == GameMode.SPECTATOR || mode == GameMode.CREATIVE) {
            if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
                languages.send(sender, "player.vital.invalid-gamemode");
            } else {
                languages.send(sender, "player.vital.invalid-gamemode-other", Placeholders.with("target", target.getName()));
            }
            return true;
        }

        double maxHealth = target.getHealth();
        try {
            AttributeInstance attribute = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attribute != null) {
                maxHealth = attribute.getValue();
            }
        } catch (NoSuchMethodError ignored) {
            maxHealth = target.getMaxHealth();
        }

        target.setHealth(maxHealth);
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setFireTicks(0);

        boolean self = sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());
        Map<String, String> placeholders = Placeholders.with("target", target.getName(), "sender", sender.getName());
        if (self) {
            languages.send(target, "player.vital.success");
        } else {
            languages.send(sender, "player.vital.success-other", placeholders);
            languages.send(target, "player.vital.success-target", placeholders);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String current = args[0].toLowerCase(Locale.ENGLISH);
        List<String> suggestions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (name.toLowerCase(Locale.ENGLISH).startsWith(current)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }

    private Player resolvePlayer(String name) {
        if (name == null) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }
}
