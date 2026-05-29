package dev.sergeantfuzzy.sfcore.hologram.command.sub;

import dev.sergeantfuzzy.sfcore.hologram.command.HoloContext;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloSubCommand;
import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /holo gui <id>} — opens the Phase 7 chest-GUI editor for a hologram. */
public final class GuiSub implements HoloSubCommand {

    private final HoloContext ctx;
    private final dev.sergeantfuzzy.sfcore.hologram.gui.HologramEditorMenu menu;

    public GuiSub(HoloContext ctx, dev.sergeantfuzzy.sfcore.hologram.gui.HologramEditorMenu menu) {
        this.ctx = ctx;
        this.menu = menu;
    }

    @Override
    public String name() {
        return "gui";
    }

    @Override
    public String permission() {
        return "sfcore.holo.gui";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            ctx.msg(sender, "core.player-only");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§6/holo gui §f<id>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        menu.open((Player) sender, hologram);
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
