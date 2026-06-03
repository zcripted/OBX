package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /holo movehere <id>} — move the hologram to the caller's location. */
public final class MoveHereSub implements HoloSubCommand {

    private final HoloContext ctx;

    public MoveHereSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "movehere";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            ctx.msg(sender, "core.player-only");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§5/holo movehere §f<id>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        hologram.setLocation(((Player) sender).getLocation().clone().add(0.0, 2.0, 0.0));
        ctx.persistAndRefresh(hologram);
        ctx.msg(sender, "hologram.move.success", "name", hologram.getId().value());
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
