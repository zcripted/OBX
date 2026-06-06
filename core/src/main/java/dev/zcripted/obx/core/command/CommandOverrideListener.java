package dev.zcripted.obx.core.command;

import dev.zcripted.obx.core.ObxPlugin;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Forces OBX's {@code /heal} and {@code /god} executors to run regardless of which plugin
 * loaded first or which namespaced label the client typed. Catching the command at
 * {@link PlayerCommandPreprocessEvent} with {@link EventPriority#LOWEST} short-circuits the
 * normal command-map dispatch, so other plugins (or vanilla aliases like
 * {@code minecraft:heal}) never see the message.
 */
public final class CommandOverrideListener implements Listener {

    /**
     * Maps every accepted alias (always lowercase) to the ObxPlugin plugin command name that
     * should service it. Both the bare command and any namespaced variant route to the
     * canonical {@code heal} / {@code god} executor.
     */
    private static final Map<String, String> ALIAS_TO_COMMAND;

    static {
        Map<String, String> map = new HashMap<>();
        for (String alias : Arrays.asList("heal", "bukkit:heal", "minecraft:heal", "essentials:heal", "obx:heal")) {
            map.put(alias, "heal");
        }
        // NOTE: "gmode" is intentionally NOT a /god alias — it belongs to /gamemode
        // ("game mode"). Routing it here would shadow /gamemode at LOWEST priority.
        for (String alias : Arrays.asList("god", "godmode", "invincible", "immortal",
                "bukkit:god", "minecraft:god", "essentials:god", "obx:god")) {
            map.put(alias, "god");
        }
        for (String alias : Arrays.asList("tps", "bukkit:tps", "minecraft:tps", "paper:tps",
                "purpur:tps", "spigot:tps", "essentials:tps", "obx:tps")) {
            map.put(alias, "tps");
        }
        for (String alias : Arrays.asList("pl", "plugins", "bukkit:pl", "bukkit:plugins",
                "minecraft:pl", "minecraft:plugins", "paper:pl", "paper:plugins",
                "purpur:pl", "purpur:plugins", "spigot:pl", "spigot:plugins",
                "obx:pl", "obx:plugins")) {
            map.put(alias, "pl");
        }
        // Replace the vanilla /clear with OBX's /clearinv. The remaining args
        // ([player] [item] [maxCount]) are passed straight through, so the targeted
        // item/count forms keep working through the OBX executor.
        for (String alias : Arrays.asList("clear", "bukkit:clear", "minecraft:clear",
                "essentials:clear", "paper:clear", "purpur:clear", "spigot:clear", "obx:clear")) {
            map.put(alias, "clearinv");
        }
        ALIAS_TO_COMMAND = Collections.unmodifiableMap(map);
    }

    private static final Set<String> KNOWN_NAMESPACES = new HashSet<>(Arrays.asList(
            "bukkit", "minecraft", "essentials", "obx", "paper", "purpur", "spigot"));

    /** Routed commands whose executor self-checks finer-grained permissions (e.g. {@code .others}). */
    private static final Set<String> SELF_PERMISSION_CHECKED = Collections.singleton("clearinv");

    private final ObxPlugin plugin;

    public CommandOverrideListener(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.length() < 2 || message.charAt(0) != '/') {
            return;
        }
        int spaceIdx = message.indexOf(' ');
        String label = (spaceIdx == -1 ? message.substring(1) : message.substring(1, spaceIdx))
                .toLowerCase(Locale.ENGLISH);

        String target = ALIAS_TO_COMMAND.get(label);
        if (target == null) {
            target = ALIAS_TO_COMMAND.get(stripNamespace(label));
            if (target == null) {
                return;
            }
        }

        PluginCommand pluginCommand = plugin.getCommand(target);
        if (pluginCommand == null || pluginCommand.getExecutor() == null) {
            return;
        }

        Player player = event.getPlayer();
        // Respect Bukkit's permission gate before re-dispatching (the routed executors also
        // self-check, but this keeps the override path consistent with normal dispatch and
        // doesn't silently run a command the sender lacks permission for).
        //
        // Exception: commands whose executor has a finer-grained sub-permission (e.g. clearinv's
        // obx.clearinv.others lets a staffer clear OTHERS without obx.clearinv for self). For those
        // we defer entirely to the executor's own checks, or this gate would wrongly block a holder
        // of only the sub-permission.
        if (!SELF_PERMISSION_CHECKED.contains(target) && !pluginCommand.testPermissionSilent(player)) {
            event.setCancelled(true);
            plugin.getLanguageManager().send(player, "core.no-permission");
            return;
        }

        event.setCancelled(true);
        String[] args = (spaceIdx == -1 || spaceIdx + 1 >= message.length())
                ? new String[0]
                : message.substring(spaceIdx + 1).trim().split("\\s+");
        if (args.length == 1 && args[0].isEmpty()) {
            args = new String[0];
        }
        try {
            pluginCommand.getExecutor().onCommand(player, pluginCommand, target, args);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to execute overridden command /" + target + ": " + throwable.getMessage());
        }
    }

    private static String stripNamespace(String label) {
        int colon = label.indexOf(':');
        if (colon <= 0 || colon == label.length() - 1) {
            return label;
        }
        String namespace = label.substring(0, colon);
        if (!KNOWN_NAMESPACES.contains(namespace)) {
            return label;
        }
        return label.substring(colon + 1);
    }
}
