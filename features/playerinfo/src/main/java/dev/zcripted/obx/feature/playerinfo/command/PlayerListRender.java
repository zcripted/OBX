package dev.zcripted.obx.feature.playerinfo.command;

import dev.zcripted.obx.core.language.LanguageManager;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.ComponentMessenger.InteractiveMessagePart;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Shared rendering for the player-list family ({@code /list}, {@code /stafflist}): turns a
 * section's players into a clean, click-to-message names row, so both commands share one
 * implementation of the interactive-name layout.
 */
final class PlayerListRender {

    /** Permission node that marks a player as staff in addition to op status. */
    static final String STAFF_PERMISSION = "obx.staff";

    private PlayerListRender() {
    }

    /**
     * Whether {@code player} counts as staff for the player-list family: op <em>or</em>
     * holding {@link #STAFF_PERMISSION}, so staff can be granted by op, by permission, or
     * both.
     */
    static boolean isStaff(Player player) {
        return player != null && (player.isOp() || player.hasPermission(STAFF_PERMISSION));
    }

    /** A listed player: the raw account name (click target) + the colored display string. */
    static final class Entry {
        final String rawName;
        final String display;

        Entry(String rawName, String display) {
            this.rawName = rawName;
            this.display = display;
        }
    }

    /** Sorts entries by the visible name, ignoring the &-color prefix. */
    static Comparator<Entry> byVisibleName() {
        return (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', a.display)),
                ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', b.display)));
    }

    /**
     * Renders one section's names. For a player recipient each name is an interactive
     * component: hovering shows a hint and clicking suggests {@code /msg <name> } (trailing
     * space ready). Console (no click) falls back to the plain joined {@code info.list.names}
     * line. An empty section renders {@code emptyKey}.
     */
    static void sendNames(LanguageManager languages, CommandSender sender, List<Entry> entries, String emptyKey) {
        if (entries.isEmpty()) {
            languages.send(sender, emptyKey, Collections.<String, String>emptyMap());
            return;
        }
        if (!(sender instanceof Player)) {
            List<String> displays = new ArrayList<>(entries.size());
            for (Entry entry : entries) {
                displays.add(entry.display);
            }
            languages.send(sender, "info.list.names", Collections.singletonMap("names", String.join("&7, ", displays)));
            return;
        }
        String separator = colorize("&7, ");
        List<InteractiveMessagePart> parts = new ArrayList<>();
        parts.add(InteractiveMessagePart.plain(colorize("    "))); // indent matches info.list.names
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                parts.add(InteractiveMessagePart.plain(separator));
            }
            Entry entry = entries.get(i);
            List<String> hover = Collections.singletonList(
                    languages.get(sender, "info.list.name-hover", Collections.singletonMap("player", entry.rawName)));
            // runCommand = false → SUGGEST_COMMAND (prefills the chat box, trailing space ready).
            parts.add(InteractiveMessagePart.interactive(colorize(entry.display), hover, "/msg " + entry.rawName + " ", false));
        }
        ComponentMessenger.sendJoinedHoverMessages(sender, parts);
    }

    static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
