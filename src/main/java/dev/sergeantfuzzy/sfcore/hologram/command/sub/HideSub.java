package dev.sergeantfuzzy.sfcore.hologram.command.sub;

import dev.sergeantfuzzy.sfcore.hologram.command.HoloContext;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloSubCommand;
import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /holo hide <id>} — personally hide for the caller. */
public final class HideSub implements HoloSubCommand {

    private final HoloContext ctx;

    public HideSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "hide";
    }

    @Override
    public String permission() {
        return "sfcore.holo.use";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            ctx.msg(sender, "core.player-only");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§6/holo hide §f<id>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        hologram.getPersonallyHidden().add(((Player) sender).getUniqueId());
        ctx.service().getBackend().updateVisibility(hologram, (Player) sender, false);
        ctx.msg(sender, "hologram.visibility.hidden", "name", hologram.getId().value());
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
