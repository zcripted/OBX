package dev.zcripted.obx.command;

import dev.zcripted.obx.OBX;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Template for the most common OBX command shape: <em>player-only</em>, gated by
 * a <em>single permission node</em>, acting on the executing player.
 *
 * <p>The base handles the two guard clauses (player-only + permission) and then
 * dispatches to {@link #run(Player, String[])}. Subclasses only express the
 * actual behaviour. Commands that act on other players, take subcommands, or
 * allow console senders should extend {@link AbstractObxCommand} directly and
 * use its helper methods instead.</p>
 */
public abstract class PlayerActionCommand extends AbstractObxCommand {

    private final String permission;

    protected PlayerActionCommand(OBX plugin, String permission) {
        super(plugin);
        this.permission = permission;
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        if (!requirePermission(player, permission)) return true;
        run(player, args);
        return true;
    }

    /**
     * Execute the command for {@code player}, who is guaranteed to be online and
     * to hold the required permission node.
     */
    protected abstract void run(Player player, String[] args);
}
