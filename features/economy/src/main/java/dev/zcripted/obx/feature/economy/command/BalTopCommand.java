package dev.zcripted.obx.feature.economy.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.api.economy.EconomyService;
import dev.zcripted.obx.feature.economy.sink.WeeklyTopService;
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

    public BalTopCommand(ObxPlugin plugin) {
        super(plugin);
        this.economy = plugin.getEconomyService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.baltop")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("weekly")) {
            showWeekly(sender);
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

    private void showWeekly(CommandSender sender) {
        WeeklyTopService weekly = plugin.getServiceRegistry().get(WeeklyTopService.class);
        if (weekly == null) {
            languages.send(sender, "economy.baltop.weekly.unavailable");
            return;
        }
        String weekKey = WeeklyTopService.weekKey();
        List<WeeklyTopService.WeeklyEntry> entries = weekly.getWeek(weekKey);
        if (entries.isEmpty()) {
            languages.send(sender, "economy.baltop.weekly.empty",
                    java.util.Collections.singletonMap("week", weekKey));
            return;
        }
        languages.send(sender, "economy.baltop.weekly.header",
                Placeholders.with("week", weekKey, "page", 1, "totalPages", 1));
        for (WeeklyTopService.WeeklyEntry entry : entries) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("rank", String.valueOf(entry.rank()));
            placeholders.put("player", entry.playerName() == null ? "?" : entry.playerName());
            placeholders.put("amount", economy.format(entry.balance()));
            languages.send(sender, "economy.baltop.entry", placeholders);
        }
        languages.send(sender, "economy.baltop.weekly.footer",
                java.util.Collections.singletonMap("week", weekKey));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> matches = new java.util.ArrayList<>();
        if ("weekly".startsWith(prefix)) matches.add("weekly");
        for (String page : new String[]{"1", "2", "3", "4", "5"}) {
            if (page.startsWith(prefix)) matches.add(page);
        }
        return matches;
    }
}