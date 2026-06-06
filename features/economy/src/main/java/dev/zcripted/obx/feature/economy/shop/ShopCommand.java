package dev.zcripted.obx.feature.economy.shop;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.command.AbstractObxCommand;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /shop} (alias {@code /market}) — opens the categorized buy/sell shop GUI.
 *
 * <ul>
 *   <li>{@code /shop} — the main category menu ({@code obx.shop}).</li>
 *   <li>{@code /shop <category>} — jump straight into a category.</li>
 *   <li>{@code /shop sell} — the dump-and-close sell inventory ({@code obx.shop.sell}).</li>
 *   <li>{@code /shop reload} — re-read shop.yml + shops/*.yml ({@code obx.shop.admin}).</li>
 * </ul>
 */
public class ShopCommand extends AbstractObxCommand implements TabCompleter {

    public ShopCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!requirePermission(sender, "obx.shop.admin")) {
                return true;
            }
            ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
            if (shop != null) {
                shop.reload();
                languages.send(sender, "shop.reloaded",
                        Placeholders.with("count", shop.getCategories().size()));
            }
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!requirePermission(player, "obx.shop")) {
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("sell")) {
            if (!requirePermission(player, "obx.shop.sell")) {
                return true;
            }
            ShopMenu.openSellGui(plugin, player);
            return true;
        }
        if (args.length >= 1) {
            ShopMenu.openCategory(plugin, player, args[0].toLowerCase(Locale.ENGLISH), 0);
            return true;
        }
        ShopMenu.openMain(plugin, player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> options = new ArrayList<>();
        options.add("sell");
        if (sender.hasPermission("obx.shop.admin")) {
            options.add("reload");
        }
        ShopService shop = plugin.getServiceRegistry().get(ShopService.class);
        if (shop != null) {
            for (ShopService.ShopCategory category : shop.getCategories()) {
                options.add(category.id());
            }
        }
        String prefix = args[0].toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
