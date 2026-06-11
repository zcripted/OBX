package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.HoloMessages;
import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * {@code /holo aim} — resolves the hologram closest to the caller's look
 * direction and forwards the rest of the args to the matching subcommand.
 * Equivalent to CMI's {@code holo aimlines}/{@code aimgui}. Phase 2 supports
 * the common pattern {@code /holo aim <subcommand> [args]} by injecting
 * the resolved id into position 0 of the remaining args.
 *
 * <p>Resolution is geometric: project every hologram's location onto the
 * caller's view ray, keep the closest one within 12 blocks whose projection
 * is in front of the caller (positive depth).
 */
public final class AimGuiSub implements HoloSubCommand {

    private final HoloContext ctx;

    public AimGuiSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "aim";
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
        Player player = (Player) sender;
        Hologram target = findAimed(player);
        if (target == null) {
            ctx.msg(sender, "hologram.error.no_aim_target");
            return true;
        }
        sender.sendMessage(HoloMessages.inline("§7Closest hologram: §f" + target.getId().value()));
        return true;
    }

    private Hologram findAimed(Player player) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Hologram best = null;
        double bestPerp = Double.MAX_VALUE;
        for (Hologram hologram : ctx.service().getRegistry().all()) {
            Location loc = hologram.getLocation();
            if (loc.getWorld() == null || !loc.getWorld().equals(eye.getWorld())) {
                continue;
            }
            Vector delta = loc.toVector().subtract(eye.toVector());
            double along = delta.dot(dir);
            if (along < 0.0 || along > 12.0) {
                continue;
            }
            Vector projected = dir.clone().multiply(along);
            double perp = delta.clone().subtract(projected).length();
            if (perp < bestPerp && perp < 1.5) {
                bestPerp = perp;
                best = hologram;
            }
        }
        return best;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return java.util.Collections.emptyList();
    }
}