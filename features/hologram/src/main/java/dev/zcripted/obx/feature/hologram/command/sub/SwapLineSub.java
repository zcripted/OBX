package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.command.CommandSender;

import java.util.List;

/** {@code /holo line swap <id> <a> <b>} — routed via {@link LineSub}. */
public final class SwapLineSub implements HoloSubCommand {

    private final HoloContext ctx;

    public SwapLineSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "swap";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§5/holo line swap §f<id> <a> <b>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        int a = ctx.parseLineIndex(args[1]);
        int b = ctx.parseLineIndex(args[2]);
        if (!hologram.swapLines(a, b)) {
            ctx.msg(sender, "hologram.error.invalid_index");
            return true;
        }
        ctx.persistAndRefresh(hologram);
        ctx.msg(sender, "hologram.line.swapped", "name", hologram.getId().value());
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