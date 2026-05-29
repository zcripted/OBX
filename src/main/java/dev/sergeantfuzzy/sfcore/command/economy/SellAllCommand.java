package dev.sergeantfuzzy.sfcore.command.economy;

import dev.sergeantfuzzy.sfcore.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class SellAllCommand implements CommandExecutor, TabCompleter {

    private final SellCommand delegate;

    public SellAllCommand(Main plugin) {
        this.delegate = new SellCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Sell-all defers all guards (permission + player-only) to /sell — both
        // sfcore.sell and sfcore.sellall are accepted via the wildcard bundle.
        return delegate.onCommand(sender, command, "sell", new String[]{"all"});
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
