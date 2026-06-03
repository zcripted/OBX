package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.model.HologramLine;
import org.bukkit.command.CommandSender;

import java.util.List;

/** {@code /holo insertbefore <id> <index> <value…>} */
public final class InsertBeforeSub implements HoloSubCommand {

    private final HoloContext ctx;

    public InsertBeforeSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "insertbefore";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§5/holo line insertbefore §f<id> <index> §7<value…>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        int index = ctx.parseLineIndex(args[1]);
        if (index < 0 || index > hologram.getLines().size()) {
            ctx.msg(sender, "hologram.error.invalid_index");
            return true;
        }
        HologramLine line = AddLineSub.parseLine(args, 2);
        if (line == null) {
            ctx.msg(sender, "hologram.error.invalid_material");
            return true;
        }
        hologram.insertLine(index, line);
        ctx.persistAndRefresh(hologram);
        ctx.msg(sender, "hologram.line.inserted", "name", hologram.getId().value());
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
