package dev.zcripted.obx.feature.economy.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BalTopCommand extends AbstractObxCommand implements TabCompleter {

    private static final int PER_PAGE = 10;

    private final EconomyService economy;

    public BalTopCommand(OBX plugin) {
        super(plugin);
        this.economy = plugin.getEconomyService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.baltop")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        int page = 1;
        if (args.length >= 1) {
            try { page = Math.max(1, Integer.parseInt(args[0])); } catch (NumberFormatException ignored) {}
        }
        List<EconomyService.BalanceEntry> all = economy.topBalances(0);
        if (all.isEmpty()) {
            languages.send(sender, "economy.baltop.empty");
            return true;
        }
        int totalPages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, totalPages);
        int start = (page - 1) * PER_PAGE;
        int end = Math.min(all.size(), start + PER_PAGE);
        languages.send(sender, "economy.baltop.header",
                Placeholders.with("page", page, "totalPages", totalPages));
        for (int i = start; i < end; i++) {
            EconomyService.BalanceEntry entry = all.get(i);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("rank", String.valueOf(i + 1));
            placeholders.put("player", entry.getName());
            placeholders.put("amount", economy.format(entry.getBalance()));
            languages.send(sender, "economy.baltop.entry", placeholders);
        }
        languages.send(sender, "economy.baltop.footer",
                Placeholders.with("page", page, "totalPages", totalPages));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> matches = new java.util.ArrayList<>();
        for (String page : new String[]{"1", "2", "3", "4", "5"}) {
            if (page.startsWith(prefix)) matches.add(page);
        }
        return matches;
    }
}
