package dev.zcripted.obx.hologram.command.sub;

import dev.zcripted.obx.hologram.command.HoloContext;
import dev.zcripted.obx.hologram.command.HoloSubCommand;
import dev.zcripted.obx.hologram.model.Hologram;
import dev.zcripted.obx.hologram.model.HologramLine;
import dev.zcripted.obx.hologram.text.HologramTextResolver;
import dev.zcripted.obx.hologram.text.PageState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /holo page <holo> [player] <next|prev|N>} —
 * advances or sets the page cursor for a viewer. When {@code [player]} is
 * omitted the caller is the target. Pages are delimited by {@code !nextpage!}
 * inside a {@code TextLine} template (plan §D / Phase 3).
 */
public final class PageSub implements HoloSubCommand {

    private final HoloContext ctx;

    public PageSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "page";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5/holo page §f<holo> §7[player] §f<next|prev|N>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        Player target;
        String action;
        if (args.length >= 3) {
            target = ctx.plugin().getServer().getPlayerExact(args[1]);
            if (target == null) {
                ctx.msg(sender, "hologram.error.player_not_found", "name", args[1]);
                return true;
            }
            action = args[2];
        } else {
            if (!(sender instanceof Player)) {
                ctx.msg(sender, "core.player-only");
                return true;
            }
            target = (Player) sender;
            action = args[1];
        }
        int totalPages = computeMaxPages(hologram);
        int newPage;
        if ("next".equalsIgnoreCase(action)) {
            newPage = PageState.next(target.getUniqueId(), hologram.getId(), totalPages);
        } else if ("prev".equalsIgnoreCase(action) || "previous".equalsIgnoreCase(action)) {
            newPage = PageState.prev(target.getUniqueId(), hologram.getId(), totalPages);
        } else {
            int requested;
            try {
                requested = Integer.parseInt(action) - 1;
            } catch (NumberFormatException ex) {
                ctx.msg(sender, "hologram.error.invalid_number");
                return true;
            }
            if (requested < 0 || requested >= totalPages) {
                ctx.msg(sender, "hologram.page.unknown");
                return true;
            }
            PageState.set(target.getUniqueId(), hologram.getId(), requested);
            newPage = requested;
        }
        hologram.markDirty();
        if (ctx.service().getRenderer() != null) {
            ctx.service().getRenderer().refreshFor(hologram, target);
        }
        Map<String, String> reps = new HashMap<>();
        reps.put("page", String.valueOf(newPage + 1));
        reps.put("total", String.valueOf(totalPages));
        ctx.plugin().getLanguageManager().send(sender, "hologram.page.changed", reps);
        return true;
    }

    private int computeMaxPages(Hologram hologram) {
        int max = 1;
        for (HologramLine line : hologram.getLines()) {
            if (line.getType() == HologramLine.Type.TEXT) {
                int n = HologramTextResolver.pageCount(((HologramLine.TextLine) line).getTemplate());
                if (n > max) {
                    max = n;
                }
            }
        }
        return max;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return DeleteSub.holoIds(ctx, args[0]);
        }
        return java.util.Collections.emptyList();
    }
}
