package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * {@code /holo background <id> <ARGB>} — color as 8-digit hex or integer.
 * Use {@code transparent} for the default 0x40000000 background, or
 * {@code none} to fully transparent (0x00000000).
 */
public final class BackgroundSub implements HoloSubCommand {

    private final HoloContext ctx;

    public BackgroundSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "background";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5/holo background §f<id> §7<ARGB hex | int | transparent | none>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        int argb;
        String raw = args[1];
        if ("transparent".equalsIgnoreCase(raw)) {
            argb = 0x40000000;
        } else if ("none".equalsIgnoreCase(raw)) {
            argb = 0x00000000;
        } else {
            try {
                argb = raw.startsWith("0x") || raw.startsWith("0X")
                        ? (int) Long.parseLong(raw.substring(2), 16)
                        : raw.length() == 8 ? (int) Long.parseLong(raw, 16)
                        : Integer.parseInt(raw);
            } catch (NumberFormatException ex) {
                ctx.msg(sender, "hologram.error.invalid_number");
                return true;
            }
        }
        hologram.getSettings().setBackgroundColor(argb);
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
