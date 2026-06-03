package dev.zcripted.obx.hologram.command.sub;

import dev.zcripted.obx.hologram.command.HoloContext;
import dev.zcripted.obx.hologram.command.HoloSubCommand;
import dev.zcripted.obx.hologram.model.Hologram;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /holo tp <id>} — teleport to a hologram. */
public final class TpSub implements HoloSubCommand {

    private final HoloContext ctx;

    public TpSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "tp";
    }

    @Override
    public String permission() {
        return "obx.holo.tp";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            ctx.msg(sender, "core.player-only");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§5/holo tp §f<id>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        ((Player) sender).teleport(hologram.getLocation());
        ctx.msg(sender, "hologram.tp.success", "name", hologram.getId().value());
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
