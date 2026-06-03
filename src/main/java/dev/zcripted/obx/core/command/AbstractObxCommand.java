package dev.zcripted.obx.core.command;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.language.LanguageManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Common base for every OBX command executor.
 *
 * <p>Captures the boilerplate that was previously copy-pasted into each command:
 * the {@link OBX} handle, the shared {@link LanguageManager}, and the two guard
 * clauses that virtually every command repeats — "this is player-only" and
 * "you lack the permission node". Subclasses keep implementing
 * {@link CommandExecutor#onCommand} as before, but can now lean on
 * {@link #requirePlayer(CommandSender)} and {@link #requirePermission(CommandSender, String)}
 * instead of re-spelling the {@code instanceof} / {@code hasPermission} dance.</p>
 *
 * <p>This class is deliberately un-opinionated about flow so it can sit under
 * both simple self-commands and the larger multi-subcommand routers without
 * forcing either into a mould. For the very common "player-only, single
 * permission" shape, prefer {@link PlayerActionCommand}.</p>
 */
public abstract class AbstractObxCommand implements CommandExecutor {

    protected final OBX plugin;
    protected final LanguageManager languages;

    protected AbstractObxCommand(OBX plugin) {
        this.plugin = plugin;
        this.languages = plugin.getLanguageManager();
    }

    /**
     * Resolve the sender as a {@link Player}, or send the standard
     * {@code core.player-only} feedback and return {@code null}.
     *
     * <pre>{@code
     * Player player = requirePlayer(sender);
     * if (player == null) return true;
     * }</pre>
     */
    protected Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            languages.send(sender, "core.player-only");
            return null;
        }
        return (Player) sender;
    }

    /**
     * Check a permission node, sending the standard {@code core.no-permission}
     * feedback when the sender lacks it.
     *
     * @return {@code true} if the sender holds {@code node} (caller may proceed),
     *         {@code false} if the sender was rejected (caller should
     *         {@code return true}).
     */
    protected boolean requirePermission(CommandSender sender, String node) {
        if (!sender.hasPermission(node)) {
            languages.send(sender, "core.no-permission");
            return false;
        }
        return true;
    }
}
