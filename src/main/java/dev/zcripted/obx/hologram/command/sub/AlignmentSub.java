package dev.zcripted.obx.hologram.command.sub;

import dev.zcripted.obx.hologram.command.HoloContext;
import dev.zcripted.obx.hologram.command.HoloSubCommand;
import dev.zcripted.obx.hologram.model.Hologram;
import dev.zcripted.obx.hologram.model.HologramSettings;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/** {@code /holo alignment <id> <CENTER|LEFT|RIGHT>} */
public final class AlignmentSub implements HoloSubCommand {

    private final HoloContext ctx;

    public AlignmentSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "alignment";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5/holo alignment §f<id> §7<CENTER|LEFT|RIGHT>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        try {
            hologram.getSettings().setTextAlignment(
                    HologramSettings.TextAlignment.valueOf(args[1].toUpperCase()));
        } catch (IllegalArgumentException ex) {
            ctx.msg(sender, "hologram.error.invalid_value");
            return true;
        }
        ctx.persistAndRefresh(hologram);
        ctx.msg(sender, "hologram.setting.updated", "name", hologram.getId().value());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return DeleteSub.holoIds(ctx, args[0]);
        }
        if (args.length == 2) {
            return Arrays.asList("CENTER", "LEFT", "RIGHT");
        }
        return java.util.Collections.emptyList();
    }
}
