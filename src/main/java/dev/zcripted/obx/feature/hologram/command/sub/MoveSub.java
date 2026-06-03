package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /holo move <id> <x> <y> <z> [yaw] [world]} — move to explicit coords. */
public final class MoveSub implements HoloSubCommand {

    private final HoloContext ctx;

    public MoveSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "move";
    }

    @Override
    public String permission() {
        return "obx.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§5/holo move §f<id> <x> <y> <z> §7[yaw] [world]");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        double x, y, z;
        try {
            x = Double.parseDouble(args[1]);
            y = Double.parseDouble(args[2]);
            z = Double.parseDouble(args[3]);
        } catch (NumberFormatException ex) {
            ctx.msg(sender, "hologram.error.invalid_number");
            return true;
        }
        float yaw = hologram.getLocation().getYaw();
        if (args.length >= 5) {
            try {
                yaw = Float.parseFloat(args[4]);
            } catch (NumberFormatException ignored) {
            }
        }
        World world = hologram.getLocation().getWorld();
        if (args.length >= 6) {
            World w = ctx.plugin().getServer().getWorld(args[5]);
            if (w != null) {
                world = w;
            }
        } else if (world == null && sender instanceof Player) {
            world = ((Player) sender).getWorld();
        }
        if (world == null) {
            ctx.msg(sender, "hologram.error.world_unknown");
            return true;
        }
        hologram.setLocation(new Location(world, x, y, z, yaw, 0.0f));
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
