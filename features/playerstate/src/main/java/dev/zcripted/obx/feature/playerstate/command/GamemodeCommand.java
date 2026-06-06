package dev.zcripted.obx.feature.playerstate.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.language.LanguageRegistry;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GamemodeCommand extends AbstractObxCommand implements TabCompleter {

    private static final Map<String, GameMode> MODE_ALIASES = createModeAliases();
    private static final Map<String, GameMode> LABEL_ALIASES = createLabelAliases();


    public GamemodeCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.gamemode")) {
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
                sendModeUsage(player, null);
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
                sendModeUsage(sender, targetOnly.getName());
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
            sendModeUsage(sender, playerFirst.getName());
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
        String prevLower = previous.name().toLowerCase(Locale.ENGLISH);
        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
            Map<String, String> hover = new HashMap<>();
            hover.put("previous", previous.name());
            sendModeChange(target, "gamemode.changed-self", placeholders, mode,
                    "gamemode.revert-hover-self", hover, "/gamemode " + prevLower);
        } else {
            Map<String, String> hover = new HashMap<>();
            hover.put("previous", previous.name());
            hover.put("target", target.getName());
            sendModeChange(sender, "gamemode.changed-other", placeholders, mode,
                    "gamemode.revert-hover-other", hover, "/gamemode " + prevLower + " " + target.getName());
            languages.send(target, "gamemode.changed-target", placeholders);
            String timestamp = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a (zzz) zzzz", java.util.Locale.ENGLISH)
                    .withZone(java.time.ZoneId.of("America/New_York"))
                    .format(java.time.Instant.now());
            String prefix = languages.getPrefix(LanguageRegistry.EN);
            String consoleLine = prefix
                    + ChatColor.YELLOW + "Gamemode change by " + ChatColor.DARK_PURPLE + sender.getName()
                    + ChatColor.GRAY + " -> " + ChatColor.DARK_PURPLE + target.getName()
                    + ChatColor.YELLOW + " from " + ChatColor.DARK_PURPLE + previous.name()
                    + ChatColor.YELLOW + " to " + ChatColor.DARK_PURPLE + mode.name()
                    + ChatColor.YELLOW + " @ " + ChatColor.DARK_PURPLE + target.getWorld().getName()
                    + ChatColor.YELLOW + " at " + ChatColor.DARK_PURPLE + timestamp;
            plugin.getServer().getConsoleSender().sendMessage(consoleLine);
        }
    }

    /**
     * Renders a "set gamemode to {mode}" confirmation where the {@code {mode}} value is an
     * interactive component: hovering shows a revert tooltip and clicking runs
     * {@code revertCommand} (which switches the gamemode back to the previous value). The
     * surrounding text keeps the localized template intact by substituting a private
     * sentinel for {@code {mode}}, then splitting around it so the value can be swapped for
     * the clickable component. Console (non-player) recipients fall back to the plain
     * message, since they cannot click.
     */
    private void sendModeChange(CommandSender recipient, String key, Map<String, String> placeholders,
                                GameMode mode, String hoverKey, Map<String, String> hoverPlaceholders,
                                String revertCommand) {
        if (!(recipient instanceof Player)) {
            languages.send(recipient, key, placeholders);
            return;
        }
        final String sentinel = " OBX_MODE ";
        Map<String, String> rendered = new HashMap<>(placeholders);
        rendered.put("mode", sentinel);
        String full = colorize(languages.get(recipient, key, rendered));
        int idx = full.indexOf(sentinel);
        if (idx < 0) {
            languages.send(recipient, key, placeholders);
            return;
        }
        String before = full.substring(0, idx);
        String after = full.substring(idx + sentinel.length());
        String hover = colorize(languages.get(recipient, hoverKey, hoverPlaceholders));
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(ComponentMessenger.InteractiveMessagePart.plain(before));
        parts.add(ComponentMessenger.InteractiveMessagePart.interactive(
                ChatColor.DARK_PURPLE + mode.name(), Collections.singletonList(hover), revertCommand, true));
        parts.add(ComponentMessenger.InteractiveMessagePart.plain(after));
        ComponentMessenger.sendJoinedHoverMessages(recipient, parts);
    }

    /** Canonical mode keys, in display order, for the click-to-run usage rows. */
    private static final String[] USAGE_MODES = {"survival", "creative", "adventure", "spectator"};

    /**
     * Sends the gamemode usage: the boxed header + click-to-suggest
     * {@code /gamemode <mode>} line (rendered by the shared usage renderer), then an
     * indented list of the four modes, each a click-to-run row that switches into
     * that mode immediately. When {@code targetName} is set, the rows target that
     * player ({@code /gamemode <mode> <target>}).
     */
    private void sendModeUsage(CommandSender sender, String targetName) {
        String baseKey = targetName == null ? "gamemode.usage.self" : "gamemode.usage.target";
        Map<String, String> base = new HashMap<>();
        if (targetName != null) {
            base.put("target", targetName);
        }
        languages.send(sender, baseKey, base);

        // One organized action row of clickable mode buttons (instead of a
        // bullet list of one-per-line click-to-run rows): [Survival] [Creative]
        // [Adventure] [Spectator]. Console can't click, so it just gets the box.
        if (!(sender instanceof Player)) {
            return;
        }
        String hoverKey = targetName == null ? "gamemode.usage.row-hover" : "gamemode.usage.row-hover-other";
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(ComponentMessenger.InteractiveMessagePart.plain(colorize("  &8» ")));
        boolean first = true;
        for (String mode : USAGE_MODES) {
            if (!first) {
                parts.add(ComponentMessenger.InteractiveMessagePart.plain("  "));
            }
            first = false;
            String label = languages.get(sender, "gamemode.usage.label." + mode);
            String buttonLabel = colorize(languages.get(sender, "gamemode.usage.button", Placeholders.with("label", label)));
            Map<String, String> hoverPlaceholders = new HashMap<>();
            hoverPlaceholders.put("label", label);
            if (targetName != null) {
                hoverPlaceholders.put("target", targetName);
            }
            String hover = colorize(languages.get(sender, hoverKey, hoverPlaceholders));
            String click = "/gamemode " + mode + (targetName != null ? " " + targetName : "");
            parts.add(ComponentMessenger.InteractiveMessagePart.interactive(
                    buttonLabel, Collections.singletonList(hover), click, true));
        }
        ComponentMessenger.sendJoinedHoverMessages(sender, parts);
        sender.sendMessage("");
    }

    private static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private boolean hasModePermission(CommandSender sender, boolean self, GameMode mode) {
        String base = self ? "obx.gamemode.self" : "obx.gamemode.others";
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
