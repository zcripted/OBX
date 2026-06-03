package dev.zcripted.obx.hologram.command.sub;

import dev.zcripted.obx.hologram.command.HoloContext;
import dev.zcripted.obx.hologram.command.HoloSubCommand;
import dev.zcripted.obx.hologram.model.Hologram;
import dev.zcripted.obx.hologram.model.HologramId;
import dev.zcripted.obx.hologram.model.HologramLine;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** {@code /holo create <id> [first line text…]} — creates a new hologram at the caller's location. */
public final class CreateSub implements HoloSubCommand {

    private final HoloContext ctx;

    public CreateSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "create";
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
        if (args.length < 1) {
            sender.sendMessage("§5/holo create §f<id> §7[first line]");
            return true;
        }
        HologramId id = HologramId.parse(args[0]);
        if (id == null) {
            ctx.msg(sender, "hologram.error.invalid_id");
            return true;
        }
        if (ctx.service().getRegistry().contains(id)) {
            ctx.msg(sender, "hologram.error.already_exists", "name", id.value());
            return true;
        }
        Player player = (Player) sender;
        Location loc = player.getLocation().clone().add(0.0, 2.0, 0.0);
        Hologram hologram = new Hologram(id, loc);
        if (args.length >= 2) {
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) {
                    builder.append(' ');
                }
                builder.append(args[i]);
            }
            hologram.addLine(HologramLine.text(builder.toString()));
        } else {
            hologram.addLine(HologramLine.text("&fNew hologram &7• &f" + id.value()));
        }
        ctx.service().getRegistry().register(hologram);
        ctx.service().getStorage().save(hologram);
        ctx.service().getBackend().spawn(hologram, Collections.singletonList(player));
        ctx.service().getRegistry().rebuildEntityIndex(hologram);
        ctx.msg(sender, "hologram.create.success", "name", id.value());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
