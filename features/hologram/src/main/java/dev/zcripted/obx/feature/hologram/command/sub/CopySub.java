package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.model.HologramId;
import dev.zcripted.obx.feature.hologram.model.HologramLine;
import dev.zcripted.obx.feature.hologram.model.HologramSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** {@code /holo copy <src> <dest>} — duplicates an existing hologram at the caller's feet. */
public final class CopySub implements HoloSubCommand {

    private final HoloContext ctx;

    public CopySub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "copy";
    }

    @Override
    public String permission() {
        return "obx.holo.create";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            ctx.msg(sender, "core.player-only");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§5/holo copy §f<src> <dest>");
            return true;
        }
        Hologram source = ctx.resolveHologram(sender, args[0]);
        if (source == null) {
            return true;
        }
        HologramId newId = HologramId.parse(args[1]);
        if (newId == null) {
            ctx.msg(sender, "hologram.error.invalid_id");
            return true;
        }
        if (ctx.service().getRegistry().contains(newId)) {
            ctx.msg(sender, "hologram.error.already_exists", "name", newId.value());
            return true;
        }
        Hologram copy = new Hologram(newId, ((Player) sender).getLocation().clone().add(0.0, 2.0, 0.0));
        HologramSettings src = source.getSettings();
        HologramSettings dst = copy.getSettings();
        dst.setBillboard(src.getBillboard());
        dst.setTextAlignment(src.getTextAlignment());
        dst.setScale(src.getScale());
        dst.setShowRange(src.getShowRange());
        dst.setUpdateRange(src.getUpdateRange());
        dst.setDoubleSided(src.isDoubleSided());
        dst.setShadow(src.hasShadow());
        dst.setSeeThrough(src.isSeeThrough());
        dst.setBackgroundColor(src.getBackgroundColor());
        dst.setTextOpacity(src.getTextOpacity());
        dst.setLineWidth(src.getLineWidth());
        dst.setInteractionEnabled(src.isInteractionEnabled());
        dst.setInteractionWidth(src.getInteractionWidth());
        dst.setInteractionHeight(src.getInteractionHeight());
        dst.setInteractionCooldownMs(src.getInteractionCooldownMs());
        dst.setViewPermission(src.getViewPermission());
        dst.setHideBehindWalls(src.isHideBehindWalls());
        for (HologramLine line : source.getLines()) {
            copy.addLine(line);
        }
        ctx.service().getRegistry().register(copy);
        ctx.service().getStorage().save(copy);
        ctx.service().getBackend().spawn(copy, Collections.singletonList((Player) sender));
        ctx.service().getRegistry().rebuildEntityIndex(copy);
        ctx.msg(sender, "hologram.copy.success", "name", newId.value());
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