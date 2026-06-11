package dev.zcripted.obx.feature.hologram.command;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Contract for one {@code /holo <subcommand>}. Mirrors the per-subcommand
 * dispatcher patterns already used by OBX's larger commands
 * (see {@code command/teleportation/}, {@code command/moderation/}).
 *
 * <p>Every subcommand owns its own permission node and language keys; the
 * root {@link HologramCommand} only routes based on the first arg.
 */
public interface HoloSubCommand {

    /** Subcommand label (the literal first arg). */
    String name();

    /** Permission required, or {@code null} for none. */
    String permission();

    /** Run the subcommand. {@code args} excludes the subcommand label itself. */
    boolean execute(CommandSender sender, String[] args);

    /** Optional tab-completion. Default returns no suggestions. */
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}