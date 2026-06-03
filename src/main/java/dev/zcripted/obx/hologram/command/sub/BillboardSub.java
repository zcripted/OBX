package dev.zcripted.obx.hologram.command.sub;

import dev.zcripted.obx.hologram.command.HoloContext;
import dev.zcripted.obx.hologram.command.HoloSubCommand;
import dev.zcripted.obx.hologram.model.Hologram;
import dev.zcripted.obx.hologram.model.HologramSettings;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/** {@code /holo billboard <id> <FIXED|VERTICAL|HORIZONTAL|CENTER>} */
public final class BillboardSub implements HoloSubCommand {

    private final HoloContext ctx;

    public BillboardSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "billboard";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5/holo billboard §f<id> §7<FIXED|VERTICAL|HORIZONTAL|CENTER>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        try {
            hologram.getSettings().setBillboard(
                    HologramSettings.Billboard.valueOf(args[1].toUpperCase()));
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
            return Arrays.asList("FIXED", "VERTICAL", "HORIZONTAL", "CENTER");
        }
        return java.util.Collections.emptyList();
    }
}
