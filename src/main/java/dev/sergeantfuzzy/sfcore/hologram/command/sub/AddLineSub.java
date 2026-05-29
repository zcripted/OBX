package dev.sergeantfuzzy.sfcore.hologram.command.sub;

import dev.sergeantfuzzy.sfcore.hologram.command.HoloContext;
import dev.sergeantfuzzy.sfcore.hologram.command.HoloSubCommand;
import dev.sergeantfuzzy.sfcore.hologram.model.Hologram;
import dev.sergeantfuzzy.sfcore.hologram.model.HologramLine;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * {@code /holo line add <id> <text…>}<br>
 * {@code /holo line add <id> icon <material> [amount]}<br>
 * {@code /holo line add <id> block <material>}<br>
 * Routed via {@link LineSub}.
 */
public final class AddLineSub implements HoloSubCommand {

    private final HoloContext ctx;

    public AddLineSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "add";
    }

    @Override
    public String permission() {
        return "sfcore.holo.edit";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§6/holo line add §f<id> §7<text…|icon <mat>|block <mat>>");
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        HologramLine line = parseLine(args, 1);
        if (line == null) {
            ctx.msg(sender, "hologram.error.invalid_material");
            return true;
        }
        hologram.addLine(line);
        ctx.persistAndRefresh(hologram);
        ctx.msg(sender, "hologram.line.added", "name", hologram.getId().value());
        return true;
    }

    /** Used by {@link InsertBeforeSub} / {@link InsertAfterSub} / {@link SetLineSub} too. */
    static HologramLine parseLine(String[] args, int from) {
        if (args.length <= from) {
            return null;
        }
        String first = args[from];
        if ("icon".equalsIgnoreCase(first)) {
            if (args.length <= from + 1) {
                return null;
            }
            Material material = Material.matchMaterial(args[from + 1]);
            if (material == null) {
                return null;
            }
            int amount = 1;
            if (args.length > from + 2) {
                try {
                    amount = Math.max(1, Integer.parseInt(args[from + 2]));
                } catch (NumberFormatException ignored) {
                }
            }
            return HologramLine.icon(new ItemStack(material, amount));
        }
        if ("block".equalsIgnoreCase(first)) {
            if (args.length <= from + 1) {
                return null;
            }
            Material material = Material.matchMaterial(args[from + 1]);
            if (material == null) {
                return null;
            }
            return HologramLine.block(material);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return HologramLine.text(builder.toString());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return DeleteSub.holoIds(ctx, args[0]);
        }
        return java.util.Collections.emptyList();
    }
}
