package dev.zcripted.obx.core.command;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.feature.world.service.ServerControlActions;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Locale;

/**
 * Hidden click-bridge for the server-control actions. Minecraft chat buttons can
 * only run a command (there is no "call a method" click action), so the styled
 * whitelist / join-lock / clear-entity buttons invoke this single internal
 * sub-command, which calls the exact same {@link ServerControlActions} source the
 * Admin GUI uses — no duplicated logic, no public command surface.
 *
 * <p>The bridge token is deliberately not advertised in tab-complete, {@code /obx
 * help}, or the command docs; it is gated by {@code obx.admin.menu} in
 * {@link ObxCommand}.</p>
 */
final class ObxAdminActions {

    /** Internal {@code /obx} sub-token the chat buttons target. Not advertised. */
    static final String BRIDGE_TOKEN = "x-action";

    private final ObxPlugin plugin;

    ObxAdminActions(ObxPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Dispatch a click-bridge invocation: {@code /obx x-action <action> [mode]}.
     * {@code args[0]} is the bridge token; {@code args[1]} the action.
     */
    void bridge(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return;
        }
        // Drop the bridge token so the per-action handlers read mode from args[1].
        String[] sub = Arrays.copyOfRange(args, 1, args.length);
        switch (args[1].toLowerCase(Locale.ENGLISH)) {
            case "whitelist": whitelist(sender, sub); break;
            case "joinlock": joinlock(sender, sub); break;
            case "autosave": autosave(sender, sub); break;
            case "clearentities": clearEntities(sender, sub); break;
            case "kicknonops": kickNonOps(sender); break;
            case "spectator": spectator(sender); break;
            default: break;
        }
    }

    void whitelist(CommandSender sender, String[] args) {
        switch (mode(args, "toggle")) {
            case "on": case "true": case "enable": case "enabled":
                ServerControlActions.setWhitelist(plugin, sender, true);
                break;
            case "off": case "false": case "disable": case "disabled":
                ServerControlActions.setWhitelist(plugin, sender, false);
                break;
            default:
                ServerControlActions.toggleWhitelist(plugin, sender);
        }
    }

    void joinlock(CommandSender sender, String[] args) {
        switch (mode(args, "toggle")) {
            case "on": case "true": case "enable": case "enabled":
                ServerControlActions.setJoinLock(plugin, sender, true);
                break;
            case "off": case "false": case "disable": case "disabled":
                ServerControlActions.setJoinLock(plugin, sender, false);
                break;
            default:
                ServerControlActions.toggleJoinLock(plugin, sender);
        }
    }

    void autosave(CommandSender sender, String[] args) {
        switch (mode(args, "toggle")) {
            case "on": case "true": case "enable": case "enabled":
                ServerControlActions.setAutoSave(plugin, sender, true);
                break;
            case "off": case "false": case "disable": case "disabled":
                ServerControlActions.setAutoSave(plugin, sender, false);
                break;
            default:
                ServerControlActions.toggleAutoSave(plugin, sender);
        }
    }

    void clearEntities(CommandSender sender, String[] args) {
        String which = mode(args, "all");
        ServerControlActions.ClearMode clearMode = ServerControlActions.ClearMode.ALL;
        if (which.startsWith("mob")) {
            clearMode = ServerControlActions.ClearMode.MOBS_ONLY;
        } else if (which.startsWith("item")) {
            clearMode = ServerControlActions.ClearMode.ITEMS_ONLY;
        }
        ServerControlActions.clearEntities(plugin, sender, clearMode);
    }

    void kickNonOps(CommandSender sender) {
        ServerControlActions.kickNonOps(plugin, sender);
    }

    void spectator(CommandSender sender) {
        ServerControlActions.forceSpectator(plugin, sender);
    }

    private static String mode(String[] args, String fallback) {
        return args.length >= 2 ? args[1].toLowerCase(Locale.ENGLISH) : fallback;
    }
}
