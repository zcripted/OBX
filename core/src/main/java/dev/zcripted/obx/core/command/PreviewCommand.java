package dev.zcripted.obx.core.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.message.AdventureMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Console color preview tool. Renders a MiniMessage / legacy-{@code &} / hex / gradient string with
 * full 24-bit color and text attributes (bold / italic / underline / strikethrough) into ANSI
 * truecolor for the server console — hex colors can't be expressed with legacy {@code §} codes, so
 * the rendered ANSI is written straight to the terminal. In-game it renders the string as a normal
 * chat message instead. With no arguments it prints a set of worked examples.
 *
 * <p>Examples:
 * <pre>
 *   /preview &lt;gradient:#ff0000:#00ff00&gt;Rules:&lt;/gradient&gt; &lt;yellow&gt;1. No Griefing
 *   /preview &amp;a&amp;lGreen Bold &amp;c&amp;nRed Underline
 *   /preview &lt;#9d4edd&gt;&lt;bold&gt;Purple bold hex&lt;/bold&gt;&lt;/#9d4edd&gt;
 * </pre>
 */
public final class PreviewCommand extends AbstractObxCommand implements TabCompleter {

    public PreviewCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.preview")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length == 0) {
            printExamples(sender);
            return true;
        }
        emit(sender, String.join(" ", args));
        return true;
    }

    /** Renders one line: ANSI truecolor to the console, or a normal Adventure message in-game. */
    private void emit(CommandSender sender, String raw) {
        if (sender instanceof Player) {
            AdventureMessageUtil.send((Player) sender, raw, Collections.<String, String>emptyMap());
        } else {
            // writeConsoleLine writes 24-bit ANSI straight to the terminal (the same path the
            // startup banner uses), so hex/gradient colors render even though § codes can't carry them.
            plugin.writeConsoleLine(AdventureMessageUtil.renderAnsi(raw, Collections.<String, String>emptyMap()));
        }
    }

    private void printExamples(CommandSender sender) {
        emit(sender, "<gold><bold>OBX Preview</bold></gold> <gray>— color & format examples</gray>");
        emit(sender, "<dark_gray>──────────────────────────────</dark_gray>");
        emit(sender, "&7Legacy &8» &a&lGreen Bold &r&c&nRed Underline &r&9Blue &eYellow");
        emit(sender, "<gray>Hex</gray> <dark_gray>»</dark_gray> <#ff0000>FF0000</#ff0000> <#00ff00>00FF00</#00ff00> <#9d4edd>9D4EDD</#9d4edd>");
        emit(sender, "<gray>Named</gray> <dark_gray>»</dark_gray> <red>red</red> <gold>gold</gold> <aqua>aqua</aqua> <light_purple>pink</light_purple>");
        emit(sender, "<gray>Attributes</gray> <dark_gray>»</dark_gray> <bold>bold</bold> <italic>italic</italic> <underlined>underline</underlined> <strikethrough>strike</strikethrough>");
        emit(sender, "<gray>Gradient</gray> <dark_gray>»</dark_gray> <gradient:#ff0000:#00ff00:#0000ff>Rainbow gradient text</gradient>");
        emit(sender, "<dark_gray>──────────────────────────────</dark_gray>");
        emit(sender, "<gray>Type</gray> <yellow>/preview</yellow> <gray>followed by your colored text.</gray>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}