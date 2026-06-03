package dev.zcripted.obx.hologram.command.sub;

import dev.zcripted.obx.hologram.command.HoloContext;
import dev.zcripted.obx.hologram.command.HoloSubCommand;
import dev.zcripted.obx.hologram.model.Hologram;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * {@code /holo view <id> <permission <node>|hide-behind-walls <true|false>|reset>} —
 * Configures the per-player visibility filters introduced in Phase 6.
 */
public final class ViewSub implements HoloSubCommand {

    private final HoloContext ctx;

    public ViewSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "view";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5/holo view §f<id> §7<permission <node>|hide-behind-walls <true|false>|reset>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        String op = args[1].toLowerCase();
        switch (op) {
            case "permission":
                if (args.length < 3) {
                    hologram.getSettings().setViewPermission(null);
                } else {
                    hologram.getSettings().setViewPermission(args[2]);
                }
                break;
            case "hide-behind-walls":
                hologram.getSettings().setHideBehindWalls(args.length >= 3 && Boolean.parseBoolean(args[2]));
                break;
            case "reset":
                hologram.getSettings().setViewPermission(null);
                hologram.getSettings().setHideBehindWalls(false);
                break;
            default:
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
            return Arrays.asList("permission", "hide-behind-walls", "reset");
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("hide-behind-walls")) {
            return Arrays.asList("true", "false");
        }
        return java.util.Collections.emptyList();
    }
}
