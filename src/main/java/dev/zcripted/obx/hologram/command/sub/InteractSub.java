package dev.zcripted.obx.hologram.command.sub;

import dev.zcripted.obx.hologram.command.HoloContext;
import dev.zcripted.obx.hologram.command.HoloSubCommand;
import dev.zcripted.obx.hologram.model.Hologram;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * {@code /holo interact <id> <enable|disable|cooldown> [arg]} —
 * Configure the interaction surface for a hologram. Toggling on registers
 * the hologram for click dispatch (packet layer or raycast fallback),
 * toggling off restores it to passive display.
 */
public final class InteractSub implements HoloSubCommand {

    private final HoloContext ctx;

    public InteractSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "interact";
    }

    @Override
    public String permission() {
        return "obx.holo.interact";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§5/holo interact §f<id> §7<enable|disable|cooldown|width|height> [value]");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        String op = args[1].toLowerCase();
        switch (op) {
            case "enable":
                hologram.getSettings().setInteractionEnabled(true);
                break;
            case "disable":
                hologram.getSettings().setInteractionEnabled(false);
                break;
            case "cooldown":
                if (args.length < 3) {
                    sender.sendMessage("§5/holo interact §f<id> cooldown <ms>");
                    return true;
                }
                try {
                    hologram.getSettings().setInteractionCooldownMs(Long.parseLong(args[2]));
                } catch (NumberFormatException ex) {
                    ctx.msg(sender, "hologram.error.invalid_number");
                    return true;
                }
                break;
            case "width":
                if (args.length < 3) {
                    sender.sendMessage("§5/holo interact §f<id> width <blocks>");
                    return true;
                }
                try {
                    hologram.getSettings().setInteractionWidth(Double.parseDouble(args[2]));
                } catch (NumberFormatException ex) {
                    ctx.msg(sender, "hologram.error.invalid_number");
                    return true;
                }
                break;
            case "height":
                if (args.length < 3) {
                    sender.sendMessage("§5/holo interact §f<id> height <blocks>");
                    return true;
                }
                try {
                    hologram.getSettings().setInteractionHeight(Double.parseDouble(args[2]));
                } catch (NumberFormatException ex) {
                    ctx.msg(sender, "hologram.error.invalid_number");
                    return true;
                }
                break;
            default:
                ctx.msg(sender, "hologram.error.invalid_value");
                return true;
        }
        ctx.persistAndRefresh(hologram);
        ctx.msg(sender, "hologram.setting.updated", "name", hologram.getId().value());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return DeleteSub.holoIds(ctx, args[0]);
        }
        if (args.length == 2) {
            return Arrays.asList("enable", "disable", "cooldown", "width", "height");
        }
        return java.util.Collections.emptyList();
    }
}
