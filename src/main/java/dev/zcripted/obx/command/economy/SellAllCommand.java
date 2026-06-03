package dev.zcripted.obx.command.economy;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class SellAllCommand extends AbstractObxCommand implements TabCompleter {

    private final SellCommand delegate;

    public SellAllCommand(Main plugin) {
        super(plugin);
        this.delegate = new SellCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Sell-all defers all guards (permission + player-only) to /sell — both
        // obx.sell and obx.sellall are accepted via the wildcard bundle.
        return delegate.onCommand(sender, command, "sell", new String[]{"all"});
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
