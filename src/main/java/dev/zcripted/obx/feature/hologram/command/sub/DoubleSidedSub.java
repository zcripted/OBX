package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/** {@code /holo doublesided <id> <true|false>} */
public final class DoubleSidedSub implements HoloSubCommand {

    private final HoloContext ctx;

    public DoubleSidedSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "doublesided";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5/holo doublesided §f<id> §7<true|false>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        hologram.getSettings().setDoubleSided(Boolean.parseBoolean(args[1]));
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
            return Arrays.asList("true", "false");
        }
        return java.util.Collections.emptyList();
    }
}
