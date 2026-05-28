package dev.sergeantfuzzy.sfcore.command.core;

import dev.sergeantfuzzy.sfcore.Main;
import dev.sergeantfuzzy.sfcore.language.LanguageManager;
import dev.sergeantfuzzy.sfcore.util.text.ComponentMessenger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /list} — shows everyone online, split into a Staff (OP) section and a
 * Players section. Styled to match the {@code /pl} ({@link
 * dev.sergeantfuzzy.sfcore.command.admin.PluginListCommand}) report: a blank
 * line, the {@code ▍ 𝗦𝗙-𝗖𝗢𝗥𝗘 › …} title bar, a divider, the indented
 * {@code  &c&lSection  &8·  &fcount} section headers each followed by an
 * indented, comma-joined {@code    names} line, then a footer legend.
 *
 * <p>Staff names render red; player names white. For players, each name is
 * clickable — it suggests {@code /msg <name> } in the chat box (with a hover
 * tooltip) so you can quickly message anyone online. Usable by all players
 * (permission {@code sfcore.list}, default true). Console output routes through
 * SF-Core's ANSI console writer exactly like {@code /pl}.
 */
public final class ListCommand implements CommandExecutor {

    private final Main plugin;

    public ListCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager languages = plugin.getLanguageManager();
        if (!sender.hasPermission("sfcore.list")) {
            languages.send(sender, "core.no-permission");
            return true;
        }

        List<String> staff = new ArrayList<>();
        List<String> players = new ArrayList<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online == null) {
                continue;
            }
            (online.isOp() ? staff : players).add(online.getName());
        }
        Collections.sort(staff, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(players, String.CASE_INSENSITIVE_ORDER);

        int total = staff.size() + players.size();
        int max = plugin.getServer().getMaxPlayers();

        Map<String, String> base = new LinkedHashMap<>();
        base.put("online", Integer.toString(total));
        base.put("max", Integer.toString(max));

        emitLines(sender, languages.list(sender, "player.list.header", base));

        Map<String, String> staffSection = new LinkedHashMap<>(base);
        staffSection.put("count", Integer.toString(staff.size()));
        emitLine(sender, languages.get(sender, "player.list.staff", staffSection));
        emitNames(sender, languages, staff, "&c");

        Map<String, String> playerSection = new LinkedHashMap<>(base);
        playerSection.put("count", Integer.toString(players.size()));
        emitLine(sender, languages.get(sender, "player.list.players", playerSection));
        emitNames(sender, languages, players, "&f");

        emitLines(sender, languages.list(sender, "player.list.footer", base));
        return true;
    }

    /**
     * Emits a section's indented name line. For players, each name is a click-to-message
     * (suggests {@code /msg <name> } in the chat box) with a hover tooltip; for the console
     * (which can't click) it falls back to a plain comma-joined line. Empty sections show
     * the {@code none} placeholder.
     */
    private void emitNames(CommandSender sender, LanguageManager languages, List<String> names, String colorCode) {
        if (names.isEmpty()) {
            emitLine(sender, languages.get(sender, "player.list.line",
                    Collections.singletonMap("names", languages.get(sender, "player.list.none"))));
            return;
        }
        if (!(sender instanceof Player)) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    builder.append("&8, ");
                }
                builder.append(colorCode).append(names.get(i));
            }
            emitLine(sender, languages.get(sender, "player.list.line", Collections.singletonMap("names", builder.toString())));
            return;
        }
        // Derive the template's indent/prefix + suffix around {names} (using a token that
        // can't appear in a chat template) so the clickable line aligns with the plain one.
        String marker = "%NAMES_MARKER%";
        String template = languages.get(sender, "player.list.line", Collections.singletonMap("names", marker));
        int idx = template.indexOf(marker);
        String pre = idx >= 0 ? template.substring(0, idx) : "";
        String post = idx >= 0 ? template.substring(idx + marker.length()) : template;
        String comma = ChatColor.translateAlternateColorCodes('&', "&8, ");
        List<ComponentMessenger.InteractiveMessagePart> parts = new ArrayList<>();
        if (!pre.isEmpty()) {
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(pre));
        }
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                parts.add(ComponentMessenger.InteractiveMessagePart.plain(comma));
            }
            String name = names.get(i);
            String text = ChatColor.translateAlternateColorCodes('&', colorCode + name);
            String hover = languages.get(sender, "player.list.message-hover", Collections.singletonMap("player", name));
            parts.add(ComponentMessenger.InteractiveMessagePart.interactive(
                    text, Collections.singletonList(hover), "/msg " + name + " ", false));
        }
        if (!post.isEmpty()) {
            parts.add(ComponentMessenger.InteractiveMessagePart.plain(post));
        }
        ComponentMessenger.sendJoinedHoverMessages(sender, parts);
    }

    private void emitLine(CommandSender sender, String line) {
        if (sender instanceof ConsoleCommandSender) {
            plugin.writeConsoleLine(ChatColor.translateAlternateColorCodes('&', line));
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    private void emitLines(CommandSender sender, List<String> lines) {
        for (String line : lines) {
            emitLine(sender, line);
        }
    }
}
