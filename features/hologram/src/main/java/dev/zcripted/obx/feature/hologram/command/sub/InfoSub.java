package dev.zcripted.obx.feature.hologram.command.sub;

import dev.zcripted.obx.feature.hologram.HoloMessages;
import dev.zcripted.obx.feature.hologram.command.HoloContext;
import dev.zcripted.obx.feature.hologram.command.HoloSubCommand;
import dev.zcripted.obx.feature.hologram.model.Hologram;
import dev.zcripted.obx.feature.hologram.model.HologramLine;
import dev.zcripted.obx.feature.hologram.model.HologramSettings;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.List;

/** {@code /holo info <id>} — detailed settings + line summary. */
public final class InfoSub implements HoloSubCommand {

    private final HoloContext ctx;

    public InfoSub(HoloContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public String permission() {
        return "obx.holo.info";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(HoloMessages.header("Module overview"));
            sender.sendMessage(HoloMessages.DIVIDER);
            sender.sendMessage("§7Backend: §f" + ctx.service().getBackend().describe());
            sender.sendMessage("§7Loaded holograms: §f" + ctx.service().getRegistry().size());
            sender.sendMessage("§7Packet layer: §f"
                    + dev.zcripted.obx.feature.hologram.packet.PacketAvailability.describe());
            return true;
        }
        Hologram hologram = ctx.resolveHologram(sender, args[0]);
        if (hologram == null) {
            return true;
        }
        Location loc = hologram.getLocation();
        HologramSettings s = hologram.getSettings();
        sender.sendMessage(HoloMessages.header(hologram.getId().value()));
        sender.sendMessage(HoloMessages.DIVIDER);
        sender.sendMessage(String.format("§7Location: §f%s §8• §f%.2f, %.2f, %.2f §8(yaw §f%.0f§8)",
                loc.getWorld() == null ? "?" : loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw()));
        sender.sendMessage(String.format("§7Billboard: §f%s §8• §7Scale: §f%.2f §8• §7Double-sided: §f%s",
                s.getBillboard().name(), s.getScale(), s.isDoubleSided()));
        sender.sendMessage(String.format("§7Range: show §f%.0f§7, update §f%.0f§7 • §7See-through: §f%s §8• §7Shadow: §f%s",
                s.getShowRange(), s.getUpdateRange(), s.isSeeThrough(), s.hasShadow()));
        sender.sendMessage(String.format("§7Alignment: §f%s §8• §7Text opacity: §f%d §8• §7Line width: §f%d",
                s.getTextAlignment().name(), s.getTextOpacity(), s.getLineWidth()));
        sender.sendMessage(String.format("§7Board: %s §8· §f%s §8· §f%.2f §7× §f%s §8· §7back §f%.2f",
                s.isBoardEnabled() ? "§aenabled" : "§cdisabled",
                s.getBoardMaterial(),
                s.getBoardWidth(),
                s.getBoardHeight() <= 0.0 ? "auto" : String.format("%.2f", s.getBoardHeight()),
                s.getBoardOffsetBack()));
        sender.sendMessage("§7Lines:");
        List<HologramLine> lines = hologram.getLines();
        for (int i = 0; i < lines.size(); i++) {
            HologramLine line = lines.get(i);
            String preview;
            switch (line.getType()) {
                case TEXT:
                    preview = "§f\"" + ((HologramLine.TextLine) line).getTemplate() + "§f\"";
                    break;
                case ICON: {
                    HologramLine.IconLine icon = (HologramLine.IconLine) line;
                    preview = "§7icon §f" + (icon.getStack() == null ? "?" : icon.getStack().getType().name());
                    break;
                }
                case BLOCK:
                    preview = "§7block §f" + ((HologramLine.BlockLine) line).getMaterial().name();
                    break;
                default:
                    preview = "§7?";
                    break;
            }
            sender.sendMessage(String.format("§8  %d) §7%s §8› %s", i + 1, line.getType().name(), preview));
        }
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