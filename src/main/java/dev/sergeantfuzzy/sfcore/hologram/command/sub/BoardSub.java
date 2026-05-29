package dev.sergeantfuzzy.sfcore.hologram.command.sub;

import dev.sergeantfuzzy.sfcore.hologram.command.HoloContext;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloSubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * {@code /holo board <id> <enable|material|size|offset|direction> [args]} —
 * Configures the optional block-display slab backing behind the text. Phase 2
 * registers the subcommand so the tree is complete; the board renderer itself
 * is a Phase 5+ refinement (see plan §4.A "Full transformation support").
 * Until then this subcommand reports the feature as upcoming so operators
 * know to wait rather than thinking it's broken.
 */
public final class BoardSub implements HoloSubCommand {

    private final HoloContext ctx;

    public BoardSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "board";
    }

    @Override
    public String permission() {
        return "sfcore.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("§e§lSF-Core Holograms §8› §7Board backing is wired in Phase 5 (transforms / boards). " +
                "The model already persists board state — use this command after Phase 5 lands to configure it.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return DeleteSub.holoIds(ctx, args[0]);
        }
        return java.util.Collections.emptyList();
    }
}
