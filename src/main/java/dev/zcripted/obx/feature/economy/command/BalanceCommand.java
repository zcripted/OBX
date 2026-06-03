package dev.zcripted.obx.feature.economy.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BalanceCommand extends AbstractObxCommand implements TabCompleter {

    private final EconomyService economy;

    public BalanceCommand(OBX plugin) {
        super(plugin);
        this.economy = plugin.getEconomyService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.balance")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length >= 1) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target.getName() == null && target.getFirstPlayed() == 0L) {
                languages.send(sender, "economy.unknown-player", Placeholders.with("player", args[0]));
                return true;
            }
            double balance = economy.getBalance(target.getUniqueId());
            languages.send(sender, "economy.balance.other",
                    Placeholders.with("player", target.getName() == null ? args[0] : target.getName(),
                            "amount", economy.format(balance)));
            return true;
        }
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return true;
        }
        Player player = (Player) sender;
        double balance = economy.getBalance(player.getUniqueId());
        languages.send(player, "economy.balance.self", Placeholders.with("amount", economy.format(balance)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) names.add(online.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
