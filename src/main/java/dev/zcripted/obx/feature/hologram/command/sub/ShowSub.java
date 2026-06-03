package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /holo show <id>} — un-hides a personally-hidden hologram. */
public final class ShowSub implements HoloSubCommand {

    private final HoloContext ctx;

    public ShowSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "show";
    }

    @Override
    public String permission() {
        return "obx.holo.use";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            ctx.msg(sender, "core.player-only");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§5/holo show §f<id>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        hologram.getPersonallyHidden().remove(((Player) sender).getUniqueId());
        if (ctx.service().getRenderer() != null) {
            ctx.service().getRenderer().refreshFor(hologram, (Player) sender);
        }
        ctx.msg(sender, "hologram.visibility.shown", "name", hologram.getId().value());
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
