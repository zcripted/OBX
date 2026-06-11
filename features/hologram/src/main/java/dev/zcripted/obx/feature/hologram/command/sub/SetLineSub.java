package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.model.HologramLine;
import org.bukkit.command.CommandSender;

import java.util.List;

/** {@code /holo setline <id> <index> <text…|icon <mat>|block <mat>>} */
public final class SetLineSub implements HoloSubCommand {

    private final HoloContext ctx;

    public SetLineSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "set";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§5/holo line set §f<id> <index> §7<value…>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        int index = ctx.parseLineIndex(args[1]);
        if (index < 0 || index >= hologram.getLines().size()) {
            ctx.msg(sender, "hologram.error.invalid_index");
            return true;
        }
        HologramLine line = AddLineSub.parseLine(args, 2);
        if (line == null) {
            ctx.msg(sender, "hologram.error.invalid_material");
            return true;
        }
        hologram.setLine(index, line);
        ctx.persistAndRefresh(hologram);
        ctx.msg(sender, "hologram.line.updated", "name", hologram.getId().value());
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