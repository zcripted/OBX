package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/** {@code /holo delete <id>} — destroys live entities, removes from disk + registry. */
public final class DeleteSub implements HoloSubCommand {

    private final HoloContext ctx;

    public DeleteSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "delete";
    }

    @Override
    public String permission() {
        return "obx.holo.delete";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§5/holo delete §f<id>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        ctx.service().getBackend().destroy(hologram);
        ctx.service().getRegistry().unregister(hologram.getId());
        ctx.service().getStorage().delete(hologram.getId());
        ctx.msg(sender, "hologram.delete.success", "name", hologram.getId().value());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return holoIds(ctx, args[0]);
        }
        return java.util.Collections.emptyList();
    }

    static List<String> holoIds(HoloContext ctx, String prefix) {
        List<String> out = new ArrayList<>();
        String lower = prefix == null ? "" : prefix.toLowerCase();
        for (Hologram hologram : ctx.service().getRegistry().all()) {
            String value = hologram.getId().value();
            if (value.startsWith(lower)) {
                out.add(value);
            }
        }
        return out;
    }
}
