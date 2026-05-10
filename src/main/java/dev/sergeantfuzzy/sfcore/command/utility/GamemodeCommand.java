package dev.sergeantfuzzy.sfcore.command.utility;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.language.LanguageRegistry;
import dev.sergeantfuzzy.sfcore.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GamemodeCommand implements CommandExecutor, TabCompleter {

    private static final Map<String, GameMode> MODE_ALIASES = createModeAliases();
    private static final Map<String, GameMode> LABEL_ALIASES = createLabelAliases();

    private final Main plugin;
    private final LanguageManager languages;

    public GamemodeCommand(Main plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sfcore.gamemode")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        GameMode labelMode = resolveLabelMode(label);
        if (labelMode != null) {
            String[] adjusted = new String[args.length + 1];
            adjusted[0] = labelMode.name().toLowerCase(Locale.ENGLISH);
            if (args.length > 0) {
                System.arraycopy(args, 0, adjusted, 1, args.length);
            }
            args = adjusted;
        }

        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                languages.send(player, "gamemode.current", Collections.singletonMap("mode", player.getGameMode().name()));
                languages.send(player, "gamemode.usage.self");
            } else {
                languages.send(sender, "gamemode.usage.console");
            }
            return true;
        }

        if (args.length == 1) {
            GameMode mode = resolveMode(args[0]);
            if (mode != null) {
                if (!(sender instanceof Player)) {
                    languages.send(sender, "gamemode.usage.console");
                    return true;
                }
                Player player = (Player) sender;
                if (!hasModePermission(sender, true, mode)) {
                    languages.send(sender, "core.no-permission");
                    return true;
                }
                changeMode(sender, player, mode);
                return true;
            }
            Player targetOnly = resolvePlayer(args[0]);
            if (targetOnly != null) {
                languages.send(sender, "gamemode.usage.target", Collections.singletonMap("target", targetOnly.getName()));
                return true;
            }
            languages.send(sender, "gamemode.unknown-mode-or-player");
            return true;
        }

        String first = args[0];
        String second = args[1];
        Player playerFirst = resolvePlayer(first);
        Player playerSecond = resolvePlayer(second);
        GameMode modeFirst = resolveMode(first);
        GameMode modeSecond = resolveMode(second);

        Player target = null;
        GameMode mode = null;

        if (modeFirst != null && playerSecond != null) {
            mode = modeFirst;
            target = playerSecond;
        } else if (playerFirst != null && modeSecond != null) {
            target = playerFirst;
            mode = modeSecond;
        } else if (playerFirst != null && modeFirst != null) {
            target = playerFirst;
            mode = modeSecond != null ? modeSecond : modeFirst;
        } else if (playerSecond != null && modeSecond != null) {
            target = playerSecond;
            mode = modeFirst != null ? modeFirst : modeSecond;
        } else if (modeFirst != null && playerSecond == null) {
            if (!(sender instanceof Player)) {
                languages.send(sender, "gamemode.usage.console");
                return true;
            }
            target = (Player) sender;
            mode = modeFirst;
        } else if (playerFirst != null) {
            languages.send(sender, "gamemode.usage.target", Collections.singletonMap("target", playerFirst.getName()));
            return true;
        }

        if (target == null || mode == null) {
            languages.send(sender, "gamemode.unknown-mode-or-player");
            return true;
        }

        boolean self = sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());
        if (!hasModePermission(sender, self, mode)) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        changeMode(sender, target, mode);
        return true;
    }

    private void changeMode(CommandSender sender, Player target, GameMode mode) {
        GameMode previous = target.getGameMode();
        if (previous == mode) {
            languages.send(sender, "gamemode.already", Placeholders.with("mode", mode.name()));
            return;
        }
        target.setGameMode(mode);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("mode", mode.name());
        placeholders.put("target", target.getName());
        placeholders.put("sender", sender.getName());
        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
            languages.send(target, "gamemode.changed-self", placeholders);
        } else {
            languages.send(sender, "gamemode.changed-other", placeholders);
            languages.send(target, "gamemode.changed-target", placeholders);
            String timestamp = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a (zzz) zzzz", java.util.Locale.ENGLISH)
                    .withZone(java.time.ZoneId.of("America/New_York"))
                    .format(java.time.Instant.now());
            String prefix = languages.getPrefix(LanguageRegistry.EN);
            String consoleLine = prefix
                    + ChatColor.YELLOW + "Gamemode change by " + ChatColor.GOLD + sender.getName()
                    + ChatColor.GRAY + " -> " + ChatColor.GOLD + target.getName()
                    + ChatColor.YELLOW + " from " + ChatColor.GOLD + previous.name()
                    + ChatColor.YELLOW + " to " + ChatColor.GOLD + mode.name()
                    + ChatColor.YELLOW + " @ " + ChatColor.GOLD + target.getWorld().getName()
                    + ChatColor.YELLOW + " at " + ChatColor.GOLD + timestamp;
            plugin.getServer().getConsoleSender().sendMessage(consoleLine);
        }
    }

    private boolean hasModePermission(CommandSender sender, boolean self, GameMode mode) {
        String base = self ? "sfcore.gamemode.self" : "sfcore.gamemode.others";
        String specific = base + "." + mode.name().toLowerCase(Locale.ENGLISH);
        if (sender.isPermissionSet(specific) || sender.getServer().getPluginManager().getPermission(specific) != null) {
            return sender.hasPermission(specific);
        }
        if (sender.hasPermission(specific)) {
            return true;
        }
        return sender.hasPermission(base);
    }

    private GameMode resolveMode(String input) {
        if (input == null) {
            return null;
        }
        return MODE_ALIASES.get(input.toLowerCase(Locale.ENGLISH));
    }

    private GameMode resolveLabelMode(String label) {
        if (label == null) {
            return null;
        }
        return LABEL_ALIASES.get(label.toLowerCase(Locale.ENGLISH));
    }

    private Player resolvePlayer(String input) {
        if (input == null) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(input);
        if (exact != null) {
            return exact;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(input)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        GameMode labelMode = resolveLabelMode(alias);
        if (labelMode != null) {
            if (args.length == 1) {
                String current = args[0].toLowerCase(Locale.ENGLISH);
                suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ENGLISH).startsWith(current))
                        .collect(Collectors.toList()));
            }
            return suggestions;
        }
        if (args.length == 1) {
            String current = args[0].toLowerCase(Locale.ENGLISH);
            suggestions.addAll(modeKeys().stream().filter(s -> s.startsWith(current)).collect(Collectors.toList()));
            suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ENGLISH).startsWith(current))
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            GameMode mode = resolveMode(args[0]);
            Player player = resolvePlayer(args[0]);
            if (mode != null) {
                String current = args[1].toLowerCase(Locale.ENGLISH);
                suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ENGLISH).startsWith(current))
                        .collect(Collectors.toList()));
            } else if (player != null) {
                String current = args[1].toLowerCase(Locale.ENGLISH);
                suggestions.addAll(modeKeys().stream().filter(s -> s.startsWith(current)).collect(Collectors.toList()));
            } else {
                String current = args[1].toLowerCase(Locale.ENGLISH);
                suggestions.addAll(modeKeys().stream().filter(s -> s.startsWith(current)).collect(Collectors.toList()));
                suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ENGLISH).startsWith(current))
                        .collect(Collectors.toList()));
            }
        }
        return suggestions;
    }

    private static Set<String> modeKeys() {
        return MODE_ALIASES.keySet();
    }

    private static Map<String, GameMode> createModeAliases() {
        Map<String, GameMode> map = new HashMap<>();
        map.put("survival", GameMode.SURVIVAL);
        map.put("s", GameMode.SURVIVAL);
        map.put("surv", GameMode.SURVIVAL);
        map.put("gms", GameMode.SURVIVAL);
        map.put("0", GameMode.SURVIVAL);

        map.put("creative", GameMode.CREATIVE);
        map.put("c", GameMode.CREATIVE);
        map.put("crea", GameMode.CREATIVE);
        map.put("gmc", GameMode.CREATIVE);
        map.put("1", GameMode.CREATIVE);

        map.put("adventure", GameMode.ADVENTURE);
        map.put("a", GameMode.ADVENTURE);
        map.put("adv", GameMode.ADVENTURE);
        map.put("gma", GameMode.ADVENTURE);
        map.put("2", GameMode.ADVENTURE);

        map.put("spectator", GameMode.SPECTATOR);
        map.put("sp", GameMode.SPECTATOR);
        map.put("spec", GameMode.SPECTATOR);
        map.put("gmsp", GameMode.SPECTATOR);
        map.put("3", GameMode.SPECTATOR);
        return map;
    }

    private static Map<String, GameMode> createLabelAliases() {
        Map<String, GameMode> map = new HashMap<>();
        map.put("gms", GameMode.SURVIVAL);
        map.put("gmc", GameMode.CREATIVE);
        map.put("gma", GameMode.ADVENTURE);
        map.put("gmsp", GameMode.SPECTATOR);
        return map;
    }
}
