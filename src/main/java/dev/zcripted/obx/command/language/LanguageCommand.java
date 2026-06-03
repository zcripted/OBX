package dev.zcripted.obx.command.language;

import dev.zcripted.obx.command.AbstractObxCommand;

import dev.zcripted.obx.Main;
import dev.zcripted.obx.language.LanguageRegistry;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LanguageCommand extends AbstractObxCommand implements TabCompleter {

    private static final List<String> OPTIONS = Arrays.asList("English", "EN", "German", "DE", "Englisch", "Deutsch");


    public LanguageCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.language")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (!(sender instanceof Player)) {
            languages.send(sender, "language.console-only");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            LanguageRegistry current = languages.getLanguage(player.getUniqueId());
            languages.send(player, "language.current", Placeholders.with("language", current.displayName()));
            languages.send(player, "language.usage");
            return true;
        }
        LanguageRegistry target = LanguageRegistry.fromInput(args[0]);
        if (target == null) {
            languages.send(player, "language.invalid");
            languages.send(player, "language.usage");
            return true;
        }
        LanguageRegistry current = languages.getLanguage(player.getUniqueId());
        if (current == target) {
            languages.send(player, "language.already", Placeholders.with("language", target.displayName()));
            return true;
        }
        languages.setLanguage(player.getUniqueId(), target);
        languages.send(player, "language.changed", Placeholders.with("language", target.displayName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return OPTIONS;
        }
        String input = args[args.length - 1].toLowerCase(Locale.ENGLISH);
        return OPTIONS.stream()
                .filter(option -> option.toLowerCase(Locale.ENGLISH).startsWith(input))
                .collect(Collectors.toList());
    }
}
