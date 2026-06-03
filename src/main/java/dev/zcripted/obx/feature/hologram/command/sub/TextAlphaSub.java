package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.command.CommandSender;

import java.util.List;

/** {@code /holo textalpha <id> <0-255>} */
public final class TextAlphaSub implements HoloSubCommand {

    private final HoloContext ctx;

    public TextAlphaSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "textalpha";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5/holo textalpha §f<id> <0-255>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        try {
            hologram.getSettings().setTextOpacity(Integer.parseInt(args[1]));
        } catch (NumberFormatException ex) {
            ctx.msg(sender, "hologram.error.invalid_number");
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
        return java.util.Collections.emptyList();
    }
}
